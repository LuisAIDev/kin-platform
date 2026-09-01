import { api, API_URL } from "./api";

export type DocumentStatus = "ACTIVE" | "DELETED";

export interface ClinicalDocument {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  patientId: string;
  physicianId: string;
  description: string;
  status: DocumentStatus;
  uploadedAt: string;
}

async function uploadMultipart(endpoint: string, form: FormData): Promise<ClinicalDocument> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    method: "POST",
    body: form,
    credentials: "include",
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    const message = body?.error ?? `Request failed (${res.status})`;
    throw new Error(message);
  }
  return res.json();
}

export const documentsService = {
  upload: (file: File, patientId: string, description?: string) => {
    const form = new FormData();
    form.append("file", file);
    form.append("patientId", patientId);
    if (description) form.append("description", description);
    return uploadMultipart("/health/documents/upload", form);
  },

  documentsForPatient: (patientId: string) =>
    api.get<ClinicalDocument[]>(`/health/documents/patients/${patientId}`),

  myDocuments: () => api.get<ClinicalDocument[]>("/health/documents/my"),

  downloadUrl: (documentId: string) => `${API_URL}/health/documents/${documentId}/download`,

  delete: (documentId: string) => api.delete<void>(`/health/documents/${documentId}`),
};
