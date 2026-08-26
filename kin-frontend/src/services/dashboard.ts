import { api } from "./api";
import type { PageResponse } from "@/types";
import type { TriageHistoryEntry } from "./triage";

export interface HealthSummary {
  totalConsultations: number;
  totalDifferentials: number;
  topConditions: { name: string; occurrences: number }[];
  lastTriageAt: string | null;
  activeReminders: number;
}

export interface PatientProfile {
  userId: string;
  riskFactors: string[];
  chronicConditions: string[];
  updatedAt: string;
}

export interface CarePlan {
  recommendations: string[];
  sourceConditions: string[];
  detailed: { condition: string; advice: string; priority: string }[];
}

export type ReminderType = "CITA" | "MEDICACION" | "GENERAL";

export interface Reminder {
  id: string;
  type: ReminderType;
  title: string;
  scheduledAt: string;
  active: boolean;
  createdAt: string;
}

export const RISK_FACTOR_OPTIONS = [
  "fumador",
  "embarazo",
  "edad avanzada",
  "obesidad",
  "inmunodepresión",
  "alcohol",
  "diabetes",
  "hipertensión",
];

export const dashboardService = {
  summary: () => api.get<HealthSummary>("/health/dashboard/summary"),

  history: (page = 0, size = 10) =>
    api.get<PageResponse<TriageHistoryEntry>>(
      `/health/dashboard/history?page=${page}&size=${size}`,
    ),

  consultationDetail: (id: string) =>
    api.get<TriageHistoryEntry>(`/health/dashboard/history/${id}`),

  profile: () => api.get<PatientProfile>("/health/dashboard/profile"),

  updateProfile: (riskFactors: string[], chronicConditions: string[]) =>
    api.put<PatientProfile>("/health/dashboard/profile", {
      riskFactors,
      chronicConditions,
    }),

  carePlan: () => api.get<CarePlan>("/health/dashboard/care-plan"),

  reminders: () => api.get<Reminder[]>("/health/dashboard/reminders"),

  createReminder: (type: ReminderType, title: string, scheduledAt: string) =>
    api.post<Reminder>("/health/dashboard/reminders", { type, title, scheduledAt }),
};
