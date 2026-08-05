import { beforeEach, describe, expect, it, vi } from "vitest";
import { analytics } from "@/services/analytics";

describe("analytics", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(console, "info").mockImplementation(() => {});
  });

  it("registra eventos en localStorage sin enviar datos externos", () => {
    analytics.track("project_created", { id: "p1" });

    const events = analytics.events();
    expect(events).toHaveLength(1);
    expect(events[0].name).toBe("project_created");
    expect(events[0].props?.id).toBe("p1");
    expect(events[0].timestamp).toBeTruthy();
  });

  it("acumula eventos y limita el log", () => {
    for (let i = 0; i < 150; i++) {
      analytics.track(`e${i}`);
    }

    const events = analytics.events();
    expect(events.length).toBeLessThanOrEqual(100);
  });

  it("clear vacía el log", () => {
    analytics.track("a");
    analytics.clear();

    expect(analytics.events()).toHaveLength(0);
  });

  it("events devuelve vacío sin datos", () => {
    expect(analytics.events()).toEqual([]);
  });
});
