import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import HealthSummaryCards from "@/components/health/HealthSummaryCards";

describe("HealthSummaryCards", () => {
  it("muestra las tarjetas de resumen", () => {
    render(
      <HealthSummaryCards
        summary={{
          totalConsultations: 5,
          totalDifferentials: 2,
          topConditions: [
            { name: "Gripe", occurrences: 3 },
            { name: "Migraña", occurrences: 1 },
          ],
          lastTriageAt: "2026-08-20T10:00:00Z",
          activeReminders: 1,
        }}
      />,
    );

    expect(screen.getByText("Consultas de triaje")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByText("Diagnósticos diferenciales")).toBeInTheDocument();
    expect(screen.getByText("Condiciones más frecuentes")).toBeInTheDocument();
    expect(screen.getByText(/Gripe · 3 veces/)).toBeInTheDocument();
    expect(screen.getByText("Recordatorios activos")).toBeInTheDocument();
  });

  it("muestra guion si no hay último triaje", () => {
    render(
      <HealthSummaryCards
        summary={{
          totalConsultations: 0,
          totalDifferentials: 0,
          topConditions: [],
          lastTriageAt: null,
          activeReminders: 0,
        }}
      />,
    );

    expect(screen.getByText("—")).toBeInTheDocument();
    expect(screen.queryByText("Condiciones más frecuentes")).not.toBeInTheDocument();
  });
});
