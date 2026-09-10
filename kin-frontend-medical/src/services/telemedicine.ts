import { api } from "./api";

export interface TelemedicineMessage {
  id: string;
  senderId: string;
  receiverId: string;
  conversationId: string;
  content: string;
  read: boolean;
  mine: boolean;
  createdAt: string;
}

export interface Conversation {
  otherId: string;
  otherName: string;
  lastMessage: string;
  lastMessageAt: string;
  unread: number;
}

export interface Contact {
  id: string;
  name: string;
  role: string;
}

export type AppointmentStatus = "PENDIENTE" | "CONFIRMADA" | "CANCELADA" | "COMPLETADA";

export interface Appointment {
  id: string;
  patientId: string;
  physicianId: string;
  scheduledAt: string;
  reason: string;
  status: AppointmentStatus;
  createdAt: string;
}

export const telemedicineService = {
  conversations: () => api.get<Conversation[]>("/medical/telemedicine/conversations"),

  contacts: () => api.get<Contact[]>("/medical/telemedicine/contacts"),

  messages: (withId: string) =>
    api.get<TelemedicineMessage[]>(`/medical/telemedicine/messages?with=${withId}`),

  sendMessage: (receiverId: string, content: string) =>
    api.post<TelemedicineMessage>("/medical/telemedicine/messages", { receiverId, content }),

  unread: () => api.get<{ unread: number }>("/medical/telemedicine/unread"),

  requestAppointment: (physicianId: string, scheduledAt: string, reason: string) =>
    api.post<Appointment>("/medical/telemedicine/appointments", {
      physicianId,
      scheduledAt,
      reason,
    }),

  updateAppointmentStatus: (appointmentId: string, status: AppointmentStatus) =>
    api.put<Appointment>(`/medical/telemedicine/appointments/${appointmentId}/status`, { status }),

  appointments: () => api.get<Appointment[]>("/medical/telemedicine/appointments"),
};
