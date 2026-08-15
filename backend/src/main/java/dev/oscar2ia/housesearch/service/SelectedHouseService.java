package dev.oscar2ia.housesearch.service;

import dev.oscar2ia.housesearch.dto.SelectedHouseResponse;
import dev.oscar2ia.housesearch.model.House;
import dev.oscar2ia.housesearch.model.HouseMatchScore;
import dev.oscar2ia.housesearch.model.SelectedHouse;
import dev.oscar2ia.housesearch.model.SelectedHouseStatusChange;
import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.repository.HouseMatchScoreRepository;
import dev.oscar2ia.housesearch.repository.HouseRepository;
import dev.oscar2ia.housesearch.repository.SelectedHouseRepository;
import dev.oscar2ia.housesearch.repository.SelectedHouseStatusChangeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelectedHouseService {

	private final HouseRepository houseRepository;
	private final HouseMatchScoreRepository houseMatchScoreRepository;
	private final SelectedHouseRepository selectedHouseRepository;
	private final SelectedHouseStatusChangeRepository selectedHouseStatusChangeRepository;

	public SelectedHouseService(
			HouseRepository houseRepository,
			HouseMatchScoreRepository houseMatchScoreRepository,
			SelectedHouseRepository selectedHouseRepository,
			SelectedHouseStatusChangeRepository selectedHouseStatusChangeRepository) {
		this.houseRepository = houseRepository;
		this.houseMatchScoreRepository = houseMatchScoreRepository;
		this.selectedHouseRepository = selectedHouseRepository;
		this.selectedHouseStatusChangeRepository = selectedHouseStatusChangeRepository;
	}

	/**
	 * "Seleccionar" es un move, no un copy: se copian los campos por coincidencia
	 * de nombre a una nueva SelectedHouse y se borra la House (y su
	 * HouseMatchScore si existe) original, en la misma transaccion.
	 */
	@Transactional
	public SelectedHouseResponse copiarDesdeCasa(Long idCasa) {
		House house = houseRepository.findById(idCasa).orElseThrow(NoSuchElementException::new);
		HouseMatchScore score = houseMatchScoreRepository.findById(idCasa).orElse(null);

		SelectedHouse sh = new SelectedHouse();
		sh.setTitulo(house.getTitulo());
		sh.setUbicacion(house.getUbicacion());
		sh.setPrecio(house.getPrecio());
		sh.setTamano(house.getTamano());
		sh.setHabitaciones(house.getHabitaciones());
		sh.setBanos(house.getBanos());
		sh.setPlanta(house.getPlanta());
		sh.setEstado(house.getEstado());
		sh.setTerraza(house.getTerraza());
		sh.setOrientacion(house.getOrientacion());
		sh.setAscensor(house.getAscensor());
		sh.setDescripcion(house.getDescripcion());
		sh.setClimatizacion(house.getClimatizacion());
		sh.setCalefaccion(house.getCalefaccion());
		sh.setTipoCalefaccion(house.getTipoCalefaccion());
		sh.setCaracteristicasBasicas(house.getCaracteristicasBasicas());
		sh.setConsumoEnergetico(house.getConsumoEnergetico());
		sh.setFuente(house.getFuente());
		sh.setPrioridad(house.getPrioridad());
		sh.setEmailContacto(house.getEmailContacto());
		sh.setFechaLocalizacionCasa(house.getFechaLocalizacionCasa());
		sh.setEnlaceCasa(house.getEnlaceCasa());
		sh.setFechaSeleccionCasa(LocalDate.now());
		if (score != null) {
			sh.setPuntuacion(score.getPuntuacion());
			sh.setRazonesAFavor(score.getRazonesAFavor());
			sh.setRazonesEnContra(score.getRazonesEnContra());
			sh.setFechaEvaluacion(score.getFechaEvaluacion());
			sh.setModeloUsado(score.getModeloUsado());
		}
		sh.setEstadoGestion(EstadoGestionCasa.PARA_INVESTIGAR);

		selectedHouseRepository.save(sh);

		if (score != null) {
			houseMatchScoreRepository.delete(score);
		}
		houseRepository.delete(house);

		return SelectedHouseResponse.from(sh);
	}

	@Transactional(readOnly = true)
	public List<SelectedHouseResponse> listar(EstadoGestionCasa estado, Fuente fuente, Integer scoreMin) {
		return selectedHouseRepository.findByFiltros(estado, fuente, scoreMin).stream()
				.map(SelectedHouseResponse::from)
				.toList();
	}

	@Transactional
	public SelectedHouseResponse cambiarEstado(Long id, EstadoGestionCasa nuevoEstado) {
		SelectedHouse sh = selectedHouseRepository.findById(id).orElseThrow(NoSuchElementException::new);
		EstadoGestionCasa anterior = sh.getEstadoGestion();
		sh.setEstadoGestion(nuevoEstado);

		SelectedHouseStatusChange cambio = new SelectedHouseStatusChange();
		cambio.setSelectedHouse(sh);
		cambio.setEstadoAnterior(anterior);
		cambio.setEstadoNuevo(nuevoEstado);
		cambio.setFechaCambioEstado(LocalDateTime.now());
		selectedHouseStatusChangeRepository.save(cambio);

		return SelectedHouseResponse.from(sh);
	}

	@Transactional
	public void borrar(Long id) {
		if (!selectedHouseRepository.existsById(id)) {
			throw new NoSuchElementException();
		}
		selectedHouseStatusChangeRepository.deleteBySelectedHouse_IdCasaSeleccionada(id);
		selectedHouseRepository.deleteById(id);
	}
}
