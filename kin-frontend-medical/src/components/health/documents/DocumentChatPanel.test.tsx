import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DocumentChatPanel, { IMPORT_PROMPT } from "@/components/health/documents/DocumentChatPanel";
import type { ClinicalDocument } from "@/services/documents";
import type { DocumentChatMessage } from "@/services/documentChat";

const mocks = vi.hoisted(() => ({
  history: vi.fn(),
  send: vi.fn(),
  clear: vi.fn(),
}));

vi.mock("@/services/documentChat", () => ({
  documentChatService: {
    history: mocks.history,
    send: mocks.send,
    clear: mocks.clear,
  },
  VERIFICATION_LABELS: {
    VERIFIED: "Respuesta verificada contra la base de la OMS (CIE-11).",
    UNVERIFIED: "",
    UNCONFIGURED: "",
    UNAVAILABLE: "",
    "": "",
  },
}));

const document: ClinicalDocument = {
  id: "d1",
  fileName: "laboratorio_glucosa.pdf",
  fileSize: 2048,
  mimeType: "application/pdf",
  patientId: "u1",
  physicianId: null,
  description: "Examen",
  status: "ACTIVE",
  uploadedAt: "2026-08-01T10:00:00Z",
  analyzable: true,
};

function message(over: Partial<DocumentChatMessage>): DocumentChatMessage {
  return {
    id: "m",
    documentId: document.id,
    userId: "u1",
    role: "ASSISTANT",
    content: "",
    createdAt: new Date().toISOString(),
    ...over,
  };
}

describe("DocumentChatPanel", () => {
  beforeEach(() => {
    mocks.history.mockReset();
    mocks.send.mockReset();
    mocks.clear.mockReset();
    mocks.history.mockResolvedValue([]);
  });

  it("carga el historial y permite enviar una pregunta", async () => {
    mocks.send.mockResolvedValue({
      userMessage: message({ id: "um1", role: "USER", content: "¿Qué significa 110?" }),
      assistantMessage: message({
        id: "am1",
        content: "Tu glucosa está en 110 mg/dL, ligeramente elevada. Consulta a tu médico.",
      }),
      verificationStatus: "VERIFIED",
    });
    const user = userEvent.setup();
    render(<DocumentChatPanel document={document} />);

    await waitFor(() => expect(mocks.history).toHaveBeenCalledWith("d1"));
    await user.type(
      screen.getByRole("textbox", { name: "Pregunta sobre el documento" }),
      "¿Qué significa 110?",
    );
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await screen.findByText(/Tu glucosa está en 110 mg\/dL/);
    expect(mocks.send).toHaveBeenCalledWith("d1", "¿Qué significa 110?");
    await screen.findByText(/verificada contra la base de la OMS/);
  });

  it('"Importar información" envía la extracción automática', async () => {
    mocks.send.mockResolvedValue({
      userMessage: message({ id: "um2", role: "USER", content: IMPORT_PROMPT }),
      assistantMessage: message({ id: "am2", content: "Glucosa: 110 mg/dL (elevada)." }),
      verificationStatus: "",
    });
    const user = userEvent.setup();
    render(<DocumentChatPanel document={document} />);

    await user.click(screen.getByRole("button", { name: "Importar información" }));

    expect(mocks.send).toHaveBeenCalledWith("d1", IMPORT_PROMPT);
    await screen.findByText(/Glucosa: 110 mg\/dL/);
  });

  it("muestra aviso cuando el documento no es analizable", async () => {
    render(<DocumentChatPanel document={{ ...document, analyzable: false }} />);

    await screen.findByText(/No se pudo extraer texto/i);
    expect(screen.queryByRole("button", { name: "Importar información" })).not.toBeInTheDocument();
  });

  it("muestra el error si el envío falla", async () => {
    mocks.send.mockRejectedValue(new Error("Servicio no disponible"));
    const user = userEvent.setup();
    render(<DocumentChatPanel document={document} />);

    await user.type(
      screen.getByRole("textbox", { name: "Pregunta sobre el documento" }),
      "Hola",
    );
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await screen.findByText(/Servicio no disponible/);
  });
});
