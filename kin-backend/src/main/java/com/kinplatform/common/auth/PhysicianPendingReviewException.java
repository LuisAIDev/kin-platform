package com.kinplatform.common.auth;

/**
 * El usuario es un médico con la cuenta pendiente de verificación de identidad
 * (auto-registro). Se traduce a HTTP 403 con el código {@code ACCOUNT_PENDING_REVIEW}.
 */
public class PhysicianPendingReviewException extends RuntimeException {

    public PhysicianPendingReviewException(String message) {
        super(message);
    }
}

