import { api } from "./api";
import { isChatMessageTooLong, MAX_CHAT_MESSAGE_LENGTH } from "@/utils/chatLimits";

export type DocumentChatRole = "USER" | "ASSISTANT";

/** Estado de la verificación clínica contra la OMS (fuente oficial). */
export type VerificationStatus = "VERIFIED" | "UNVERIFIED" | "UNCONFIGURED" | "UNAVAILABLE" | "";

export interface DocumentChatMessage {
  id: string;
  documentId: string;
  userId: string;
  role: DocumentChatRole;
  content: string;
  createdAt: string;
}

export interface DocumentChatTurnResponse {
  userMessage: DocumentChatMessage;
  assistantMessage: DocumentChatMessage;
  verificationStatus: VerificationStatus;
}

export const VERIFICATION_LABELS: Record<string, string> = {
  VERIFIED: "Respuesta verificada contra la base de la OMS (CIE-11).",
  UNVERIFIED: "No se encontraron coincidencias verificables contra la OMS: la IA no citó códigos oficiales.",
  UNCONFIGURED:
    "La verificación automática contra la ICD-API de la OMS no está configurada en esta instalación.",
  UNAVAILABLE: "La base oficial de la OMS no estuvo disponible: la respuesta no se pudo verificar.",
  "": "",
};

export const documentChatService = {
  history: (documentId: string) =>
    api.get<DocumentChatMessage[]>(`/health/documents/${documentId}/chat/messages`),

  send: (documentId: string, content: string): Promise<DocumentChatTurnResponse> => {
    if (isChatMessageTooLong(content)) {
      return Promise.reject(
        new Error(
          `El mensaje no puede superar ${MAX_CHAT_MESSAGE_LENGTH} caracteres (${content.length}).`,
        ),
      );
    }
    return api.post<DocumentChatTurnResponse>(`/health/documents/${documentId}/chat/messages`, {
      content,
    });
  },

  clear: (documentId: string) =>
    api.delete<void>(`/health/documents/${documentId}/chat/messages`),
};
