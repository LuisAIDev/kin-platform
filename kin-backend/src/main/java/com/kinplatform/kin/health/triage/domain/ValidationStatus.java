package com.kinplatform.kin.health.triage.domain;

/**
 * Estado de validación clínica de una condición (ADR-028, fase de
 * consolidación). Audita la calidad de los datos del catálogo:
 * <ul>
 *   <li>{@code PENDING}: sin revisión (importado o nuevo).</li>
 *   <li>{@code REVIEWED}: revisado por un profesional de la salud.</li>
 *   <li>{@code APPROVED}: validado y apto para uso en producción.</li>
 *   <li>{@code REJECTED}: rechazado (no debe usarse).</li>
 * </ul>
 */
public enum ValidationStatus {
    PENDING,
    REVIEWED,
    APPROVED,
    REJECTED
}
