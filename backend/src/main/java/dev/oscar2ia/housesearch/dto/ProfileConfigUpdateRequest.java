package dev.oscar2ia.housesearch.dto;

import java.util.List;

public record ProfileConfigUpdateRequest(
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
}
