package com.kinplatform.user;

/**
 * Estado de verificación de identidad de un médico.
 *
 * <p>El auto-registro de médicos crea la cuenta con rol {@code PHYSICIAN} y
 * estado {@code PENDING}: no puede iniciar sesión hasta que un administrador
 * apruebe su cédula profesional. {@code null} en la entidad equivale a "no
 * sujeto a revisión" (médicos provisionados por piloto/admin = aprobados).</p>
 */
public enum PhysicianVerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
