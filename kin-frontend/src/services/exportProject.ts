import { API_URL, getToken } from "@/services/session";

export type ExportFormat = "DOCX" | "PDF" | "MARKDOWN";
export type ExportMode = "COMPLETE" | "SUMMARY";

/** Acción aditiva de exportación adjunta a una respuesta del chat. */
export interface ExportAction {
  type: string;
  format: ExportFormat;
  templateDocumentId: string | null;
  templateDocumentName?: string | null;
}

export interface ExportOptions {
  formats: ExportFormat[];
  hasReport: boolean;
  reportSections: string[];
  infoSections: string[];
  filenameBase: string;
}

/** Error de API que conserva el código HTTP para mensajes comprensibles. */
export class ExportApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ExportApiError";
    this.status = status;
  }
}

/** Mismo mecanismo de autenticación que el resto de servicios KIN (Bearer + cookie). */
function exportHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseError(res: Response): Promise<ExportApiError> {
  const body = await res.json().catch(() => null);
  const message =
    (body as { error?: string } | null)?.error ?? `Request failed (${res.status})`;
  return new ExportApiError(message, res.status);
}

async function jsonRequest<T>(endpoint: string): Promise<T> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    headers: { ...exportHeaders(), Accept: "application/json" },
    credentials: "include",
  });
  if (!res.ok) {
    throw await parseError(res);
  }
  return (await res.json()) as T;
}

async function binaryRequest(endpoint: string): Promise<Blob> {
  const res = await fetch(`${API_URL}${endpoint}`, { headers: exportHeaders() });
  if (!res.ok) {
    throw await parseError(res);
  }
  return res.blob();
}

export const exportProjectService = {
  options: (projectId: string): Promise<ExportOptions> =>
    jsonRequest<ExportOptions>(`/projects/${projectId}/export`),

  /**
   * Descarga el documento exportado del proyecto.
   *
   * @param projectId proyecto a exportar
   * @param format    DOCX, PDF o MARKDOWN
   * @param mode      complete o summary
   * @param templateDocumentId documento de referencia (estructura) si aplica
   */
  download: (
    projectId: string,
    format: ExportFormat,
    mode: ExportMode,
    templateDocumentId?: string,
  ): Promise<Blob> => {
    const params = new URLSearchParams({ mode });
    if (templateDocumentId) {
      params.set("templateDocumentId", templateDocumentId);
    }
    const path = format === "MARKDOWN" ? "markdown" : format.toLowerCase();
    return binaryRequest(`/projects/${projectId}/export/${path}?${params.toString()}`);
  },
};
