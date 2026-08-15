package dev.oscar2ia.housesearch.model;

import dev.oscar2ia.housesearch.model.enums.EstadoCasa;
import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.model.enums.Prioridad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Snapshot independiente de {@link House}: al seleccionar una casa se copian sus
 * campos aqui (y se borra la fila original de House/HouseMatchScore) en la misma
 * transaccion. No tiene foreign key hacia House.
 */
@Entity
@Table(name = "selected_house")
@Getter
@Setter
public class SelectedHouse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_casa_seleccionada")
	private Long idCasaSeleccionada;

	@Column(nullable = false)
	private String titulo;

	private String ubicacion;

	private Integer precio;

	private Integer tamano;

	private Integer habitaciones;

	private Integer banos;

	private String planta;

	@Enumerated(EnumType.STRING)
	private EstadoCasa estado;

	private Boolean terraza;

	private String orientacion;

	private Boolean ascensor;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	private String climatizacion;

	private String calefaccion;

	@Column(name = "tipo_calefaccion")
	private String tipoCalefaccion;

	@Column(name = "caracteristicas_basicas", columnDefinition = "TEXT")
	private String caracteristicasBasicas;

	@Column(name = "consumo_energetico")
	private String consumoEnergetico;

	@Enumerated(EnumType.STRING)
	private Fuente fuente;

	@Enumerated(EnumType.STRING)
	private Prioridad prioridad;

	@Column(name = "email_contacto")
	private String emailContacto;

	@Column(name = "fecha_localizacion_casa")
	private LocalDate fechaLocalizacionCasa;

	@Column(name = "enlace_casa")
	private String enlaceCasa;

	@Column(name = "fecha_seleccion_casa")
	private LocalDate fechaSeleccionCasa;

	private Integer puntuacion;

	@Column(name = "razones_a_favor", columnDefinition = "TEXT")
	private String razonesAFavor;

	@Column(name = "razones_en_contra", columnDefinition = "TEXT")
	private String razonesEnContra;

	@Column(name = "fecha_evaluacion")
	private LocalDateTime fechaEvaluacion;

	@Column(name = "modelo_usado")
	private String modeloUsado;

	/**
	 * Estado de gestion de la casa (contactar, cita, oferta...). A diferencia de
	 * job-search, aqui vive como columna directa en vez de en una entidad 1:1
	 * separada, porque no hay dos conceptos de negocio distintos que separar.
	 */
	@Enumerated(EnumType.STRING)
	private EstadoGestionCasa estadoGestion;
}
