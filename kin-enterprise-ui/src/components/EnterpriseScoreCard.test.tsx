import { render, screen } from "@testing-library/react";
import { EnterpriseScoreCard } from "./EnterpriseScoreCard";
import type { EnterpriseScoreSection } from "../types/enterprise";

describe("EnterpriseScoreCard", () => {
  it("muestra estado pendiente cuando no hay score", () => {
    render(<EnterpriseScoreCard score={null} />);
    expect(screen.getByText(/Pendiente de generación/)).toBeInTheDocument();
  });

  it("muestra el score global y el grado", () => {
    const score: EnterpriseScoreSection = {
      overall: 72,
      grade: "FAIR",
      confidence: 0.82,
      market: 70,
      innovation: 65,
      viability: 80,
      financial: 60,
      risk: 50,
      scalability: 75,
      team: 68,
      sustainability: 55,
    };
    render(<EnterpriseScoreCard score={score} />);
    expect(screen.getByText("72")).toBeInTheDocument();
    expect(screen.getByText(/FAIR/)).toBeInTheDocument();
    expect(screen.getByText(/Mercado: 70/)).toBeInTheDocument();
    expect(screen.getByText(/Innovación: 65/)).toBeInTheDocument();
  });

  it("muestra el score cuando el objeto está presente pero overall es null", () => {
    render(<EnterpriseScoreCard score={{ overall: null } as EnterpriseScoreSection} />);
    expect(screen.getByText(/Pendiente de generación/)).toBeInTheDocument();
  });
});
