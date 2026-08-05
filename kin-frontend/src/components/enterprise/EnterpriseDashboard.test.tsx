import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import EnterpriseDashboard from "@/components/enterprise/EnterpriseDashboard";
import { enterpriseApi } from "@/services/enterpriseApi";
import type { EnterpriseDashboard as Dashboard } from "@/types/enterprise";

vi.mock("@/services/enterpriseApi", async () => {
  const actual = await vi.importActual<typeof import("@/services/enterpriseApi")>(
    "@/services/enterpriseApi",
  );
  return {
    ...actual,
    enterpriseApi: {
      getDashboard: vi.fn(),
      downloadDocument: vi.fn(),
      downloadBundle: vi.fn(),
      generate: vi.fn(),
    },
  };
});

let mockConnection = {
  events: [] as Array<{ state: string }>,
  connected: true,
  error: null as string | null,
  terminal: false,
};

vi.mock("@/hooks/useEnterpriseProgress", () => ({
  useEnterpriseProgress: () => mockConnection,
}));

const baseDashboard: Dashboard = {
  projectId: "p1",
  version: 1,
  status: "COMPLETED",
  progress: 100,
  documentCount: 2,
  versionsCount: 2,
  createdAt: "2026-08-02T10:00:00Z",
  updatedAt: "2026-08-02T10:01:00Z",
  completedAt: "2026-08-02T10:01:00Z",
  failedReason: null,
  generationDurationMillis: 60000,
  score: null,
  documents: [
    {
      id: "a",
      type: "LEAN_CANVAS",
      size: 2048,
      createdAt: "2026-08-02T10:00:00Z",
      generatedBy: "BusinessModelEngine",
      engineVersion: "1.0.0",
      version: 1,
      inputHash: "hash-a",
    },
    {
      id: "b",
      type: "KPI",
      size: 512,
      createdAt: "2026-08-02T10:00:00Z",
      generatedBy: "KpiEngine",
      engineVersion: "1.0.0",
      version: 1,
      inputHash: "hash-b",
    },
  ],
  versions: [
    {
      version: 1,
      status: "COMPLETED",
      createdAt: "2026-08-02T10:00:00Z",
      updatedAt: "2026-08-02T10:01:00Z",
      documentCount: 2,
    },
    {
      version: 2,
      status: "COMPLETED",
      createdAt: "2026-08-02T11:00:00Z",
      updatedAt: "2026-08-02T11:01:00Z",
      documentCount: 0,
    },
  ],
  statistics: { documentCount: 2, versionsCount: 2 },
};

const dashboardWithActiveVersion: Dashboard = {
  ...baseDashboard,
  versions: [
    ...baseDashboard.versions,
    {
      version: 3,
      status: "RUNNING",
      createdAt: "2026-08-02T12:00:00Z",
      updatedAt: "2026-08-02T12:01:00Z",
      documentCount: 0,
    },
  ],
};

