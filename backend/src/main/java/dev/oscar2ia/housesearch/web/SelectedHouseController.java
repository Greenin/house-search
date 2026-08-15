package dev.oscar2ia.housesearch.web;

import dev.oscar2ia.housesearch.dto.ChangeStatusRequest;
import dev.oscar2ia.housesearch.dto.SelectedHouseResponse;
import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.service.SelectedHouseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/selected_house")
public class SelectedHouseController {

	private final SelectedHouseService selectedHouseService;

	public SelectedHouseController(SelectedHouseService selectedHouseService) {
		this.selectedHouseService = selectedHouseService;
	}

	@PostMapping("/{id}/copy")
	@ResponseStatus(HttpStatus.CREATED)
	public SelectedHouseResponse copiar(@PathVariable Long id) {
		return selectedHouseService.copiarDesdeCasa(id);
	}

	@GetMapping
	public List<SelectedHouseResponse> listar(
			@RequestParam(required = false) EstadoGestionCasa estado,
			@RequestParam(required = false) Fuente fuente,
			@RequestParam(required = false) Integer scoreMin) {
		return selectedHouseService.listar(estado, fuente, scoreMin);
	}

	@PatchMapping("/{id}/status")
	public SelectedHouseResponse cambiarEstado(@PathVariable Long id, @RequestBody @Valid ChangeStatusRequest request) {
		return selectedHouseService.cambiarEstado(id, request.estado());
	}

	@DeleteMapping("/{id}/delete")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void borrar(@PathVariable Long id) {
		selectedHouseService.borrar(id);
	}
}
