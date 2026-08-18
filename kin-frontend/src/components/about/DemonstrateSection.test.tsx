import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import DemonstrateSection from "@/components/about/DemonstrateSection";
import { ABOUT_CONTENT } from "@/components/about/aboutContent";

describe("DemonstrateSection", () => {
  it("renderiza el título de la sección", () => {
    render(<DemonstrateSection />);
    expect(screen.getByRole("heading", { name: "Lo que KIN demuestra" })).toBeInTheDocument();
  });

  it("muestra las 8 capacidades de ingeniería", () => {
    render(<DemonstrateSection />);
    expect(ABOUT_CONTENT.demonstrates.cards).toHaveLength(8);
    for (const card of ABOUT_CONTENT.demonstrates.cards) {
      expect(
        screen.getByRole("heading", { name: card.title, level: 3 }),
      ).toBeInTheDocument();
      expect(screen.getByText(card.description)).toBeInTheDocument();
    }
  });
});
