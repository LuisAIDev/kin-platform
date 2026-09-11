import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import EvolutionTimeline from "@/components/followup/EvolutionTimeline";
import type { PatientEvolution } from "@/services/followup";

const EVOLUTIONS: PatientEvolution[] = [
  {
    id: "e1",
    patientId: "p1",
    physicianId: "m1",
    recordedAt: "2026-08-26T10:00:00Z",
    symptoms: "Mejoría notable",
    vitals: { presion: "120/80", peso: "70" },
    medicationAdherence: true,
    notes: "Seguir con el plan",
  },
];

describe("EvolutionTimeline", () => {
  it("muestra los registros de evolución con sus signos vitales", () => {
    render(<EvolutionTimeline evolutions={EVOLUTIONS} />);

    expect(screen.getByText("Mejoría notable")).toBeInTheDocument();
    expect(screen.getByText("Cumple medicación")).toBeInTheDocument();
    expect(screen.getByText("presion:")).toBeInTheDocument();
    expect(screen.getByText("120/80")).toBeInTheDocument();
    expect(screen.getByText("Seguir con el plan")).toBeInTheDocument();
  });

  it("muestra estado vacío sin registros", () => {
    render(<EvolutionTimeline evolutions={[]} />);

    expect(screen.getByText("Aún no hay registros de evolución.")).toBeInTheDocument();
  });
});
