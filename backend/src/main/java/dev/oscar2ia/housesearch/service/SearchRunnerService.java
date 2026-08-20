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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lanza el scraper Node.js como proceso hijo en un hilo daemon (no bloquea el
 * request HTTP), con un lock de instancia para evitar solapar ejecuciones.
 * Soporta dos modos, cada uno con su propia fila de {@link SearchExecution}
 * (ver esa clase): "completa" (todas las fuentes, limitada a una vez al dia,
 * persistido en BD para sobrevivir a reinicios) y "sin Playwright" (solo
 * fuentes que no necesitan navegador, sin limite diario). Ambos modos
 * comparten el mismo lock de instancia: no pueden correr dos scrapers a la
 * vez porque podrian golpear la misma fuente por duplicado.
 */
@Service
public class SearchRunnerService {

	private static final Logger log = LoggerFactory.getLogger(SearchRunnerService.class);

	private static final int LINEAS_MENSAJE = 20;
	private static final int TIMEOUT_MINUTOS = 30;

	private final Object lock = new Object();

	private final SearchExecutionRepository searchExecutionRepository;
	private final HouseService houseService;
	private final String workingDir;
	private final String command;

	public SearchRunnerService(
			SearchExecutionRepository searchExecutionRepository,
			HouseService houseService,
			@Value("${app.scraper.working-dir:../scraper}") String workingDir,
			@Value("${app.scraper.command:node index.js}") String command) {
		this.searchExecutionRepository = searchExecutionRepository;
		this.houseService = houseService;
		this.workingDir = workingDir;
		this.command = command;
	}

	/**
	 * Boton "Ejecutar busqueda": todas las fuentes, maximo una vez al dia. Antes
	 * de lanzar el scraper se borra toda la tabla de casas encontradas (ver
	 * HouseService.borrarTodasLasCasas): el listado de "Casas encontradas"
	 * refleja siempre la ultima ejecucion, no un acumulado historico. Las casas
	 * ya movidas a SelectedHouse no se ven afectadas.
	 */
	public ResultadoInicioBusqueda iniciarBusqueda() {
		return iniciarEjecucion(SearchExecution.ID_COMPLETA, true, Map.of(), houseService::borrarTodasLasCasas);
	}

	/** Boton "Busqueda sin Playwright": solo fuentes sin Playwright, sin limite diario. Mismo borrado previo que arriba. */
	public ResultadoInicioBusqueda iniciarBusquedaSinPlaywright() {
		return iniciarEjecucion(
				SearchExecution.ID_SIN_PLAYWRIGHT,
				false,
				Map.of("MODO_SCRAPER", "SIN_PLAYWRIGHT"),
				houseService::borrarTodasLasCasas);
	}

	private ResultadoInicioBusqueda iniciarEjecucion(
			Long idEjecucion, boolean aplicarLimiteDiario, Map<String, String> variablesExtra, Runnable antesDeArrancar) {
		synchronized (lock) {
			if (hayAlgunaEjecucionEnCurso()) {
				return ResultadoInicioBusqueda.YA_EN_EJECUCION;
			}

			SearchExecution ejecucion = obtenerOCrearEjecucion(idEjecucion);
			if (aplicarLimiteDiario
					&& ejecucion.getFechaInicio() != null
					&& ejecucion.getFechaInicio().toLocalDate().isEqual(LocalDate.now())) {
				return ResultadoInicioBusqueda.LIMITE_DIARIO_ALCANZADO;
			}

			// Dentro del lock: garantiza que el borrado termina antes de que el
			// scraper arranque y pueda insertar resultados nuevos.
			if (antesDeArrancar != null) {
				antesDeArrancar.run();
			}

			ejecucion.setEstado(EstadoBusqueda.EN_EJECUCION);
			ejecucion.setFechaInicio(LocalDateTime.now());
			ejecucion.setFechaFin(null);
			ejecucion.setCodigoSalida(null);
			ejecucion.setMensaje(null);
			searchExecutionRepository.save(ejecucion);
		}

		Thread hilo = new Thread(() -> ejecutarProceso(idEjecucion, variablesExtra), "scraper-runner");
		hilo.setDaemon(true);
		hilo.start();

		return ResultadoInicioBusqueda.INICIADO;
	}

