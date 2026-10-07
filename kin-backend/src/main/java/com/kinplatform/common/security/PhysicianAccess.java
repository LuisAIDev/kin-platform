package com.kinplatform.common.security;

import com.kinplatform.common.user.PhysicianVerificationStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRole;

/**
 * Predicado central y ÚNICA fuente de verdad para decidir si un {@link User}
 * tiene capacidad profesional de médico (PHYSICIAN), INDEPENDIENTEMENTE de su
 * persona/rol comercial ({@code users.role}).
 *
 * <p>Contexto (Alternativa B, KIN Salud 2.0): un usuario puede conservar su
 * persona actual (FREE/PREMIUM/PATIENT/…), su plan y su vertical, y ADEMÁS
 * obtener la capacidad profesional cuando un ADMIN aprueba su solicitud
 * ({@code physician_verification_status = APPROVED}). La capacidad se deriva
 * exclusivamente del estado persistido; NUNCA de datos enviados por el
 * cliente. Por eso {@code users.role} NO se modifica en la conversión.</p>
 *
 * <p>Semántica de {@code physician_verification_status} (ver
 * {@code PhysicianVerificationStatus} y la migración V29):</p>
 * <ul>
 *   <li>{@code APPROVED} → profesional habilitado, para cualquier persona.</li>
 *   <li>{@code PENDING} / {@code REJECTED} → sin capacidad profesional
 *       (el rol PHYSICIAN legacy además tiene bloqueado el login).</li>
 *   <li>{@code null} → "no sujeto a revisión": médicos provisionados por
 *       piloto/administrador con {@code role=PHYSICIAN}; equivalen a
 *       APROBADO (compatibilidad hacia atrás). Para el resto de personas,
 *       {@code null} significa "sin solicitud".</li>
 * </ul>
 *
 * <p>Única fuente de verdad: los servicios/controladores/filtros deben llamar
 * a {@link #isPhysician(User)} y NO replicar comparaciones contra
 * {@code UserRole.PHYSICIAN} ni contra el estado de verificación.</p>
 */
public final class PhysicianAccess {

    private PhysicianAccess() {}

    /**
     * Determina si el usuario tiene capacidad profesional de médico.
     *
     * @param user usuario persistido (puede ser {@code null}).
     * @return {@code true} si el usuario es un médico legacy aprobado
     *     ({@code role=PHYSICIAN} con {@code APPROVED} o {@code null}) o si
     *     cualquier otra persona tiene {@code physician_verification_status
     *     = APPROVED}.
     */
    public static boolean isPhysician(User user) {
        if (user == null) {
            return false;
        }
        PhysicianVerificationStatus status = user.getPhysicianVerificationStatus();
        if (user.getRole() == UserRole.PHYSICIAN) {
            // Semántica actual (V29): null = médico provisionado/aprobado.
            return status == null || status == PhysicianVerificationStatus.APPROVED;
        }
        // Persona FREE/PREMIUM/FACILITADOR/PATIENT/ADMIN con capacidad aprobada.
        return status == PhysicianVerificationStatus.APPROVED;
    }
}

