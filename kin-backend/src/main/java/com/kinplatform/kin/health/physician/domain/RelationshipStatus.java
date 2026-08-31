package com.kinplatform.kin.health.physician.domain;

/**
 * Estado del ciclo de vida de la relación médico-paciente.
 *
 * <p>V30 amplía la asignación binaria original (ADR-031) a un ciclo de vida
 * con invitación y aceptación:
 * <ul>
 *   <li>{@code PENDING} — el médico invitó al paciente, pendiente de aceptación.</li>
 *   <li>{@code ACTIVE} — relación activa (aceptada por el paciente o creada
 *       directamente por ADMIN/piloto). Es el estado que habilita mensajería
 *       y citas.</li>
 *   <li>{@code SUSPENDED} — relación suspendida temporalmente.</li>
 *   <li>{@code ENDED} — relación finalizada (rechazo, baja, etc.).</li>
 * </ul>
 * Solo {@code ACTIVE} habilita el acceso clínico y la comunicación.</p>
 */
public enum RelationshipStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    ENDED
}
