import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ProgressCircle from "@/components/ProgressCircle";

describe("ProgressCircle", () => {
  it("muestra el porcentaje de forma accesible", () => {
    render(<ProgressCircle percentage={75} />);

    expect(screen.getAllByText("75%").length).toBeGreaterThan(0);
    expect(screen.getByText("Progreso")).toBeInTheDocument();
  });

  it("usa color verde para porcentaje alto", () => {
    const { container } = render(<ProgressCircle percentage={90} />);
    const stroke = container.querySelector("circle[stroke='#16a34a']");
    expect(stroke).not.toBeNull();
  });

  it("usa color azul para porcentaje medio", () => {
    const { container } = render(<ProgressCircle percentage={60} />);
    expect(container.querySelector("circle[stroke='#4f46e5']")).not.toBeNull();
  });

  it("usa color rojo para porcentaje bajo", () => {
    const { container } = render(<ProgressCircle percentage={20} />);
    expect(container.querySelector("circle[stroke='#dc2626']")).not.toBeNull();
  });
});
