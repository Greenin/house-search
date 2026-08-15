package dev.oscar2ia.housesearch.web;

import dev.oscar2ia.housesearch.dto.ProfileConfigResponse;
import dev.oscar2ia.housesearch.dto.ProfileConfigUpdateRequest;
import dev.oscar2ia.housesearch.service.ProfileConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile-config")
public class ProfileConfigController {

	private final ProfileConfigService profileConfigService;

	public ProfileConfigController(ProfileConfigService profileConfigService) {
		this.profileConfigService = profileConfigService;
	}

	@GetMapping
	public ProfileConfigResponse obtener() {
		return profileConfigService.obtenerConfiguracion();
	}

	@PutMapping
	public ProfileConfigResponse guardar(@RequestBody @Valid ProfileConfigUpdateRequest request) {
		return profileConfigService.actualizarConfiguracion(request);
	}
}
