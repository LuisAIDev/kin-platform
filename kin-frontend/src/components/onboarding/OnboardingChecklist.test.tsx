import { act, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import OnboardingChecklist from "@/components/onboarding/OnboardingChecklist";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>{children}</a>
  ),
}));

describe("OnboardingChecklist", () => {
  beforeEach(() => localStorage.clear());

  it("muestra la bienvenida con los pasos", () => {
    render(<OnboardingChecklist />);

    expect(screen.getByText("Bienvenido a KIN 🎉")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Crear tu primer proyecto/ })).toBeInTheDocument();
    expect(screen.getByText(/0\/3 completados/)).toBeInTheDocument();
  });

  it("marca un paso como completado al pulsar su enlace", async () => {
    render(<OnboardingChecklist />);

    const link = screen.getByRole("link", { name: /Crear tu primer proyecto/ });
    act(() => link.click());

    await vi.waitFor(() => {
      const stored = JSON.parse(localStorage.getItem("kin_onboarding_v1") ?? "[]");
      expect(stored).toContain("create-project");
    });
  });

  it("se puede cerrar con el botón de cierre", () => {
    render(<OnboardingChecklist />);

    act(() => screen.getByRole("button", { name: "Cerrar bienvenida" }).click());

    expect(screen.queryByText("Bienvenido a KIN 🎉")).toBeNull();
  });

  it("no se muestra cuando el checklist está completo", () => {
    localStorage.setItem("kin_onboarding_v1", JSON.stringify(["create-project", "chat-ai", "explore-enterprise"]));

    render(<OnboardingChecklist />);

    expect(screen.queryByText("Bienvenido a KIN 🎉")).toBeNull();
  });
});
