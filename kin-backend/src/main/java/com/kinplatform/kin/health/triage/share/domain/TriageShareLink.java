package com.kinplatform.kin.health.triage.share.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Enlace temporal de compartición de un informe de triaje (ADR, feature de
 * KIN Salud Personal+).
 *
 * <p>Un paciente genera un token UUID impredecible que permite a un médico
 * externo (sin cuenta en KIN) consultar el contenido de UN triaje concreto
 * durante una ventana de tiempo ({@code expiresAt}). El enlace es reutilizable
 * dentro de esa ventana y puede revocarse manualmente ({@code revokedAt}).</p>
 */
public record TriageShareLink(
        UUID id,
        UUID triageId,
        UUID patientId,
        String token,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime revokedAt,
        UUID createdBy) {

    public TriageShareLink {
        if (id == null) {
            throw new IllegalArgumentException("id no puede ser null");
        }
        if (triageId == null) {
            throw new IllegalArgumentException("triageId no puede ser null");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("patientId no puede ser null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token no puede ser null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt no puede ser null");
        }
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        createdBy = createdBy == null ? patientId : createdBy;
    }

    public boolean isActive(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public static TriageShareLink of(
            UUID id,
            UUID triageId,
            UUID patientId,
            String token,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt,
            OffsetDateTime revokedAt,
            UUID createdBy) {
        return new TriageShareLink(id, triageId, patientId, token, expiresAt, createdAt, revokedAt, createdBy);
    }
}
