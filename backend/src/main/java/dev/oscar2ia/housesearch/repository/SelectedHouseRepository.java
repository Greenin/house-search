package dev.oscar2ia.housesearch.repository;

import dev.oscar2ia.housesearch.model.SelectedHouse;
import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SelectedHouseRepository extends JpaRepository<SelectedHouse, Long> {

	@Query("""
			SELECT sh FROM SelectedHouse sh
			WHERE (:estado IS NULL OR sh.estadoGestion = :estado)
			  AND (:fuente IS NULL OR sh.fuente = :fuente)
			  AND (:scoreMin IS NULL OR sh.puntuacion >= :scoreMin)
			ORDER BY sh.fechaSeleccionCasa DESC, sh.idCasaSeleccionada DESC
			""")
	List<SelectedHouse> findByFiltros(
			@Param("estado") EstadoGestionCasa estado,
			@Param("fuente") Fuente fuente,
			@Param("scoreMin") Integer scoreMin);
}
