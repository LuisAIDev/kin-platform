package com.kinplatform.auth.email;

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
     */
    void sendInvitationEmail(String to, String patientName, String physicianName, String specialty, String message, String invitationLink);

    /**
     * Recordatorio automático de una cita confirmada (ADR-034, scheduler diario).
     *
     * @param to            email del paciente
     * @param patientName   nombre del paciente
     * @param physicianName nombre del médico
     * @param scheduledAtText fecha/hora de la cita en texto legible
     */
    void sendAppointmentReminderEmail(String to, String patientName, String physicianName, String scheduledAtText);
}
