import { api } from "./api";
import type { Severity, Urgency } from "./triage";

export interface DifferentialRiskFactor {
  factor: string;
  weight: number;
  description: string | null;
}

export interface DifferentialRecommendedTest {
  test: string;
  description: string | null;
}

export interface DifferentialItem {
  conditionId: string;
  condition: string;
  description: string;
  probability: number;
  severity: Severity;
  urgency: Urgency;
  matchedSymptoms: string[];
  riskFactors: DifferentialRiskFactor[];
  recommendedTests: DifferentialRecommendedTest[];
  reasoning: string;
}

export interface DifferentialResponse {
  status: "SUCCESS" | "NO_MATCH";
  items: DifferentialItem[];
  unrecognizedSymptoms: string[];
  disclaimer: string;
}

export interface DifferentialRequest {
  symptoms: string[];
  riskFactors?: string[];
}

export const differentialService = {
  analyze: (symptoms: string[], riskFactors: string[] = []) =>
    api.post<DifferentialResponse>("/medical/differential", {
      symptoms,
      riskFactors,
    } as DifferentialRequest),

  byConsultation: (consultationId: string, riskFactors: string[] = []) =>
    api.get<DifferentialResponse>(
      `/medical/differential?consultationId=${consultationId}${
        riskFactors.length ? `&riskFactors=${riskFactors.join(",")}` : ""
      }`,
    ),
};
