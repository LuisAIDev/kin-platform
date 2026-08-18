import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import TechnologySection from "@/components/about/TechnologySection";
import { ABOUT_CONTENT } from "@/components/about/aboutContent";

describe("TechnologySection", () => {
  it("renderiza el título de la sección", () => {
    render(<TechnologySection />);
    expect(
      screen.getByRole("heading", { name: "Tecnologías y capacidades demostradas" }),
    ).toBeInTheDocument();
  });

  it("renderiza cada categoría con su título", () => {
    render(<TechnologySection />);
    for (const category of ABOUT_CONTENT.technology.categories) {
      expect(
        screen.getByRole("heading", { name: category.title, level: 3 }),
      ).toBeInTheDocument();
    }
  });

  it("renderiza cada tecnología verificada en el código", () => {
    render(<TechnologySection />);
    const allItems = ABOUT_CONTENT.technology.categories.flatMap((c) => c.items);
    expect(allItems.length).toBeGreaterThan(0);
    for (const item of allItems) {
      expect(screen.getByText(item.name)).toBeInTheDocument();
    }
  });

  it("agrupa las tecnologías por categoría (dentro de la misma tarjeta)", () => {
    const { container } = render(<TechnologySection />);
    const categoryCards = container.querySelectorAll("article");
    const categories = ABOUT_CONTENT.technology.categories;

    expect(categoryCards).toHaveLength(categories.length);

    categories.forEach((category, index) => {
      const card = categoryCards[index];
      for (const item of category.items) {
        expect(card).toHaveTextContent(item.name);
      }
      const otherCategories = categories.filter((c) => c.id !== category.id);
      for (const other of otherCategories) {
        expect(card).not.toHaveTextContent(other.title);
      }
    });
  });
});
