package com.jornal.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Cuerpo uniforme de todas las respuestas de error de la API. */
@Schema(description = "Error estandar de la API")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        @Schema(example = "2026-10-29T09:15:00-05:00") OffsetDateTime timestamp,
        @Schema(example = "409") int status,
        @Schema(example = "Conflict") String error,
        @Schema(example = "JORNADA_PERIODO_LIQUIDADO") String code,
        @Schema(example = "No se puede registrar ni corregir una jornada de un periodo ya liquidado (hasta el 16 oct 2026).")
        String message,
        @Schema(example = "/api/asignaciones/3f2b.../jornadas") String path,
        List<FieldError> fieldErrors) {

    /** Error de validacion de un campo del cuerpo de la peticion. */
    public record FieldError(
            @Schema(example = "checkOutTime") String field,
            @Schema(example = "La hora de salida es obligatoria.") String message) {
    }

    /** Convierte un codigo de mensaje (jornada.periodo.liquidado) en un codigo de API (JORNADA_PERIODO_LIQUIDADO). */
    public static String toApiCode(String messageCode) {
        return messageCode == null ? null : messageCode.replace('.', '_').toUpperCase(java.util.Locale.ROOT);
    }
}
