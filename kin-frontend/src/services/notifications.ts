import { api } from "./api";

/**
 * Contadores de notificaciones dentro de la plataforma, por rol.
 * - Paciente: invitaciones pendientes, mensajes no leídos, próximas citas,
 *   tareas de seguimiento pendientes.
 * - Médico: mensajes no leídos, citas pendientes de confirmación, alertas ALTA,
 *   tareas de seguimiento vencidas.
 */
export interface NotificationCounts {
  invitations: number;
  unreadMessages: number;
  pendingAppointments: number;
  upcomingAppointments: number;
  highUrgencyAlerts: number;
  pendingTasks: number;
  overdueTasks: number;
}

export const notificationService = {
  counts: () => api.get<NotificationCounts>("/health/notifications/counts"),
};
