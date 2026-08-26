import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SessionGuard from "@/components/auth/SessionGuard";

const { routerReplace } = vi.hoisted(() => ({ routerReplace: vi.fn() }));
const { checkForceLogout } = vi.hoisted(() => ({ checkForceLogout: vi.fn() }));
const { clearSession } = vi.hoisted(() => ({ clearSession: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: routerReplace }) }));

vi.mock("@/services/session", () => ({
  checkForceLogout,
  clearSession,
}));

describe("SessionGuard", () => {
  it("renderiza los hijos cuando no hay cookie de force logout", () => {
    checkForceLogout.mockReturnValue(false);

    render(<SessionGuard><div>Contenido</div></SessionGuard>);

    expect(screen.getByText("Contenido")).toBeInTheDocument();
    expect(routerReplace).not.toHaveBeenCalled();
  });

  it("fuerza logout y redirige cuando hay cookie de fuerza", () => {
    checkForceLogout.mockReturnValue(true);

    render(<SessionGuard><div>Contenido</div></SessionGuard>);

    expect(clearSession).toHaveBeenCalled();
    expect(routerReplace).toHaveBeenCalledWith("/login");
  });
});
