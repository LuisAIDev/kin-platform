import { api } from "./api";

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
}

export interface StreamCallbacks {
  onToken: (token: string) => void;
  onDone: (response: ChatResponse) => void;
  onError: (error: Error) => void;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("kin_token_v2");
}

export const chatService = {
  sendMessage: (projectId: string, content: string) =>
    api.post<ChatResponse>(`/projects/${projectId}/chat`, { content }),

  sendMessageStream: (projectId: string, content: string, callbacks: StreamCallbacks): AbortController => {
    const controller = new AbortController();
    const token = getToken();

    const headers: Record<string, string> = { "Content-Type": "application/json" };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    (async () => {
      try {
        const res = await fetch(`${API_URL}/projects/${projectId}/chat/stream`, {
          method: "POST",
          headers,
          body: JSON.stringify({ content }),
          signal: controller.signal,
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
                  callbacks.onError(new Error(parsed.error ?? "Unknown server error"));
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
          callbacks.onError(new Error("Stream ended without completion"));
        }
      } catch (err: unknown) {
        if (err instanceof DOMException && err.name === "AbortError") return;
        callbacks.onError(err instanceof Error ? err : new Error(String(err)));
      }
    })();

    return controller;
  },

  getHistory: (projectId: string) =>
    api.get<ChatMessage[]>(`/projects/${projectId}/messages`),

  clearConversation: (projectId: string) =>
    api.delete<void>(`/projects/${projectId}/messages`),
};
