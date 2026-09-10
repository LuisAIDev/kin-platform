import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PhysicianRegisterForm from "@/components/auth/PhysicianRegisterForm";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
const { registerPhysician } = vi.hoisted(() => ({ registerPhysician: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/services/auth", () => ({
  authService: { registerPhysician },
}));
vi.mock("@/components/auth/PasswordInput", () => {
  return {
    __esModule: true,
    PasswordInput: (props: Record<string, unknown>) => {
      const { placeholder, ...rest } = props;
      return <input type="password" placeholder={placeholder as string} {...rest} />;
    },
  };
});

function fillForm() {
  const user = userEvent.setup();
  return (async () => {
    await user.type(screen.getByPlaceholderText("Nombre y apellidos"), "Dr. García");
    await user.type(screen.getByPlaceholderText("Email"), "m@kin.com");
    await user.type(screen.getByPlaceholderText("Contraseña (mín. 8 caracteres)"), "KINpass123!a");
    await user.type(screen.getByPlaceholderText("Número de cédula profesional"), "CEDULA-12345");
    await user.selectOptions(screen.getByDisplayValue("Selecciona"), "Medicina Interna");
    await user.type(screen.getByPlaceholderText("Ej. México"), "México");
    const checkbox = screen.getByRole("checkbox");
    await user.click(checkbox);
    await user.click(screen.getByRole("button", { name: "Solicitar registro como médico" }));
  })();
}

async function fillAndSubmit(returns: { data: Record<string, unknown> | null; error: string | null }) {
  registerPhysician.mockResolvedValue(returns);
  render(<PhysicianRegisterForm />);
  await fillForm();
}

describe("PhysicianRegisterForm (estado real del registro)", () => {
  beforeEach(() => {
    push.mockReset();
    registerPhysician.mockReset();
  });

  it("email nuevo (NEW_REGISTRATION) navega a verify-email", async () => {
    await fillAndSubmit({ data: { state: "NEW_REGISTRATION" }, error: null });

    await waitFor(() => expect(push).toHaveBeenCalledWith("/verify-email?email=m%40kin.com&pending=1"));
  });

  it("cuenta ya verificada NO navega y muestra mensaje de iniciar sesión", async () => {
    await fillAndSubmit({ data: { state: "ACCOUNT_ALREADY_VERIFIED", emailVerified: true }, error: null });

    await waitFor(() =>
      expect(
        screen.getByText("Esta cuenta ya está verificada. Inicia sesión para solicitar el registro como profesional."),
      ).toBeInTheDocument(),
    );
    expect(push).not.toHaveBeenCalled();
  });

  it("solicitud pendiente (PHYSICIAN_PENDING) muestra mensaje y no navega", async () => {
    await fillAndSubmit({ data: { state: "PHYSICIAN_PENDING" }, error: null });

    await waitFor(() =>
      expect(screen.getByText("Tu solicitud profesional está pendiente de revisión.")).toBeInTheDocument(),
    );
    expect(push).not.toHaveBeenCalled();
  });

  it("solicitud aprobada (PHYSICIAN_APPROVED) muestra mensaje y no navega", async () => {
    await fillAndSubmit({ data: { state: "PHYSICIAN_APPROVED" }, error: null });

    await waitFor(() =>
      expect(screen.getByText("Esta cuenta ya está habilitada como profesional.")).toBeInTheDocument(),
    );
    expect(push).not.toHaveBeenCalled();
  });

  it("cuenta no verificada (ACCOUNT_NOT_VERIFIED) pide verificar y no navega a 'enviado'", async () => {
    await fillAndSubmit({ data: { state: "ACCOUNT_NOT_VERIFIED", emailVerified: false }, error: null });

    await waitFor(() =>
      expect(screen.getByText("Debes verificar primero tu correo. Revisa tu bandeja de entrada.")).toBeInTheDocument(),
    );
    expect(push).not.toHaveBeenCalled();
  });
});
