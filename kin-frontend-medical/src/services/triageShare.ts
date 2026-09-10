import { api } from "./api";

export interface TriageShareResponse {
  token: string;
  url: string;
  expiresAt: string;
}

export interface SharedCondition {
  name: string;
  description: string;
  probability: number;
  severity: string | null;
  urgency: string | null;
  recommendation: string;
}

export interface SharedTriageContent {
  patientName: string;
  triageDate: string;
  symptoms: string[];
  conditions: SharedCondition[];
  disclaimer: string;
}

export const triageShareService = {
  /** Crea o reutiliza el enlace de un triaje del paciente autenticado. */
  createShare: (triageId: string) =>
    api.post<TriageShareResponse>(`/medical/triage/${triageId}/share`, {}),

  /** Revoca el enlace de un triaje del paciente autenticado. */
  revokeShare: (triageId: string) =>
    api.delete<void>(`/medical/triage/${triageId}/share`),

  /** Contenido público de un token (sin autenticación). */
  getPublicContent: (token: string) =>
    api.get<SharedTriageContent>(`/medical/triage/share/${token}`),
};
