import { api } from "./api";
import { isChatMessageTooLong, MAX_CHAT_MESSAGE_LENGTH } from "@/utils/chatLimits";
import type { ExportAction } from "@/services/exportProject";

export interface ChatMessage {
  id: string;
  projectId: string;
  userId: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  metadata: string | null;
  tokensUsed: number;
  createdAt: string;
}

export interface ChatResponse {
  userMessageId: string;
  assistantMessageId: string;
  content: string;
  tokensUsed: number;
  action?: ExportAction | null;
}

export interface StreamCallbacks {
  onToken: (token: string) => void;
  onDone: (response: ChatResponse) => void;
  onError: (error: Error) => void;
  onStarted?: () => void;
  onKeepAlive?: () => void;
}

export interface ChatStreamError extends Error {
  hadTokens: boolean;
}

function streamError(message: string, hadTokens: boolean): ChatStreamError {
  const err = new Error(message) as ChatStreamError;
  err.hadTokens = hadTokens;
  return err;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export const chatService = {
  sendMessage: (projectId: string, content: string) => {
    if (isChatMessageTooLong(content)) {
      return Promise.reject(
        new Error(
          `El mensaje no puede superar ${MAX_CHAT_MESSAGE_LENGTH} caracteres (${content.length}).`,
        ),
      );
    }
    return api.post<ChatResponse>(`/projects/${projectId}/chat`, { content });
  },

  sendMessageStream: (projectId: string, content: string, callbacks: StreamCallbacks): AbortController => {
    const controller = new AbortController();

    const headers: Record<string, string> = { "Content-Type": "application/json" };

    (async () => {
      if (isChatMessageTooLong(content)) {
        callbacks.onError(
          new Error(
            `El mensaje no puede superar ${MAX_CHAT_MESSAGE_LENGTH} caracteres (${content.length}).`,
          ),
        );
        return;
      }
      let receivedTokens = false;
      let receivedStarted = false;
      try {
        const url = `${API_URL}/projects/${projectId}/chat/stream`;

        const res = await fetch(url, {
          method: "POST",
          headers,
          body: JSON.stringify({ content }),
          signal: controller.signal,
          credentials: "include",
        });

        if (!res.ok) {
          const body = await res.json().catch(() => null);
          throw new Error(body?.error ?? `Request failed (${res.status})`);
        }

        const reader = res.body?.getReader();
        if (!reader) throw new Error("No response body");

        const decoder = new TextDecoder();
        let buffer = "";
        let eventType = "";
        let receivedDone = false;

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });

          const parts = buffer.split("\n");
          buffer = parts.pop() || "";

          for (const line of parts) {
            const trimmed = line.trim();
            if (!trimmed) {
              eventType = "";
              continue;
            }
            if (trimmed.startsWith("event:")) {
              eventType = trimmed.slice(6).trim();
            } else if (trimmed.startsWith("data:")) {
              const data = trimmed.slice(5).trim();
              if (!data) continue;
              try {
                const parsed = JSON.parse(data);
                if (eventType === "token" && parsed.token) {
                  receivedTokens = true;
                  callbacks.onToken(parsed.token);
                } else if (eventType === "done") {
                  receivedDone = true;
                  callbacks.onDone({
                    userMessageId: parsed.userMessageId ?? "",
                    assistantMessageId: parsed.assistantMessageId ?? "",
                    content: parsed.content ?? "",
                    tokensUsed: parsed.tokensUsed ?? 0,
                  });
                  break;
                } else if (eventType === "error") {
                  callbacks.onError(
                    streamError(parsed.error ?? "Unknown server error", receivedTokens),
                  );
                } else if (eventType === "started") {
                  if (!receivedStarted) {
                    receivedStarted = true;
                    callbacks.onStarted?.();
                  }
                } else if (eventType === "keepalive") {
                  callbacks.onKeepAlive?.();
                }
              } catch {
                // ignore parse errors for individual tokens
              }
              eventType = "";
            }
          }
          if (receivedDone) break;
        }

        if (!receivedDone) {
          callbacks.onError(streamError("Stream ended without completion", receivedTokens));
        }
      } catch (err: unknown) {
        if (err instanceof DOMException && err.name === "AbortError") return;
        callbacks.onError(
          streamError(err instanceof Error ? err.message : String(err), receivedTokens),
        );
      }
    })();

    return controller;
  },

  getHistory: (projectId: string) =>
    api.get<ChatMessage[]>(`/projects/${projectId}/messages`),

  clearConversation: (projectId: string) =>
    api.delete<void>(`/projects/${projectId}/messages`),
};
