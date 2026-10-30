package com.jornal.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo obligatorio de una accion que deja constancia: anular, cancelar u objetar. */
public class ReasonForm {

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
