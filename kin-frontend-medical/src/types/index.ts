/**
 * Tipos compartidos de KIN Medical (vertical Salud en exclusiva).
 */

export type UserRole =
  | "FREE"
  | "PREMIUM"
  | "FACILITADOR"
  | "PATIENT"
  | "PHYSICIAN"
  | "ADMIN"
  | "USER";

export type PhysicianVerificationStatus = "PENDING" | "APPROVED" | "REJECTED";

/** Contexto de rol/capacidad devuelto por el backend (/auth/me, login). */
export interface PhysicianContext {
  role?: string | null;
  verificationStatus?: string | null;
  /** Capacidad profesional derivada por el backend. Fuente de verdad. */
  physicianCapability?: boolean;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: string;
  emailVerified: boolean;
  verificationStatus?: string | null;
  physicianCapability?: boolean;
}

export interface NotificationCounts {
  unreadMessages: number;
  upcomingAppointments: number;
  pendingAppointments: number;
  pendingTasks: number;
  overdueTasks: number;
  invitations: number;
  highUrgencyAlerts: number;
}
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
}
