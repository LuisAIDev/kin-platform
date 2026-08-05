import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SessionGuard from "@/components/auth/SessionGuard";

const { routerReplace } = vi.hoisted(() => ({ routerReplace: vi.fn() }));
const { getToken } = vi.hoisted(() => ({ getToken: vi.fn() }));
const { checkForceLogout } = vi.hoisted(() => ({ checkForceLogout: vi.fn() }));
const { clearSession } = vi.hoisted(() => ({ clearSession: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: routerReplace }) }));

vi.mock("@/services/auth", () => ({
  authService: { getToken },
}));

vi.mock("@/services/session", () => ({
  checkForceLogout,
  clearSession,
}));

describe("SessionGuard", () => {
  it("renderiza los hijos cuando hay sesión", () => {
    getToken.mockReturnValue("tok");

    render(<SessionGuard><div>Contenido</div></SessionGuard>);

    expect(screen.getByText("Contenido")).toBeInTheDocument();
    expect(routerReplace).not.toHaveBeenCalled();
  });

  it("redirige a /login cuando no hay token", () => {
    getToken.mockReturnValue(null);
    checkForceLogout.mockReturnValue(false);

    render(<SessionGuard><div>Contenido</div></SessionGuard>);

    expect(routerReplace).toHaveBeenCalledWith("/login");
  });

  it("fuerza logout y redirige cuando hay cookie de fuerza", () => {
    getToken.mockReturnValue("tok");
    checkForceLogout.mockReturnValue(true);

    render(<SessionGuard><div>Contenido</div></SessionGuard>);

    expect(clearSession).toHaveBeenCalled();
    expect(routerReplace).toHaveBeenCalledWith("/login");
  });
});
