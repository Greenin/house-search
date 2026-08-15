package dev.oscar2ia.housesearch.dto;

import dev.oscar2ia.housesearch.model.SelectedHouse;
import dev.oscar2ia.housesearch.model.enums.EstadoCasa;
import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.model.enums.Prioridad;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SelectedHouseResponse(
		Long idCasaSeleccionada,
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
		LocalDate fechaSeleccionCasa,
		Integer puntuacion,
		String razonesAFavor,
		String razonesEnContra,
		LocalDateTime fechaEvaluacion,
		String modeloUsado,
		EstadoGestionCasa estadoGestion) {

	public static SelectedHouseResponse from(SelectedHouse sh) {
		return new SelectedHouseResponse(
				sh.getIdCasaSeleccionada(),
				sh.getTitulo(),
				sh.getUbicacion(),
				sh.getPrecio(),
				sh.getTamano(),
				sh.getHabitaciones(),
				sh.getBanos(),
				sh.getPlanta(),
				sh.getEstado(),
				sh.getTerraza(),
				sh.getOrientacion(),
				sh.getAscensor(),
				sh.getDescripcion(),
				sh.getClimatizacion(),
				sh.getCalefaccion(),
				sh.getTipoCalefaccion(),
				sh.getCaracteristicasBasicas(),
				sh.getConsumoEnergetico(),
				sh.getFuente(),
				sh.getPrioridad(),
				sh.getEmailContacto(),
				sh.getFechaLocalizacionCasa(),
				sh.getEnlaceCasa(),
				sh.getFechaSeleccionCasa(),
				sh.getPuntuacion(),
				sh.getRazonesAFavor(),
				sh.getRazonesEnContra(),
				sh.getFechaEvaluacion(),
				sh.getModeloUsado(),
				sh.getEstadoGestion());
	}
}
