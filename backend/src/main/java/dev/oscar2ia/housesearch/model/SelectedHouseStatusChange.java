package dev.oscar2ia.housesearch.model;

import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Log de auditoria: se inserta una fila cada vez que cambia el estado de
 * gestion de una SelectedHouse (no solo se sobreescribe el valor actual).
 */
@Entity
@Table(name = "selected_house_status_change")
@Getter
@Setter
public class SelectedHouseStatusChange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_casa_seleccionada", nullable = false)
	private SelectedHouse selectedHouse;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_anterior")
	private EstadoGestionCasa estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_nuevo", nullable = false)
	private EstadoGestionCasa estadoNuevo;

	@Column(name = "fecha_cambio_estado", nullable = false)
	private LocalDateTime fechaCambioEstado;
}
