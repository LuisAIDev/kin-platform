import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { fireEvent } from "@testing-library/react";
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
      expect(screen.getByText(/Invitación enviada/)).toBeInTheDocument(),
    );
    expect(onInvited).toHaveBeenCalled();
  });

  it("normaliza espacios, mayúsculas y caracteres invisibles antes de invitar", async () => {
    const user = userEvent.setup();
    (physicianService.invitePatient as ReturnType<typeof vi.fn>).mockResolvedValue({
      patientEmail: "paciente@kin.com",
      status: "PENDING",
    });

    render(<InvitePatientModal onClose={vi.fn()} onInvited={vi.fn()} />);

    const input = screen.getByLabelText("Correo del paciente");
    fireEvent.change(input, { target: { value: "\u00A0PACIENTE@KIN.COM\u200B " } });
    await user.click(screen.getByRole("button", { name: "Enviar invitación" }));

    await waitFor(() =>
      expect(physicianService.invitePatient).toHaveBeenCalledWith("paciente@kin.com", ""),
    );
  });

  it("404: muestra que el correo no está registrado en KIN", async () => {
    const user = userEvent.setup();
    const error = Object.assign(new Error("No existe una cuenta KIN con ese correo."), {
      status: 404,
    });
    (physicianService.invitePatient as ReturnType<typeof vi.fn>).mockRejectedValue(error);

    render(<InvitePatientModal onClose={vi.fn()} onInvited={vi.fn()} />);

    await user.type(screen.getByLabelText("Correo del paciente"), "x@kin.com");
    await user.click(screen.getByRole("button", { name: "Enviar invitación" }));

    await waitFor(() =>
      expect(screen.getByText("El correo no está registrado en KIN.")).toBeInTheDocument(),
    );
  });

  it("409: muestra que ya existe una invitación o relación activa", async () => {
    const user = userEvent.setup();
    const error = Object.assign(new Error("Ya existe una invitación o relación activa con este paciente."), {
      status: 409,
    });
    (physicianService.invitePatient as ReturnType<typeof vi.fn>).mockRejectedValue(error);

    render(<InvitePatientModal onClose={vi.fn()} onInvited={vi.fn()} />);

    await user.type(screen.getByLabelText("Correo del paciente"), "x@kin.com");
    await user.click(screen.getByRole("button", { name: "Enviar invitación" }));

    await waitFor(() =>
      expect(
        screen.getByText("Ya existe una invitación o relación activa con este paciente."),
      ).toBeInTheDocument(),
    );
  });

  it("400: pide revisar el formato del correo", async () => {
    const user = userEvent.setup();
    const error = Object.assign(new Error("patientEmail: El correo no tiene un formato válido"), {
      status: 400,
    });
    (physicianService.invitePatient as ReturnType<typeof vi.fn>).mockRejectedValue(error);

    render(<InvitePatientModal onClose={vi.fn()} onInvited={vi.fn()} />);

    await user.type(screen.getByLabelText("Correo del paciente"), "x@kin.com");
    await user.click(screen.getByRole("button", { name: "Enviar invitación" }));

    await waitFor(() =>
      expect(screen.getByText("Por favor revisa el formato del correo.")).toBeInTheDocument(),
    );
  });
});
