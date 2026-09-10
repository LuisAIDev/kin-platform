import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import CarePlanView from "@/components/health/CarePlanView";

describe("CarePlanView", () => {
  it("muestra las recomendaciones del plan", () => {
    render(
      <CarePlanView
        plan={{
          recommendations: ["Controla tu glucosa."],
          sourceConditions: ["diabetes"],
          detailed: [
            { condition: "diabetes", advice: "Controla tu glucosa.", priority: "ALTA" },
          ],
        }}
      />,
    );

    expect(screen.getByRole("heading", { name: "Plan de cuidado" })).toBeInTheDocument();
    expect(screen.getByText("diabetes")).toBeInTheDocument();
    expect(screen.getByText("Controla tu glucosa.")).toBeInTheDocument();
    expect(screen.getByText("ALTA")).toBeInTheDocument();
  });

  it("muestra estado vacío sin recomendaciones", () => {
    render(
      <CarePlanView
        plan={{ recommendations: [], sourceConditions: [], detailed: [] }}
      />,
    );

    expect(screen.getByText(/Aún no hay recomendaciones de cuidado/)).toBeInTheDocument();
  });
});
