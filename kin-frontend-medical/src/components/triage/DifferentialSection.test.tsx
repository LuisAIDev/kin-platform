import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DifferentialSection from "@/components/triage/DifferentialSection";

const { differentialService } = vi.hoisted(() => ({ differentialService: { analyze: vi.fn() } }));

vi.mock("@/services/differential", () => ({ differentialService }));

const DIFFERENTIAL_RESULT = {
  status: "SUCCESS" as const,
  items: [
    {
      conditionId: "c1",
      condition: "Gripe",
      description: "Infección viral",
      probability: 0.62,
      severity: "MODERADO" as const,
      urgency: "MEDIA" as const,
      matchedSymptoms: ["fiebre", "tos"],
      riskFactors: [{ factor: "fumador", weight: 0.4, description: "Tabaquismo" }],
      recommendedTests: [{ test: "PCR respiratoria", description: "Detección viral" }],
      reasoning: "Gripe se sugiere por la coincidencia de 2 síntomas.",
    },
  ],
  unrecognizedSymptoms: [],
  disclaimer: "El diagnóstico diferencial es una herramienta de apoyo informativo.",
};

describe("DifferentialSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    differentialService.analyze.mockResolvedValue(DIFFERENTIAL_RESULT);
  });

  it("no renderiza nada sin síntomas", () => {
    const { container } = render(<DifferentialSection symptoms={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("muestra el diagnóstico diferencial con factores de riesgo y pruebas", async () => {
    const user = userEvent.setup();
    render(<DifferentialSection symptoms={["fiebre", "tos"]} />);

    expect(
      screen.getByRole("heading", { name: "Diagnóstico diferencial" }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /Analizar diagnóstico diferencial/ }));

    expect(await screen.findByRole("heading", { name: "Gripe" })).toBeInTheDocument();
    expect(screen.getByText("62%")).toBeInTheDocument();
    expect(screen.getByText("fumador · Tabaquismo")).toBeInTheDocument();
    expect(screen.getByText(/PCR respiratoria/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "MedlinePlus" })).toHaveAttribute(
      "href",
      "https://vsearch.nlm.nih.gov/vivisimo/cgi-bin/query-meta?v%3Aproject=medlineplus&v%3Asources=medlineplus-bundle&query=Gripe",
    );
    expect(screen.getByRole("link", { name: "Mayo Clinic" })).toHaveAttribute(
      "href",
      "https://www.mayoclinic.org/es/search?query=Gripe",
    );
    expect(screen.getByRole("link", { name: "MSD Manuals" })).toHaveAttribute(
      "href",
      "https://www.msdmanuals.com/es/hogar/searchresults?query=Gripe",
    );
    expect(screen.getByRole("link", { name: "CDC" })).toHaveAttribute(
      "href",
      "https://www.cdc.gov/spanish/enfermedades/index.html",
    );
    expect(screen.getByRole("link", { name: "OMS" })).toHaveAttribute(
      "href",
      "https://www.who.int/es/health-topics",
    );
    expect(screen.getByRole("link", { name: "AAFP" })).toHaveAttribute(
      "href",
      "https://www.aafp.org/family-physician/patient-care/conditions-diseases.html",
    );
    expect(screen.queryByRole("link", { name: "Wikipedia" })).not.toBeInTheDocument();
    expect(differentialService.analyze).toHaveBeenCalledWith(["fiebre", "tos"], []);
  });

  it("envía los factores de riesgo seleccionados", async () => {
    const user = userEvent.setup();
    render(<DifferentialSection symptoms={["fiebre"]} />);

    await user.click(screen.getByRole("button", { name: "Fumador" }));
    await user.click(screen.getByRole("button", { name: /Analizar diagnóstico diferencial/ }));

    await screen.findByRole("heading", { name: "Gripe" });
    expect(differentialService.analyze).toHaveBeenCalledWith(["fiebre"], ["fumador"]);
  });

  it("muestra estado vacío cuando no hay coincidencias", async () => {
    const user = userEvent.setup();
    differentialService.analyze.mockResolvedValue({
      status: "NO_MATCH" as const,
      items: [],
      unrecognizedSymptoms: ["xyz"],
      disclaimer: "Herramienta de apoyo.",
    });
    render(<DifferentialSection symptoms={["fiebre"]} />);

    await user.click(screen.getByRole("button", { name: /Analizar diagnóstico diferencial/ }));

    expect(
      await screen.findByText(/No se pudo construir un diagnóstico diferencial/),
    ).toBeInTheDocument();
  });
});
