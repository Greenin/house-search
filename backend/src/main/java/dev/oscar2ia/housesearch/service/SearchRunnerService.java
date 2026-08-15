package dev.oscar2ia.housesearch.service;

import dev.oscar2ia.housesearch.dto.SearchStatusResponse;
import dev.oscar2ia.housesearch.model.SearchExecution;
import dev.oscar2ia.housesearch.model.enums.EstadoBusqueda;
import dev.oscar2ia.housesearch.repository.SearchExecutionRepository;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lanza el scraper Node.js como proceso hijo en un hilo daemon (no bloquea el
 * request HTTP), con un lock de instancia para evitar solapar ejecuciones y un
 * limite de una ejecucion completa al dia persistido en BD (sobrevive a
 * reinicios del backend). No hay variante "sin Playwright": todas las fuentes
 * de casas lo requieren por igual.
 */
@Service
public class SearchRunnerService {

	private static final Logger log = LoggerFactory.getLogger(SearchRunnerService.class);

	private static final int LINEAS_MENSAJE = 20;
	private static final int TIMEOUT_MINUTOS = 30;

	private final Object lock = new Object();

	private final SearchExecutionRepository searchExecutionRepository;
	private final String workingDir;
	private final String command;

	public SearchRunnerService(
			SearchExecutionRepository searchExecutionRepository,
			@Value("${app.scraper.working-dir:../scraper}") String workingDir,
			@Value("${app.scraper.command:node index.js}") String command) {
		this.searchExecutionRepository = searchExecutionRepository;
		this.workingDir = workingDir;
		this.command = command;
	}

	public ResultadoInicioBusqueda iniciarBusqueda() {
		synchronized (lock) {
			SearchExecution ejecucion = obtenerOCrearEjecucion();

			if (ejecucion.getEstado() == EstadoBusqueda.EN_EJECUCION) {
				return ResultadoInicioBusqueda.YA_EN_EJECUCION;
			}
			if (ejecucion.getFechaInicio() != null
					&& ejecucion.getFechaInicio().toLocalDate().isEqual(LocalDate.now())) {
				return ResultadoInicioBusqueda.LIMITE_DIARIO_ALCANZADO;
			}

			ejecucion.setEstado(EstadoBusqueda.EN_EJECUCION);
			ejecucion.setFechaInicio(LocalDateTime.now());
			ejecucion.setFechaFin(null);
			ejecucion.setCodigoSalida(null);
			ejecucion.setMensaje(null);
			searchExecutionRepository.save(ejecucion);
		}

		Thread hilo = new Thread(this::ejecutarProceso, "scraper-runner");
		hilo.setDaemon(true);
		hilo.start();

		return ResultadoInicioBusqueda.INICIADO;
	}

	@Transactional(readOnly = true)
	public SearchStatusResponse estadoActual() {
		return SearchStatusResponse.from(obtenerOCrearEjecucionSoloLectura());
	}

	private void ejecutarProceso() {
		Deque<String> ultimasLineas = new ArrayDeque<>();
		try {
			ProcessBuilder pb = new ProcessBuilder(command.trim().split("\\s+"));
			pb.directory(new File(workingDir));
			pb.redirectErrorStream(true);
			Process proceso = pb.start();

			Thread lector = new Thread(() -> leerSalida(proceso, ultimasLineas), "scraper-runner-lector");
			lector.setDaemon(true);
			lector.start();

			boolean terminoATiempo = proceso.waitFor(TIMEOUT_MINUTOS, TimeUnit.MINUTES);
			String mensaje;
			synchronized (ultimasLineas) {
				mensaje = String.join("\n", ultimasLineas);
			}

			if (!terminoATiempo) {
				proceso.destroyForcibly();
				actualizarResultado(
						EstadoBusqueda.FALLIDA, null, "Timeout tras " + TIMEOUT_MINUTOS + " minutos.\n" + mensaje);
				return;
			}

			int codigo = proceso.exitValue();
			EstadoBusqueda estadoFinal = codigo == 0 ? EstadoBusqueda.COMPLETADA : EstadoBusqueda.FALLIDA;
			actualizarResultado(estadoFinal, codigo, mensaje);
		} catch (Throwable t) {
			// Red de seguridad final: un fallo inesperado no debe dejar la fila
			// colgada en EN_EJECUCION para siempre.
			log.error("Fallo inesperado ejecutando el scraper", t);
			actualizarResultado(EstadoBusqueda.FALLIDA, null, "Error inesperado: " + t.getMessage());
		}
	}

	private void leerSalida(Process proceso, Deque<String> ultimasLineas) {
		try (BufferedReader reader =
				new BufferedReader(new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
			String linea;
			while ((linea = reader.readLine()) != null) {
				synchronized (ultimasLineas) {
					ultimasLineas.addLast(linea);
					if (ultimasLineas.size() > LINEAS_MENSAJE) {
						ultimasLineas.removeFirst();
					}
				}
			}
		} catch (IOException e) {
			log.warn("Error leyendo la salida del scraper: {}", e.getMessage());
		}
	}

	private void actualizarResultado(EstadoBusqueda estado, Integer codigoSalida, String mensaje) {
		synchronized (lock) {
			SearchExecution ejecucion = obtenerOCrearEjecucion();
			ejecucion.setEstado(estado);
			ejecucion.setFechaFin(LocalDateTime.now());
			ejecucion.setCodigoSalida(codigoSalida);
			ejecucion.setMensaje(mensaje);
			searchExecutionRepository.save(ejecucion);
		}
	}

	private SearchExecution obtenerOCrearEjecucion() {
		return searchExecutionRepository.findById(SearchExecution.ID_UNICO).orElseGet(SearchExecution::new);
	}

	@Transactional(readOnly = true)
	protected SearchExecution obtenerOCrearEjecucionSoloLectura() {
		return searchExecutionRepository.findById(SearchExecution.ID_UNICO).orElseGet(SearchExecution::new);
	}
}
