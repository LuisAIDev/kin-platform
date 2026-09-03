import { api } from "./api";

export interface PatientPlan {
  id: string;
  code: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  billingPeriod: string;
  isActive: boolean;
  maxTriagesPerMonth: number | null;
  trialDays: number | null;
  maxPatients: number | null;
  triageSharing: boolean;
  viabilityScoringDetail: "BASIC" | "DETAILED";
  supportLevel: "BASIC" | "DETAILED";
  advancedAI: boolean;
  pdfExport: boolean;
  isPopular: boolean;
  displayOrder: number;
  features: string[];
}

export interface PatientPlansResponse {
  plans: PatientPlan[];
  currentPlan: PatientPlan | null;
  remainingTriages: number | null;
}

export const patientPlansService = {
  getPatientPlans: () =>
    api.get<PatientPlansResponse>("/pricing-plans/vertical/SALUD_PERSONAL"),

  createCheckoutSession: (planId: string) =>
    api.post<{ checkoutUrl: string }>(
      "/stripe/patient/create-checkout-session",
      { planId }
    ),
};

export type { PatientPlan };