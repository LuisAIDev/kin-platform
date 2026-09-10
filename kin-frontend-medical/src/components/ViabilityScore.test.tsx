import { act, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ViabilityScore from "@/components/ViabilityScore";

describe("ViabilityScore", () => {
  it("muestra la puntuación y la etiqueta Alto", () => {
    vi.useFakeTimers();
    render(<ViabilityScore score={85} />);
    vi.advanceTimersByTime(100);
    vi.useRealTimers();

    expect(screen.getByText("85")).toBeInTheDocument();
    expect(screen.getByText("Alto")).toBeInTheDocument();
  });

  it("muestra la etiqueta Medio", () => {
    vi.useFakeTimers();
    render(<ViabilityScore score={50} />);
    vi.advanceTimersByTime(100);
    vi.useRealTimers();

    expect(screen.getByText("Medio")).toBeInTheDocument();
  });

  it("muestra la etiqueta Bajo", () => {
    vi.useFakeTimers();
    render(<ViabilityScore score={20} />);
    vi.advanceTimersByTime(100);
    vi.useRealTimers();

    expect(screen.getByText("Bajo")).toBeInTheDocument();
  });

  it("anima el offset del círculo tras el timeout", () => {
    vi.useFakeTimers();
    const { container } = render(<ViabilityScore score={80} />);
    const circle = container.querySelector("circle[stroke='#22c55e']") as SVGCircleElement;
    const before = circle.getAttribute("stroke-dashoffset");
    act(() => {
      vi.advanceTimersByTime(100);
    });
    const after = circle.getAttribute("stroke-dashoffset");
    vi.useRealTimers();

    expect(before).not.toBe(after);
  });
});
