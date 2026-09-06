package com.kinplatform.common.security;

import com.kinplatform.user.User;
import com.kinplatform.user.UserRole;

/**
 * Predicado central y ÚNICA fuente de verdad para decidir si un {@link User}
 * tiene capacidad de paciente, INDEPENDIENTEMENTE de su persona/rol comercial
 * ({@code users.role}).
 *
 * <p>Contexto (ADR-040): espejo de {@link PhysicianAccess} (ADR-039) para la
 * vertical Salud. Un usuario puede conservar su persona actual
 * (FREE/PREMIUM/PHYSICIAN/…) y ADEMÁS tener capacidad de paciente. La
 * capacidad se deriva exclusivamente del estado persistido; NUNCA de datos
 * enviados por el cliente. Por eso {@code users.role} NO se modifica.</p>
 *
 * <p>Regla: un usuario es paciente si</p>
 * <ul>
 *   <li>su rol es {@code PATIENT} (legacy, compatibilidad hacia atrás), o</li>
 *   <li>dio consentimiento explícito para el tratamiento de datos de salud
 *       ({@code health_data_consent = true}, migración V29).</li>
 * </ul>
 *
 * <p>Única fuente de verdad: los servicios/controladores/filtros deben llamar
 * a {@link #isPatient(User)} y NO replicar comparaciones contra
 * {@code UserRole.PATIENT}.</p>
 */
public final class PatientAccess {

    private PatientAccess() {}

    /**
     * Determina si el usuario tiene capacidad de paciente.
     *
     * @param user usuario persistido (puede ser {@code null}).
     * @return {@code true} si el usuario es {@code PATIENT} legacy o si tiene
     *     {@code health_data_consent = true}.
     */
    public static boolean isPatient(User user) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == UserRole.PATIENT) {
            // Legacy: los pacientes creados como rol PATIENT siempre lo son,
            // aunque por un dato histórico no tengan el consentimiento en true.
            return true;
        }
        return Boolean.TRUE.equals(user.getHealthDataConsent());
    }
}
