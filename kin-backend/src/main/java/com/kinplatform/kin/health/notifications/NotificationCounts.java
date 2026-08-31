package com.kinplatform.kin.health.notifications;

/**
 * Contadores de notificaciones dentro de la plataforma, agregados por rol.
 *
 * <p>Devueltos por {@code GET /api/v1/health/notifications/counts}: el paciente
 * ve invitaciones pendientes, mensajes no leídos, próximas citas, tareas de
 * seguimiento pendientes y documentos compartidos; el médico ve mensajes no
 * leídos, citas pendientes de confirmación, alertas de alta urgencia y tareas
 * de seguimiento vencidas. Alimentan los badges del sidebar.</p>
 */
public record NotificationCounts(
        int invitations,
        long unreadMessages,
        long pendingAppointments,
        long upcomingAppointments,
        long highUrgencyAlerts,
        long pendingTasks,
        long overdueTasks,
        long documents) {

    public static NotificationCounts empty() {
        return new NotificationCounts(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** Total de novedades (para el badge agregado, si se desea). */
    public long total() {
        return invitations + unreadMessages + pendingAppointments + upcomingAppointments + highUrgencyAlerts
                + pendingTasks + overdueTasks + documents;
    }
}
