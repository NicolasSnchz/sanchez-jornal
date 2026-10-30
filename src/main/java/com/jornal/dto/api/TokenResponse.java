package com.jornal.dto.api;

import com.jornal.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Token de acceso. Se envia en cada peticion como Authorization: Bearer {accessToken}")
public record TokenResponse(
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Segundos de validez", example = "3600") long expiresIn,
        UUID userId,
        Role role,
        String fullName) {
}
