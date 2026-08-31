import { api } from "./api";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "./triage";

export type RelationshipStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "ENDED";

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
}

export interface PatientInviteRequest {
  patientEmail: string;
  message?: string;
}

export const physicianService = {
  patients: (page = 0, size = 10, status?: "ACTIVE" | "PENDING" | "ALL") =>
    api.get<PageResponse<PhysicianPatientSummary>>(
      `/health/physician/patients?page=${page}&size=${size}${status ? `&status=${status}` : ""}`,
    ),

  patientSummary: (patientId: string) =>
    api.get<PhysicianPatientSummary>(`/health/physician/patients/${patientId}/summary`),

  patientHistory: (patientId: string) =>
    api.get<TriageHistoryEntry[]>(`/health/physician/patients/${patientId}/history`),

  alerts: () => api.get<ClinicalAlert[]>("/health/physician/alerts"),

  acknowledgeAlert: (alertId: string) =>
    api.post<ClinicalAlert>(`/health/physician/alerts/${alertId}/acknowledge`, {}),

  invitePatient: (patientEmail: string, message?: string) =>
    api.post<Invitation>("/health/physician/patients/invite", {
      patientEmail,
      message: message ?? "",
    }),
};
