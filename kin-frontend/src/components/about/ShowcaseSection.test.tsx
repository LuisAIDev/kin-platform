import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ShowcaseSection from "@/components/about/ShowcaseSection";
import { ABOUT_CONTENT, QA_METRICS } from "@/components/about/aboutContent";

describe("ShowcaseSection", () => {
  it("renderiza el título de la sección", () => {
    render(<ShowcaseSection />);
    expect(screen.getByRole("heading", { name: "Engineering Showcase" })).toBeInTheDocument();
  });

  it("renderiza todas las áreas técnicas del showcase", () => {
    render(<ShowcaseSection />);
    const groups = ABOUT_CONTENT.showcase.groups;
    expect(groups.length).toBeGreaterThanOrEqual(6);
    for (const group of groups) {
      expect(
        screen.getByRole("heading", { name: group.title, level: 3 }),
      ).toBeInTheDocument();
    }
  });

  it("muestra las métricas verificadas de QA", () => {
    render(<ShowcaseSection />);
    expect(screen.getByText(String(QA_METRICS.frontendTests))).toBeInTheDocument();
    expect(screen.getByText(String(QA_METRICS.testFiles))).toBeInTheDocument();
    expect(screen.getByText(QA_METRICS.e2e)).toBeInTheDocument();
  });

  it("cada tecnología mostrada corresponde al proyecto real (config verificada)", () => {
    render(<ShowcaseSection />);
    const allItems = ABOUT_CONTENT.showcase.groups.flatMap((g) => g.items);
    expect(allItems.length).toBeGreaterThan(0);
    for (const item of allItems) {
      expect(screen.getAllByText(item.name).length).toBeGreaterThanOrEqual(1);
    }
  });

  it("no inventa proveedores de IA: usa exactamente los grupos configurados", () => {
    const { container } = render(<ShowcaseSection />);
    const groupCards = container.querySelectorAll("article");
    expect(groupCards).toHaveLength(ABOUT_CONTENT.showcase.groups.length);

    const iaGroup = ABOUT_CONTENT.showcase.groups.find((g) => g.id === "ia")!;
    for (const item of iaGroup.items) {
      expect(screen.getByText(item.name)).toBeInTheDocument();
    }
    expect(screen.queryByText("Ollama")).toBeNull();
  });
});
