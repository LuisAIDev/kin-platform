import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PhysicianApplicationWidget from "@/components/physician/PhysicianApplicationWidget";

const { getUser } = vi.hoisted(() => ({ getUser: vi.fn() }));
const { applicationStatus } = vi.hoisted(() => ({ applicationStatus: vi.fn() }));
const { applyAsPhysician } = vi.hoisted(() => ({ applyAsPhysician: vi.fn() }));

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => <a href={href}>{children}</a>,
}));
vi.mock("@/services/auth", () => ({ authService: { getUser } }));
vi.mock("@/services/physician", () => ({
  physicianService: { applicationStatus, applyAsPhysician },
}));

describe("PhysicianApplicationWidget (solicitud profesional)", () => {
  beforeEach(() => {
    getUser.mockReset();
    applicationStatus.mockReset();
    applyAsPhysician.mockReset();
    getUser.mockReturnValue({ role: "FREE", physicianCapability: false, email: "a@kin.com" });
  });

  it("sin solicitud muestra el formulario de solicitud", async () => {
    applicationStatus.mockResolvedValue({
      status: "NOT_FOUND",
      role: "FREE",
      licenseNumber: null,
      specialty: null,
      country: null,
      phone: null,
      physicianVerificationStatus: null,
    });

    render(<PhysicianApplicationWidget />);

    await waitFor(() =>
      expect(screen.getByText("Solicitar registro como profesional")).toBeInTheDocument(),
    );
  });

  it("PENDING muestra banner informativo (no bloquea)", async () => {
    applicationStatus.mockResolvedValue({
      status: "PENDING",
      role: "FREE",
      licenseNumber: "C-1",
      specialty: "Medicina Interna",
      country: "México",
      phone: null,
      physicianVerificationStatus: "PENDING",
    });

    render(<PhysicianApplicationWidget />);

    await waitFor(() =>
      expect(screen.getByText("Tu solicitud profesional está pendiente de revisión.")).toBeInTheDocument(),
    );
  });

  it("APPROVED muestra acceso al portal médico", async () => {
    applicationStatus.mockResolvedValue({
      status: "APPROVED",
      role: "FREE",
      licenseNumber: "C-1",
      specialty: "Medicina Interna",
      country: "México",
      phone: null,
      physicianVerificationStatus: "APPROVED",
    });

    render(<PhysicianApplicationWidget />);

    await waitFor(() =>
      expect(
        screen.getByText("Esta cuenta ya está habilitada como profesional."),
      ).toBeInTheDocument(),
    );
  });

  it("al enviar llama a POST /health/physician/application y muestra pendiente", async () => {
    applicationStatus.mockResolvedValue({
      status: "NOT_FOUND",
      role: "FREE",
      licenseNumber: null,
      specialty: null,
      country: null,
      phone: null,
      physicianVerificationStatus: null,
    });
    applyAsPhysician.mockResolvedValue({ status: "PENDING" });

    render(<PhysicianApplicationWidget />);

    await waitFor(() =>
      expect(screen.getByText("Solicitar registro como profesional")).toBeInTheDocument(),
    );

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText("Número de cédula profesional"), "CEDULA-123");
    await user.selectOptions(screen.getByDisplayValue("Especialidad"), "Medicina Interna");
    await user.type(screen.getByPlaceholderText("País (ej. México)"), "México");
    await user.click(screen.getByRole("checkbox"));
    await user.click(screen.getByRole("button", { name: "Enviar solicitud" }));

    await waitFor(() => expect(applyAsPhysician).toHaveBeenCalledTimes(1));
    await waitFor(() =>
      expect(screen.getByText("Tu solicitud profesional está pendiente de revisión.")).toBeInTheDocument(),
    );
  });

  it("usuario ya con capacidad profesional (médico legacy) no muestra nada", async () => {
    getUser.mockReturnValue({ role: "FREE", physicianCapability: true, email: "m@kin.com" });

    const { container } = render(<PhysicianApplicationWidget />);

    await waitFor(() => expect(applicationStatus).not.toHaveBeenCalled());
    expect(container.firstChild).toBeNull();
  });
});
