package com.kinplatform.auth.verification;

/** Resultado de intentar verificar un token de correo. */
public enum VerifyEmailOutcome {
    SUCCESS,
    INVALID,
    EXPIRED,
    ALREADY_USED
}
