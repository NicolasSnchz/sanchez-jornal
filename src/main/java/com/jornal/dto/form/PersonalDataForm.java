package com.jornal.dto.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jornal.entity.DocumentType;
import com.jornal.entity.Gender;
import com.jornal.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Datos comunes del registro. Las validaciones de formato viven aqui (capa de
 * entrada); las reglas de negocio (mayoria de edad, correo unico) las revisa el
 * servicio, porque dependen de la fecha actual o de la base de datos.
 */
public abstract class PersonalDataForm {

    @NotNull
    private DocumentType documentType;

    @NotBlank
    @Pattern(regexp = "[0-9A-Za-z]{5,15}", message = "{validacion.documento}")
    private String documentNumber;

    @NotBlank
    @Size(max = 60)
    private String firstName;

    @NotBlank
    @Size(max = 60)
    private String lastName;

    @NotNull
    @Past
    private LocalDate birthDate;

    @NotNull
    private Gender gender;

    /** Celular colombiano: 10 digitos que empiezan por 3. */
    @NotBlank
    @Pattern(regexp = "3\\d{9}", message = "{validacion.celular}")
    private String phone;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank
    @Size(max = 60)
    private String city;

    /**
     * Minimo 10 caracteres con letras y numeros. Maximo 64: BCrypt solo usa los
     * primeros 72 bytes, asi que una clave mas larga daria una falsa sensacion de seguridad.
     */
    @NotBlank
    @Size(min = 10, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "{validacion.clave.debil}")
    private String password;

    @NotBlank
    private String passwordConfirmation;

    /** Autorizacion de tratamiento de datos personales (Ley 1581 de 2012). */
    @AssertTrue(message = "{validacion.habeas.data}")
    private boolean acceptsDataPolicy;

    @AssertTrue(message = "{validacion.clave.no.coincide}")
    @Schema(hidden = true)
    @JsonIgnore
    public boolean isPasswordConfirmed() {
        return password == null || password.equals(passwordConfirmation);
    }

    public User.PersonalData toPersonalData() {
        return new User.PersonalData(documentType, documentNumber, firstName, lastName, birthDate, gender,
                phone, email, city);
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public boolean isAcceptsDataPolicy() {
        return acceptsDataPolicy;
    }

    public void setAcceptsDataPolicy(boolean acceptsDataPolicy) {
        this.acceptsDataPolicy = acceptsDataPolicy;
    }
}
