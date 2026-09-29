import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import EncounterCreateForm from "@/components/hce/EncounterCreateForm";
import type { PhysicianPatientSummary } from "@/services/physician";

vi.mock("@/lib/hce/api/hce.api", () => ({
  hceApi: {
    createEncounter: vi.fn(),
  },
}));

import { hceApi } from "@/lib/hce/api/hce.api";

function patient(patientId: string, patientName: string): PhysicianPatientSummary {
  return {
    patientId,
    patientName,
    activeConditions: [],
    riskFactors: [],
    chronicConditions: [],
    totalTriages: 0,
    lastTriageAt: null,
    activeAlerts: 0,
    relationshipStatus: "ACTIVE",
  };
}

const PATIENTS = [patient("p1", "Ana Gómez"), patient("p2", "Luis Pérez")];

describe("EncounterCreateForm", () => {
  it("muestra el formulario con sus campos", () => {
    render(<EncounterCreateForm patients={PATIENTS} onCreated={vi.fn()} />);

    expect(screen.getByRole("form", { name: "Nueva consulta" })).toBeInTheDocument();
    expect(screen.getByLabelText("Buscar paciente")).toBeInTheDocument();
    expect(screen.getByLabelText("Paciente")).toBeInTheDocument();
    expect(screen.getByLabelText("Motivo de consulta")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear consulta" })).toBeInTheDocument();
  });

  it("crea el encounter y notifica onCreated", async () => {
    const onCreated = vi.fn();
    const user = userEvent.setup();
    (hceApi.createEncounter as ReturnType<typeof vi.fn>).mockResolvedValue({
      id: "e1",
      patientId: "p1",
      encounterType: "OUTPATIENT",
      chiefComplaint: "Dolor abdominal",
      status: "IN_PROGRESS",
      startedAt: "2026-09-01T10:00:00Z",
      closedAt: null,
    });

    render(<EncounterCreateForm patients={PATIENTS} onCreated={onCreated} />);

    await user.selectOptions(screen.getByLabelText("Paciente"), "p1");
    await user.type(screen.getByLabelText("Motivo de consulta"), "Dolor abdominal");
    await user.click(screen.getByRole("button", { name: "Crear consulta" }));

    await waitFor(() =>
      expect(hceApi.createEncounter).toHaveBeenCalledWith({
        patientId: "p1",
        encounterType: "OUTPATIENT",
        chiefComplaint: "Dolor abdominal",
      }),
    );
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(expect.objectContaining({ id: "e1" })));
  });

  it("filtra la lista de pacientes por búsqueda", async () => {
    const user = userEvent.setup();
    render(<EncounterCreateForm patients={PATIENTS} onCreated={vi.fn()} />);

    await user.type(screen.getByLabelText("Buscar paciente"), "Luis");

    expect(screen.queryByRole("option", { name: "Ana Gómez" })).not.toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Luis Pérez" })).toBeInTheDocument();
  });
});
