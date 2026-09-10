import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DocumentAnalysisPdfButton from "@/components/health/documents/DocumentAnalysisPdfButton";
import type { ClinicalDocument } from "@/services/documents";
import type { DocumentChatMessage } from "@/services/documentChat";

const { saveMock } = vi.hoisted(() => ({ saveMock: vi.fn() }));

class MockJsPDF {
  internal = { pageSize: { getWidth: () => 210 } };
  setFillColor = vi.fn();
  rect = vi.fn();
  setFont = vi.fn();
  setFontSize = vi.fn();
  setTextColor = vi.fn();
  text = vi.fn();
  setDrawColor = vi.fn();
  line = vi.fn();
  roundedRect = vi.fn();
  addPage = vi.fn();
  setLineWidth = vi.fn();
  splitTextToSize = (value: string) => [value];
  save = saveMock;
}

vi.mock("jspdf", () => ({ default: vi.fn(() => new MockJsPDF()) }));

const document: ClinicalDocument = {
  id: "d1",
  fileName: "laboratorio_glucosa.pdf",
  fileSize: 2048,
  mimeType: "application/pdf",
  patientId: "u1",
  physicianId: null,
  description: "Examen de laboratorio",
  status: "ACTIVE",
  uploadedAt: "2026-08-01T10:00:00Z",
  analyzable: true,
};

const message: DocumentChatMessage = {
  id: "m1",
  documentId: "d1",
  userId: "u1",
  role: "ASSISTANT",
  content: "Tu nivel de glucosa está en 110 mg/dL, ligeramente por encima del rango normal.",
  createdAt: "2026-08-01T10:00:00Z",
};

describe("DocumentAnalysisPdfButton", () => {
  beforeEach(() => saveMock.mockReset());

  it("deshabilita el botón sin mensajes", () => {
    render(<DocumentAnalysisPdfButton document={document} messages={[]} />);
    expect(screen.getByRole("button", { name: "Descargar PDF del análisis" })).toBeDisabled();
  });

  it("genera y guarda el PDF al hacer clic con conversación", async () => {
    const user = userEvent.setup();
    render(<DocumentAnalysisPdfButton document={document} messages={[message]} />);

    await user.click(screen.getByRole("button", { name: "Descargar PDF del análisis" }));

    expect(saveMock).toHaveBeenCalledTimes(1);
    expect(String(saveMock.mock.calls[0][0])).toContain("KIN_Analisis_laboratorio_glucosa.pdf");
  });
});
