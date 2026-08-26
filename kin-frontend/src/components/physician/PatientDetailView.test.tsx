import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import PatientDetailView from "@/components/physician/PatientDetailView";

const SUMMARY = {
  patientId: "p1",
  patientName: "Paciente Test",
  activeConditions: ["Gripe", "Migraña"],
  riskFactors: ["fumador"],
  chronicConditions: ["hipertensión"],
  totalTriages: 2,
  lastTriageAt: "2026-08-20T10:00:00Z",
  activeAlerts: 0,
};

const HISTORY = [
  {
    id: "c1",
    symptoms: ["fiebre", "tos"],
    results: [
      {
        conditionId: "x1",
        condition: "Gripe",
        description: "",
        probability: 0.8,
        severity: "MODERADO" as const,
        urgency: "MEDIA" as const,
        recommendation: "Consulta médica.",
        matchedSymptoms: ["fiebre"],
      },
    ],
    createdAt: "2026-08-20T10:00:00Z",
  },
];

describe("PatientDetailView", () => {
  it("muestra el resumen clínico del paciente", () => {
    render(
      <PatientDetailView summary={SUMMARY} history={HISTORY} onClose={vi.fn()} />,
    );

    expect(screen.getByText("Paciente Test")).toBeInTheDocument();
    expect(screen.getByText("fumador")).toBeInTheDocument();
    expect(screen.getByText("hipertensión")).toBeInTheDocument();
    expect(screen.getAllByText("Gripe").length).toBeGreaterThan(0);
    expect(screen.getByText("fiebre, tos")).toBeInTheDocument();
  });

  it("no renderiza nada sin resumen", () => {
    const { container } = render(
      <PatientDetailView summary={null} history={[]} onClose={vi.fn()} />,
    );
    expect(container).toBeEmptyDOMElement();
  });
});
