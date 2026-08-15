package dev.oscar2ia.housesearch.web;

import dev.oscar2ia.housesearch.dto.BulkInsertResponse;
import dev.oscar2ia.housesearch.dto.HouseInsertRequest;
import dev.oscar2ia.housesearch.dto.HouseResponse;
import dev.oscar2ia.housesearch.model.enums.Fuente;
import dev.oscar2ia.housesearch.service.HouseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/house")
public class HouseController {

	private final HouseService houseService;

	public HouseController(HouseService houseService) {
		this.houseService = houseService;
	}

	@PostMapping("/insert")
	public BulkInsertResponse insertar(@RequestBody @Valid List<HouseInsertRequest> casas) {
		return houseService.insertarCasas(casas);
	}

	@GetMapping
	public List<HouseResponse> listar(
			@RequestParam(required = false) Fuente fuente,
			@RequestParam(required = false) Integer tamanoMinimo,
			@RequestParam(required = false) Integer scoreMin,
			@RequestParam(required = false) Integer precioMaximo) {
		return houseService.listarCasas(fuente, tamanoMinimo, scoreMin, precioMaximo);
	}

	@GetMapping("/{id}")
	public ResponseEntity<HouseResponse> obtener(@PathVariable Long id) {
		return houseService.obtenerCasa(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void borrar(@PathVariable Long id) {
		houseService.borrarCasa(id);
	}

	@PostMapping("/clear")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void limpiar() {
		houseService.borrarTodasLasCasas();
	}
}
