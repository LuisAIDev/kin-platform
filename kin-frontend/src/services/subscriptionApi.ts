import { api } from "./api";
import type { PricingPlan } from "./pricing";

export interface SubscriptionStatus {
  isActive: boolean;
  planName: string;
  planDescription: string;
  remainingMessages: number;
  canCreateProject: boolean;
  aiLevel: "FLASH" | "PRO";
  messagesPerMonth: number | null;
  maxProjects: number | null;
  advancedAI: boolean;
  pdfExport: boolean;
  supportLevel: string;
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

  getStatus: () => api.get<SubscriptionStatus>("/subscriptions/status"),

  getCurrent: () => api.get<SubscriptionResponse>("/subscriptions/current"),

  subscribe: (planId: string) =>
    api.post<SubscriptionResponse>("/subscriptions", { planId }),

  cancel: () =>
    api.post<SubscriptionResponse>("/subscriptions/cancel", {}),

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
