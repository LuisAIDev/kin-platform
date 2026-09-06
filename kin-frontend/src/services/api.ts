import { forceLogout } from "./session";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

/**
 * Endpoints de autenticación/registro: en ellos un 401/400 es un error de
 * NEGOCIO (credenciales inválidas, email no verificado, cuenta pendiente), NO
 * una sesión expirada. En esos casos NO se debe llamar a `forceLogout()` (que
 * recargaba /login o limpiaba la sesión) sino propagar el error al llamante
 * para que la UI muestre un mensaje.
 */
const AUTH_ENDPOINTS = [
  "/auth/login",
  "/auth/register",
  "/auth/register/patient",
  "/auth/register/physician",
  "/auth/resend-verification",
  "/auth/forgot-password",
  "/auth/reset-password",
  "/auth/verify-email",
  "/auth/refresh",
];

function isAuthEndpoint(endpoint: string): boolean {
  return AUTH_ENDPOINTS.some((prefix) => endpoint.startsWith(prefix));
}

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  const res = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers,
    credentials: "include",
  });

  if (!res.ok) {
    const body = await res.json().catch(() => null);
    const message = body?.error ?? `Request failed (${res.status})`;

    const error = new Error(message) as Error & { code?: string; status?: number };
    error.code = body?.code;
    error.status = res.status;

    if (res.status === 401 && !isAuthEndpoint(endpoint)) {
      forceLogout();
      error.message = "Unauthorized";
    }

    if (res.status === 400 && !isAuthEndpoint(endpoint) && message.toLowerCase().includes("authenticated user")) {
      forceLogout();
    }

    throw error;
  }

  if (res.status === 204) return undefined as T;

  return res.json();
}

export const api = {
  get: <T>(endpoint: string) => request<T>(endpoint),
  post: <T>(endpoint: string, data: unknown) =>
    request<T>(endpoint, { method: "POST", body: JSON.stringify(data) }),
  put: <T>(endpoint: string, data: unknown) =>
    request<T>(endpoint, { method: "PUT", body: JSON.stringify(data) }),
  delete: <T>(endpoint: string) =>
    request<T>(endpoint, { method: "DELETE" }),
};
