import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import StatCard from "@/components/insights/StatCard";
import RecommendationList from "@/components/insights/RecommendationList";
import TimelineView from "@/components/insights/TimelineView";
import FeatureAdoptionList from "@/components/insights/FeatureAdoptionList";

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

describe("StatCard", () => {
  it("muestra label, valor y hint", () => {
    render(<StatCard label="Mensajes" value="42" hint="hoy" />);

    expect(screen.getByText("Mensajes")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
    expect(screen.getByText("hoy")).toBeInTheDocument();
  });
});

describe("RecommendationList", () => {
  it("muestra recomendaciones con enlace", () => {
    render(<RecommendationList recommendations={[
      { id: "r1", type: "next-step", title: "Crea un proyecto", description: "Empieza", href: "/x" },
    ]} />);

    expect(screen.getByRole("link", { name: "Ir →" })).toHaveAttribute("href", "/x");
    expect(screen.getByText("Crea un proyecto")).toBeInTheDocument();
  });

  it("muestra estado vacío", () => {
    render(<RecommendationList recommendations={[]} />);
    expect(screen.getByText(/Sin recomendaciones/)).toBeInTheDocument();
  });
});

describe("TimelineView", () => {
  it("ordena y muestra eventos", () => {
    render(<TimelineView events={[
      { id: "1", timestamp: "2026-08-05T10:00:00Z", name: "ai_message" },
    ]} />);

    expect(screen.getByText("ai_message")).toBeInTheDocument();
  });

  it("muestra estado vacío", () => {
    render(<TimelineView events={[]} />);
    expect(screen.getByText(/Aún no hay actividad/)).toBeInTheDocument();
  });
});

describe("FeatureAdoptionList", () => {
  it("muestra funciones ordenadas por uso", () => {
    render(<FeatureAdoptionList features={[
      { feature: "enterprise", uses: 1, lastUsed: null },
      { feature: "analytics", uses: 3, lastUsed: null },
    ]} />);

    expect(screen.getByText("analytics")).toBeInTheDocument();
    expect(screen.getByText("3 usos")).toBeInTheDocument();
  });

  it("muestra estado vacío", () => {
    render(<FeatureAdoptionList features={[]} />);
    expect(screen.getByText(/Aún no hay datos de adopción/)).toBeInTheDocument();
  });
});
