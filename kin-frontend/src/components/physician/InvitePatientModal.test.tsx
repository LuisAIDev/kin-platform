import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import InvitePatientModal from "@/components/physician/InvitePatientModal";

vi.mock("@/services/physician", () => ({
  physicianService: {
    invitePatient: vi.fn(),
  },
}));

import { physicianService } from "@/services/physician";

describe("InvitePatientModal", () => {
  it("muestra el formulario de invitación", () => {
    render(<InvitePatientModal onClose={vi.fn()} onInvited={vi.fn()} />);

    expect(screen.getByRole("dialog", { name: "Invitar paciente" })).toBeInTheDocument();
    expect(screen.getByLabelText("Correo del paciente")).toBeInTheDocument();
    expect(screen.getByLabelText("Mensaje (opcional)")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Enviar invitación" })).toBeInTheDocument();
  });

  it("invita al paciente por email y notifica", async () => {
    const onInvited = vi.fn();
    const onClose = vi.fn();
    const user = userEvent.setup();
    (physicianService.invitePatient as ReturnType<typeof vi.fn>).mockResolvedValue({
      patientEmail: "paciente@kin.com",
      status: "PENDING",
    });

    render(<InvitePatientModal onClose={onClose} onInvited={onInvited} />);

    await user.type(screen.getByLabelText("Correo del paciente"), "paciente@kin.com");
    await user.click(screen.getByRole("button", { name: "Enviar invitación" }));

    await waitFor(() =>
      expect(physicianService.invitePatient).toHaveBeenCalledWith("paciente@kin.com", ""),
    );
    await waitFor(() =>
      expect(screen.getByText(/Invitación enviada a/)).toBeInTheDocument(),
    );
    expect(onInvited).toHaveBeenCalled();
  });

  it("muestra el error si el paciente no está registrado", async () => {
    const user = userEvent.setup();
    (physicianService.invitePatient as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("Paciente no registrado en KIN: x@kin.com"),
    );

    render(<InvitePatientModal onClose={vi.fn()} onInvited={vi.fn()} />);

    await user.type(screen.getByLabelText("Correo del paciente"), "x@kin.com");
    await user.click(screen.getByRole("button", { name: "Enviar invitación" }));

    await waitFor(() =>
      expect(screen.getByText(/Paciente no registrado en KIN/)).toBeInTheDocument(),
    );
  });
});
