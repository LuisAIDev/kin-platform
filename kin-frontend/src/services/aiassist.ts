import { api } from "./api";

export type AIAssistType = "SUMMARY" | "ORGANIZE" | "PREPARE" | "EXPLAIN" | "DRAFT";

export interface AIAssistRequest {
  type: AIAssistType;
  inputData: string;
  patientId: string;
  context?: string;
}

export interface AIAssistResponse {
  id: string;
  type: AIAssistType;
  inputData: string;
  response: string;
  timestamp: string;
  userId: string;
  patientId: string;
  context: string;
}

export const aiassistService = {
  async generateSummary(physicianId: string, patientId: string): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/health/aiassist/patients/${patientId}/summary`,
      { type: "SUMMARY", patientId, context: "resumen previo a consulta" }
    );
  },

  async prepareConsultation(physicianId: string, patientId: string): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/health/aiassist/patients/${patientId}/consultation-prep`,
      { type: "PREPARE", patientId, context: "preparacion de consulta" }
    );
  },

  async draftMessage(physicianId: string, patientId: string, recommendation: string): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/health/aiassist/patients/${patientId}/draft-message`,
      { type: "DRAFT", patientId, context: "redaccion de mensaje", recommendation }
    );
  },

  async explainDifferential(patientId: string, differentialResult: any): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/health/aiassist/differential-explain`,
      { type: "EXPLAIN", patientId, context: "explicacion diagnostico", differentialResult }
    );
  },

  async getHistory(patientId: string): Promise<AIAssistResponse[]> {
    return api.get<AIAssistResponse[]>(`/health/aiassist/patients/${patientId}/history`);
  },

  async getMyHistory(): Promise<AIAssistResponse[]> {
    return api.get<AIAssistResponse[]>("/health/aiassist/my/history");
  },
};