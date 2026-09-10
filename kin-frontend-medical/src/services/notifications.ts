export interface NotificationCounts {
  unreadMessages: number;
  upcomingAppointments: number;
  pendingAppointments: number;
  pendingTasks: number;
  overdueTasks: number;
  invitations: number;
  highUrgencyAlerts: number;
}

export const EMPTY_COUNTS: NotificationCounts = {
  unreadMessages: 0,
  upcomingAppointments: 0,
  pendingAppointments: 0,
  pendingTasks: 0,
  overdueTasks: 0,
  invitations: 0,
  highUrgencyAlerts: 0,
};