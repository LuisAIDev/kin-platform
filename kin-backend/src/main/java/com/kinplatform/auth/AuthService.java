package com.kinplatform.auth;

import com.kinplatform.auth.dto.AuthResponse;
import com.kinplatform.auth.dto.LoginRequest;
import com.kinplatform.auth.dto.RegisterRequest;
import com.kinplatform.auth.dto.UserDTO;
import com.kinplatform.auth.verification.VerifyEmailOutcome;

public interface AuthService {

    /** Registra la cuenta (emailVerified=false) y envía el correo de verificación. No entrega JWT. */
    AuthResponse register(RegisterRequest request);

    /** Login: solo emite JWT si el correo está verificado. */
    AuthResponse login(LoginRequest request);

    /**
     * Renueva el access token a partir de un refresh token válido (fase de
     * producción). Devuelve {@code null} si el refresh es inválido.
     */
    String refreshAccessToken(String refreshToken);

    UserDTO getCurrentUser(String token);

    void logout(String token);

    /** Verifica un token de correo. Devuelve el desenlace (éxito, inválido, expirado o ya usado). */
    VerifyEmailOutcome verifyEmail(String token);

    /** Reenvía el correo de verificación (respuesta genérica, cooldown, sin enumeración de usuarios). */
    void resendVerification(String email);
}
