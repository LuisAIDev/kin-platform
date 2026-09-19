import { api } from "./api";

/**
 * Plan comercial de la vertical SALUD_PROFESIONAL (respuesta de
 * GET /pricing-plans/vertical/SALUD_PROFESIONAL). Coincide con
 * PricingPlanResponse del backend.
 */
export interface PhysicianPlan {
  id: string;
  code: string;
  name: string;
  description: string;
  price: number;
  priceCop: number | null;
  features: string[];
  maxProjects: number | null;
  messagesPerMonth: number | null;
  advancedAI: boolean;
  pdfExport: boolean;
  supportLevel: "BASIC" | "PREMIUM" | "SUPPORT_24_7";
  viabilityScoringDetail: "BASIC" | "DETAILED";
  isActive: boolean;
  vertical: "EMPRESAS" | "SALUD_PERSONAL" | "SALUD_PROFESIONAL";
  maxTriagesPerMonth: number | null;
  trialDays: number | null;
  maxPatients: number | null;
  triageSharing: boolean;
}

export const physicianPlansService = {
  getPhysicianPlans: () =>
    api.get<PhysicianPlan[]>("/pricing-plans/vertical/SALUD_PROFESIONAL"),

  createCheckoutSession: (planId: string) =>
    api.post<{ sessionId: string; url: string }>("/stripe/create-checkout-session", {
      planId,
    }),
};
