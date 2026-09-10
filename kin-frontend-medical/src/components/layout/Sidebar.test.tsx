import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { SelectedVertical } from "@/services/session";
import Sidebar from "@/components/layout/Sidebar";

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
const { getUser } = vi.hoisted(() => ({ getUser: vi.fn() }));
const { logout } = vi.hoisted(() => ({ logout: vi.fn() }));
const { getSelectedVertical, setSelectedVertical } = vi.hoisted(() => ({
  getSelectedVertical: vi.fn<() => SelectedVertical | null>(() => null),
  setSelectedVertical: vi.fn<(v: SelectedVertical) => void>(),
}));

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
vi.mock("@/services/session", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/session")>();
  return { ...actual, getSelectedVertical, setSelectedVertical };
});

function mockUser(role: string) {
  getUser.mockReturnValue({ token: "t", email: "a@b.c", fullName: "Ana", role });
}

describe("Sidebar", () => {
  beforeEach(() => {
    getUser.mockReset();
    logout.mockReset();
    push.mockReset();
    getSelectedVertical.mockReset();
    getSelectedVertical.mockReturnValue(null);
    setSelectedVertical.mockReset();
    pathname = "/dashboard/projects";
  });

  it("usuario FREE ve solo opciones empresariales", () => {
    mockUser("FREE");
    render(<Sidebar />);

    expect(screen.getAllByText("Mis Proyectos").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Nuevo Proyecto").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Analytics").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Sobre KIN").length).toBeGreaterThan(0);
    expect(screen.queryByText("Mi Salud")).toBeNull();
    expect(screen.queryByText("Triaje Digital")).toBeNull();
    expect(screen.queryByText("Portal Médico")).toBeNull();
    expect(screen.queryByText("Administración")).toBeNull();
  });

  it("usuario normal (USER legacy) ve opciones empresariales", () => {
    mockUser("USER");
    render(<Sidebar />);

    expect(screen.getAllByText("Mis Proyectos").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Nuevo Proyecto").length).toBeGreaterThan(0);
    expect(screen.queryByText("Administración")).toBeNull();
    expect(screen.queryByText("Mi Salud")).toBeNull();
    expect(screen.queryByText("Triaje Digital")).toBeNull();
  });

  it("paciente ve solo opciones de salud", () => {
    mockUser("PATIENT");
    render(<Sidebar />);

    expect(screen.getAllByText("Mi Salud").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Triaje Digital").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Mensajes").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Citas").length).toBeGreaterThan(0);
    expect(screen.queryByText("Mis Proyectos")).toBeNull();
    expect(screen.queryByText("Analytics")).toBeNull();
    expect(screen.queryByText("Portal Médico")).toBeNull();
  });

  it("médico ve solo su portal", () => {
    mockUser("PHYSICIAN");
    render(<Sidebar />);

    expect(screen.getAllByText("Portal Médico").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Mensajes").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Citas").length).toBeGreaterThan(0);
    expect(screen.queryByText("Mis Proyectos")).toBeNull();
    expect(screen.queryByText("Mi Salud")).toBeNull();
    expect(screen.queryByText("Triaje Digital")).toBeNull();
  });

  it("administrador ve menú completo", () => {
    mockUser("ADMIN");
    render(<Sidebar />);

    expect(screen.getAllByText("Mis Proyectos").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Analytics").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Mi Salud").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Triaje Digital").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Portal Médico").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Mensajes").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Citas").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Administración").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Sobre KIN").length).toBeGreaterThan(0);
  });

  it("incluye el ítem 'Sobre KIN' enlazando a /sobre-kin", () => {
    mockUser("FREE");
    render(<Sidebar />);
    const links = screen.getAllByText("Sobre KIN");
    expect(links.length).toBeGreaterThan(0);
    expect(links[0].closest("a")).toHaveAttribute("href", "/sobre-kin");
  });

  it("marca activo 'Sobre KIN' cuando el pathname es /sobre-kin", () => {
    pathname = "/sobre-kin";
    mockUser("FREE");
    render(<Sidebar />);
    const link = screen.getAllByText("Sobre KIN")[0].closest("a");
    expect(link?.className).toContain("bg-primary-600");
  });

  it("marca activo el enlace de proyectos según pathname", () => {
    mockUser("FREE");
    render(<Sidebar />);

    const active = screen.getAllByText("Mis Proyectos")[0].closest("a");
    expect(active?.className).toContain("bg-primary-600");
  });

  it("cierra sesión y navega a /login", async () => {
    const user = userEvent.setup();
    mockUser("FREE");
    render(<Sidebar />);

    await user.click(screen.getAllByText("Cerrar sesión")[0]);

    expect(logout).toHaveBeenCalledTimes(1);
    expect(push).toHaveBeenCalledWith("/login");
  });

  it("abre y cierra el menú móvil (aria-label accesible)", async () => {
    const user = userEvent.setup();
    mockUser("FREE");
    render(<Sidebar />);

    const toggle = screen.getByRole("button", { name: "Abrir menú" });
    expect(toggle).toBeInTheDocument();

    await user.click(toggle);
    expect(screen.getByRole("button", { name: "Cerrar menú" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cerrar menú" }));
    expect(screen.queryByRole("button", { name: "Cerrar menú" })).toBeNull();
  });

  describe("selector de vertical (contexto de navegación)", () => {
    it("empresario con vertical salud ve menú de Salud, no proyectos", () => {
      mockUser("FREE");
      getSelectedVertical.mockReturnValue("salud");
      render(<Sidebar />);

      expect(screen.getAllByText("Mi Salud").length).toBeGreaterThan(0);
      expect(screen.getAllByText("Triaje Digital").length).toBeGreaterThan(0);
      expect(screen.queryByText("Mis Proyectos")).toBeNull();
      expect(screen.queryByText("Analytics")).toBeNull();
      expect(screen.queryByText("Portal Médico")).toBeNull();
    });

    it("el selector refleja la vertical seleccionada (Salud activo)", () => {
      mockUser("FREE");
      getSelectedVertical.mockReturnValue("salud");
      render(<Sidebar />);

      const saludBtn = screen.getByRole("button", { name: "Vertical Salud" });
      const empresaBtn = screen.getByRole("button", { name: "Vertical Empresa" });
      expect(saludBtn).toHaveAttribute("aria-pressed", "true");
      expect(empresaBtn).toHaveAttribute("aria-pressed", "false");
    });

    it("cambiar a Salud guarda la selección y navega a /dashboard/salud", async () => {
      const user = userEvent.setup();
      mockUser("FREE");
      render(<Sidebar />);

      await user.click(screen.getByRole("button", { name: "Vertical Salud" }));

      expect(setSelectedVertical).toHaveBeenCalledWith("salud");
      expect(push).toHaveBeenCalledWith("/dashboard/salud");
    });

    it("cambiar a Empresa guarda la selección y navega a /dashboard/empresa", async () => {
      const user = userEvent.setup();
      mockUser("FREE");
      getSelectedVertical.mockReturnValue("salud");
      render(<Sidebar />);

      await user.click(screen.getByRole("button", { name: "Vertical Empresa" }));

      expect(setSelectedVertical).toHaveBeenCalledWith("empresa");
      expect(push).toHaveBeenCalledWith("/dashboard/empresa");
    });

    it("no muestra el selector a PATIENT, PHYSICIAN ni ADMIN", () => {
      mockUser("PATIENT");
      const { unmount: unmountPatient } = render(<Sidebar />);
      expect(screen.queryByRole("button", { name: "Vertical Salud" })).toBeNull();
      unmountPatient();

      mockUser("PHYSICIAN");
      const { unmount: unmountPhysician } = render(<Sidebar />);
      expect(screen.queryByRole("button", { name: "Vertical Salud" })).toBeNull();
      unmountPhysician();

      mockUser("ADMIN");
      render(<Sidebar />);
      expect(screen.queryByRole("button", { name: "Vertical Salud" })).toBeNull();
    });
  });
});
