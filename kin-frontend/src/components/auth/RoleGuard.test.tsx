import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RoleGuard from "@/components/auth/RoleGuard";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
const { getUser } = vi.hoisted(() => ({ getUser: vi.fn() }));
const { fetchCurrentUser } = vi.hoisted(() => ({ fetchCurrentUser: vi.fn() }));

let pathname = "/dashboard/empresa";

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ replace: push }),
}));
vi.mock("@/services/auth", () => ({ authService: { getUser, fetchCurrentUser } }));
vi.mock("@/services/session", () => ({
  storeSession: (user: unknown) =>
    localStorage.setItem("kin_user_v2", JSON.stringify(user)),
}));
vi.mock("@/components/auth/AccountReviewScreen", () => ({
  default: () => <div data-testid="review">Cuenta en revisión</div>,
}));

describe("RoleGuard", () => {
  beforeEach(() => {
    getUser.mockReset();
    fetchCurrentUser.mockReset();
    push.mockReset();
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
});
