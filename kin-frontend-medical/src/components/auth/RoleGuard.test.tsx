import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { SelectedVertical } from "@/services/session";
import RoleGuard from "@/components/auth/RoleGuard";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
const { getUser } = vi.hoisted(() => ({ getUser: vi.fn() }));
const { fetchCurrentUser } = vi.hoisted(() => ({ fetchCurrentUser: vi.fn() }));
const { getSelectedVertical } = vi.hoisted(() => ({
  getSelectedVertical: vi.fn<() => SelectedVertical | null>(() => null),
}));

let pathname = "/dashboard/empresa";

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ replace: push }),
}));
vi.mock("@/services/auth", () => ({ authService: { getUser, fetchCurrentUser } }));
vi.mock("@/services/session", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/session")>();
  return { ...actual, getSelectedVertical };
});
vi.mock("@/components/auth/AccountReviewScreen", () => ({
  default: () => <div data-testid="review">Cuenta en revisión</div>,
}));

describe("RoleGuard", () => {
  beforeEach(() => {
    getUser.mockReset();
    fetchCurrentUser.mockReset();
    push.mockReset();
    getSelectedVertical.mockReset();
    getSelectedVertical.mockReturnValue(null);
    pathname = "/dashboard/empresa";
    localStorage.clear();
    // getUser lee del espejo local (como el real).
    getUser.mockImplementation(() => {
      const raw = localStorage.getItem("kin_user_v2");
      return raw ? JSON.parse(raw) : null;
    });
  });

  it("con sesión local permite el render y no redirige", () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ role: "FREE", email: "a@b.c" }));

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByText("contenido")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("sin sesión local pero con cookie válida re-sincroniza en lugar de redirigir a /login (evita el bucle)", async () => {
    fetchCurrentUser.mockResolvedValue({
      token: null,
      role: "FREE",
      email: "a@b.c",
      fullName: "Ana",
      emailVerified: true,
      verificationStatus: null,
    });

    render(<RoleGuard>contenido</RoleGuard>);

    // Inicialmente muestra carga; tras re-sincronizar renderiza el contenido.
    expect(screen.getByText("Verificando sesión...")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("contenido")).toBeInTheDocument());
    expect(push).not.toHaveBeenCalled();
  });

  it("sin sesión y en ruta de otra vertical redirige al home del rol", async () => {
    pathname = "/dashboard/patient/health";
    fetchCurrentUser.mockResolvedValue({ token: null, role: "FREE", email: "a@b.c", emailVerified: true });

    render(<RoleGuard>contenido</RoleGuard>);

    await waitFor(() => expect(push).toHaveBeenCalledWith("/dashboard/empresa"));
  });

  it("sin sesión válida redirige a /login", async () => {
    fetchCurrentUser.mockResolvedValue(null);

    render(<RoleGuard>contenido</RoleGuard>);

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
  });

  it("médico pendiente muestra la pantalla de revisión", () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ role: "PHYSICIAN", verificationStatus: "PENDING", email: "m@kin.com" }));

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByTestId("review")).toBeInTheDocument();
  });

  it("empresario con vertical seleccionada salud permanece en /dashboard/salud (sin redirect)", () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ role: "FREE", email: "a@b.c" }));
    getSelectedVertical.mockReturnValue("salud");
    pathname = "/dashboard/salud";

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByText("contenido")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("empresario con vertical salud puede acceder a la subárea de paciente", () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ role: "FREE", email: "a@b.c" }));
    getSelectedVertical.mockReturnValue("salud");
    pathname = "/dashboard/patient/health";

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByText("contenido")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("empresario SIN vertical seleccionada en subárea de paciente redirige a Empresa", () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ role: "FREE", email: "a@b.c" }));
    getSelectedVertical.mockReturnValue(null);
    pathname = "/dashboard/patient/health";

    render(<RoleGuard>contenido</RoleGuard>);

    expect(push).toHaveBeenCalledWith("/dashboard/empresa");
  });

  it("empresario con vertical salud NO accede al portal médico (sin elevación)", () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ role: "FREE", email: "a@b.c" }));
    getSelectedVertical.mockReturnValue("salud");
    pathname = "/dashboard/physician";

    render(<RoleGuard>contenido</RoleGuard>);

    expect(push).toHaveBeenCalledWith("/dashboard/salud");
  });

  it("FREE+APPROVED (physicianCapability true) puede acceder al portal médico", () => {
    localStorage.setItem(
      "kin_user_v2",
      JSON.stringify({
        role: "FREE",
        email: "a@b.c",
        verificationStatus: "APPROVED",
        physicianCapability: true,
      }),
    );
    pathname = "/dashboard/physician";

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByText("contenido")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("FREE+PENDING NO accede al portal médico y NO bloquea su cuenta (sigue en su dashboard)", () => {
    localStorage.setItem(
      "kin_user_v2",
      JSON.stringify({
        role: "FREE",
        email: "a@b.c",
        verificationStatus: "PENDING",
        physicianCapability: false,
      }),
    );
    pathname = "/dashboard/physician";

    render(<RoleGuard>contenido</RoleGuard>);

    // No muestra la pantalla de "cuenta en revisión" (no es un médico legacy en revisión).
    expect(screen.queryByTestId("review")).not.toBeInTheDocument();
    // Redirige a su dashboard (Empresa) por no poder acceder al portal médico.
    expect(push).toHaveBeenCalledWith("/dashboard/empresa");
  });

  it("PHYSICIAN+APPROVED (legacy) puede acceder al portal médico", () => {
    localStorage.setItem(
      "kin_user_v2",
      JSON.stringify({
        role: "PHYSICIAN",
        email: "m@kin.com",
        verificationStatus: "APPROVED",
        physicianCapability: true,
      }),
    );
    pathname = "/dashboard/physician";

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByText("contenido")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
