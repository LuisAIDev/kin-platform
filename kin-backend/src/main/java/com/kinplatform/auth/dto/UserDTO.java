package com.kinplatform.auth.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
    private Integer credits;
    private Boolean emailVerified;

    /** Estado de verificación de identidad (médicos): PENDING/APPROVED/REJECTED o null. */
    private String verificationStatus;

    /**
     * Capacidad profesional derivada (Alternativa B): {@code true} si el
     * usuario puede operar como médico ({@code PhysicianAccess.isPhysician}),
     * INDEPENDIENTEMENTE de {@code role} (FREE/PREMIUM/PATIENT + APPROVED
     * también pueden ser médicos). Nunca la decide el frontend.
     */
    private boolean physicianCapability;

    /**
     * Verticales (productos) a las que tiene acceso este usuario.
     * - FREE, PREMIUM, FACILITADOR, ADMIN → "empresas"
     * - PATIENT, PHYSICIAN, o physicianCapability == true → "medical"
     * - Un usuario puede tener ambos valores.
     */
    private List<String> verticalAccess;
}
