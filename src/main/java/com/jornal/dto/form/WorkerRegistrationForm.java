package com.jornal.dto.form;

import com.jornal.entity.Trade;
import com.jornal.entity.Worker;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Registro de un trabajador: datos personales mas su perfil de trabajo. */
public class WorkerRegistrationForm extends PersonalDataForm {

    @NotNull
    private Trade trade;

    @Min(0)
    @Max(60)
    private int yearsOfExperience;

    @NotBlank
    @Size(max = 60)
    private String neighborhood;

    private boolean hasOwnTools;

    /** Fecha hasta la que esta pagada su afiliacion a riesgos laborales (ARL). */
    @NotNull
    private LocalDate arlValidUntil;

    @NotBlank
    @Pattern(regexp = "3\\d{9}|6\\d{9}", message = "{validacion.telefono}")
    private String emergencyPhone;

    public Worker.WorkProfile toWorkProfile() {
        return new Worker.WorkProfile(trade, yearsOfExperience, neighborhood, hasOwnTools, arlValidUntil,
                emergencyPhone);
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public boolean isHasOwnTools() {
        return hasOwnTools;
    }

    public void setHasOwnTools(boolean hasOwnTools) {
        this.hasOwnTools = hasOwnTools;
    }

    public LocalDate getArlValidUntil() {
        return arlValidUntil;
    }

    public void setArlValidUntil(LocalDate arlValidUntil) {
        this.arlValidUntil = arlValidUntil;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }
}
