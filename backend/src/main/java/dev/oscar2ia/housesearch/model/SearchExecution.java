package dev.oscar2ia.housesearch.model;

import dev.oscar2ia.housesearch.model.enums.EstadoBusqueda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Registra el estado de la ultima ejecucion del scraper. Dos filas de id fijo
 * en vez de una: ID_COMPLETA (boton "Ejecutar busqueda", todas las fuentes,
 * limitado a una vez al dia) e ID_SIN_PLAYWRIGHT (boton "Busqueda sin
 * Playwright", solo fuentes sin Playwright, sin limite diario) -> el limite
 * diario de una no debe verse afectado por ejecuciones de la otra. Persistida
 * en BD para que el limite sobreviva a reinicios del backend.
 */
@Entity
@Table(name = "search_execution")
@Getter
@Setter
public class SearchExecution {

	public static final Long ID_COMPLETA = 1L;
	public static final Long ID_SIN_PLAYWRIGHT = 2L;

	@Id
	private Long id = ID_COMPLETA;

	@Enumerated(EnumType.STRING)
	private EstadoBusqueda estado = EstadoBusqueda.INACTIVA;

	@Column(name = "fecha_inicio")
	private LocalDateTime fechaInicio;

	@Column(name = "fecha_fin")
	private LocalDateTime fechaFin;

	@Column(name = "codigo_salida")
	private Integer codigoSalida;

	@Column(columnDefinition = "TEXT")
	private String mensaje;
}
