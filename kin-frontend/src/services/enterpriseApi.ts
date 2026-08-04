import type { EnterpriseDashboard } from "@/types/enterprise";
import { API_URL } from "@/services/session";

/** URL base de la API del backend KIN (compartida con el resto de servicios). */
export { API_URL };

/** Token JWT almacenado por la aplicación principal (misma clave que el resto de servicios). */
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
  /**
   * Solicita la generación del proyecto empresarial (M3G).
   *
   * <p>Invoca {@code POST /enterprise/{projectId}/generate} con
   * {@code {"async": true}}. Devuelve el estado HTTP: {@code 202} (generación
   * aceptada, asíncrona) o {@code 409} (ya hay una generación en curso). Ante
   * cualquier otro código de error lanza {@code Error} con el mensaje del
   * backend ({@code message}).</p>
   */
  generate(projectId: string, asyncMode?: boolean): Promise<number>;
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
  generate: async (projectId, asyncMode = true) => {
    const res = await fetch(`${API_URL}/enterprise/${projectId}/generate`, {
      method: "POST",
      headers: { ...authHeaders(), "Content-Type": "application/json" },
      body: JSON.stringify({ async: asyncMode }),
    });
    // 201 (síncrona), 202 (asíncrona) o 409 (ya en curso): la generación
    // existe o está en curso y la UI puede mostrar su progreso.
    if (res.ok || res.status === 409) {
      return res.status;
    }
    const body = await res.json().catch(() => null);
    const message =
      (body as { message?: string } | null)?.message ??
      `Request failed (${res.status})`;
    throw new Error(message);
  },
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
