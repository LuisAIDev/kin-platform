import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import PatientList from "@/components/physician/PatientList";

const PAGE = {
  content: [
    {
      patientId: "p1",
      patientName: "Paciente Test",
      activeConditions: ["Gripe"],
      riskFactors: ["fumador"],
      chronicConditions: [],
      totalTriages: 3,
      lastTriageAt: "2026-08-20T10:00:00Z",
      activeAlerts: 1,
    },
  ],
  totalElements: 1,
  totalPages: 1,
  currentPage: 0,
  size: 10,
};

describe("PatientList", () => {
  it("muestra los pacientes asignados", () => {
    render(<PatientList page={PAGE} onPageChange={vi.fn()} onSelect={vi.fn()} />);

    expect(screen.getByText("Paciente Test")).toBeInTheDocument();
    expect(screen.getByText("Gripe")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Ver resumen" })).toBeInTheDocument();
  });

  it("selecciona un paciente al pulsar Ver resumen", async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<PatientList page={PAGE} onPageChange={vi.fn()} onSelect={onSelect} />);

    await user.click(screen.getByRole("button", { name: "Ver resumen" }));

    expect(onSelect).toHaveBeenCalledWith("p1");
  });

  it("muestra estado vacío sin pacientes", () => {
    render(
      <PatientList
        page={{ content: [], totalElements: 0, totalPages: 0, currentPage: 0, size: 10 }}
        onPageChange={vi.fn()}
        onSelect={vi.fn()}
      />,
    );

    expect(screen.getByText("No tienes pacientes asignados.")).toBeInTheDocument();
  });
});
