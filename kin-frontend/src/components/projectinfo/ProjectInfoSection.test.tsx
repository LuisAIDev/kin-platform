import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ProjectInfoSection } from "@/components/projectinfo/ProjectInfoSection";
import { projectInfoService } from "@/services/projectInfo";

vi.mock("@/services/projectInfo", async () => {
  const actual =
    await vi.importActual<typeof import("@/services/projectInfo")>("@/services/projectInfo");
  return {
    ...actual,
    projectInfoService: {
      ...actual.projectInfoService,
      listInfo: vi.fn(),
      listDocuments: vi.fn(),
      saveInfo: vi.fn(),
      uploadDocument: vi.fn(),
    },
  };
});

const DOCUMENTO_PROCESADO = {
  id: "d1",
  projectId: "p1",
  filename: "plan_negocio.pdf",
  mimeType: "application/pdf",
  size: 2048,
  status: "PROCESADO" as const,
  errorMessage: null,
  createdAt: "2026-08-10T00:00:00Z",
  updatedAt: "2026-08-10T00:00:00Z",
};

describe("ProjectInfoSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(projectInfoService.listInfo).mockResolvedValue([]);
    vi.mocked(projectInfoService.listDocuments).mockResolvedValue([]);
  });

  it("muestra los botones de importar y agregar documento", async () => {
    render(<ProjectInfoSection projectId="p1" />);

    expect(
      await screen.findByRole("button", { name: "📋 Importar información" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "📎 Agregar documento" })).toBeInTheDocument();
  });

  it("abre el modal de importación y guarda campos como USER_INPUT", async () => {
    const user = userEvent.setup();
    vi.mocked(projectInfoService.saveInfo).mockResolvedValue([]);
    render(<ProjectInfoSection projectId="p1" />);

    await user.click(await screen.findByRole("button", { name: "📋 Importar información" }));
    const nombreInput = screen.getByLabelText("Nombre");
    await user.type(nombreInput, "KIN SaaS");

    await user.click(screen.getByRole("button", { name: "Guardar información" }));

    await waitFor(() =>
      expect(projectInfoService.saveInfo).toHaveBeenCalledWith(
        "p1",
        expect.arrayContaining([
          expect.objectContaining({
            section: "DATOS_GENERALES",
            key: "nombre",
            value: "KIN SaaS",
            sourceType: "USER_INPUT",
          }),
        ]),
      ),
    );
  });

  it("abre el modal de documento y rechaza un archivo demasiado grande", async () => {
    const user = userEvent.setup();
    render(<ProjectInfoSection projectId="p1" />);

    await user.click(await screen.findByRole("button", { name: "📎 Agregar documento" }));
    const input = screen.getByTestId("document-file-input");
    const file = new File([new Uint8Array(10 * 1024 * 1024 + 1)], "grande.pdf", {
      type: "application/pdf",
    });

    await user.upload(input, file);

    expect(await screen.findByText(/supera el tamaño máximo de 10 MB/)).toBeInTheDocument();
    expect(projectInfoService.uploadDocument).not.toHaveBeenCalled();
  });

  it("muestra el nombre del archivo seleccionado y habilita el botón de subida", async () => {
    const user = userEvent.setup();
    render(<ProjectInfoSection projectId="p1" />);

    await user.click(await screen.findByRole("button", { name: "📎 Agregar documento" }));
    const input = screen.getByTestId("document-file-input");
    const file = new File(["contenido"], "notas.txt", { type: "text/plain" });

    await user.upload(input, file);

    expect(
      await screen.findByText(/Archivo seleccionado: notas\.txt/),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Subir y procesar" })).toBeEnabled();
  });

  it("deshabilita el botón de subida cuando no hay archivo", async () => {
    const user = userEvent.setup();
    render(<ProjectInfoSection projectId="p1" />);

    await user.click(await screen.findByRole("button", { name: "📎 Agregar documento" }));

    expect(screen.getByRole("button", { name: "Subir y procesar" })).toBeDisabled();
  });

  it("resetea el estado al reabrir el modal", async () => {
    const user = userEvent.setup();
    render(<ProjectInfoSection projectId="p1" />);

    await user.click(await screen.findByRole("button", { name: "📎 Agregar documento" }));
    const input = screen.getByTestId("document-file-input");
    await user.upload(input, new File(["x"], "a.txt", { type: "text/plain" }));
    expect(await screen.findByText(/Archivo seleccionado: a\.txt/)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Cancelar" }));
    await user.click(screen.getByRole("button", { name: "📎 Agregar documento" }));

    expect(screen.getByRole("button", { name: "Subir y procesar" })).toBeDisabled();
    expect(screen.queryByText(/Archivo seleccionado:/)).not.toBeInTheDocument();
  });

  it("sube un archivo válido y muestra el documento procesado", async () => {
    const user = userEvent.setup();
    vi.mocked(projectInfoService.uploadDocument).mockResolvedValue(DOCUMENTO_PROCESADO);
    vi.mocked(projectInfoService.listDocuments).mockResolvedValue([DOCUMENTO_PROCESADO]);
    render(<ProjectInfoSection projectId="p1" />);

    await user.click(await screen.findByRole("button", { name: "📎 Agregar documento" }));
    const input = screen.getByTestId("document-file-input");
    const file = new File(["contenido"], "plan_negocio.pdf", { type: "application/pdf" });

    await user.upload(input, file);
    await user.click(screen.getByRole("button", { name: "Subir y procesar" }));

    await waitFor(() =>
      expect(projectInfoService.uploadDocument).toHaveBeenCalledWith("p1", file),
    );
    expect(await screen.findByText("📄 plan_negocio.pdf")).toBeInTheDocument();
    expect(screen.getByText("Procesado correctamente")).toBeInTheDocument();
  });

  it("muestra el estado ERROR de un documento", async () => {
    vi.mocked(projectInfoService.listDocuments).mockResolvedValue([
      { ...DOCUMENTO_PROCESADO, status: "ERROR", errorMessage: "corrupto" },
    ]);
    render(<ProjectInfoSection projectId="p1" />);

    expect(await screen.findByText("📄 plan_negocio.pdf")).toBeInTheDocument();
    expect(screen.getByText("Error al procesar")).toBeInTheDocument();
  });
});
