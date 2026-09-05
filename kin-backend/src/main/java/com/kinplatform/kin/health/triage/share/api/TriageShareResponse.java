package com.kinplatform.kin.health.triage.share.api;

import java.time.OffsetDateTime;

/**
 * Respuesta al crear (o reutilizar) un enlace de compartición.
 * {@code url} es la ruta pública en el frontend que el paciente copia
 * para enviar al médico externo.
 */
public record TriageShareResponse(String token, String url, OffsetDateTime expiresAt) {}
