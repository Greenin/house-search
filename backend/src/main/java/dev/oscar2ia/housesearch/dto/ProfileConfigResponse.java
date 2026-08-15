package dev.oscar2ia.housesearch.dto;

import dev.oscar2ia.housesearch.model.ProfileConfig;
import java.util.List;

public record ProfileConfigResponse(
		Long id,
		String contextoCasaBuscada,
		Integer precioMaximo,
		Integer tamanoMinimo,
		Integer numeroHabitacionesMinimo,
		Integer numeroBanosMinimo,
		Boolean terrazaRequerida,
		String orientacion,
		Boolean ascensorRequerido,
		Boolean climatizacionRequerida,
		List<String> palabrasClave,
		List<String> filtrosNegativos) {

	/**
	 * Debe llamarse dentro de una transaccion (spring.jpa.open-in-view=false): las
	 * listas @ElementCollection se copian aqui con List.copyOf antes de que la
	 * sesion de Hibernate se cierre.
	 */
	public static ProfileConfigResponse from(ProfileConfig pc) {
		return new ProfileConfigResponse(
				pc.getId(),
				pc.getContextoCasaBuscada(),
				pc.getPrecioMaximo(),
				pc.getTamanoMinimo(),
				pc.getNumeroHabitacionesMinimo(),
				pc.getNumeroBanosMinimo(),
				pc.getTerrazaRequerida(),
				pc.getOrientacion(),
				pc.getAscensorRequerido(),
				pc.getClimatizacionRequerida(),
				List.copyOf(pc.getPalabrasClave()),
				List.copyOf(pc.getFiltrosNegativos()));
	}
}
