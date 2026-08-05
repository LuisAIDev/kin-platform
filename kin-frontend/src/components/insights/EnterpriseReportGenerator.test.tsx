import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import EnterpriseReportGenerator from "@/components/insights/EnterpriseReportGenerator";
import { Exporters } from "@/services/intelligence/exporters";
import { analytics } from "@/services/analytics";
import type { ProductIntelligence } from "@/services/intelligence/types";

const { pdfSaveMock } = vi.hoisted(() => ({ pdfSaveMock: vi.fn() }));
vi.mock("jspdf", () => ({
  default: vi.fn(() => ({ text: vi.fn(), setFontSize: vi.fn(), addPage: vi.fn(), save: pdfSaveMock })),
}));

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

describe("EnterpriseReportGenerator", () => {
  it("exporta JSON y registra analytics", async () => {
    const user = userEvent.setup();
    const download = vi.spyOn(Exporters, "download").mockImplementation(() => {});
    const track = vi.spyOn(analytics, "track").mockImplementation(() => {});
    render(<EnterpriseReportGenerator intelligence={pi} />);

    await user.click(screen.getByRole("button", { name: "Exportar JSON" }));

    expect(download).toHaveBeenCalledWith(
      "KIN_metrics.json", expect.stringContaining("usage"), "application/json");
    expect(track).toHaveBeenCalledWith("report_generated", { format: "json" });
    download.mockRestore();
    track.mockRestore();
  });

  it("exporta CSV", async () => {
    const user = userEvent.setup();
    const download = vi.spyOn(Exporters, "download").mockImplementation(() => {});
    render(<EnterpriseReportGenerator intelligence={pi} />);

    await user.click(screen.getByRole("button", { name: "Exportar CSV" }));

    expect(download).toHaveBeenCalledWith("KIN_metrics.csv", expect.stringContaining("metric,value"), "text/csv");
    download.mockRestore();
  });

  it("exporta PDF sin lanzar", async () => {
    const user = userEvent.setup();
    pdfSaveMock.mockClear();
    render(<EnterpriseReportGenerator intelligence={pi} />);

    await user.click(screen.getByRole("button", { name: "Exportar PDF" }));

    await vi.waitFor(() => expect(pdfSaveMock).toHaveBeenCalled());
  });
});
