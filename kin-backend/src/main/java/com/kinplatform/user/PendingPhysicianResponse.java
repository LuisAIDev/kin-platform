package com.kinplatform.user;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO de médico pendiente de verificación (panel de ADMIN).
 */
@Data
@AllArgsConstructor
@Builder
public class PendingPhysicianResponse {

    private UUID id;
    private String email;
    private String fullName;

    /** Persona/rol ORIGINAL del solicitante (FREE/PREMIUM/PATIENT/PHYSICIAN). No se modifica al decidir. */
    private String role;

    private String licenseNumber;
    private String specialty;
    private String country;
    private String phone;
    private OffsetDateTime createdAt;
}
