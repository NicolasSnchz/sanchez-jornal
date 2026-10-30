package com.jornal.dto.form;

import com.jornal.entity.Foreman;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el maestro puede actualizar. El documento y el correo no se editan: identifican la cuenta. */
public class ForemanProfileForm {

    @NotBlank
    @Pattern(regexp = "3\\d{9}", message = "{validacion.celular}")
    private String phone;

    @NotBlank
    @Size(max = 60)
    private String city;

    @Size(max = 120)
    private String companyName;

    public static ForemanProfileForm from(Foreman foreman) {
        ForemanProfileForm form = new ForemanProfileForm();
        form.phone = foreman.getPhone();
        form.city = foreman.getCity();
        form.companyName = foreman.getCompanyName();
        return form;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
