package dev.oscar2ia.housesearch.service;

import dev.oscar2ia.housesearch.dto.BulkInsertResponse;
import dev.oscar2ia.housesearch.dto.HouseInsertRequest;
import dev.oscar2ia.housesearch.dto.HouseResponse;
import dev.oscar2ia.housesearch.model.House;
import dev.oscar2ia.housesearch.model.HouseMatchScore;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.repository.HouseMatchScoreRepository;
import dev.oscar2ia.housesearch.repository.HouseRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class HouseService {

	private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}");
	private static final Pattern ESPACIOS = Pattern.compile("\\s+");

	private final HouseRepository houseRepository;
	private final HouseMatchScoreRepository houseMatchScoreRepository;
	private final HouseScoringService houseScoringService;

	public HouseService(
			HouseRepository houseRepository,
			HouseMatchScoreRepository houseMatchScoreRepository,
			HouseScoringService houseScoringService) {
		this.houseRepository = houseRepository;
		this.houseMatchScoreRepository = houseMatchScoreRepository;
		this.houseScoringService = houseScoringService;
	}

	@Transactional
	public BulkInsertResponse insertarCasas(List<HouseInsertRequest> casas) {
		int insertadas = 0;
		int duplicadas = 0;
		List<House> nuevasCasas = new ArrayList<>();

		for (HouseInsertRequest request : casas) {
			String idTituloUbicacion = calcularIdTituloUbicacion(request.titulo(), request.ubicacion());
			if (houseRepository.findByIdTituloUbicacion(idTituloUbicacion).isPresent()) {
				duplicadas++;
				continue;
			}

			House house = new House();
			house.setTitulo(request.titulo());
			house.setUbicacion(request.ubicacion());
			house.setPrecio(request.precio());
			house.setTamano(request.tamano());
			house.setHabitaciones(request.habitaciones());
			house.setBanos(request.banos());
			house.setPlanta(request.planta());
			house.setEstado(request.estado());
			house.setTerraza(request.terraza());
			house.setOrientacion(request.orientacion());
			house.setAscensor(request.ascensor());
			house.setDescripcion(request.descripcion());
			house.setClimatizacion(request.climatizacion());
			house.setCalefaccion(request.calefaccion());
			house.setTipoCalefaccion(request.tipoCalefaccion());
			house.setCaracteristicasBasicas(request.caracteristicasBasicas());
			house.setConsumoEnergetico(request.consumoEnergetico());
			house.setFuente(request.fuente());
			house.setPrioridad(request.prioridad());
			house.setEmailContacto(request.emailContacto());
			house.setFechaLocalizacionCasa(request.fechaLocalizacionCasa());
			house.setEnlaceCasa(request.enlaceCasa());
			house.setIdTituloUbicacion(idTituloUbicacion);

			houseRepository.save(house);
			nuevasCasas.add(house);
			insertadas++;
		}

		// El scoring solo debe arrancar una vez que las casas nuevas esten
		// realmente commiteadas: si se lanzara el hilo daemon mientras esta
		// transaccion sigue abierta, house_match_score podria intentar insertar
		// referenciando un id_casa que otra transaccion todavia no ve.
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					houseScoringService.puntuarAsync(nuevasCasas);
				}
			});
		} else {
			houseScoringService.puntuarAsync(nuevasCasas);
		}

		return new BulkInsertResponse(casas.size(), insertadas, duplicadas);
	}

	@Transactional(readOnly = true)
	public List<HouseResponse> listarCasas(Fuente fuente, Integer tamanoMinimo, Integer scoreMin, Integer precioMaximo) {
		List<House> houses = houseRepository.findByFiltros(fuente, tamanoMinimo, scoreMin, precioMaximo);
		List<Long> ids = houses.stream().map(House::getIdCasa).toList();
		Map<Long, HouseMatchScore> scoresPorId =
				houseMatchScoreRepository.findAllById(ids).stream()
						.collect(java.util.stream.Collectors.toMap(HouseMatchScore::getIdCasa, s -> s));
		return houses.stream()
				.map(h -> HouseResponse.from(h, scoresPorId.get(h.getIdCasa())))
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<HouseResponse> obtenerCasa(Long id) {
		return houseRepository.findById(id)
				.map(h -> HouseResponse.from(h, houseMatchScoreRepository.findById(id).orElse(null)));
	}

	@Transactional
	public void borrarCasa(Long id) {
		House house = houseRepository.findById(id).orElseThrow(NoSuchElementException::new);
		houseMatchScoreRepository.findById(id).ifPresent(houseMatchScoreRepository::delete);
		houseRepository.delete(house);
	}

	@Transactional
	public void borrarTodasLasCasas() {
		houseMatchScoreRepository.deleteAll();
		houseRepository.deleteAll();
	}

	/**
	 * Hash SHA-256 de titulo+ubicacion normalizados (sin acentos, minusculas,
	 * espacios colapsados) usado como clave natural de deduplicacion.
	 */
	static String calcularIdTituloUbicacion(String titulo, String ubicacion) {
		String texto = normalizar(titulo) + "|" + normalizar(ubicacion);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}

	private static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String sinAcentos = DIACRITICOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
		return ESPACIOS.matcher(sinAcentos.toLowerCase().trim()).replaceAll(" ");
	}
}
