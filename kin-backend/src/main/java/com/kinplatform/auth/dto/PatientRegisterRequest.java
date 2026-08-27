package com.kinplatform.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/**
 * Auto-registro de paciente (vertical Salud).
 *
 * <p>Incluye datos personales de salud (fecha de nacimiento, sexo) y exige el
 * consentimiento explícito para su tratamiento.</p>
 */
@Data
public class PatientRegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 180, message = "Full name must be between 2 and 180 characters")
    private String fullName;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Sexo inválido")
    private String sex;

    @Size(max = 30, message = "Teléfono inválido")
    private String phone;

    @AssertTrue(message = "Debes aceptar el consentimiento para el tratamiento de tus datos de salud")
    private Boolean healthDataConsent;
}
