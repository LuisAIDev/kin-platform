package com.kinplatform.auth;

/**
 * Se lanza al intentar iniciar sesión con una cuenta cuyo correo aún no ha
 * sido verificado. Se traduce a HTTP 403 con código
 * {@code EMAIL_VERIFICATION_REQUIRED}.
 */
public class EmailVerificationRequiredException extends RuntimeException {

    public EmailVerificationRequiredException(String message) {
        super(message);
    }
}
