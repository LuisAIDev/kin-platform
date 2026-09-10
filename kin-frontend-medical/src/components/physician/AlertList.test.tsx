import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AlertList from "@/components/physician/AlertList";

describe("AlertList", () => {
  it("muestra las alertas activas", () => {
    render(
      <AlertList
        alerts={[
          {
            id: "a1",
            patientId: "p1",
            type: "HIGH_URGENCY_TRIAGE",
            severity: "ALTA",
            message: "Triaje de alta urgencia: Angina de pecho",
            status: "PENDING",
            createdAt: "2026-08-26T10:00:00Z",
            acknowledgedAt: null,
          },
        ]}
        onAcknowledge={vi.fn()}
      />,
    );

    expect(screen.getByText("ALTA")).toBeInTheDocument();
    expect(screen.getByText(/Angina de pecho/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Marcar atendida" })).toBeInTheDocument();
  });

  it("marca la alerta como atendida", async () => {
    const onAcknowledge = vi.fn();
    const user = userEvent.setup();
    render(
      <AlertList
        alerts={[
          {
            id: "a1",
            patientId: "p1",
            type: "HIGH_URGENCY_TRIAGE",
            severity: "ALTA",
            message: "Triaje de alta urgencia: Angina de pecho",
            status: "PENDING",
            createdAt: "2026-08-26T10:00:00Z",
            acknowledgedAt: null,
          },
        ]}
        onAcknowledge={onAcknowledge}
      />,
    );

    await user.click(screen.getByRole("button", { name: "Marcar atendida" }));

    expect(onAcknowledge).toHaveBeenCalledWith("a1");
  });

  it("muestra estado vacío sin alertas", () => {
    render(<AlertList alerts={[]} onAcknowledge={vi.fn()} />);

    expect(screen.getByText("No hay alertas activas.")).toBeInTheDocument();
  });
});
