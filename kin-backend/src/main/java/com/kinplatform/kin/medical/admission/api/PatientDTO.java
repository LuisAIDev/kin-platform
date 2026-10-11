package com.kinplatform.kin.medical.admission.api;

import com.kinplatform.kin.medical.admission.domain.Patient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public class PatientDTO {

    private UUID id;

    @NotNull(message = "documentType es obligatorio")
    private Patient.DocumentType documentType;

    @NotBlank(message = "documentNumber es obligatorio")
    @Size(min = 1, max = 20, message = "documentNumber debe tener entre 1 y 20 caracteres")
    private String documentNumber;

    @NotBlank(message = "firstName es obligatorio")
    @Size(min = 1, max = 100, message = "firstName debe tener entre 1 y 100 caracteres")
    private String firstName;

    @Size(max = 100, message = "middleName debe tener máximo 100 caracteres")
    private String middleName;

    @NotBlank(message = "lastName es obligatorio")
    @Size(min = 1, max = 100, message = "lastName debe tener entre 1 y 100 caracteres")
    private String lastName;

    @Size(max = 100, message = "secondLastName debe tener máximo 100 caracteres")
    private String secondLastName;

    @NotNull(message = "birthDate es obligatorio")
    @Past(message = "birthDate debe ser una fecha pasada")
    private LocalDate birthDate;

    @NotNull(message = "gender es obligatorio")
    private Patient.Gender gender;

    @Size(max = 30, message = "phone debe tener máximo 30 caracteres")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,30}$", message = "phone debe ser un número de teléfono válido")
    private String phone;

    @Email(message = "email debe ser un correo válido")
    @Size(max = 255, message = "email debe tener máximo 255 caracteres")
    private String email;

    @Size(max = 500, message = "address debe tener máximo 500 caracteres")
    private String address;

    @Size(max = 100, message = "city debe tener máximo 100 caracteres")
    private String city;

    @Size(max = 100, message = "department debe tener máximo 100 caracteres")
    private String department;

    @Size(max = 200, message = "eps debe tener máximo 200 caracteres")
    private String eps;

    private Patient.Regime regime;

    @Size(max = 5, message = "rhFactor debe tener máximo 5 caracteres")
    private String rhFactor;

    @Size(max = 200, message = "emergencyContactName debe tener máximo 200 caracteres")
    private String emergencyContactName;

    @Size(max = 30, message = "emergencyContactPhone debe tener máximo 30 caracteres")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,30}$", message = "emergencyContactPhone debe ser un número de teléfono válido")
    private String emergencyContactPhone;

    @Size(max = 50, message = "emergencyContactRelationship debe tener máximo 50 caracteres")
    private String emergencyContactRelationship;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Patient.DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(Patient.DocumentType documentType) {
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

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Patient.Gender getGender() {
        return gender;
    }

    public void setGender(Patient.Gender gender) {
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEps() {
        return eps;
    }

    public void setEps(String eps) {
        this.eps = eps;
    }

    public Patient.Regime getRegime() {
        return regime;
    }

    public void setRegime(Patient.Regime regime) {
        this.regime = regime;
    }

    public String getRhFactor() {
        return rhFactor;
    }

    public void setRhFactor(String rhFactor) {
        this.rhFactor = rhFactor;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getEmergencyContactRelationship() {
        return emergencyContactRelationship;
    }

    public void setEmergencyContactRelationship(String emergencyContactRelationship) {
        this.emergencyContactRelationship = emergencyContactRelationship;
    }
}