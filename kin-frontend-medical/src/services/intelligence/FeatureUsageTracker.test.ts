import { beforeEach, describe, expect, it } from "vitest";
import { FeatureUsageTracker } from "@/services/intelligence/FeatureUsageTracker";

function event(name: string, timestamp: string, props?: Record<string, unknown>) {
  return { name, props, timestamp };
}

describe("FeatureUsageTracker", () => {
  beforeEach(() => localStorage.clear());

  it("registra y lista uso de funciones", () => {
    FeatureUsageTracker.record("dashboard_analytics");
    FeatureUsageTracker.record("dashboard_analytics");

    const list = FeatureUsageTracker.list();
    expect(list).toHaveLength(1);
    expect(list[0].uses).toBe(2);
    expect(list[0].lastUsed).toBeTruthy();
  });

  it("mergea uso local con eventos de analytics", () => {
    FeatureUsageTracker.record("dashboard_insights");
    const merged = FeatureUsageTracker.mergeWithEvents([
      event("feature_used", "2026-08-05T10:00:00Z", { feature: "dashboard_insights" }),
      event("feature_used", "2026-08-05T10:00:00Z", { feature: "enterprise" }),
    ]);

    const insights = merged.find((f) => f.feature === "dashboard_insights");
    const enterprise = merged.find((f) => f.feature === "enterprise");
    expect(insights?.uses).toBe(2);
    expect(enterprise?.uses).toBe(1);
  });

  it("detecta funciones no utilizadas", () => {
    FeatureUsageTracker.record("dashboard_analytics");

    const unused = FeatureUsageTracker.unused();
    expect(unused).toContain("enterprise");
    expect(unused).not.toContain("dashboard_analytics");
  });

  it("maneja ausencia de datos", () => {
    expect(FeatureUsageTracker.list()).toEqual([]);
    expect(FeatureUsageTracker.unused()).toContain("dashboard_analytics");
    expect(FeatureUsageTracker.mergeWithEvents([])).toEqual([]);
  });
});
