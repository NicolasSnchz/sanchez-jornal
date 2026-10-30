package com.jornal.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Respuesta del trabajador a una invitacion")
public record RespondRequest(@Schema(example = "true") @NotNull Boolean accept) {
}
