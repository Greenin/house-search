package dev.oscar2ia.housesearch.model;

import dev.oscar2ia.housesearch.model.enums.EstadoCasa;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "house")
@Getter
@Setter
public class House {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_casa")
	private Long idCasa;

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
	@Column(nullable = false)
	private Fuente fuente;

	@Enumerated(EnumType.STRING)
	private Prioridad prioridad;

	@Column(name = "email_contacto")
	private String emailContacto;

	@Column(name = "fecha_localizacion_casa")
	private LocalDate fechaLocalizacionCasa;

	@Column(name = "enlace_casa")
	private String enlaceCasa;

	/**
	 * Hash SHA-256 de titulo+ubicacion normalizados, usado para deduplicar
	 * casas repetidas entre ejecuciones del scraper.
	 */
	@Column(name = "id_titulo_ubicacion", unique = true, nullable = false, length = 64)
	private String idTituloUbicacion;
}
