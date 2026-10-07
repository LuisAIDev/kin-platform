package com.kinplatform.common.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Auto-registro de médico (vertical Salud).
 *
 * <p>Incluye los datos de identidad profesional (cédula/licencia, especialidad,
 * país) que un administrador debe verificar antes de habilitar la cuenta.</p>
 */
@Data
public class PhysicianRegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 180, message = "Full name must be between 2 and 180 characters")
    private String fullName;

    @NotBlank(message = "El número de cédula profesional es obligatorio")
    @Size(min = 4, max = 20, message = "El número de cédula profesional debe tener entre 4 y 20 caracteres")
    private String licenseNumber;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 100, message = "Especialidad demasiado larga")
    private String specialty;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 60, message = "País demasiado largo")
    private String country;

    @Size(max = 30, message = "Teléfono inválido")
    private String phone;

    @AssertTrue(message = "Debes aceptar el consentimiento para el tratamiento de los datos de salud")
    private Boolean healthDataConsent;
}

