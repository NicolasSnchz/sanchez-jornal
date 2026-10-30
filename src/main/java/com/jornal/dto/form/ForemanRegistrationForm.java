package com.jornal.dto.form;

import jakarta.validation.constraints.Size;

/** Registro de un maestro de obra. La empresa es opcional: muchos trabajan como personas naturales. */
public class ForemanRegistrationForm extends PersonalDataForm {

    @Size(max = 120)
    private String companyName;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
