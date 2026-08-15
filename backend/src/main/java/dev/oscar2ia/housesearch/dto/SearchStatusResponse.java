package dev.oscar2ia.housesearch.dto;

import dev.oscar2ia.housesearch.model.SearchExecution;
import dev.oscar2ia.housesearch.model.enums.EstadoBusqueda;
import java.time.LocalDateTime;

public record SearchStatusResponse(
		EstadoBusqueda estado,
		LocalDateTime fechaInicio,
		LocalDateTime fechaFin,
		Integer codigoSalida,
		String mensaje) {

	public static SearchStatusResponse from(SearchExecution se) {
		return new SearchStatusResponse(
				se.getEstado(), se.getFechaInicio(), se.getFechaFin(), se.getCodigoSalida(), se.getMensaje());
	}
}
