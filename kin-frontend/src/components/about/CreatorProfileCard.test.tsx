import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import CreatorProfileCard from "@/components/about/CreatorProfileCard";
import { ABOUT_CONTENT, ABOUT_LINKS } from "@/components/about/aboutContent";

describe("CreatorProfileCard", () => {
  it("muestra el nombre y el rol del creador", () => {
    render(<CreatorProfileCard />);
    expect(
      screen.getByRole("heading", { name: ABOUT_CONTENT.profile.name }),
    ).toBeInTheDocument();
    expect(screen.getByText(ABOUT_CONTENT.profile.role)).toBeInTheDocument();
  });

  it("muestra la descripción del perfil", () => {
    render(<CreatorProfileCard />);
    expect(screen.getByText(ABOUT_CONTENT.profile.description)).toBeInTheDocument();
  });

  it("enlaza el perfil de GitHub del creador", () => {
    render(<CreatorProfileCard />);
    const link = screen.getByRole("link", { name: /GitHub/ });
    expect(link).toHaveAttribute("href", ABOUT_LINKS.github.href);
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("enlaza el perfil de LinkedIn existente en el proyecto", () => {
    render(<CreatorProfileCard />);
    const link = screen.getByRole("link", { name: /LinkedIn/ });
    expect(link).toHaveAttribute("href", ABOUT_LINKS.linkedin.href);
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("no inventa enlaces adicionales", () => {
    render(<CreatorProfileCard />);
    const links = screen.getAllByRole("link");
    expect(links).toHaveLength(2);
  });
});
