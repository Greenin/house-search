package dev.oscar2ia.housesearch.dto;

import dev.oscar2ia.housesearch.model.enums.EstadoCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.model.enums.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record HouseInsertRequest(
		@NotBlank String titulo,
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
		@NotNull Fuente fuente,
		Prioridad prioridad,
		String emailContacto,
		LocalDate fechaLocalizacionCasa,
		String enlaceCasa) {
}
