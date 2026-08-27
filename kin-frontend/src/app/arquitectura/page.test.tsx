import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ArquitecturaPage from "./page";
import {
  ARCH_ADR,
  ARCH_HERO,
  ARCH_PIPELINE,
  ARCH_QUALITY,
  ARCH_PRIVATE_REPO,
} from "@/components/architecture/architectureContent";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

describe("Página pública /arquitectura", () => {
  it("renderiza el hero con el título y el subtítulo público", () => {
    render(<ArquitecturaPage />);
    expect(screen.getByRole("heading", { name: ARCH_HERO.title })).toBeInTheDocument();
    expect(screen.getByText(ARCH_HERO.subtitle)).toBeInTheDocument();
  });

  it("muestra el principio arquitectónico central", () => {
    render(<ArquitecturaPage />);
    expect(
      screen.getByRole("heading", { name: "Java decide. El LLM únicamente comunica." }),
    ).toBeInTheDocument();
  });

  it("incluye el resumen ejecutivo para evaluación técnica", () => {
    render(<ArquitecturaPage />);
    expect(
      screen.getByRole("heading", { name: "KIN en una mirada técnica" }),
    ).toBeInTheDocument();
    expect(screen.getByText("El problema")).toBeInTheDocument();
    expect(screen.getByText("La arquitectura")).toBeInTheDocument();
  });

  it("muestra las 19 categorías de proyecto", () => {
    render(<ArquitecturaPage />);
    expect(screen.getByText("categorías de proyecto")).toBeInTheDocument();
    expect(screen.getAllByText("Fintech").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Logística").length).toBeGreaterThan(0);
  });

  it("muestra la cifra verificada de etapas del pipeline", () => {
    render(<ArquitecturaPage />);
    expect(screen.getByText("etapas de procesamiento")).toBeInTheDocument();
    expect(screen.getAllByText(String(ARCH_PIPELINE.count)).length).toBeGreaterThan(0);
  });

  it("incluye la sección Knowledge Engine", () => {
    render(<ArquitecturaPage />);
    expect(
      screen.getByRole("heading", { name: /Knowledge Engine/ }),
    ).toBeInTheDocument();
  });

  it("muestra las métricas de calidad de la última suite", () => {
    render(<ArquitecturaPage />);
    const frontendMetric = ARCH_QUALITY.metrics.find((m) => m.label === "tests frontend");
    expect(frontendMetric).toBeDefined();
    expect(screen.getByText(frontendMetric!.value)).toBeInTheDocument();
  });

  it("muestra el número verificado de ADRs", () => {
    render(<ArquitecturaPage />);
    expect(screen.getByText(String(ARCH_ADR.count))).toBeInTheDocument();
    expect(screen.getByText("ADRs documentados")).toBeInTheDocument();
  });

  it("enlaza la solicitud de evaluación al mecanismo de contacto existente", () => {
    render(<ArquitecturaPage />);
    const cta = screen.getByRole("link", { name: ARCH_PRIVATE_REPO.ctaLabel });
    expect(cta).toHaveAttribute("href", ARCH_PRIVATE_REPO.ctaHref);
  });

  it("navega a la portada y a Sobre KIN desde el footer", () => {
    render(<ArquitecturaPage />);
    const footer = screen.getByRole("contentinfo");
    expect(within(footer).getByRole("link", { name: "Plataforma" })).toHaveAttribute("href", "/");
    expect(within(footer).getByRole("link", { name: "Sobre KIN" })).toHaveAttribute(
      "href",
      "/sobre-kin",
    );
    expect(within(footer).getByRole("link", { name: "Arquitectura técnica" })).toHaveAttribute(
      "href",
      "/arquitectura",
    );
  });
});
