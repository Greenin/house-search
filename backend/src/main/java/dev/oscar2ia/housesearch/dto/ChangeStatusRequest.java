package dev.oscar2ia.housesearch.dto;

import dev.oscar2ia.housesearch.model.enums.EstadoGestionCasa;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull EstadoGestionCasa estado) {
}
