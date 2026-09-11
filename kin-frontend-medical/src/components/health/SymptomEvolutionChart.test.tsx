import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import SymptomEvolutionChart from "@/components/health/SymptomEvolutionChart";

describe("SymptomEvolutionChart", () => {
  it("muestra la evolución por mes", () => {
    render(
      <SymptomEvolutionChart
        history={[
          { id: "1", symptoms: ["fiebre"], results: [], createdAt: "2026-08-01T10:00:00Z" },
          { id: "2", symptoms: ["tos"], results: [], createdAt: "2026-08-15T10:00:00Z" },
          { id: "3", symptoms: ["dolor"], results: [], createdAt: "2026-09-05T10:00:00Z" },
        ]}
      />,
    );

    expect(screen.getByRole("heading", { name: "Evolución de consultas" })).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
  });

  it("muestra estado vacío sin historial", () => {
    render(<SymptomEvolutionChart history={[]} />);

    expect(screen.getByText(/Aún no hay suficientes consultas/)).toBeInTheDocument();
  });
});
