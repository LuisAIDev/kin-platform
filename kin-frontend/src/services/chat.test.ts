import { afterEach, describe, expect, it, vi } from "vitest";
import { chatService } from "@/services/chat";
import { MAX_CHAT_MESSAGE_LENGTH } from "@/utils/chatLimits";

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function sseResponse(chunks: string[]): Response {
  const stream = new ReadableStream<Uint8Array>({
    start(controller) {
      const encoder = new TextEncoder();
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
      controller.close();
    },
  });
  return new Response(stream, { headers: { "Content-Type": "text/event-stream" } });
}

describe("chatService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("sendMessage: POST al endpoint de chat", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ content: "ok", tokensUsed: 5 }));

    const res = await chatService.sendMessage("p1", "hola");

    expect(res.content).toBe("ok");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/projects/p1/chat");
  });

  it("getHistory: GET de mensajes", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));

    const history = await chatService.getHistory("p1");

    expect(history).toEqual([]);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/projects/p1/messages");
  });

  it("clearConversation: DELETE", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(null, 204));

    await chatService.clearConversation("p1");

    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("DELETE");
  });

  it("sendMessageStream: emite tokens y completion", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {"token":"hola"}\n\n',
      'event: token\ndata: {"token":" mundo"}\n\n',
      'event: done\ndata: {"content":"hola mundo","tokensUsed":3}\n\n',
    ]));
    const onToken = vi.fn();
    const onDone = vi.fn();
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", { onToken, onDone, onError });
    await vi.waitFor(() => expect(onDone).toHaveBeenCalled());

    expect(onToken.mock.calls.map((c) => c[0]).join("")).toBe("hola mundo");
    expect(onDone).toHaveBeenCalledWith(expect.objectContaining({ tokensUsed: 3 }));
    expect(onError).not.toHaveBeenCalled();
  });

  it("sendMessageStream: error de red dispara onError", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "boom" }, 500));
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", { onToken: vi.fn(), onDone: vi.fn(), onError });
    await vi.waitFor(() => expect(onError).toHaveBeenCalled());

    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ message: "boom" }));
  });

  it("sendMessageStream: stream termina sin done → onError", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {"token":"x"}\n\n',
    ]));
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", { onToken: vi.fn(), onDone: vi.fn(), onError });
    await vi.waitFor(() => expect(onError).toHaveBeenCalled());

    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ message: "Stream ended without completion" }));
  });

  it("sendMessageStream: contenido >10.000 no genera petición HTTP y dispara onError", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "a".repeat(MAX_CHAT_MESSAGE_LENGTH + 1), {
      onToken: vi.fn(),
      onDone: vi.fn(),
      onError,
    });
    await vi.waitFor(() => expect(onError).toHaveBeenCalled());

    expect(fetchMock).not.toHaveBeenCalled();
    expect(onError).toHaveBeenCalledWith(
      expect.objectContaining({ message: expect.stringContaining("no puede superar 10000") }));
  });

  it("sendMessageStream: contenido de exactamente 10.000 sí genera petición HTTP", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(sseResponse(['event: done\ndata: {"content":"ok","tokensUsed":1}\n\n']));

    chatService.sendMessageStream("p1", "a".repeat(MAX_CHAT_MESSAGE_LENGTH), {
      onToken: vi.fn(),
      onDone: vi.fn(),
      onError: vi.fn(),
    });
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalled());
  });

  it("sendMessage: contenido >10.000 lanza error sin llamar a la API", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");

    await expect(
      chatService.sendMessage("p1", "a".repeat(MAX_CHAT_MESSAGE_LENGTH + 1)),
    ).rejects.toThrow(/no puede superar 10000 caracteres/);

    expect(fetchMock).not.toHaveBeenCalled();
  });

  // ------------------------------------------------------------------
  // Robustez del parser SSE: conservación de contenido parcial
  // ------------------------------------------------------------------

  it("sendMessageStream: token + done con content vacío → onDone con content vacío y tokens acumulados", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {"token":"parcial"}\n\n',
      'event: done\ndata: {"done":true,"content":"","assistantMessageId":"a1","tokensUsed":0}\n\n',
    ]));
    const onToken = vi.fn();
    const onDone = vi.fn();
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", { onToken, onDone, onError });
    await vi.waitFor(() => expect(onDone).toHaveBeenCalled());

    expect(onToken.mock.calls.map((c) => c[0]).join("")).toBe("parcial");
    expect(onDone).toHaveBeenCalledWith(
      expect.objectContaining({ content: "", assistantMessageId: "a1" }),
    );
    expect(onError).not.toHaveBeenCalled();
  });

  it("sendMessageStream: event:error después de tokens → onError con hadTokens=true", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {"token":"parcial"}\n\n',
      'event: error\ndata: {"error":"stream broke"}\n\n',
    ]));
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", {
      onToken: vi.fn(),
      onDone: vi.fn(),
      onError,
    });
    await vi.waitFor(() => expect(onError).toHaveBeenCalled());

    const err = onError.mock.calls[0][0] as { hadTokens?: boolean; message?: string };
    expect(err.hadTokens).toBe(true);
    expect(err.message).toBe("stream broke");
  });

  it("sendMessageStream: event:error es terminal → un único onError (sin doble error por EOF)", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: error\ndata: {"error":"backend error"}\n\n',
    ]));
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", {
      onToken: vi.fn(),
      onDone: vi.fn(),
      onError,
    });
    await vi.waitFor(() => expect(onError).toHaveBeenCalledTimes(1));

    const err = onError.mock.calls[0][0] as { hadTokens?: boolean; message?: string };
    expect(err.message).toBe("backend error");
    expect(err.hadTokens).toBe(false);
  });

  it("sendMessageStream: EOF sin done después de tokens → onError con hadTokens=true", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {"token":"parcial"}\n\n',
    ]));
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", {
      onToken: vi.fn(),
      onDone: vi.fn(),
      onError,
    });
    await vi.waitFor(() => expect(onError).toHaveBeenCalled());

    const err = onError.mock.calls[0][0] as { hadTokens?: boolean; message?: string };
    expect(err.hadTokens).toBe(true);
    expect(err.message).toBe("Stream ended without completion");
  });

  it("sendMessageStream: error sin tokens → onError con hadTokens=false", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "boom" }, 500));
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", {
      onToken: vi.fn(),
      onDone: vi.fn(),
      onError,
    });
    await vi.waitFor(() => expect(onError).toHaveBeenCalled());

    const err = onError.mock.calls[0][0] as { hadTokens?: boolean };
    expect(err.hadTokens).toBe(false);
  });

  it("sendMessageStream: JSON SSE inválido en data se ignora sin romper el parser", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {invalid json}\n\n',
      'event: token\ndata: {"token":"ok"}\n\n',
      'event: done\ndata: {"done":true,"content":"ok","tokensUsed":1}\n\n',
    ]));
    const onToken = vi.fn();
    const onDone = vi.fn();
    const onError = vi.fn();

    chatService.sendMessageStream("p1", "hola", { onToken, onDone, onError });
    await vi.waitFor(() => expect(onDone).toHaveBeenCalled());

    expect(onToken.mock.calls.map((c) => c[0]).join("")).toBe("ok");
    expect(onError).not.toHaveBeenCalled();
  });

  it("sendMessageStream: múltiples tokens se concatenan en orden", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(sseResponse([
      'event: token\ndata: {"token":"a"}\n\n',
      'event: token\ndata: {"token":"b"}\n\n',
      'event: token\ndata: {"token":"c"}\n\n',
      'event: done\ndata: {"done":true,"content":"abc","tokensUsed":3}\n\n',
    ]));
    const onToken = vi.fn();
    const onDone = vi.fn();

    chatService.sendMessageStream("p1", "hola", { onToken, onDone, onError: vi.fn() });
    await vi.waitFor(() => expect(onDone).toHaveBeenCalled());

    expect(onToken.mock.calls.map((c) => c[0]).join("")).toBe("abc");
    expect(onDone).toHaveBeenCalledWith(
      expect.objectContaining({ content: "abc", tokensUsed: 3 }),
    );
  });
});
