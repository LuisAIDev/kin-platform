import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import AppointmentList from "@/components/telemedicine/AppointmentList";
import type { Appointment } from "@/services/telemedicine";

const APPT: Appointment = {
  id: "a1",
  patientId: "p1",
  physicianId: "m1",
  scheduledAt: "2026-08-30T10:00:00Z",
  reason: "Control de hipertensión",
  status: "PENDIENTE",
  createdAt: "2026-08-26T10:00:00Z",
};

describe("AppointmentList", () => {
  it("muestra las citas con su estado", () => {
    render(<AppointmentList appointments={[APPT]} asPhysician={false} />);

    expect(screen.getByText("PENDIENTE")).toBeInTheDocument();
    expect(screen.getByText("Control de hipertensión")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Confirmar" })).not.toBeInTheDocument();
  });

  it("permite al médico confirmar una cita pendiente", async () => {
    const onStatusChange = vi.fn();
    const user = userEvent.setup();
    render(
      <AppointmentList appointments={[APPT]} asPhysician={true} onStatusChange={onStatusChange} />,
    );

    await user.click(screen.getByRole("button", { name: "Confirmar" }));

    expect(onStatusChange).toHaveBeenCalledWith("a1", "CONFIRMADA");
  });

  it("muestra estado vacío sin citas", () => {
    render(<AppointmentList appointments={[]} asPhysician={false} />);

    expect(screen.getByText("No hay citas.")).toBeInTheDocument();
  });
});
