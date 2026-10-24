import { api } from "./api";

export interface PricingPlan {
  id: string;
  code: string | null;
  name: string;
  description: string;
  price: number;
  priceCop?: number | null;
  features: string[];
  maxProjects: number | null;
  messagesPerMonth: number | null;
  aiBudgetUsd: number | null;
  advancedAI: boolean;
  pdfExport: boolean;
  supportLevel: "BASIC" | "PREMIUM" | "SUPPORT_24_7";
  viabilityScoringDetail: "BASIC" | "DETAILED";
  isActive: boolean;
  vertical?: "EMPRESAS" | "SALUD_PERSONAL" | "SALUD_PROFESIONAL";
  trialDays?: number | null;
}

export interface UpdatePricingPlanRequest {
  name: string;
  description: string;
  price: number;
  features: string[];
  maxProjects: number | null;
  messagesPerMonth: number | null;
  advancedAI: boolean;
  pdfExport: boolean;
  supportLevel: "BASIC" | "PREMIUM" | "SUPPORT_24_7";
  viabilityScoringDetail: "BASIC" | "DETAILED";
  isActive: boolean;
}

export const pricingService = {
  getAll: () => api.get<PricingPlan[]>("/pricing-plans/vertical/EMPRESAS"),

  update: (id: string, data: UpdatePricingPlanRequest) =>
    api.put<PricingPlan>(`/admin/pricing-plans/${id}`, data),
};
