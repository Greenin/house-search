package dev.oscar2ia.housesearch.repository;

import dev.oscar2ia.housesearch.model.SelectedHouseStatusChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SelectedHouseStatusChangeRepository extends JpaRepository<SelectedHouseStatusChange, Long> {

	void deleteBySelectedHouse_IdCasaSeleccionada(Long idCasaSeleccionada);
}
