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

export const chatService = {
  sendMessage: (projectId: string, content: string) =>
    api.post<ChatResponse>(`/projects/${projectId}/chat`, { content }),

  getHistory: (projectId: string) =>
    api.get<ChatMessage[]>(`/projects/${projectId}/messages`),

  clearConversation: (projectId: string) =>
    api.delete<void>(`/projects/${projectId}/messages`),
};
