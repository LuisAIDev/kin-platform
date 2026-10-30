import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ManualUsuarioPage from "./page";
import { MANUAL_SECTIONS } from "@/components/manual/manualContent";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

describe("Página pública /manual-usuario", () => {
  it("renderiza el hero con el título y el subtítulo del manual", () => {
    render(<ManualUsuarioPage />);

    expect(
      screen.getByRole("heading", { level: 1, name: "Manual de Usuario de KIN" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Tu idea, convertida en un proyecto claro y accionable en menos de 60 minutos.",
      ),
    ).toBeInTheDocument();
  });

  it("incluye todas las secciones principales con su ancla", () => {
    render(<ManualUsuarioPage />);

    const headings = [
      "Bienvenido a KIN",
      "¿Qué hace diferente a KIN?",
      "Paso 1: Creando tu cuenta e iniciando sesión",
      "Paso 2: Tu primer proyecto (El Chat Inteligente)",
      "Paso 3: Entendiendo tu Dashboard y el Score",
      "Paso 4: Descargando tus Reportes",
      "Preguntas Frecuentes (FAQ)",
      "Consejos para aprovechar KIN al máximo",
    ];

    for (const heading of headings) {
      expect(
        screen.getByRole("heading", { level: 2, name: heading }),
      ).toBeInTheDocument();
    }

    for (const section of MANUAL_SECTIONS) {
      expect(document.getElementById(section.id)).not.toBeNull();
    }
  });

  it("muestra el sidebar con enlaces a cada sección", () => {
    render(<ManualUsuarioPage />);

    const nav = screen.getByRole("navigation", { name: "Índice del manual" });
    for (const section of MANUAL_SECTIONS) {
      expect(
        screen.getByRole("link", { name: section.label }),
      ).toHaveAttribute("href", `#${section.id}`);
    }
    expect(nav).toBeInTheDocument();
  });

  it("explica el principio rector Java decide / LLM comunica", () => {
    render(<ManualUsuarioPage />);

    expect(
      screen.getAllByText("Java decide. El LLM únicamente comunica.").length,
    ).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Motor determinista · Java")).toBeInTheDocument();
    expect(screen.getByText("Capa de comunicación · IA")).toBeInTheDocument();
  });

  it("incluye el CTA final hacia el registro", () => {
    render(<ManualUsuarioPage />);

    const cta = screen
      .getAllByRole("link", { name: "Empezar gratis ahora" })
      .find((link) => link.getAttribute("href") === "/register");
    expect(cta).toBeDefined();
    expect(cta).toHaveAttribute("href", "/register");
  });

  it("renderiza las preguntas frecuentes en acordeones", () => {
    render(<ManualUsuarioPage />);

    expect(
      screen.getByText("¿La IA toma las decisiones por mí?"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("¿Es realmente gratis?"),
    ).toBeInTheDocument();
  });
});
