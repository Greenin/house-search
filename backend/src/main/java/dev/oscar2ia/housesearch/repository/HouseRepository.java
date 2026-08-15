package dev.oscar2ia.housesearch.repository;

import dev.oscar2ia.housesearch.model.House;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseRepository extends JpaRepository<House, Long> {

	Optional<House> findByIdTituloUbicacion(String idTituloUbicacion);

	@Query("""
			SELECT h FROM House h
			LEFT JOIN HouseMatchScore s ON s.idCasa = h.idCasa
			WHERE (:fuente IS NULL OR h.fuente = :fuente)
			  AND (:tamanoMinimo IS NULL OR h.tamano >= :tamanoMinimo)
			  AND (:scoreMin IS NULL OR s.puntuacion >= :scoreMin)
			  AND (:precioMaximo IS NULL OR h.precio <= :precioMaximo)
			ORDER BY h.fechaLocalizacionCasa DESC, h.idCasa DESC
			""")
	List<House> findByFiltros(
			@Param("fuente") Fuente fuente,
			@Param("tamanoMinimo") Integer tamanoMinimo,
			@Param("scoreMin") Integer scoreMin,
			@Param("precioMaximo") Integer precioMaximo);
}