	/** Ninguno de los dos modos puede arrancar si el otro ya esta en marcha: podrian golpear la misma fuente a la vez. */
	private boolean hayAlgunaEjecucionEnCurso() {
		return obtenerOCrearEjecucion(SearchExecution.ID_COMPLETA).getEstado() == EstadoBusqueda.EN_EJECUCION
				|| obtenerOCrearEjecucion(SearchExecution.ID_SIN_PLAYWRIGHT).getEstado() == EstadoBusqueda.EN_EJECUCION;
	}

	@Transactional(readOnly = true)
	public SearchStatusResponse estadoActual() {
		return SearchStatusResponse.from(obtenerOCrearEjecucionSoloLectura(SearchExecution.ID_COMPLETA));
	}

	@Transactional(readOnly = true)
	public SearchStatusResponse estadoActualSinPlaywright() {
		return SearchStatusResponse.from(obtenerOCrearEjecucionSoloLectura(SearchExecution.ID_SIN_PLAYWRIGHT));
	}

	private void ejecutarProceso(Long idEjecucion, Map<String, String> variablesExtra) {
		Deque<String> ultimasLineas = new ArrayDeque<>();
		try {
			ProcessBuilder pb = new ProcessBuilder(command.trim().split("\\s+"));
			pb.directory(new File(workingDir));
			pb.redirectErrorStream(true);
			// El backend no tiene pantalla asociada: forzamos Playwright a modo
			// headless pase lo que pase en el entorno, aunque en desarrollo se
			// lance el scraper a mano con HEADLESS sin definir (visible) para
			// depurar selectores (ver scraper/lib/browser.js).
			pb.environment().put("HEADLESS", "true");
			pb.environment().putAll(variablesExtra);
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
						idEjecucion,
						EstadoBusqueda.FALLIDA,
						null,
						"Timeout tras " + TIMEOUT_MINUTOS + " minutos.\n" + mensaje);
				return;
			}

			int codigo = proceso.exitValue();
			EstadoBusqueda estadoFinal = codigo == 0 ? EstadoBusqueda.COMPLETADA : EstadoBusqueda.FALLIDA;
			actualizarResultado(idEjecucion, estadoFinal, codigo, mensaje);
		} catch (Throwable t) {
			// Red de seguridad final: un fallo inesperado no debe dejar la fila
			// colgada en EN_EJECUCION para siempre.
			log.error("Fallo inesperado ejecutando el scraper", t);
			actualizarResultado(idEjecucion, EstadoBusqueda.FALLIDA, null, "Error inesperado: " + t.getMessage());
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

	private void actualizarResultado(Long idEjecucion, EstadoBusqueda estado, Integer codigoSalida, String mensaje) {
		synchronized (lock) {
			SearchExecution ejecucion = obtenerOCrearEjecucion(idEjecucion);
			ejecucion.setEstado(estado);
			ejecucion.setFechaFin(LocalDateTime.now());
			ejecucion.setCodigoSalida(codigoSalida);
			ejecucion.setMensaje(mensaje);
			searchExecutionRepository.save(ejecucion);
		}
	}

	private SearchExecution obtenerOCrearEjecucion(Long id) {
		return searchExecutionRepository.findById(id).orElseGet(() -> nuevaEjecucion(id));
	}

	@Transactional(readOnly = true)
	protected SearchExecution obtenerOCrearEjecucionSoloLectura(Long id) {
		return searchExecutionRepository.findById(id).orElseGet(() -> nuevaEjecucion(id));
	}

	private SearchExecution nuevaEjecucion(Long id) {
		SearchExecution ejecucion = new SearchExecution();
		ejecucion.setId(id);
		return ejecucion;
	}
}
