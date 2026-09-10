import { describe, expect, it, vi } from "vitest";
import { AnalyticsAggregator } from "@/services/intelligence/AnalyticsAggregator";
import { Exporters } from "@/services/intelligence/exporters";
import type { ProductIntelligence } from "@/services/intelligence/types";

const { pdfSaveMock } = vi.hoisted(() => ({ pdfSaveMock: vi.fn() }));
vi.mock("jspdf", () => ({
  default: vi.fn(() => ({ text: vi.fn(), setFontSize: vi.fn(), addPage: vi.fn(), save: pdfSaveMock })),
}));

function event(name: string, timestamp: string, props?: Record<string, unknown>) {
  return { name, props, timestamp };
}

describe("AnalyticsAggregator", () => {
  it("agrega toda la inteligencia desde eventos", () => {
    const pi = AnalyticsAggregator.compute([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "Quiero abrir una panadería" }),
      event("project_created", "2026-08-05T10:01:00Z"),
      event("session_start", "2026-08-05T10:02:00Z"),
    ], new Date("2026-08-05T12:00:00Z"));

    expect(pi.usage.messagesSent).toBe(1);
    expect(pi.usage.projectsCreated).toBe(1);
    expect(pi.insights.predominantIntent).toBeTruthy();
    expect(pi.metrics.sessions).toBe(1);
    expect(pi.recommendations.length).toBeGreaterThan(0);
    expect(pi.timeline).toHaveLength(3);
  });
});

describe("Exporters", () => {
  const pi: ProductIntelligence = {
    usage: { dailyCount: 1, weeklyCount: 1, monthlyCount: 1, messagesSent: 1, sessions: 1,
      activeDays: 1, estimatedTokens: 10, estimatedCost: 0.001, positiveFeedback: 1,
      negativeFeedback: 0, projectsCreated: 1, conversations: 1 },
    insights: { averageLength: 10, questionsPerSession: 0, durationMs: 0, predominantIntent: "x",
      frequentTopics: ["tema"], responseQuality: 0, satisfaction: 100 },
    metrics: { retention: 50, activation: 100, engagement: 1, sessions: 1,
      onboardingCompletion: 100, aiUsage: 1, dashboardUsage: 1, projectsUsage: 1 },
    features: [], recommendations: [], timeline: [],
  };

  it("exporta JSON", () => {
    const json = Exporters.toJson(pi);
    expect(JSON.parse(json).usage.messagesSent).toBe(1);
  });

  it("exporta CSV con cabecera", () => {
    const csv = Exporters.toCsv(pi);
    expect(csv.startsWith("metric,value")).toBe(true);
    expect(csv).toContain("usage.messagesSent");
  });

  it("descarga contenido", () => {
    const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    Exporters.download("x.json", "{}", "application/json");

    expect(click).toHaveBeenCalled();
    revoke.mockRestore();
    click.mockRestore();
  });

  it("genera PDF con jsPDF", async () => {
    pdfSaveMock.mockClear();

    await Exporters.toPdf(pi);

    await vi.waitFor(() => expect(pdfSaveMock).toHaveBeenCalled());
    expect(pdfSaveMock.mock.calls[0][0]).toContain("KIN_Product_Intelligence_Report.pdf");
  });
});
