package dev.oscar2ia.housesearch.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import dev.oscar2ia.housesearch.dto.ProfileConfigResponse;
import dev.oscar2ia.housesearch.model.House;
import dev.oscar2ia.housesearch.model.HouseMatchScore;
import dev.oscar2ia.housesearch.repository.HouseMatchScoreRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Puntua el encaje de una casa con el perfil de busqueda del usuario usando la
 * API de Claude. Se dispara en un hilo daemon tras el insert masivo (nunca
 * bloquea la respuesta HTTP) y nunca aborta el lote si una casa falla: esa
 * casa simplemente se queda sin fila en house_match_score y se ve con
 * puntuacion "-" en el listado.
 */
@Service
public class HouseScoringService {

	private static final Logger log = LoggerFactory.getLogger(HouseScoringService.class);

	private final AnthropicClient anthropicClient;
	private final String modelo;
	private final HouseMatchScoreRepository houseMatchScoreRepository;
	private final ProfileConfigService profileConfigService;

	public HouseScoringService(
			AnthropicClient anthropicClient,
			@Value("${anthropic.model}") String modelo,
			HouseMatchScoreRepository houseMatchScoreRepository,
			ProfileConfigService profileConfigService) {
		this.anthropicClient = anthropicClient;
		this.modelo = modelo;
		this.houseMatchScoreRepository = houseMatchScoreRepository;
		this.profileConfigService = profileConfigService;
	}

	public void puntuarAsync(List<House> casasNuevas) {
		if (casasNuevas == null || casasNuevas.isEmpty()) {
			return;
		}
		Thread hilo = new Thread(() -> puntuarTodas(casasNuevas), "house-scoring");
		hilo.setDaemon(true);
		hilo.start();
	}

	private void puntuarTodas(List<House> casas) {
		String contextoPerfil = construirContextoPerfil();
		for (House casa : casas) {
			try {
				HouseMatchScore score = puntuarUna(casa, contextoPerfil);
				houseMatchScoreRepository.save(score);
			} catch (Throwable t) {
				log.warn("No se pudo puntuar la casa {}: {}", casa.getIdCasa(), t.getMessage());
			}
		}
	}

	/**
	 * Delega en ProfileConfigService (otro bean, pasa por el proxy de Spring) en
	 * vez de leer el repositorio directamente: llamar a un metodo @Transactional
	 * de este mismo objeto via "this" no pasaria por el proxy y las colecciones
	 * @ElementCollection fallarian con LazyInitializationException fuera de
	 * sesion.
	 */
	private String construirContextoPerfil() {
		ProfileConfigResponse pc = profileConfigService.obtenerConfiguracion();
		return """
				Contexto de la casa buscada: %s
				Precio maximo: %s
				Tamano minimo (m2): %s
				Numero de habitaciones minimo: %s
				Numero de banos minimo: %s
				Terraza requerida: %s
				Orientacion deseada: %s
				Ascensor requerido: %s
				Climatizacion requerida: %s
				Palabras clave: %s
				Filtros negativos (excluir si aparecen): %s
				"""
				.formatted(
						valorOTexto(pc.contextoCasaBuscada(), "(sin especificar)"),
						valorOTexto(pc.precioMaximo(), "sin limite"),
						valorOTexto(pc.tamanoMinimo(), "sin minimo"),
						valorOTexto(pc.numeroHabitacionesMinimo(), "sin minimo"),
						valorOTexto(pc.numeroBanosMinimo(), "sin minimo"),
						valorOTexto(pc.terrazaRequerida(), "no"),
						valorOTexto(pc.orientacion(), "sin preferencia"),
						valorOTexto(pc.ascensorRequerido(), "no"),
						valorOTexto(pc.climatizacionRequerida(), "no"),
						pc.palabrasClave().isEmpty() ? "(ninguna)" : String.join(", ", pc.palabrasClave()),
						pc.filtrosNegativos().isEmpty()
								? "(ninguno)"
								: String.join(", ", pc.filtrosNegativos()));
	}

	private HouseMatchScore puntuarUna(House casa, String contextoPerfil) {
		String prompt = construirPromptCasa(casa);

		StructuredMessageCreateParams<HouseScoreResult> params = MessageCreateParams.builder()
				.model(modelo)
				.maxTokens(1024L)
				.system("Eres un asistente experto en busqueda de vivienda en Espana. Puntuas el ajuste de una "
						+ "casa concreta al perfil de busqueda de un usuario. Responde siempre en espanol, con un "
						+ "tono directo y practico. razonesAFavor y razonesEnContra deben ser listas de 2 a 4 "
						+ "puntos separados por saltos de linea, breves y concretos.\n\n" + contextoPerfil)
				.outputConfig(HouseScoreResult.class)
				.addUserMessage(prompt)
				.build();

		try {
			HouseScoreResult resultado = anthropicClient.messages().create(params).content().stream()
					.flatMap(block -> block.text().stream())
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("Respuesta sin bloque de texto"))
					.text();

			HouseMatchScore score = new HouseMatchScore();
			score.setIdCasa(casa.getIdCasa());
			score.setHouse(casa);
			score.setPuntuacion(resultado.puntuacion());
			score.setRazonesAFavor(resultado.razonesAFavor());
			score.setRazonesEnContra(resultado.razonesEnContra());
			score.setFechaEvaluacion(LocalDateTime.now());
			score.setModeloUsado(modelo);
			return score;
		} catch (AnthropicServiceException e) {
			throw new IllegalStateException("Error de la API de Anthropic: " + e.getMessage(), e);
		}
	}

	private String construirPromptCasa(House casa) {
		return """
				Puntua el encaje de esta casa con el perfil anterior, del 0 (nada adecuada) al 100 (encaje perfecto).

				Titulo: %s
				Ubicacion: %s
				Precio: %s euros
				Tamano: %s m2
				Habitaciones: %s
				Banos: %s
				Planta: %s
				Estado: %s
				Terraza: %s
				Orientacion: %s
				Ascensor: %s
				Climatizacion: %s
				Calefaccion: %s
				Tipo de calefaccion: %s
				Caracteristicas basicas: %s
				Consumo energetico: %s
				Descripcion: %s
				"""
				.formatted(
						casa.getTitulo(),
						valorOTexto(casa.getUbicacion(), "no indicada"),
						valorOTexto(casa.getPrecio(), "no indicado"),
						valorOTexto(casa.getTamano(), "no indicado"),
						valorOTexto(casa.getHabitaciones(), "no indicado"),
						valorOTexto(casa.getBanos(), "no indicado"),
						valorOTexto(casa.getPlanta(), "no indicada"),
						valorOTexto(casa.getEstado(), "no indicado"),
						valorOTexto(casa.getTerraza(), "no indicado"),
						valorOTexto(casa.getOrientacion(), "no indicada"),
						valorOTexto(casa.getAscensor(), "no indicado"),
						valorOTexto(casa.getClimatizacion(), "no indicada"),
						valorOTexto(casa.getCalefaccion(), "no indicada"),
						valorOTexto(casa.getTipoCalefaccion(), "no indicado"),
						valorOTexto(casa.getCaracteristicasBasicas(), "no indicadas"),
						valorOTexto(casa.getConsumoEnergetico(), "no indicado"),
						valorOTexto(casa.getDescripcion(), "(sin descripcion)"));
	}

	private String valorOTexto(Object valor, String textoPorDefecto) {
		return valor != null ? String.valueOf(valor) : textoPorDefecto;
	}

	/** Salida estructurada esperada de Claude para cada casa puntuada. */
	public record HouseScoreResult(Integer puntuacion, String razonesAFavor, String razonesEnContra) {
	}
}
