package com.jornal.dto.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Invitacion de un trabajador a un frente con el jornal que se le ofrece. */
@Schema(description = "Invitaci\u00f3n a un trabajador para un frente")
public class InvitationForm {

    @Schema(description = "Id del trabajador, tomado de la b\u00fasqueda de candidatos")
    @NotNull
    private UUID workerId;

    @Schema(description = "Jornal pactado en pesos", example = "75000")
    @NotNull
    @Min(10_000)
    @Max(1_000_000)
    private BigDecimal dailyWage;

    @Schema(description = "Primer d\u00eda de trabajo", example = "2026-11-04")
    @NotNull
    private LocalDate startDate;

    public UUID getWorkerId() {
        return workerId;
    }

    public void setWorkerId(UUID workerId) {
        this.workerId = workerId;
    }

    public BigDecimal getDailyWage() {
        return dailyWage;
    }

    public void setDailyWage(BigDecimal dailyWage) {
        this.dailyWage = dailyWage;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
