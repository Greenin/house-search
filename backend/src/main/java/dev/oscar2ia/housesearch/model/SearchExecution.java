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
 * Fila unica (id fijo = ID_UNICO) que registra el estado de la ultima
 * ejecucion del scraper. Persistida en BD para que el limite de "una vez al
 * dia" sobreviva a reinicios del backend.
 */
@Entity
@Table(name = "search_execution")
@Getter
@Setter
public class SearchExecution {

	public static final Long ID_UNICO = 1L;

	@Id
	private Long id = ID_UNICO;

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
