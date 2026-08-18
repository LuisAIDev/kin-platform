import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ExportProjectModal from "@/components/export/ExportProjectModal";
import { ExportApiError } from "@/services/exportProject";

const mocks = vi.hoisted(() => ({
  options: vi.fn(),
  download: vi.fn(),
  listDocuments: vi.fn(),
  downloadBlob: vi.fn(),
}));

vi.mock("@/services/exportProject", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/exportProject")>();
  return {
    ...actual,
    exportProjectService: { options: mocks.options, download: mocks.download },
  };
});
vi.mock("@/services/projectInfo", () => ({
  projectInfoService: { listDocuments: mocks.listDocuments },
}));
vi.mock("@/services/enterpriseApi", () => ({ downloadBlob: mocks.downloadBlob }));

const opts = {
  formats: ["DOCX", "PDF", "MARKDOWN"],
  hasReport: true,
  reportSections: ["Resumen Ejecutivo", "Mercado"],
  infoSections: ["MERCADO"],
  filenameBase: "KIN_caf_marte_777",
};

const processed = {
  id: "doc1", projectId: "p1", filename: "Plantilla SENA.docx", mimeType: "application/vnd...",
  size: 1024, status: "PROCESADO", errorMessage: null, createdAt: "", updatedAt: "",
};
const errored = { ...processed, id: "doc2", filename: "roto.pdf", status: "ERROR", errorMessage: "x" };

function renderModal() {
  return render(
    <ExportProjectModal projectId="p1" projectTitle="CAFÉ MARTE 777" onClose={() => {}} />,
  );
}

describe("ExportProjectModal", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.options.mockResolvedValue(opts);
    mocks.listDocuments.mockResolvedValue([processed, errored]);
    mocks.download.mockResolvedValue(new Blob(["doc"]));
  });

  it("listar documentos muestra únicamente los procesados", async () => {
    renderModal();
    await userEvent.click(await screen.findByLabelText(/Proyecto usando plantilla/i));
    const select = screen.getByLabelText("Documento de referencia") as HTMLSelectElement;
    const options = Array.from(select.options).map((o) => o.textContent);
    expect(options).toContain("📄 Plantilla SENA.docx");
    expect(options).not.toContain("roto.pdf");
  });

  it("descarga DOCX en modo completo con nombre correcto", async () => {
    const user = userEvent.setup();
    renderModal();
    await user.click(await screen.findByRole("button", { name: "Descargar" }));
    await waitFor(() => expect(mocks.download).toHaveBeenCalledWith("p1", "DOCX", "COMPLETE"));
    await waitFor(() =>
      expect(mocks.downloadBlob).toHaveBeenCalledWith(
        expect.any(Blob),
        "KIN_caf_marte_777.docx",
      ),
    );
    expect(await screen.findByText("✓ Proyecto descargado correctamente.")).toBeInTheDocument();
  });

  it("descarga PDF en modo resumen con template", async () => {
    const user = userEvent.setup();
    renderModal();
    await user.click(await screen.findByLabelText(/Proyecto usando plantilla/i));
    await user.selectOptions(screen.getByLabelText("Documento de referencia"), "doc1");
    await user.click(screen.getByLabelText(/PDF \(\.pdf\)/i));
    await user.click(screen.getByRole("button", { name: "Descargar" }));

    await waitFor(() =>
      expect(mocks.download).toHaveBeenCalledWith("p1", "PDF", "COMPLETE", "doc1"),
    );
    await waitFor(() =>
      expect(mocks.downloadBlob).toHaveBeenCalledWith(expect.any(Blob), "KIN_caf_marte_777.pdf"),
    );
    expect(await screen.findByText("Estructura aplicada: ✓ Sí")).toBeInTheDocument();
  });

  it("muestra mensaje del backend si el documento no sirve como plantilla", async () => {
    mocks.download.mockRejectedValue(
      new ExportApiError("El documento seleccionado no puede utilizarse como plantilla.", 400),
    );
    const user = userEvent.setup();
    renderModal();
    await user.click(await screen.findByLabelText(/Proyecto usando plantilla/i));
    await user.selectOptions(screen.getByLabelText("Documento de referencia"), "doc1");
    await user.click(screen.getByRole("button", { name: "Descargar" }));

    expect(
      await screen.findByText("El documento seleccionado no puede utilizarse como plantilla."),
    ).toBeInTheDocument();
    expect(mocks.downloadBlob).not.toHaveBeenCalled();
  });

  it("descarga Markdown en modo resumen", async () => {
    const user = userEvent.setup();
    renderModal();
    await user.click(await screen.findByLabelText(/Solo resumen/i));
    await user.click(screen.getByLabelText(/Markdown \(\.md\)/i));
    await user.click(screen.getByRole("button", { name: "Descargar" }));

    await waitFor(() => expect(mocks.download).toHaveBeenCalledWith("p1", "MARKDOWN", "SUMMARY"));
    await waitFor(() =>
      expect(mocks.downloadBlob).toHaveBeenCalledWith(expect.any(Blob), "KIN_caf_marte_777.md"),
    );
  });

  it("bloquea descarga sin plantilla seleccionada", async () => {
    const user = userEvent.setup();
    renderModal();
    await user.click(await screen.findByLabelText(/Proyecto usando plantilla/i));
    await user.click(screen.getByRole("button", { name: "Descargar" }));
    expect(await screen.findByText("Selecciona un documento de referencia.")).toBeInTheDocument();
    expect(mocks.download).not.toHaveBeenCalled();
  });

  it.each([
    [401, "Tu sesión expiró"],
    [403, "No tienes permisos"],
    [404, "no fue encontrado"],
    [500, "error inesperado"],
  ])("maneja error HTTP %i", async (status, fragment) => {
    mocks.download.mockRejectedValue(new ExportApiError("boom", status as number));
    const user = userEvent.setup();
    renderModal();
    await user.click(await screen.findByRole("button", { name: "Descargar" }));
    expect(await screen.findByText(new RegExp(fragment))).toBeInTheDocument();
    expect(mocks.downloadBlob).not.toHaveBeenCalled();
  });

  it("evita doble descarga mientras genera", async () => {
    let resolve: (b: Blob) => void = () => {};
    mocks.download.mockReturnValue(new Promise<Blob>((r) => (resolve = r)));
    const user = userEvent.setup();
    renderModal();
    const btn = await screen.findByRole("button", { name: "Descargar" });
    await user.click(btn);
    await user.click(btn);
    expect(mocks.download).toHaveBeenCalledTimes(1);
    resolve(new Blob(["doc"]));
    await waitFor(() => expect(mocks.downloadBlob).toHaveBeenCalledTimes(1));
  });
});
