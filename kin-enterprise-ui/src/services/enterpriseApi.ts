import type { EnterpriseDashboard } from "../types/enterprise";

/** URL base de la API (configurable vía VITE_API_URL). */
export const API_URL: string =
  (import.meta.env.VITE_API_URL as string | undefined) ??
  "http://localhost:8080/api/v1";

/** Token JWT almacenado por la aplicación principal (misma clave que kin-frontend). */
export function authHeaders(): Record<string, string> {
  const token =
    typeof window !== "undefined"
      ? window.localStorage.getItem("kin_token_v2")
      : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function jsonRequest<T>(endpoint: string): Promise<T> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    headers: { ...authHeaders(), Accept: "application/json" },
  });
  if (!res.ok) {
    throw new Error(`Request failed (${res.status})`);
  }
  return (await res.json()) as T;
}

async function binaryRequest(endpoint: string): Promise<Blob> {
  const res = await fetch(`${API_URL}${endpoint}`, { headers: authHeaders() });
  if (!res.ok) {
    throw new Error(`Request failed (${res.status})`);
  }
  return res.blob();
}

export interface EnterpriseApi {
  getDashboard(projectId: string, version: number): Promise<EnterpriseDashboard>;
  downloadDocument(
    projectId: string,
    version: number,
    type: string,
    format: string,
  ): Promise<Blob>;
  downloadBundle(projectId: string, version: number, format: string): Promise<Blob>;
}

export const enterpriseApi: EnterpriseApi = {
  getDashboard: (projectId, version) =>
    jsonRequest<EnterpriseDashboard>(
      `/enterprise/${projectId}/${version}/dashboard`,
    ),
  downloadDocument: (projectId, version, type, format) =>
    binaryRequest(`/enterprise/${projectId}/${version}/export/${type}/${format}`),
  downloadBundle: (projectId, version, format) =>
    binaryRequest(`/enterprise/${projectId}/${version}/export/${format}`),
};

/** Descarga un Blob en el navegador. */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
