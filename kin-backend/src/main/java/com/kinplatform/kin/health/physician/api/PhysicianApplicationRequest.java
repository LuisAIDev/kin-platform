package com.kinplatform.kin.health.physician.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * Solicitud de capacidad profesional (médico) en KIN Salud.
 *
 * <p>El usuario autenticado envía sus datos profesionales para solicitar
 * el rol {@code PHYSICIAN}. El email se toma del usuario autenticado
 * (no se permite cambiarlo en esta solicitud).</p>
 */
@Data
@Builder
public class PhysicianApplicationRequest {

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

    @jakarta.validation.constraints.AssertTrue(message = "Debes aceptar el consentimiento para el tratamiento de tus datos de salud")
    private Boolean healthDataConsent;
}