package dev.oscar2ia.housesearch.web;

import dev.oscar2ia.housesearch.dto.SearchStatusResponse;
import dev.oscar2ia.housesearch.service.ResultadoInicioBusqueda;
import dev.oscar2ia.housesearch.service.SearchRunnerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

	private final SearchRunnerService searchRunnerService;

	public SearchController(SearchRunnerService searchRunnerService) {
		this.searchRunnerService = searchRunnerService;
	}

	@PostMapping("/run")
	public ResponseEntity<SearchStatusResponse> ejecutar() {
		ResultadoInicioBusqueda resultado = searchRunnerService.iniciarBusqueda();
		HttpStatus status = switch (resultado) {
			case INICIADO -> HttpStatus.ACCEPTED;
			case YA_EN_EJECUCION -> HttpStatus.CONFLICT;
			case LIMITE_DIARIO_ALCANZADO -> HttpStatus.TOO_MANY_REQUESTS;
		};
		return ResponseEntity.status(status).body(searchRunnerService.estadoActual());
	}

	@GetMapping("/status")
	public SearchStatusResponse estado() {
		return searchRunnerService.estadoActual();
	}
}
