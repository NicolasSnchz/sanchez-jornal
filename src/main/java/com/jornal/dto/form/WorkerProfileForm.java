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

/** Datos de contacto y de trabajo que el trabajador mantiene al dia (por ejemplo, la renovacion de la ARL). */
public class WorkerProfileForm {

    @NotBlank
    @Pattern(regexp = "3\\d{9}", message = "{validacion.celular}")
    private String phone;

    @NotBlank
    @Size(max = 60)
    private String city;

    @NotNull
    private Trade trade;

    @Min(0)
    @Max(60)
    private int yearsOfExperience;

    @NotBlank
    @Size(max = 60)
    private String neighborhood;

    private boolean hasOwnTools;

    @NotNull
    private LocalDate arlValidUntil;

    @NotBlank
    @Pattern(regexp = "3\\d{9}|6\\d{9}", message = "{validacion.telefono}")
    private String emergencyPhone;

    private boolean acceptingOffers;

    public static WorkerProfileForm from(Worker worker) {
        WorkerProfileForm form = new WorkerProfileForm();
        form.phone = worker.getPhone();
        form.city = worker.getCity();
        form.trade = worker.getTrade();
        form.yearsOfExperience = worker.getYearsOfExperience();
        form.neighborhood = worker.getNeighborhood();
        form.hasOwnTools = worker.isHasOwnTools();
        form.arlValidUntil = worker.getArlValidUntil();
        form.emergencyPhone = worker.getEmergencyPhone();
        form.acceptingOffers = worker.isAcceptingOffers();
        return form;
    }

    public Worker.WorkProfile toWorkProfile() {
        return new Worker.WorkProfile(trade, yearsOfExperience, neighborhood, hasOwnTools, arlValidUntil,
                emergencyPhone);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public boolean isAcceptingOffers() {
        return acceptingOffers;
    }

    public void setAcceptingOffers(boolean acceptingOffers) {
        this.acceptingOffers = acceptingOffers;
    }
}
