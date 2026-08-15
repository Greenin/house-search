package dev.oscar2ia.housesearch.service;

import dev.oscar2ia.housesearch.dto.ProfileConfigResponse;
import dev.oscar2ia.housesearch.dto.ProfileConfigUpdateRequest;
import dev.oscar2ia.housesearch.model.ProfileConfig;
import dev.oscar2ia.housesearch.repository.ProfileConfigRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileConfigService {

	private static final int MAX_LONGITUD_PALABRA = 255;

	private final ProfileConfigRepository profileConfigRepository;

	public ProfileConfigService(ProfileConfigRepository profileConfigRepository) {
		this.profileConfigRepository = profileConfigRepository;
	}

	@Transactional
	public ProfileConfigResponse obtenerConfiguracion() {
		return ProfileConfigResponse.from(obtenerOCrear());
	}

	@Transactional
	public ProfileConfigResponse actualizarConfiguracion(ProfileConfigUpdateRequest request) {
		ProfileConfig pc = obtenerOCrear();
		pc.setContextoCasaBuscada(request.contextoCasaBuscada());
		pc.setPrecioMaximo(request.precioMaximo());
		pc.setTamanoMinimo(request.tamanoMinimo());
		pc.setNumeroHabitacionesMinimo(request.numeroHabitacionesMinimo());
		pc.setNumeroBanosMinimo(request.numeroBanosMinimo());
		pc.setTerrazaRequerida(request.terrazaRequerida() != null ? request.terrazaRequerida() : Boolean.FALSE);
		pc.setOrientacion(request.orientacion());
		pc.setAscensorRequerido(request.ascensorRequerido() != null ? request.ascensorRequerido() : Boolean.FALSE);
		pc.setClimatizacionRequerida(
				request.climatizacionRequerida() != null ? request.climatizacionRequerida() : Boolean.FALSE);
		pc.setPalabrasClave(normalizarLista(request.palabrasClave()));
		pc.setFiltrosNegativos(normalizarLista(request.filtrosNegativos()));
		return ProfileConfigResponse.from(pc);
	}

	/** Fila unica gestionada por servicio: la primera que exista, o una nueva si no hay ninguna. */
	private ProfileConfig obtenerOCrear() {
		return profileConfigRepository.findAll().stream()
				.findFirst()
				.orElseGet(() -> profileConfigRepository.save(new ProfileConfig()));
	}

	private List<String> normalizarLista(List<String> lista) {
		if (lista == null) {
			return new ArrayList<>();
		}
		List<String> normalizada = new ArrayList<>();
		for (String elemento : lista) {
			if (elemento == null) {
				continue;
			}
			String limpio = elemento.trim();
			if (limpio.isEmpty()) {
				continue;
			}
			if (limpio.length() > MAX_LONGITUD_PALABRA) {
				throw new IllegalArgumentException(
						"Cada palabra clave o filtro negativo debe tener como maximo " + MAX_LONGITUD_PALABRA
								+ " caracteres: \"" + limpio + "\"");
			}
			normalizada.add(limpio);
		}
		return normalizada;
	}
}
