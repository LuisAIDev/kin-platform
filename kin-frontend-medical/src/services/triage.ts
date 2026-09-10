import { api } from "./api";

export interface TriageSymptom {
  id: string;
  name: string;
  description: string | null;
  icdCode: string | null;
}

export type Severity = "LEVE" | "MODERADO" | "GRAVE";
export type Urgency = "BAJA" | "MEDIA" | "ALTA";

export interface TriageConditionResult {
  conditionId: string;
  condition: string;
  description: string;
  probability: number;
  severity: Severity;
  urgency: Urgency;
  recommendation: string;
  matchedSymptoms: string[];
}

export interface TriageResponse {
  status: "SUCCESS" | "NO_MATCH";
  results: TriageConditionResult[];
  unrecognizedSymptoms: string[];
  disclaimer: string;
}

export interface TriageHistoryEntry {
  id: string;
  symptoms: string[];
  results: TriageConditionResult[];
  createdAt: string;
}

export interface TriageRequest {
  symptoms: string[];
}

export const triageService = {
  listSymptoms: () => api.get<TriageSymptom[]>("/medical/triage/symptoms"),

  analyze: (symptoms: string[]) =>
    api.post<TriageResponse>("/medical/triage", { symptoms } as TriageRequest),

  history: () => api.get<TriageHistoryEntry[]>("/medical/triage/history"),
};
