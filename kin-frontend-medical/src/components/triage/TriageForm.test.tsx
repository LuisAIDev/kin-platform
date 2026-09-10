import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TriageForm from "@/components/triage/TriageForm";

const { triageService } = vi.hoisted(() => ({ triageService: { listSymptoms: vi.fn(), analyze: vi.fn() } }));
const { differentialService } = vi.hoisted(() => ({ differentialService: { analyze: vi.fn(), byConsultation: vi.fn() } }));

vi.mock("@/services/triage", () => ({ triageService }));
vi.mock("@/services/differential", () => ({ differentialService }));

const SYMPTOMS = [
  { id: "s1", name: "fiebre", description: "Temperatura elevada", icdCode: "R50.9" },
  { id: "s2", name: "tos", description: "Tos", icdCode: null },
  { id: "s3", name: "dolor de cabeza", description: "Cefalea", icdCode: "R51" },
];

const TRIAGE_RESULT = {
  status: "SUCCESS" as const,
  results: [
    {
      conditionId: "c1",
      condition: "Gripe",
      description: "Infección viral",
      probability: 0.78,
      severity: "MODERADO" as const,
      urgency: "MEDIA" as const,
      recommendation: "Consulta médica.",
      matchedSymptoms: ["fiebre", "tos"],
    },
  ],
  unrecognizedSymptoms: [],
  disclaimer: "Herramienta de apoyo. No sustituye el diagnóstico médico.",
};

describe("TriageForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    triageService.listSymptoms.mockResolvedValue(SYMPTOMS);
    triageService.analyze.mockResolvedValue(TRIAGE_RESULT);
    differentialService.analyze.mockResolvedValue({
      status: "NO_MATCH" as const,
      items: [],
      unrecognizedSymptoms: [],
      disclaimer: "Herramienta de apoyo.",
    });
  });

  it("muestra el aviso informativo y los síntomas del catálogo", async () => {
    render(<TriageForm />);

    expect(await screen.findByRole("heading", { name: "Triaje Digital" })).toBeInTheDocument();
    expect(
      screen.getByText(/No sustituye la evaluación ni el diagnóstico/),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "fiebre" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "tos" })).toBeInTheDocument();
  });

  it("analiza los síntomas seleccionados y muestra los resultados", async () => {
    const user = userEvent.setup();
    render(<TriageForm />);

    await user.click(await screen.findByRole("button", { name: "fiebre" }));
    await user.click(screen.getByRole("button", { name: "tos" }));
    await user.click(screen.getByRole("button", { name: /Analizar/ }));

    expect(await screen.findByRole("heading", { name: "Gripe" })).toBeInTheDocument();
    expect(screen.getByText("78%")).toBeInTheDocument();
    expect(screen.getByText(/Severidad: MODERADO/)).toBeInTheDocument();
    expect(screen.getByText(/Urgencia: MEDIA/)).toBeInTheDocument();
    expect(triageService.analyze).toHaveBeenCalledWith(["fiebre", "tos"]);
  });

  it("deshabilita el botón Analizar sin síntomas seleccionados", async () => {
    render(<TriageForm />);
    await screen.findByRole("button", { name: "fiebre" });

    expect(screen.getByRole("button", { name: /Analizar/ })).toBeDisabled();
  });

  it("filtra los síntomas por búsqueda", async () => {
    const user = userEvent.setup();
    render(<TriageForm />);
    await screen.findByRole("button", { name: "fiebre" });

    await user.type(screen.getByLabelText("Buscar síntoma"), "tos");

    expect(screen.getByRole("button", { name: "tos" })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByRole("button", { name: "fiebre" })).not.toBeInTheDocument();
    });
  });

  it("muestra el estado vacío cuando no hay coincidencias", async () => {
    const user = userEvent.setup();
    triageService.analyze.mockResolvedValue({
      status: "NO_MATCH" as const,
      results: [],
      unrecognizedSymptoms: ["xyz"],
      disclaimer: "Herramienta de apoyo.",
    });
    render(<TriageForm />);

    await user.click(await screen.findByRole("button", { name: "fiebre" }));
    await user.click(screen.getByRole("button", { name: /Analizar/ }));

    expect(
      await screen.findByText(/No encontramos condiciones compatibles/),
    ).toBeInTheDocument();
  });
});
