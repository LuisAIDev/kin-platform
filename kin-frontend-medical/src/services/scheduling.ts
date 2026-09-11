import { api } from "./api";

export type AppointmentStatus = "PENDIENTE" | "CONFIRMADA" | "CANCELADA" | "COMPLETADA" | "REPROGRAMADA";

export interface PhysicianAvailability {
  id: string;
  physicianId: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  active: boolean;
}

export interface SchedulingAppointment {
  id: string;
  patientId: string;
  physicianId: string;
  scheduledAt: string;
  durationMinutes: number;
  reason: string;
  status: AppointmentStatus;
  createdAt: string;
  rescheduledFrom: string | null;
  cancellationReason: string;
  availabilitySlotId: string | null;
}

export interface SetAvailabilityRequest {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  active?: boolean;
}

export interface RequestAppointmentRequest {
  physicianId: string;
  scheduledAt: string;
  durationMinutes?: number;
  reason?: string;
}

export const schedulingService = {
  // Médico: disponibilidad
  setAvailability: (req: SetAvailabilityRequest) =>
    api.post<PhysicianAvailability>("/medical/scheduling/availability", req),

  getAvailability: () => api.get<PhysicianAvailability[]>("/medical/scheduling/availability"),

  removeAvailability: (availabilityId: string) =>
    api.delete<void>(`/medical/scheduling/availability/${availabilityId}`),

  // Slots
  slotsForPatient: (patientId: string, date: string) =>
    api.get<[string]>(`/medical/scheduling/patients/${patientId}/slots?date=${date}`),

  slotsForPhysician: (physicianId: string, date: string) =>
    api.get<[string]>(`/medical/scheduling/physicians/${physicianId}/slots?date=${date}`),

  // Citas
  requestAppointment: (req: RequestAppointmentRequest) =>
    api.post<SchedulingAppointment>("/medical/scheduling/appointments/request", req),

  confirmAppointment: (appointmentId: string) =>
    api.put<SchedulingAppointment>(`/medical/scheduling/appointments/${appointmentId}/confirm`, {}),

  cancelAppointment: (appointmentId: string, reason?: string) =>
    api.put<SchedulingAppointment>(`/medical/scheduling/appointments/${appointmentId}/cancel`, { reason }),

  rescheduleAppointment: (appointmentId: string, newScheduledAt: string, reason?: string) =>
    api.put<SchedulingAppointment>(`/medical/scheduling/appointments/${appointmentId}/reschedule`, {
      newScheduledAt,
      reason,
    }),

  completeAppointment: (appointmentId: string) =>
    api.put<SchedulingAppointment>(`/medical/scheduling/appointments/${appointmentId}/complete`, {}),

  upcomingAppointments: () => api.get<SchedulingAppointment[]>("/medical/scheduling/appointments/upcoming"),

  appointmentHistory: () => api.get<SchedulingAppointment[]>("/medical/scheduling/appointments/history"),
};
