package com.jornal.dto.form;

import com.jornal.entity.ConstructionSite;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Datos de una obra (crear y editar). */
public class SiteForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @NotBlank
    @Size(max = 150)
    private String address;

    @NotBlank
    @Size(max = 60)
    private String neighborhood;

    @NotBlank
    @Size(max = 60)
    private String city;

    /** Licencia de construccion de la curaduria; opcional en obras menores. */
    @Size(max = 40)
    private String buildingPermit;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate estimatedEndDate;

    public static SiteForm from(ConstructionSite site) {
        SiteForm form = new SiteForm();
        form.name = site.getName();
        form.description = site.getDescription();
        form.address = site.getAddress();
        form.neighborhood = site.getNeighborhood();
        form.city = site.getCity();
        form.buildingPermit = site.getBuildingPermit();
        form.startDate = site.getStartDate();
        form.estimatedEndDate = site.getEstimatedEndDate();
        return form;
    }

    public ConstructionSite.SiteData toData() {
        return new ConstructionSite.SiteData(name, description, address, neighborhood, city, buildingPermit,
                startDate, estimatedEndDate);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBuildingPermit() {
        return buildingPermit;
    }

    public void setBuildingPermit(String buildingPermit) {
        this.buildingPermit = buildingPermit;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEstimatedEndDate() {
        return estimatedEndDate;
    }

    public void setEstimatedEndDate(LocalDate estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }
}