describe("EnterpriseDashboard", () => {
  beforeEach(() => {
    vi.mocked(enterpriseApi.getDashboard).mockResolvedValue(baseDashboard);
    vi.mocked(enterpriseApi.downloadDocument).mockResolvedValue(
      new Blob(["pdf"]),
    );
    vi.mocked(enterpriseApi.downloadBundle).mockResolvedValue(new Blob(["zip"]));
    vi.mocked(enterpriseApi.generate).mockResolvedValue(202);
  });

  it("muestra el dashboard consolidado", async () => {
    render(<EnterpriseDashboard projectId="p1" version={1} />);
    expect(await screen.findByText("Enterprise Dashboard")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId("status-badge")).toHaveTextContent("COMPLETED"));
    expect(screen.getByText("100%")).toBeInTheDocument();
    expect(screen.getAllByText("LEAN CANVAS").length).toBeGreaterThan(0);
    expect(screen.getAllByText("KPI").length).toBeGreaterThan(0);
    expect(screen.getByText(/Pendiente de generación/)).toBeInTheDocument();
  });

  it("solicita el dashboard de la versión seleccionada", async () => {
    render(<EnterpriseDashboard projectId="p1" version={1} />);
    await screen.findByText("Enterprise Dashboard");
    await waitFor(() =>
      expect(enterpriseApi.getDashboard).toHaveBeenCalledWith("p1", 1),
    );
    await userEvent.selectOptions(screen.getByRole("combobox"), "2");
    await waitFor(() =>
      expect(enterpriseApi.getDashboard).toHaveBeenCalledWith("p1", 2),
    );
  });

  it("descarga un documento al pulsar su botón", async () => {
    const blobSpy = vi
      .spyOn(URL, "createObjectURL")
      .mockReturnValue("blob:mock");
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click");
    render(<EnterpriseDashboard projectId="p1" version={1} />);
    await waitFor(() => expect(screen.getAllByText("LEAN CANVAS").length).toBeGreaterThan(0));
    await userEvent.click(screen.getAllByRole("button", { name: "PDF" })[0]);
    await waitFor(() =>
      expect(enterpriseApi.downloadDocument).toHaveBeenCalledWith(
        "p1",
        1,
        "LEAN_CANVAS",
        "PDF",
      ),
    );
    expect(blobSpy).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
  });

  it("muestra el aviso cuando falta el projectId", () => {
    render(<EnterpriseDashboard projectId="" />);
    expect(screen.getByText(/Falta el identificador del proyecto/)).toBeInTheDocument();
  });

  it("muestra un error cuando el dashboard falla", async () => {
    vi.mocked(enterpriseApi.getDashboard).mockRejectedValue(new Error("boom"));
    render(<EnterpriseDashboard projectId="p1" version={1} />);
    expect(await screen.findByText(/boom/)).toBeInTheDocument();
  });

  it("muestra un error cuando la descarga falla", async () => {
    vi.mocked(enterpriseApi.downloadDocument).mockRejectedValue(
      new Error("download boom"),
    );
    render(<EnterpriseDashboard projectId="p1" version={1} />);
    await waitFor(() =>
      expect(screen.getAllByText("LEAN CANVAS").length).toBeGreaterThan(0),
    );
    await userEvent.click(screen.getAllByRole("button", { name: "PDF" })[0]);
    expect(await screen.findByText(/download boom/)).toBeInTheDocument();
  });

  it("muestra el aviso de reconexión SSE cuando hay error de conexión", async () => {
    mockConnection = {
      events: [],
      connected: false,
      error: "SSE down",
      terminal: false,
    };
    render(<EnterpriseDashboard projectId="p1" version={1} />);
    expect(await screen.findByText(/reconectando/)).toBeInTheDocument();
    mockConnection = {
      events: [],
      connected: true,
      error: null,
      terminal: false,
    };
  });

  // ------------------------------------------------------------------
  // M3G — Generación desde la interfaz
  // ------------------------------------------------------------------

  it("selecciona automáticamente la versión activa al cargar", async () => {
    vi.mocked(enterpriseApi.getDashboard).mockResolvedValue(dashboardWithActiveVersion);

    render(<EnterpriseDashboard projectId="p1" version={1} />);

    await waitFor(() => expect(screen.getByText(/· v3/)).toBeInTheDocument());
    await waitFor(() =>
      expect(enterpriseApi.getDashboard).toHaveBeenCalledWith("p1", 3),
    );
  });

  it("al pulsar Generar solicita la generación y muestra la nueva versión", async () => {
    vi.mocked(enterpriseApi.getDashboard)
      .mockResolvedValueOnce(baseDashboard)
      .mockResolvedValueOnce(dashboardWithActiveVersion);

    render(<EnterpriseDashboard projectId="p1" version={1} />);
    await screen.findByText("Enterprise Dashboard");

    await userEvent.click(screen.getByRole("button", { name: "Generar Proyecto Empresarial" }));

    await waitFor(() => expect(enterpriseApi.generate).toHaveBeenCalledWith("p1", true));
    await waitFor(() => expect(screen.getByText(/· v3/)).toBeInTheDocument());
  });

  it("muestra un error cuando la generación falla", async () => {
    vi.mocked(enterpriseApi.generate).mockRejectedValue(
      new Error("Sin contexto de conversación"),
    );

    render(<EnterpriseDashboard projectId="p1" version={1} />);
    await screen.findByText("Enterprise Dashboard");

    await userEvent.click(screen.getByRole("button", { name: "Generar Proyecto Empresarial" }));

    expect(await screen.findByText(/Sin contexto de conversación/)).toBeInTheDocument();
  });

  it("recarga el dashboard al terminar el SSE", async () => {
    mockConnection = {
      events: [{ state: "COMPLETED" }],
      connected: false,
      error: null,
      terminal: true,
    };

    render(<EnterpriseDashboard projectId="p1" version={1} />);

    await waitFor(() =>
      expect(vi.mocked(enterpriseApi.getDashboard).mock.calls.length).toBeGreaterThanOrEqual(2),
    );
    mockConnection = {
      events: [],
      connected: true,
      error: null,
      terminal: false,
    };
  });
});
