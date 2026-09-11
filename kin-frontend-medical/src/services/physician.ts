import { api } from "./api";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "./triage";

export type RelationshipStatus =
  | "PENDING"
  | "PENDING_CONSENT"
  | "ACTIVE"
  | "SUSPENDED"
  | "ENDED";

export interface PhysicianPatientSummary {
  patientId: string;
  patientName: string;
  activeConditions: string[];
  riskFactors: string[];
  chronicConditions: string[];
  totalTriages: number;
  lastTriageAt: string | null;
  activeAlerts: number;
  relationshipStatus: RelationshipStatus;
}

export interface ClinicalAlert {
  id: string;
  patientId: string;
  type: string;
  severity: "ALTA" | "MEDIA" | "BAJA";
  message: string;
  status: "PENDING" | "ACKNOWLEDGED";
  createdAt: string;
  acknowledgedAt: string | null;
}

export interface Invitation {
  physicianId: string;
  patientId: string;
  status: RelationshipStatus;
  invitedAt: string | null;
  patientEmail: string;
  /** true si la invitación era un reenvío (ya existía PENDING/PENDING_CONSENT). */
  resent: boolean;
}

export interface PatientInviteRequest {
  patientEmail: string;
  message?: string;
}

/** Solicitud de capacidad profesional (médico) enviada por un usuario existente. */
export interface PhysicianApplicationRequest {
  licenseNumber: string;
  specialty: string;
  country: string;
  phone?: string;
  healthDataConsent: boolean;
}

export interface PhysicianApplicationStatus {
  /** PENDING | APPROVED | REJECTED | NOT_FOUND */
  status: string;
  role: string | null;
  licenseNumber: string | null;
  specialty: string | null;
  country: string | null;
  phone: string | null;
  physicianVerificationStatus: string | null;
}

export const physicianService = {
  patients: (page = 0, size = 10, status?: "ACTIVE" | "PENDING" | "ALL") =>
    api.get<PageResponse<PhysicianPatientSummary>>(
      `/medical/physician/patients?page=${page}&size=${size}${status ? `&status=${status}` : ""}`,
    ),

  patientSummary: (patientId: string) =>
    api.get<PhysicianPatientSummary>(`/medical/physician/patients/${patientId}/summary`),

  patientHistory: (patientId: string) =>
    api.get<TriageHistoryEntry[]>(`/medical/physician/patients/${patientId}/history`),

  alerts: () => api.get<ClinicalAlert[]>("/medical/physician/alerts"),

  acknowledgeAlert: (alertId: string) =>
    api.post<ClinicalAlert>(`/medical/physician/alerts/${alertId}/acknowledge`, {}),

  invitePatient: (patientEmail: string, message?: string) =>
    api.post<Invitation>("/medical/physician/patients/invite", {
      patientEmail,
      message: message ?? "",
    }),

  // Solicitud de capacidad profesional (accesible a cualquier usuario autenticado).
  applyAsPhysician: (data: PhysicianApplicationRequest) =>
    api.post<{ status: string }>("/medical/physician/application", data),

  applicationStatus: () =>
    api.get<PhysicianApplicationStatus>("/medical/physician/application"),
};
