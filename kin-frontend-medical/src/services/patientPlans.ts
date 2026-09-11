import { api } from "./api";

/**
 * Plan comercial de la vertical SALUD_PERSONAL (respuesta de
 * GET /pricing-plans/vertical/SALUD_PERSONAL). Coincide con
 * PricingPlanResponse del backend.
 */
export interface PatientPlan {
  id: string;
  code: string;
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
  vertical: "EMPRESAS" | "SALUD_PERSONAL" | "SALUD_PROFESIONAL";
  maxTriagesPerMonth: number | null;
  trialDays: number | null;
  maxPatients: number | null;
  triageSharing: boolean;
}

/** Respuesta de POST /stripe/patient/create-checkout-session (CheckoutResponse). */
export interface CheckoutSessionResponse {
  sessionId: string;
  url: string;
}

/** Respuesta de GET /subscriptions/patient-status */
export interface PatientSubscriptionStatusResponse {
  isActive: boolean;
  planName: string;
  planCode: string;
  planDescription: string;
  maxTriagesPerMonth: number | null;
  triagesUsed: number;
  triagesRemaining: number | null;
  maxStorageMb: number | null;
  storageUsedMb: number;
  storageRemainingMb: number | null;
  aiBudgetUsd: number | null;
  aiBudgetUsed: number;
  aiBudgetRemaining: number | null;
  aiLevel: string;
  pdfExport: boolean;
  triageSharing: boolean;
  advancedAI: boolean;
  supportLevel: string;
  periodStart: string | null;
  periodEnd: string | null;
  subscriptionEndDate: string | null;
}

export const patientPlansService = {
  getPatientPlans: () =>
    api.get<PatientPlan[]>("/pricing-plans/vertical/SALUD_PERSONAL"),

  createCheckoutSession: (planId: string) =>
    api.post<CheckoutSessionResponse>("/stripe/patient/create-checkout-session", {
      planId,
    }),
};
