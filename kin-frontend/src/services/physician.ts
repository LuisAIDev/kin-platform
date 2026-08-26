import { api } from "./api";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "./triage";

export interface PhysicianPatientSummary {
  patientId: string;
  patientName: string;
  activeConditions: string[];
  riskFactors: string[];
  chronicConditions: string[];
  totalTriages: number;
  lastTriageAt: string | null;
  activeAlerts: number;
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

export const physicianService = {
  patients: (page = 0, size = 10) =>
    api.get<PageResponse<PhysicianPatientSummary>>(
      `/health/physician/patients?page=${page}&size=${size}`,
    ),

  patientSummary: (patientId: string) =>
    api.get<PhysicianPatientSummary>(`/health/physician/patients/${patientId}/summary`),

  patientHistory: (patientId: string) =>
    api.get<TriageHistoryEntry[]>(`/health/physician/patients/${patientId}/history`),

  alerts: () => api.get<ClinicalAlert[]>("/health/physician/alerts"),

  acknowledgeAlert: (alertId: string) =>
    api.post<ClinicalAlert>(`/health/physician/alerts/${alertId}/acknowledge`, {}),
};
