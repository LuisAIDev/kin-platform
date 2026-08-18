import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ExportChatActionCard from "@/components/export/ExportChatActionCard";
import { ExportApiError } from "@/services/exportProject";

const mocks = vi.hoisted(() => ({
  download: vi.fn(),
  downloadBlob: vi.fn(),
}));

vi.mock("@/services/exportProject", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/exportProject")>();
  return { ...actual, exportProjectService: { download: mocks.download } };
});
vi.mock("@/services/enterpriseApi", () => ({ downloadBlob: mocks.downloadBlob }));

describe("ExportChatActionCard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.download.mockResolvedValue(new Blob(["doc"]));
  });

  it("muestra la tarjeta de descarga", () => {
    render(
      <ExportChatActionCard
        action={{ type: "EXPORT_PROJECT", format: "DOCX", templateDocumentId: null }}
        projectId="p1"
        projectTitle="CAFÉ MARTE 777"
      />,
    );
    expect(screen.getByText("📄 Proyecto listo para descargar")).toBeInTheDocument();
  });

  it("muestra la estructura utilizada cuando hay plantilla", () => {
    render(
      <ExportChatActionCard
        action={{
          type: "EXPORT_PROJECT",
          format: "PDF",
          templateDocumentId: "doc1",
          templateDocumentName: "Plantilla SENA.docx",
        }}
        projectId="p1"
        projectTitle="CAFÉ MARTE 777"
      />,
    );
    expect(screen.getByText("Estructura utilizada: Plantilla SENA.docx")).toBeInTheDocument();
  });

  it("descarga el DOCX con el nombre del proyecto", async () => {
    const user = userEvent.setup();
    render(
      <ExportChatActionCard
        action={{ type: "EXPORT_PROJECT", format: "DOCX", templateDocumentId: null }}
        projectId="p1"
        projectTitle="CAFÉ MARTE 777"
      />,
    );
    await user.click(screen.getByRole("button", { name: "Descargar Word" }));

    await waitFor(() => expect(mocks.download).toHaveBeenCalledWith("p1", "DOCX", "COMPLETE", undefined));
    await waitFor(() =>
      expect(mocks.downloadBlob).toHaveBeenCalledWith(expect.any(Blob), "KIN_caf_marte_777.docx"),
    );
    expect(await screen.findByText("✓ Proyecto descargado correctamente.")).toBeInTheDocument();
  });

  it("descarga con plantilla adjuntando templateDocumentId", async () => {
    const user = userEvent.setup();
    render(
      <ExportChatActionCard
        action={{
          type: "EXPORT_PROJECT",
          format: "PDF",
          templateDocumentId: "doc1",
          templateDocumentName: "Plantilla SENA.docx",
        }}
        projectId="p1"
        projectTitle="CAFÉ MARTE 777"
      />,
    );
    await user.click(screen.getByRole("button", { name: "Descargar PDF" }));

    await waitFor(() => expect(mocks.download).toHaveBeenCalledWith("p1", "PDF", "COMPLETE", "doc1"));
  });

  it("muestra error si la descarga falla", async () => {
    mocks.download.mockRejectedValue(new ExportApiError("boom", 500));
    const user = userEvent.setup();
    render(
      <ExportChatActionCard
        action={{ type: "EXPORT_PROJECT", format: "MARKDOWN", templateDocumentId: null }}
        projectId="p1"
        projectTitle="CAFÉ MARTE 777"
      />,
    );
    await user.click(screen.getByRole("button", { name: "Descargar Markdown" }));

    expect(
      await screen.findByText("No se pudo generar el documento. Inténtalo de nuevo."),
    ).toBeInTheDocument();
    expect(mocks.downloadBlob).not.toHaveBeenCalled();
  });
});
