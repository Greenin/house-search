package dev.oscar2ia.housesearch.dto;

import dev.oscar2ia.housesearch.model.House;
import dev.oscar2ia.housesearch.model.HouseMatchScore;
import dev.oscar2ia.housesearch.model.enums.EstadoCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.model.enums.Prioridad;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HouseResponse(
		Long idCasa,
		String titulo,
		String ubicacion,
		Integer precio,
		Integer tamano,
		Integer habitaciones,
		Integer banos,
		String planta,
		EstadoCasa estado,
		Boolean terraza,
		String orientacion,
		Boolean ascensor,
		String descripcion,
		String climatizacion,
		String calefaccion,
		String tipoCalefaccion,
		String caracteristicasBasicas,
		String consumoEnergetico,
		Fuente fuente,
		Prioridad prioridad,
		String emailContacto,
		LocalDate fechaLocalizacionCasa,
		String enlaceCasa,
		Integer puntuacion,
		String razonesAFavor,
		String razonesEnContra,
		LocalDateTime fechaEvaluacion,
		String modeloUsado) {

	public static HouseResponse from(House house, HouseMatchScore score) {
		return new HouseResponse(
				house.getIdCasa(),
				house.getTitulo(),
				house.getUbicacion(),
				house.getPrecio(),
				house.getTamano(),
				house.getHabitaciones(),
				house.getBanos(),
				house.getPlanta(),
				house.getEstado(),
				house.getTerraza(),
				house.getOrientacion(),
				house.getAscensor(),
				house.getDescripcion(),
				house.getClimatizacion(),
				house.getCalefaccion(),
				house.getTipoCalefaccion(),
				house.getCaracteristicasBasicas(),
				house.getConsumoEnergetico(),
				house.getFuente(),
				house.getPrioridad(),
				house.getEmailContacto(),
				house.getFechaLocalizacionCasa(),
				house.getEnlaceCasa(),
				score != null ? score.getPuntuacion() : null,
				score != null ? score.getRazonesAFavor() : null,
				score != null ? score.getRazonesEnContra() : null,
				score != null ? score.getFechaEvaluacion() : null,
				score != null ? score.getModeloUsado() : null);
	}
}
