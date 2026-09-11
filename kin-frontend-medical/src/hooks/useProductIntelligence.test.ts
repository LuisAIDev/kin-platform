import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useProductIntelligence } from "@/hooks/useProductIntelligence";
import { analytics } from "@/services/analytics";
import { FeatureUsageTracker } from "@/services/intelligence/FeatureUsageTracker";

function event(name: string, timestamp: string, props?: Record<string, unknown>) {
  return { name, props, timestamp };
}

describe("useProductIntelligence", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(analytics, "events").mockReturnValue([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "Hola" }),
      event("project_created", "2026-08-05T10:01:00Z"),
    ]);
  });

  it("computa inteligencia desde analytics", () => {
    const { result } = renderHook(() => useProductIntelligence());

    expect(result.current.intelligence.usage.messagesSent).toBe(1);
    expect(result.current.intelligence.usage.projectsCreated).toBe(1);
  });

  it("registra el uso de la feature actual", () => {
    renderHook(() => useProductIntelligence("dashboard_analytics"));

    expect(FeatureUsageTracker.list().some((f) => f.feature === "dashboard_analytics")).toBe(true);
  });

  it("refresca los datos", () => {
    const { result } = renderHook(() => useProductIntelligence());

    vi.mocked(analytics.events).mockReturnValue([event("session_start", "2026-08-05T10:00:00Z")]);
    act(() => result.current.refresh());

    expect(result.current.intelligence.usage.sessions).toBe(1);
  });
});
