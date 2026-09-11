import { afterEach, describe, expect, it, vi } from "vitest";
import { documentChatService } from "./documentChat";
import { MAX_CHAT_MESSAGE_LENGTH } from "@/utils/chatLimits";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("documentChatService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("history: GET del historial de la conversación de un documento", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse([
        {
          id: "m1",
          documentId: "d1",
          userId: "u1",
          role: "USER",
          content: "Hola",
          createdAt: "2026-08-01T00:00:00Z",
        },
      ]),
    );

    const history = await documentChatService.history("d1");

    expect(history).toHaveLength(1);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/medical/documents/d1/chat/messages");
  });

  it("send: POST con el contenido y devuelve el turno con verificación", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({
        userMessage: { id: "um1", documentId: "d1", userId: "u1", role: "USER", content: "Hola" },
        assistantMessage: {
          id: "am1",
          documentId: "d1",
          userId: "u1",
          role: "ASSISTANT",
          content: "Hola, puedo ayudarte con tu examen.",
        },
        verificationStatus: "VERIFIED",
      }),
    );

    const turn = await documentChatService.send("d1", "Hola");

    expect(turn.verificationStatus).toBe("VERIFIED");
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/medical/documents/d1/chat/messages");
    expect((init as RequestInit).method).toBe("POST");
    const body = JSON.parse((init as RequestInit).body as string);
    expect(body.content).toBe("Hola");
  });

  it("send: rechaza mensajes que exceden el límite del contrato", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");
    await expect(
      documentChatService.send("d1", "x".repeat(MAX_CHAT_MESSAGE_LENGTH + 1)),
    ).rejects.toThrow("El mensaje no puede superar");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("clear: DELETE del historial de la conversación", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 204 }));
    await documentChatService.clear("d1");
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/medical/documents/d1/chat/messages");
    expect((init as RequestInit).method).toBe("DELETE");
  });
});
