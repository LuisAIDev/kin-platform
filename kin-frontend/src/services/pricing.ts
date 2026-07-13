import { api } from "./api";

export interface PricingPlan {
  id: string;
  name: string;
  price: number;
  currency: string;
  billingPeriod: string;
  features: string[];
  isPopular: boolean;
  displayOrder: number;
}

export interface UpdatePricingPlanRequest {
  name: string;
  price: number;
  currency: string;
  billingPeriod: string;
  features: string[];
  isPopular: boolean;
  displayOrder: number;
}

export const pricingService = {
  getAll: () => api.get<PricingPlan[]>("/pricing-plans"),

  update: (id: string, data: UpdatePricingPlanRequest) =>
    api.put<PricingPlan>(`/admin/pricing-plans/${id}`, data),
};
