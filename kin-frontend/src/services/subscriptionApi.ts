import { api } from "./api";
import { PricingPlan } from "./pricing";
import { PatientSubscriptionStatusResponse } from "./patientPlans";

export interface SubscriptionStatus {
  isActive: boolean;
  planName: string;
  planCode: string | null;
  planDescription: string;
  remainingMessages: number;
  canCreateProject: boolean;
  aiLevel: "FLASH" | "PRO";
  messagesPerMonth: number | null;
  maxProjects: number | null;
  advancedAI: boolean;
  pdfExport: boolean;
  supportLevel: string;
  completedProjects: number;
  completedProjectsLimit: number;
  canCompleteProject: boolean;
  aiCostControlEnabled: boolean;
  aiBudgetUsed: number;
  aiBudgetReserved: number;
  aiBudgetLimit: number;
  aiBudgetRemaining: number;
  aiUsagePeriodStart: string | null;
  aiUsagePeriodEnd: string | null;
}

export interface SubscriptionResponse {
  id: string;
  userId: string;
  plan: PricingPlan;
  startDate: string;
  endDate: string | null;
  status: "ACTIVE" | "EXPIRED" | "CANCELLED" | "TRIAL";
  messagesUsed: number;
  messagesPerMonth: number | null;
  lastResetDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface CheckoutResponse {
  sessionId: string;
  url: string;
}

export const subscriptionApi = {
  getPlans: () => api.get<PricingPlan[]>("/pricing-plans"),

  /** Planes de una vertical concreta (p. ej. EMPRESAS para KIN Empresas). */
  getPlansByVertical: (vertical: string) =>
    api.get<PricingPlan[]>(`/pricing-plans/vertical/${vertical}`),

  getStatus: () => api.get<SubscriptionStatus>("/subscriptions/status"),

  getPatientStatus: () => api.get<PatientSubscriptionStatusResponse>("/subscriptions/patient-status"),

  getCurrent: () => api.get<SubscriptionResponse>("/subscriptions/current"),

  subscribe: (planId: string) =>
    api.post<SubscriptionResponse>("/subscriptions", { planId }),

  cancel: () =>
    api.post<SubscriptionResponse>("/subscriptions/cancel", {}),

  cancelPatient: () =>
    api.post<SubscriptionResponse>("/subscriptions/cancel-patient", {}),

  startTrial: () =>
    api.post<SubscriptionResponse>("/subscriptions/trial", {}),

  getAvailableUpgrades: () =>
    api.get<PricingPlan[]>("/subscriptions/available-upgrades"),

  createCheckoutSession: (planId: string, successUrl?: string, cancelUrl?: string) =>
    api.post<CheckoutResponse>("/stripe/create-checkout-session", {
      planId,
      successUrl,
      cancelUrl,
    }),
};
