package com.jornal.dto.form;

import com.jornal.entity.ConstructionSite;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Datos de una obra (crear y editar). */
@Schema(description = "Datos de una obra")
public class SiteForm {

    @Schema(description = "Nombre de la obra", example = "Casa Duarte: ampliaci\u00f3n segundo piso")
    @NotBlank
    @Size(max = 120)
    private String name;

    @Schema(description = "Descripci\u00f3n del trabajo (opcional)",
            example = "Construcci\u00f3n del segundo piso: columnas, placa y muros")
    @Size(max = 500)
    private String description;

    @Schema(description = "Direcci\u00f3n", example = "Calle 12 # 2E-45")
    @NotBlank
    @Size(max = 150)
    private String address;

    @Schema(description = "Barrio", example = "Caobos")
    @NotBlank
    @Size(max = 60)
    private String neighborhood;

    @Schema(description = "Ciudad; se usa para buscar trabajadores de la misma ciudad", example = "C\u00facuta")
    @NotBlank
    @Size(max = 60)
    private String city;

    /** Licencia de construccion de la curaduria; opcional en obras menores. */
    @Schema(description = "N\u00famero de licencia de construcci\u00f3n (opcional)", example = "54001-1-26-0112")
    @Size(max = 40)
    private String buildingPermit;

    @Schema(description = "Fecha de inicio", example = "2026-11-03")
    @NotNull
    private LocalDate startDate;

    @Schema(description = "Fecha estimada de fin; no puede estar en el pasado", example = "2027-02-26")
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
