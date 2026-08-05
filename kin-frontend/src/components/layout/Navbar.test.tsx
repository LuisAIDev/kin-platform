import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Navbar from "@/components/layout/Navbar";

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
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
});
