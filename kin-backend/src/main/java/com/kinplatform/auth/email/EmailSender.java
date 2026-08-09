package com.kinplatform.auth.email;

/** Puerta de salida para el envío de correos (verificación de email). */
public interface EmailSender {

    void sendVerificationEmail(String to, String fullName, String verificationLink);
}
