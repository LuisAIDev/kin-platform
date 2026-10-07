package com.kinplatform.common.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request de renovación de token (fase de producción). El refresh token se
 * envía en el body y solo sirve para emitir un nuevo access token.
 */
public record RefreshTokenRequest(@NotBlank(message = "refreshToken es obligatorio") String refreshToken) {}

