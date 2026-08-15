import { api } from "./api";
import { getToken } from "./session";

/** Origen de un dato estructurado del proyecto (nunca se presenta un dato desconocido como 0). */
export type InfoSourceType =
  | "USER_INPUT"
  | "IMPORTED_DOCUMENT"
  | "CALCULATED"
  | "ESTIMATED"
  | "AI_SUGGESTED";

export interface StructuredInfoEntry {
  projectId: string;
  section: string;
  key: string;
  value: string;
  sourceType: InfoSourceType;
  originalSourceType?: InfoSourceType | null;
  sourceDocument?: string | null;
  confirmedAt?: string | null;
  updatedAt: string;
}

export interface InfoEntryInput {
  section: string;
  key: string;
  value: string;
  sourceType: InfoSourceType;
}

export type DocumentStatus = "PENDIENTE" | "PROCESANDO" | "PROCESADO" | "ERROR";

export interface ProjectDocumentItem {
  id: string;
  projectId: string;
  filename: string;
  mimeType: string;
  size: number;
  status: DocumentStatus;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

export const projectInfoService = {
  listInfo: (projectId: string) =>
    api.get<StructuredInfoEntry[]>(`/projects/${projectId}/info`),

  saveInfo: (projectId: string, entries: InfoEntryInput[]) =>
    api.post<StructuredInfoEntry[]>(`/projects/${projectId}/info`, { entries }),

  listDocuments: (projectId: string) =>
    api.get<ProjectDocumentItem[]>(`/projects/${projectId}/documents`),

  confirmInfo: (projectId: string, section: string, key: string) =>
    api.post<StructuredInfoEntry>(`/projects/${projectId}/info/${section}/${key}/confirm`, {}),

  uploadDocument: async (projectId: string, file: File): Promise<ProjectDocumentItem> => {
    const token = getToken();
    const form = new FormData();
    form.append("file", file);
    const res = await fetch(`${API_URL}/projects/${projectId}/documents`, {
      method: "POST",
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    });
    if (!res.ok) {
      const body = await res.json().catch(() => null);
      throw new Error(body?.error ?? `Request failed (${res.status})`);
    }
    return (await res.json()) as ProjectDocumentItem;
  },
};
