package com.kinplatform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** Estados del registro de médico ({@code POST /auth/register/physician}), HTTP 201. */
    public static final String STATE_ACCOUNT_ALREADY_VERIFIED = "ACCOUNT_ALREADY_VERIFIED";
    public static final String STATE_ACCOUNT_NOT_VERIFIED = "ACCOUNT_NOT_VERIFIED";
    public static final String STATE_PHYSICIAN_PENDING = "PHYSICIAN_PENDING";
    public static final String STATE_PHYSICIAN_APPROVED = "PHYSICIAN_APPROVED";
    public static final String STATE_PHYSICIAN_REJECTED = "PHYSICIAN_REJECTED";
    public static final String STATE_NEW_REGISTRATION = "NEW_REGISTRATION";

    private String token;
    private String email;
    private String fullName;
    private String role;
    private Boolean emailVerified;

    /** Estado de verificación de identidad (médicos): PENDING/APPROVED/REJECTED o null. */
    private String verificationStatus;

    /**
     * Capacidad profesional derivada (Alternativa B): {@code true} si el
     * usuario puede operar como médico ({@code PhysicianAccess.isPhysician}),
     * INDEPENDIENTEMENTE de {@code role}. Nunca la decide el frontend.
     */
    private boolean physicianCapability;

    /**
     * Verticales (productos) a las que tiene acceso este usuario.
     * - FREE, PREMIUM, FACILITADOR, ADMIN → "empresas"
     * - PATIENT, PHYSICIAN, o physicianCapability == true → "medical"
     * - Un usuario puede tener ambos valores.
     */
    private List<String> verticalAccess;

    /**
     * Estado real del registro de médico (register/physician): permite a la UI
     * distinguir email existente (ACCOUNT_ALREADY_VERIFIED/ACCOUNT_NOT_VERIFIED/
     * PHYSICIAN_PENDING/APPROVED/REJECTED) de un registro nuevo
     * (NEW_REGISTRATION) sin cambiar el HTTP 201 (anti-enumeración de estado).
     */
    private String state;
}
