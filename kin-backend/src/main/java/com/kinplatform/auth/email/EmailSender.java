package com.kinplatform.auth.email;

/** Puerta de salida para el envío de correos (verificación de email y recuperación de contraseña). */
public interface EmailSender {

    void sendVerificationEmail(String to, String fullName, String verificationLink);

    void sendPasswordResetEmail(String to, String fullName, String resetLink);
}
