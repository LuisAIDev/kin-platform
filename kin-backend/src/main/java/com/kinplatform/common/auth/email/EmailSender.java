package com.kinplatform.common.auth.email;

/**
 * Puerta de salida para el envío de correos (verificación de email,
 * recuperación de contraseña e invitación de un médico a un paciente, ADR-031).
 */
public interface EmailSender {

    void sendVerificationEmail(String to, String fullName, String verificationLink);

    void sendPasswordResetEmail(String to, String fullName, String resetLink);

    /**
     * Notifica a un paciente que un médico lo ha invitado a vincularse en KIN
     * Salud (ciclo de vida de relación V30).
     *
     * @param to             email del paciente
     * @param patientName    nombre del paciente (o su email si no tiene nombre)
     * @param physicianName  nombre del médico que invita
     * @param specialty      especialidad del médico (puede ser {@code null})
     * @param message        mensaje opcional del médico (puede ser {@code null})
     * @param invitationLink enlace al panel de invitaciones del paciente
     * @param consentRequired {@code true} si el paciente aún no tiene la
     *     capacidad de paciente (falta aceptar el consentimiento de datos de
     *     salud); el correo debe explicarlo explícitamente
     */
    void sendInvitationEmail(
            String to,
            String patientName,
            String physicianName,
            String specialty,
            String message,
            String invitationLink,
            boolean consentRequired);

    /**
     * Recordatorio de una invitación pendiente cuando el médico REENVÍA la
     * invitación a un paciente que aún no la ha aceptado. Mismos datos que
     * {@link #sendInvitationEmail}, pero con asunto y texto de recordatorio.
     */
    void sendInvitationReminderEmail(
            String to,
            String patientName,
            String physicianName,
            String specialty,
            String message,
            String invitationLink,
            boolean consentRequired);

    /**
     * Recordatorio automático de una cita confirmada (ADR-034, scheduler diario).
     *
     * @param to            email del paciente
     * @param patientName   nombre del paciente
     * @param physicianName nombre del médico
     * @param scheduledAtText fecha/hora de la cita en texto legible
     */
    void sendAppointmentReminderEmail(String to, String patientName, String physicianName, String scheduledAtText);

    /**
     * Notificación interna cuando una IPS/clínica solicita acceso al programa
     * Beta (formulario público de la landing).
     *
     * @param to      destinatario interno (contacto@kin-platform.com)
     * @param summary resumen de la solicitud (datos del lead)
     */
    void sendInstitutionalInquiryNotification(String to, String summary);

    /**
     * Respuesta automática al solicitante de acceso Beta para confirmar la
     * recepción de su solicitud.
     *
     * @param to          email del solicitante
     * @param contactName nombre del contacto
     */
    void sendInstitutionalInquiryAutoReply(String to, String contactName);
}

