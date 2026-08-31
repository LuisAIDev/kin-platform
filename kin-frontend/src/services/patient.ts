import { api } from "./api";
import type { RelationshipStatus } from "./physician";

/**
 * Invitación pendiente recibida por el paciente, con datos del médico
 * (nombre y especialidad para mostrar en la UI).
 */
export interface PendingInvitation {
  physicianId: string;
  physicianName: string;
  specialty: string | null;
  invitedAt: string | null;
}

export interface RelationshipResponse {
  physicianId: string;
  patientId: string;
  status: RelationshipStatus;
  acceptedAt: string | null;
  endedAt: string | null;
  endedReason: string | null;
}

/**
 * Servicio del paciente para gestionar sus relaciones con médicos
 * (invitaciones pendientes, aceptación y rechazo).
 */
export const patientRelationshipService = {
  pendingInvitations: () =>
    api.get<PendingInvitation[]>("/health/patient/relationships/pending"),

  acceptInvitation: (physicianId: string) =>
    api.post<RelationshipResponse>("/health/patient/relationships/accept", { physicianId }),

  rejectInvitation: (physicianId: string) =>
    api.post<RelationshipResponse>("/health/patient/relationships/reject", { physicianId }),
};
