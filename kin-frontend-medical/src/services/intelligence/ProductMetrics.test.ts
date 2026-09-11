import { describe, expect, it } from "vitest";
import { ProductMetrics } from "@/services/intelligence/ProductMetrics";

function event(name: string, timestamp: string, props?: Record<string, unknown>) {
  return { name, props, timestamp };
}

describe("ProductMetrics", () => {
  it("calcula retención, activación y engagement", () => {
    const metrics = ProductMetrics.compute([
      event("session_start", "2026-08-01T10:00:00Z"),
      event("conversation_started", "2026-08-01T10:00:30Z"),
      event("ai_message", "2026-08-01T10:01:00Z"),
      event("project_created", "2026-08-02T10:00:00Z"),
      event("session_start", "2026-08-05T10:00:00Z"),
    ], new Date("2026-08-05T12:00:00Z"));

    expect(metrics.sessions).toBe(2);
    expect(metrics.aiUsage).toBe(1);
    expect(metrics.projectsUsage).toBe(0);
    expect(metrics.activation).toBe(100);
    expect(metrics.retention).toBeGreaterThan(0);
    expect(metrics.engagement).toBeGreaterThan(0);
  });

  it("calcula embudo de onboarding", () => {
    const partial = ProductMetrics.compute([
      event("project_created", "2026-08-05T10:00:00Z"),
    ], new Date("2026-08-05T12:00:00Z"));
    const complete = ProductMetrics.compute([
      event("project_created", "2026-08-05T10:00:00Z"),
      event("ai_message", "2026-08-05T10:01:00Z"),
      event("feature_used", "2026-08-05T10:02:00Z", { feature: "x" }),
    ], new Date("2026-08-05T12:00:00Z"));

    expect(partial.onboardingCompletion).toBe(33);
    expect(complete.onboardingCompletion).toBe(100);
  });

  it("construye timeline cronológico descendente", () => {
    const timeline = ProductMetrics.buildTimeline([
      event("a", "2026-08-01T10:00:00Z"),
      event("b", "2026-08-05T10:00:00Z"),
    ]);

    expect(timeline[0].name).toBe("b");
    expect(timeline).toHaveLength(2);
    expect(ProductMetrics.buildTimeline([])).toEqual([]);
  });

  it("métricas sin datos", () => {
    const metrics = ProductMetrics.compute([], new Date("2026-08-05T12:00:00Z"));

    expect(metrics.sessions).toBe(0);
    expect(metrics.onboardingCompletion).toBe(0);
    expect(metrics.retention).toBe(0);
  });
});
