package dev.oscar2ia.housesearch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "house_match_score")
@Getter
@Setter
public class HouseMatchScore {

	@Id
	@Column(name = "id_casa")
	private Long idCasa;

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	@JoinColumn(name = "id_casa")
	private House house;

	@Column(nullable = false)
	private Integer puntuacion;

	@Column(name = "razones_a_favor", columnDefinition = "TEXT")
	private String razonesAFavor;

	@Column(name = "razones_en_contra", columnDefinition = "TEXT")
	private String razonesEnContra;

	@Column(name = "fecha_evaluacion")
	private LocalDateTime fechaEvaluacion;

	@Column(name = "modelo_usado")
	private String modeloUsado;
}
