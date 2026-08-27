import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import RoleGuard from "@/components/auth/RoleGuard";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
const { getUser } = vi.hoisted(() => ({ getUser: vi.fn() }));
const { meGet } = vi.hoisted(() => ({ meGet: vi.fn() }));
const { storeSession } = vi.hoisted(() => ({ storeSession: vi.fn() }));

let pathname = "/dashboard/empresa";

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ replace: push }),
}));
vi.mock("@/services/auth", () => ({ authService: { getUser } }));
vi.mock("@/services/api", () => ({
  api: { get: (url: string) => (url === "/auth/me" ? meGet() : Promise.reject(new Error("nope"))) },
}));
vi.mock("@/services/session", () => ({ storeSession }));
vi.mock("@/components/auth/AccountReviewScreen", () => ({
  default: () => <div data-testid="review">Cuenta en revisión</div>,
}));

describe("RoleGuard", () => {
  beforeEach(() => {
    getUser.mockReset();
    meGet.mockReset();
    storeSession.mockReset();
    push.mockReset();
    pathname = "/dashboard/empresa";
  });

  it("con sesión local permite el render y no redirige", () => {
    getUser.mockReturnValue({ role: "FREE", email: "a@b.c" });

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByText("contenido")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("sin sesión local pero con cookie válida re-sincroniza en lugar de redirigir a /login (evita el bucle)", async () => {
    getUser.mockReturnValue(null);
    meGet.mockResolvedValue({
      role: "FREE",
      email: "a@b.c",
      fullName: "Ana",
      emailVerified: true,
      verificationStatus: null,
    });

    render(<RoleGuard>contenido</RoleGuard>);

    await waitFor(() => expect(storeSession).toHaveBeenCalled());
    // Está en su home: NO navega a /login (no hay bucle).
    expect(push).not.toHaveBeenCalled();
    expect(screen.getByText("contenido")).toBeInTheDocument();
  });

  it("sin sesión y en ruta de otra vertical redirige al home del rol", async () => {
    pathname = "/dashboard/patient/health";
    getUser.mockReturnValue(null);
    meGet.mockResolvedValue({ role: "FREE", email: "a@b.c", emailVerified: true });

    render(<RoleGuard>contenido</RoleGuard>);

    await waitFor(() => expect(push).toHaveBeenCalledWith("/dashboard/empresa"));
  });

  it("sin sesión válida redirige a /login", async () => {
    getUser.mockReturnValue(null);
    meGet.mockRejectedValue(new Error("401"));

    render(<RoleGuard>contenido</RoleGuard>);

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
  });

  it("médico pendiente muestra la pantalla de revisión", () => {
    getUser.mockReturnValue({ role: "PHYSICIAN", verificationStatus: "PENDING", email: "m@kin.com" });

    render(<RoleGuard>contenido</RoleGuard>);

    expect(screen.getByTestId("review")).toBeInTheDocument();
  });
});
