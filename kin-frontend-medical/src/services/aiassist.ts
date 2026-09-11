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
  async generateSummary(patientId: string): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/medical/aiassist/patients/${patientId}/summary`,
      { type: "SUMMARY", patientId, context: "resumen previo a consulta" }
    );
  },

  async prepareConsultation(patientId: string): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/medical/aiassist/patients/${patientId}/consultation-prep`,
      { type: "PREPARE", patientId, context: "preparacion de consulta" }
    );
  },

  async draftMessage(patientId: string, recommendation: string): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/medical/aiassist/patients/${patientId}/draft-message`,
      { type: "DRAFT", patientId, context: "redaccion de mensaje", recommendation }
    );
  },

  async explainDifferential(patientId: string, differentialResult: any): Promise<AIAssistResponse> {
    return api.post<AIAssistResponse>(
      `/medical/aiassist/differential-explain`,
      { type: "EXPLAIN", patientId, context: "explicacion diagnostico", differentialResult }
    );
  },

  async getHistory(patientId: string): Promise<AIAssistResponse[]> {
    return api.get<AIAssistResponse[]>(`/medical/aiassist/patients/${patientId}/history`);
  },

  async getMyHistory(): Promise<AIAssistResponse[]> {
    return api.get<AIAssistResponse[]>("/medical/aiassist/my/history");
  },
};