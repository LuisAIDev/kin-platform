import { render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import SobreKinPage from "./page";
import { ABOUT_CONTENT } from "@/components/about/aboutContent";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

describe("Página Sobre KIN", () => {
  it("renderiza el título principal y el subtítulo", () => {
    render(<SobreKinPage />);
    expect(screen.getByRole("heading", { level: 1, name: "Sobre KIN" })).toBeInTheDocument();
    expect(screen.getByText(ABOUT_CONTENT.hero.subtitle)).toBeInTheDocument();
  });

  it("renderiza todas las secciones principales", () => {
    render(<SobreKinPage />);
    const sectionTitles = [
      ABOUT_CONTENT.whatIs.title,
      ABOUT_CONTENT.creator.title,
      ABOUT_CONTENT.purpose.title,
      ABOUT_CONTENT.moreThanApp.title,
      ABOUT_CONTENT.showcase.title,
      ABOUT_CONTENT.technology.title,
      ABOUT_CONTENT.architecture.title,
      ABOUT_CONTENT.demonstrates.title,
      ABOUT_CONTENT.vision.title,
      ABOUT_CONTENT.visionProfessional.title,
      ABOUT_CONTENT.whyBuilt.title,
      ABOUT_CONTENT.keepLearning.title,
      ABOUT_CONTENT.audience.title,
    ];
    for (const title of sectionTitles) {
      expect(screen.getByRole("heading", { name: title, level: 2 })).toBeInTheDocument();
    }
  });

  it("muestra el contenido de la sección '¿Qué es KIN?'", () => {
    render(<SobreKinPage />);
    const heading = screen.getByRole("heading", { name: ABOUT_CONTENT.whatIs.title });
    const section = heading.closest("section");
    expect(section).not.toBeNull();
    for (const paragraph of ABOUT_CONTENT.whatIs.paragraphs) {
      expect(within(section!).getByText(paragraph)).toBeInTheDocument();
    }
  });

  it("muestra el creador y su presentación", () => {
    render(<SobreKinPage />);
    expect(screen.getAllByText(ABOUT_CONTENT.creator.name).length).toBeGreaterThanOrEqual(2);
    for (const paragraph of ABOUT_CONTENT.creator.paragraphs) {
      expect(screen.getByText(paragraph)).toBeInTheDocument();
    }
  });

  it("muestra las tecnologías verificadas y la arquitectura", () => {
    render(<SobreKinPage />);
    const techTitle = ABOUT_CONTENT.technology.title;
    expect(screen.getByRole("heading", { name: techTitle })).toBeInTheDocument();

    const firstCategory = ABOUT_CONTENT.technology.categories[0];
    const firstTech = firstCategory.items[0].name;
    expect(screen.getAllByText(firstTech).length).toBeGreaterThanOrEqual(1);

    const architectureSection = screen
      .getByRole("heading", { name: ABOUT_CONTENT.architecture.title })
      .closest("section");
    expect(architectureSection).not.toBeNull();
    const systemFlow = ABOUT_CONTENT.architecture.flows.find((f) => f.id === "sistema")!;
    expect(
      within(architectureSection!).getAllByText(systemFlow.layers[0].label).length,
    ).toBeGreaterThanOrEqual(1);
    expect(
      within(architectureSection!).getAllByText(
        systemFlow.layers[systemFlow.layers.length - 1].label,
      ).length,
    ).toBeGreaterThanOrEqual(1);
  });

  it("muestra la frase final sobria", () => {
    render(<SobreKinPage />);
    for (const line of ABOUT_CONTENT.closing.lines) {
      expect(screen.getByText(line)).toBeInTheDocument();
    }
    expect(screen.getByText(ABOUT_CONTENT.closing.after)).toBeInTheDocument();
  });

  it("muestra la línea conceptual de evolución y las áreas de aprendizaje", () => {
    render(<SobreKinPage />);
    for (const step of ABOUT_CONTENT.vision.timeline) {
      expect(screen.getAllByText(step).length).toBeGreaterThanOrEqual(1);
    }
    for (const area of ABOUT_CONTENT.keepLearning.areas) {
      expect(screen.getAllByText(area).length).toBeGreaterThanOrEqual(1);
    }
    expect(screen.getByText(ABOUT_CONTENT.keepLearning.intro)).toBeInTheDocument();
  });

  it("incluye las secciones personales del perfil profesional", () => {
    render(<SobreKinPage />);
    for (const paragraph of ABOUT_CONTENT.visionProfessional.paragraphs) {
      expect(screen.getByText(paragraph)).toBeInTheDocument();
    }
    for (const paragraph of ABOUT_CONTENT.whyBuilt.paragraphs) {
      expect(screen.getByText(paragraph)).toBeInTheDocument();
    }
  });

  it("incluye el perfil profesional con enlaces externos existentes", () => {
    render(<SobreKinPage />);
    expect(
      screen.getByRole("heading", { name: ABOUT_CONTENT.profile.name }),
    ).toBeInTheDocument();
    expect(screen.getByText(ABOUT_CONTENT.profile.role)).toBeInTheDocument();

    const github = screen.getByRole("link", { name: /GitHub/ });
    const linkedin = screen.getByRole("link", { name: /LinkedIn/ });
    expect(github.getAttribute("href")).toMatch(/^https:\/\//);
    expect(linkedin.getAttribute("href")).toMatch(/^https:\/\//);
    expect(linkedin.getAttribute("href")).toContain("linkedin.com");
  });

  it("mantiene una jerarquía de encabezados accesible (un solo h1, h2 de sección, h3 de subelemento)", () => {
    const { container } = render(<SobreKinPage />);
    const headings = Array.from(container.querySelectorAll("h1, h2, h3"));
    expect(headings.length).toBeGreaterThan(0);
    expect(headings[0].tagName).toBe("H1");

    const count = (tag: string) => headings.filter((h) => h.tagName === tag).length;
    expect(count("H1")).toBe(1);
    expect(count("H2")).toBeGreaterThanOrEqual(13);
    expect(count("H3")).toBeGreaterThanOrEqual(15);
  });

  it("navega hacia Sobre KIN desde la barra superior (variante about)", () => {
    render(<SobreKinPage />);
    const aboutLink = screen.getByRole("link", { name: "Sobre KIN" });
    expect(aboutLink).toHaveAttribute("href", "/sobre-kin");
    expect(aboutLink).toHaveAttribute("aria-current", "page");
  });
});
