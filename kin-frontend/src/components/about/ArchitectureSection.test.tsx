import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ArchitectureSection from "@/components/about/ArchitectureSection";
import { ABOUT_CONTENT } from "@/components/about/aboutContent";

describe("ArchitectureSection", () => {
  it("renderiza el título de la sección", () => {
    render(<ArchitectureSection />);
    expect(
      screen.getByRole("heading", { name: ABOUT_CONTENT.architecture.title }),
    ).toBeInTheDocument();
  });

  it("representa el diagrama general (KIN → Frontend/Backend/IA → PostgreSQL → Cloud)", () => {
    render(<ArchitectureSection />);
    const hub = screen.getByLabelText("Diagrama de arquitectura");
    const { root, branches, base, deploy } = ABOUT_CONTENT.architecture.hub;

    expect(within(hub).getByText(root.label)).toBeInTheDocument();
    for (const branch of branches) {
      expect(within(hub).getByText(branch.label)).toBeInTheDocument();
    }
    expect(within(hub).getByText(base.label)).toBeInTheDocument();
    expect(within(hub).getByText(deploy.label)).toBeInTheDocument();
  });

  it("representa los componentes reales del sistema", () => {
    render(<ArchitectureSection />);
    const componentsHeading = screen.getByRole("heading", { name: "Componentes del sistema" });
    const componentsWrapper = componentsHeading.closest("div");
    expect(componentsWrapper).not.toBeNull();

    for (const component of ABOUT_CONTENT.architecture.components) {
      expect(within(componentsWrapper!).getByText(component.label)).toBeInTheDocument();
    }
  });

  it("representa ambas capas del flujo del sistema", () => {
    render(<ArchitectureSection />);
    const systemFlow = ABOUT_CONTENT.architecture.flows.find((f) => f.id === "sistema");
    expect(systemFlow).toBeDefined();

    const flowWrapper = screen.getByRole("heading", { name: systemFlow!.title }).closest("div");
    expect(flowWrapper).not.toBeNull();
    for (const layer of systemFlow!.layers) {
      expect(within(flowWrapper!).getByText(layer.label)).toBeInTheDocument();
    }
  });

  it("representa la integración de IA (proveedor real)", () => {
    render(<ArchitectureSection />);
    const aiFlow = ABOUT_CONTENT.architecture.flows.find((f) => f.id === "ia");
    expect(aiFlow).toBeDefined();

    const flowWrapper = screen.getByRole("heading", { name: aiFlow!.title }).closest("div");
    expect(flowWrapper).not.toBeNull();
    for (const layer of aiFlow!.layers) {
      expect(within(flowWrapper!).getByText(layer.label)).toBeInTheDocument();
    }
  });

  it("cada flujo usa exactamente sus capas configuradas", () => {
    const { container } = render(<ArchitectureSection />);

    for (const flow of ABOUT_CONTENT.architecture.flows) {
      const flowWrapper = screen.getByRole("heading", { name: flow.title }).closest("div");
      const layerItems = flowWrapper!.querySelectorAll("li");
      expect(layerItems.length).toBe(flow.layers.length);
    }
    expect(container.querySelectorAll("li").length).toBeGreaterThan(0);
  });
});
