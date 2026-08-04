import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { ProgressBar } from "@/components/enterprise/ProgressBar";

describe("ProgressBar", () => {
  it("muestra el porcentaje y el ancho correcto", () => {
    render(<ProgressBar progress={42} />);
    expect(screen.getByTestId("progress-bar")).toBeInTheDocument();
    expect(screen.getByText("42%")).toBeInTheDocument();
    const fill = document.querySelector(".progress-fill");
    expect(fill).toHaveStyle({ width: "42%" });
  });

  it("acota el progreso a 0-100", () => {
    const { rerender } = render(<ProgressBar progress={150} />);
    expect(screen.getByText("100%")).toBeInTheDocument();
    rerender(<ProgressBar progress={-5} />);
    expect(screen.getByText("0%")).toBeInTheDocument();
  });

  it("aplica la clase de color según el estado terminal", () => {
    render(<ProgressBar progress={100} status="COMPLETED" />);
    expect(document.querySelector(".progress-fill-COMPLETED")).toBeInTheDocument();
  });

  it("muestra una etiqueta personalizada", () => {
    render(<ProgressBar progress={10} label="Avance" />);
    expect(screen.getByText("Avance")).toBeInTheDocument();
  });

  it("expone role progressbar con valores de accesibilidad", () => {
    render(<ProgressBar progress={70} />);
    const bar = screen.getByRole("progressbar");
    expect(bar).toHaveAttribute("aria-valuenow", "70");
  });
});
