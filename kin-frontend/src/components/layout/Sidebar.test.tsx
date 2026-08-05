import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Sidebar from "@/components/layout/Sidebar";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
const { getUser } = vi.hoisted(() => ({ getUser: vi.fn() }));
const { logout } = vi.hoisted(() => ({ logout: vi.fn() }));

let pathname = "/dashboard/projects";
vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ push }),
}));
vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>{children}</a>
  ),
}));
vi.mock("@/services/auth", () => ({ authService: { getUser, logout } }));

describe("Sidebar", () => {
  beforeEach(() => {
    getUser.mockReset();
    logout.mockReset();
    push.mockReset();
    pathname = "/dashboard/projects";
    getUser.mockReturnValue({ token: "t", email: "a@b.c", fullName: "Ana", role: "USER" });
  });

  it("renderiza los ítems de navegación para usuario normal", () => {
    render(<Sidebar />);

    expect(screen.getAllByText("Mis Proyectos").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Nuevo Proyecto").length).toBeGreaterThan(0);
    expect(screen.queryByText("Administración")).toBeNull();
  });

  it("muestra el ítem de administración para ADMIN", () => {
    getUser.mockReturnValue({ token: "t", email: "a@b.c", fullName: "Admin", role: "ADMIN" });

    render(<Sidebar />);

    expect(screen.getAllByText("Administración").length).toBeGreaterThan(0);
  });

  it("marca activo el enlace de proyectos según pathname", () => {
    render(<Sidebar />);

    const active = screen.getAllByText("Mis Proyectos")[0].closest("a");
    expect(active?.className).toContain("bg-primary-600");
  });

  it("cierra sesión y navega a /login", async () => {
    const user = userEvent.setup();
    render(<Sidebar />);

    await user.click(screen.getAllByText("Cerrar sesión")[0]);

    expect(logout).toHaveBeenCalledTimes(1);
    expect(push).toHaveBeenCalledWith("/login");
  });

  it("abre y cierra el menú móvil (aria-label accesible)", async () => {
    const user = userEvent.setup();
    render(<Sidebar />);

    const toggle = screen.getByRole("button", { name: "Abrir menú" });
    expect(toggle).toBeInTheDocument();

    await user.click(toggle);
    expect(screen.getByRole("button", { name: "Cerrar menú" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cerrar menú" }));
    expect(screen.queryByRole("button", { name: "Cerrar menú" })).toBeNull();
  });
});
