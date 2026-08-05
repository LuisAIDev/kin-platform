import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PdfReportButton from "@/components/PdfReportButton";
import type { Project } from "@/services/projects";
import type { ChatMessage } from "@/services/chat";

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

const project: Project = {
  id: "p1", userId: "u1", title: "Panadería", description: "Plan de negocio",
  category: "alimentos", categoryName: "Alimentos", categoryColor: "#f00",
  status: "ACTIVE", viabilityScore: 72, aiSummary: null, startedAt: null,
  completedAt: null, createdAt: "2026-08-01T10:00:00Z", updatedAt: "2026-08-01T10:00:00Z",
  progressPercentage: 80,
};

const message: ChatMessage = {
  id: "m1", projectId: "p1", userId: "u1", role: "ASSISTANT",
  content: "### Scoring de Viabilidad Estimado: **72/100**", metadata: null,
  tokensUsed: 0, createdAt: "2026-08-01T10:00:00Z",
};

describe("PdfReportButton", () => {
  beforeEach(() => saveMock.mockReset());

  it("renderiza el botón con rol accesible", () => {
    render(<PdfReportButton project={project} messages={[message]} />);
    expect(screen.getByRole("button", { name: "Descargar Reporte PDF" })).toBeInTheDocument();
  });

  it("genera y guarda el PDF al hacer clic", async () => {
    const user = userEvent.setup();
    render(<PdfReportButton project={project} messages={[message]} />);

    await user.click(screen.getByRole("button", { name: "Descargar Reporte PDF" }));

    expect(saveMock).toHaveBeenCalledTimes(1);
    expect(String(saveMock.mock.calls[0][0])).toContain("KIN_Reporte_Panadería.pdf");
  });

  it("funciona con proyecto sin descripción ni score (usa regex de mensajes)", async () => {
    const user = userEvent.setup();
    const minimal: Project = { ...project, description: null, viabilityScore: null };
    render(<PdfReportButton project={minimal} messages={[message]} />);

    await user.click(screen.getByRole("button", { name: "Descargar Reporte PDF" }));

    expect(saveMock).toHaveBeenCalledTimes(1);
  });
});
