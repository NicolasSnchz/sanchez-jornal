package com.jornal.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciales para obtener un token")
public record LoginRequest(
        @Schema(example = "maestro@jornal.co") @NotBlank @Email String email,
        @Schema(example = "Jornal2026*") @NotBlank @Size(max = 64) String password) {

    /** Nunca imprimir la clave, ni siquiera por accidente en un log. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
