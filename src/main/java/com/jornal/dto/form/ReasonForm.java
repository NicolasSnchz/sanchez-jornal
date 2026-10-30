package com.jornal.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo obligatorio de una accion que deja constancia: anular, cancelar u objetar. */
@Schema(description = "Motivo de la acci\u00f3n")
public class ReasonForm {

    @Schema(description = "Motivo (entre 5 y 300 caracteres)", example = "La obra se suspendi\u00f3 por lluvias")
    @NotBlank
    @Size(min = 5, max = 300)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
