import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Navbar from "@/components/layout/Navbar";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

describe("Navbar", () => {
  it("renderiza enlaces de navegación con roles accesibles", () => {
    render(<Navbar />);

    expect(screen.getByRole("link", { name: "Características" })).toHaveAttribute("href", "#caracteristicas");
    expect(screen.getByRole("link", { name: "Precios" })).toHaveAttribute("href", "#precios");
    expect(screen.getByRole("link", { name: "Iniciar sesión" })).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "Comenzar gratis" })).toHaveAttribute("href", "/register");
  });

  it("cambia de estado al hacer scroll", () => {
    render(<Navbar />);
    const nav = screen.getByRole("navigation");
    expect(nav.className).toContain("bg-transparent");

    Object.defineProperty(window, "scrollY", { value: 100, configurable: true });
    fireEvent.scroll(window);

    expect(nav.className).toContain("bg-white/80");

    Object.defineProperty(window, "scrollY", { value: 0, configurable: true });
    fireEvent.scroll(window);
    expect(nav.className).toContain("bg-transparent");
  });

  it("incluye el enlace 'Sobre KIN' en la variante landing", () => {
    render(<Navbar />);
    const aboutLink = screen.getByRole("link", { name: "Sobre KIN" });
    expect(aboutLink).toHaveAttribute("href", "/sobre-kin");
  });

  it("variante about: muestra 'Plataforma' y 'Sobre KIN' activo, sin anclas de la portada", () => {
    render(<Navbar variant="about" />);

    const aboutLink = screen.getByRole("link", { name: "Sobre KIN" });
    expect(aboutLink).toHaveAttribute("href", "/sobre-kin");
    expect(aboutLink).toHaveAttribute("aria-current", "page");

    const platformLink = screen.getByRole("link", { name: "Plataforma" });
    expect(platformLink).toHaveAttribute("href", "/");

    expect(screen.queryByRole("link", { name: "Características" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Precios" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Contacto" })).toBeNull();
  });
});
