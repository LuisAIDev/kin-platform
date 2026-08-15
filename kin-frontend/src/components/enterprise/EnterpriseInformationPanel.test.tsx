import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { EnterpriseInformationPanel } from "@/components/enterprise/EnterpriseInformationPanel";
import { enterpriseApi } from "@/services/enterpriseApi";
import { projectInfoService } from "@/services/projectInfo";
import type { EnterpriseInformation } from "@/types/enterprise";

vi.mock("@/services/enterpriseApi", async () => {
  const actual = await vi.importActual<typeof import("@/services/enterpriseApi")>(
    "@/services/enterpriseApi",
  );
  return {
    ...actual,
    enterpriseApi: { ...actual.enterpriseApi, getInformation: vi.fn() },
  };
});

vi.mock("@/services/projectInfo", async () => {
  const actual = await vi.importActual<typeof import("@/services/projectInfo")>(
    "@/services/projectInfo",
  );
  return {
    ...actual,
    projectInfoService: { ...actual.projectInfoService, confirmInfo: vi.fn() },
  };
});

const information: EnterpriseInformation = {
  resolvedDimensions: [
    {
      dimension: "PROJECT_NAME",
      displayName: "Nombre del proyecto",
      value: "KIN SaaS",
      sourceType: "USER_INPUT",
      state: "CONFIRMED",
      origin: "Usuario",
    },
    {
      dimension: "MVP",
      displayName: "MVP / validación temprana",
      value: null,
      sourceType: null,
      state: "PENDING",
      origin: null,
    },
  ],
  supplemental: {
    financial: {},
    market: { mercado_objetivo: { value: "SME", sourceType: "IMPORTED_DOCUMENT", state: "IMPORTED", origin: "Documento" } },
    impact: {},
    risk: {},
    breakeven: { value: null, sourceType: null, state: "NOT_AVAILABLE", origin: null },
    breakevenMissing: ["costos_fijos", "margen de contribución"],
    breakevenCalculable: false,
  },
  documents: [
    {
      id: "d1",
      filename: "plan_negocio.pdf",
      mimeType: "application/pdf",
      size: 2048,
      status: "PROCESADO",
      createdAt: "2026-08-10T00:00:00Z",
      summary: "Plan de negocio con inversión inicial.",
      relevantFacts: ["inversión inicial de 80 millones"],
    },
  ],
  conflicts: [],
};

describe("EnterpriseInformationPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(enterpriseApi.getInformation).mockResolvedValue(information);
  });

  it("muestra dimensiones con dato, origen y estado", async () => {
    render(<EnterpriseInformationPanel projectId="p1" />);

    expect(await screen.findByTestId("info-sources-panel")).toBeInTheDocument();
    expect(screen.getByText("Nombre del proyecto")).toBeInTheDocument();
    expect(screen.getByText("KIN SaaS")).toBeInTheDocument();
    expect(screen.getByText("Confirmado")).toBeInTheDocument();
  });

  it("dato ausente se muestra como Por definir y Pendiente, nunca 0", async () => {
    render(<EnterpriseInformationPanel projectId="p1" />);

    await screen.findByTestId("info-sources-panel");
    expect(screen.getAllByText("Por definir").length).toBeGreaterThan(0);
    expect(screen.getByText("Pendiente")).toBeInTheDocument();
  });

  it("dato de documento conserva su origen", async () => {
    render(<EnterpriseInformationPanel projectId="p1" />);

    await screen.findByTestId("info-sources-panel");
    expect(screen.getByText("SME")).toBeInTheDocument();
    expect(screen.getByText("Importado")).toBeInTheDocument();
  });

  it("punto de equilibrio no calculable lista los datos faltantes", async () => {
    render(<EnterpriseInformationPanel projectId="p1" />);

    await screen.findByTestId("info-sources-panel");
    expect(screen.getByText(/No calculable con los datos disponibles/)).toBeInTheDocument();
    expect(screen.getByText(/costos_fijos, margen de contribución/)).toBeInTheDocument();
  });

  it("muestra los documentos sin exponer su texto completo", async () => {
    render(<EnterpriseInformationPanel projectId="p1" />);

    await screen.findByTestId("info-sources-panel");
    expect(screen.getByText("📄 plan_negocio.pdf")).toBeInTheDocument();
    expect(screen.getByText(/Plan de negocio con inversión inicial/)).toBeInTheDocument();
  });

  it("confirma un dato importado y lo muestra como confirmado", async () => {
    const user = userEvent.setup();
    let confirmed = false;
    vi.mocked(projectInfoService.confirmInfo).mockImplementation(async () => {
      confirmed = true;
      return {
        projectId: "p1",
        section: "MERCADO",
        key: "mercado_objetivo",
        value: "SME",
        sourceType: "USER_INPUT",
        originalSourceType: "IMPORTED_DOCUMENT",
        sourceDocument: "documento.pdf",
        updatedAt: "2026-08-10T00:00:00Z",
      };
    });
    vi.mocked(enterpriseApi.getInformation).mockImplementation(async () =>
      confirmed
        ? {
            ...information,
            supplemental: {
              ...information.supplemental,
              market: {
                mercado_objetivo: {
                  value: "SME",
                  sourceType: "USER_INPUT",
                  state: "CONFIRMED",
                  origin: "Usuario",
                },
              },
            },
          }
        : information,
    );

    render(<EnterpriseInformationPanel projectId="p1" />);

    await screen.findByTestId("info-sources-panel");
    await user.click(screen.getByTestId("confirm-MERCADO-mercado_objetivo"));

    await vi.waitFor(() =>
      expect(projectInfoService.confirmInfo).toHaveBeenCalledWith(
        "p1",
        "MERCADO",
        "mercado_objetivo",
      ),
    );
    await vi.waitFor(() =>
      expect(
        screen.queryByTestId("confirm-MERCADO-mercado_objetivo"),
      ).not.toBeInTheDocument(),
    );
  });
});
