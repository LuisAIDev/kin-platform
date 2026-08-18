import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ExportProjectButton from "@/components/export/ExportProjectButton";

const mocks = vi.hoisted(() => ({
  options: vi.fn(),
  download: vi.fn(),
  listDocuments: vi.fn(),
  downloadBlob: vi.fn(),
}));

vi.mock("@/services/exportProject", () => ({
  exportProjectService: { options: mocks.options, download: mocks.download },
  ExportApiError: class extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
}));
vi.mock("@/services/projectInfo", () => ({
  projectInfoService: { listDocuments: mocks.listDocuments },
}));
vi.mock("@/services/enterpriseApi", () => ({ downloadBlob: mocks.downloadBlob }));

const project = { id: "p1", title: "CAFÉ MARTE 777" };

describe("ExportProjectButton", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.options.mockResolvedValue({
      formats: ["DOCX", "PDF", "MARKDOWN"],
      hasReport: true,
      reportSections: [],
      infoSections: [],
      filenameBase: "KIN_caf_marte_777",
    });
    mocks.listDocuments.mockResolvedValue([]);
  });

  it("abre el modal al pulsar Exportar proyecto", async () => {
    const user = userEvent.setup();
    render(<ExportProjectButton project={project} />);

    await user.click(screen.getByRole("button", { name: "Exportar proyecto" }));

    expect(await screen.findByRole("dialog", { name: "Exportar proyecto" })).toBeInTheDocument();
  });

  it("cierra el modal al pulsar el botón de cerrar", async () => {
    const user = userEvent.setup();
    render(<ExportProjectButton project={project} />);

    await user.click(screen.getByRole("button", { name: "Exportar proyecto" }));
    await screen.findByRole("dialog", { name: "Exportar proyecto" });

    await user.click(screen.getByLabelText("Cerrar"));

    await waitFor(() =>
      expect(screen.queryByRole("dialog", { name: "Exportar proyecto" })).not.toBeInTheDocument(),
    );
  });
});
