import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import InvitationsList from "@/components/health/InvitationsList";

vi.mock("@/services/patient", () => ({
  patientRelationshipService: {
    pendingInvitations: vi.fn(),
    acceptInvitation: vi.fn(),
    rejectInvitation: vi.fn(),
  },
}));

import { patientRelationshipService } from "@/services/patient";

const INVITATION = {
  physicianId: "m1",
  physicianName: "Dr. Ana García",
  specialty: "Cardiología",
  invitedAt: "2026-08-26T10:00:00Z",
};

describe("InvitationsList", () => {
  it("muestra las invitaciones pendientes con el médico", async () => {
    (patientRelationshipService.pendingInvitations as ReturnType<typeof vi.fn>).mockResolvedValue([
      INVITATION,
    ]);

    render(<InvitationsList />);

    await waitFor(() => expect(screen.getByText("Dr. Ana García")).toBeInTheDocument());
    expect(screen.getByText("Cardiología")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Aceptar" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Rechazar" })).toBeInTheDocument();
  });

  it("acepta una invitación y la quita de la lista", async () => {
    (patientRelationshipService.pendingInvitations as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce([INVITATION])
      .mockResolvedValueOnce([]);
    (patientRelationshipService.acceptInvitation as ReturnType<typeof vi.fn>).mockResolvedValue({});

    const user = userEvent.setup();
    render(<InvitationsList />);

    await waitFor(() => expect(screen.getByText("Dr. Ana García")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Aceptar" }));

    await waitFor(() =>
      expect(patientRelationshipService.acceptInvitation).toHaveBeenCalledWith("m1"),
    );
    await waitFor(() =>
      expect(screen.getByText(/Has aceptado la invitación de Dr. Ana García/)).toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(screen.getByText("No tienes invitaciones pendientes de médicos.")).toBeInTheDocument(),
    );
  });

  it("rechaza una invitación", async () => {
    (patientRelationshipService.pendingInvitations as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce([INVITATION])
      .mockResolvedValueOnce([]);
    (patientRelationshipService.rejectInvitation as ReturnType<typeof vi.fn>).mockResolvedValue({});

    const user = userEvent.setup();
    render(<InvitationsList />);

    await waitFor(() => expect(screen.getByText("Dr. Ana García")).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Rechazar" }));

    await waitFor(() =>
      expect(patientRelationshipService.rejectInvitation).toHaveBeenCalledWith("m1"),
    );
    await waitFor(() =>
      expect(screen.getByText(/Has rechazado la invitación de Dr. Ana García/)).toBeInTheDocument(),
    );
  });

  it("muestra estado vacío sin invitaciones", async () => {
    (patientRelationshipService.pendingInvitations as ReturnType<typeof vi.fn>).mockResolvedValue([]);

    render(<InvitationsList />);

    await waitFor(() =>
      expect(screen.getByText("No tienes invitaciones pendientes de médicos.")).toBeInTheDocument(),
    );
  });
});
