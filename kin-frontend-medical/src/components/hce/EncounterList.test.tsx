import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import EncounterList from "@/components/hce/EncounterList";
import type { EncounterResponse } from "@/lib/hce/api/hce.api";

const ENCOUNTERS: EncounterResponse[] = [
  {
    id: "e1",
    patientId: "p1",
    encounterType: "OUTPATIENT",
    chiefComplaint: "Dolor abdominal",
    status: "IN_PROGRESS",
    startedAt: "2026-09-01T10:00:00Z",
    closedAt: null,
  },
];

describe("EncounterList", () => {
  it("lista las consultas con motivo, tipo y estado", () => {
    render(<EncounterList encounters={ENCOUNTERS} />);

    expect(screen.getByText("Dolor abdominal")).toBeInTheDocument();
    expect(screen.getByText("Consulta externa")).toBeInTheDocument();
    expect(screen.getByText("En curso")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Abrir" })).toHaveAttribute(
      "href",
      "/dashboard/physician/hce/e1/edit",
    );
  });

  it("muestra estado vacío sin consultas", () => {
    render(<EncounterList encounters={[]} />);

    expect(screen.getByText("Sin consultas previas")).toBeInTheDocument();
  });
});
