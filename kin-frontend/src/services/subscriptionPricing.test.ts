import { afterEach, describe, expect, it, vi } from "vitest";
import { pricingService } from "@/services/pricing";
import { subscriptionApi } from "@/services/subscriptionApi";

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("pricingService", () => {
  afterEach(() => vi.restoreAllMocks());

  it("getAll: GET a /pricing-plans", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse([{ id: "free", name: "Free" }]));

    const plans = await pricingService.getAll();

    expect(plans).toHaveLength(1);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/pricing-plans");
  });

  it("update: PUT a /admin/pricing-plans/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ id: "p", name: "Pro" }));

    await pricingService.update("p", {
      name: "Pro", description: "", price: 10, features: [],
      maxProjects: null, messagesPerMonth: null, advancedAI: false, pdfExport: false,
      supportLevel: "BASIC", viabilityScoringDetail: "BASIC", isActive: true,
    });

    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/admin/pricing-plans/p");
  });
});

describe("subscriptionApi", () => {
  afterEach(() => vi.restoreAllMocks());

  it("getStatus: GET a /subscriptions/status", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ isActive: true }));

    const status = await subscriptionApi.getStatus();

    expect(status.isActive).toBe(true);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/subscriptions/status");
  });

  it("subscribe: POST a /subscriptions", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ id: "s1" }));

    await subscriptionApi.subscribe("plan-1");

    const [, init] = fetchMock.mock.calls[0];
    expect((init as RequestInit).method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/subscriptions");
  });

  it("cancel/startTrial: POST a endpoints", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockImplementation(() => Promise.resolve(jsonResponse({ id: "s1" })));

    await subscriptionApi.cancel();
    await subscriptionApi.startTrial();

    expect(String(fetchMock.mock.calls[0][0])).toContain("/subscriptions/cancel");
    expect(String(fetchMock.mock.calls[1][0])).toContain("/subscriptions/trial");
  });

  it("createCheckoutSession: POST a /stripe/create-checkout-session", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ sessionId: "s", url: "https://stripe.com/c" }));

    const checkout = await subscriptionApi.createCheckoutSession("plan-1");

    expect(checkout.url).toContain("stripe.com");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/stripe/create-checkout-session");
  });

  it("getAvailableUpgrades/getPlans: GET", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockImplementation(() => Promise.resolve(jsonResponse([])));

    await subscriptionApi.getAvailableUpgrades();
    await subscriptionApi.getPlans();

    expect(String(fetchMock.mock.calls[0][0])).toContain("/subscriptions/available-upgrades");
    expect(String(fetchMock.mock.calls[1][0])).toContain("/pricing-plans");
  });
});
