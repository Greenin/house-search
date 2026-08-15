package dev.oscar2ia.housesearch.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Fila unica gestionada por servicio (no por constraint de BD) con la
 * configuracion de busqueda del usuario.
 */
@Entity
@Table(name = "profile_config")
@Getter
@Setter
public class ProfileConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "contexto_casa_buscada", columnDefinition = "TEXT")
	private String contextoCasaBuscada;

	@Column(name = "precio_maximo")
	private Integer precioMaximo;

	@Column(name = "tamano_minimo")
	private Integer tamanoMinimo;

	@Column(name = "numero_habitaciones_minimo")
	private Integer numeroHabitacionesMinimo;

	@Column(name = "numero_banos_minimo")
	private Integer numeroBanosMinimo;

	@Column(name = "terraza_requerida")
	private Boolean terrazaRequerida = Boolean.FALSE;

	private String orientacion;

	@Column(name = "ascensor_requerido")
	private Boolean ascensorRequerido = Boolean.FALSE;

	@Column(name = "climatizacion_requerida")
	private Boolean climatizacionRequerida = Boolean.FALSE;

	@ElementCollection
	@CollectionTable(name = "profile_config_palabra_clave", joinColumns = @JoinColumn(name = "id_profile_config"))
	@Column(name = "palabra_clave")
	private List<String> palabrasClave = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "profile_config_filtro_negativo", joinColumns = @JoinColumn(name = "id_profile_config"))
	@Column(name = "filtro_negativo")
	private List<String> filtrosNegativos = new ArrayList<>();
}
