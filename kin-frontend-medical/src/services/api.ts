import { forceLogout } from "./session";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

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

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
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

    if (
      res.status === 400 &&
      !isAuthEndpoint(endpoint) &&
      message.toLowerCase().includes("authenticated user")
    ) {
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
  delete: <T>(endpoint: string) => request<T>(endpoint, { method: "DELETE" }),
};

/**
 * Cliente para la vertical Medical. Antepone `/medical` a cada endpoint,
 * apuntando a `/api/v1/medical/*` del backend (alias por contexto).
 */
export const medicalApi = {
  get: <T>(endpoint: string) => request<T>(`/medical${endpoint}`),
  post: <T>(endpoint: string, data: unknown) =>
    request<T>(`/medical${endpoint}`, { method: "POST", body: JSON.stringify(data) }),
  put: <T>(endpoint: string, data: unknown) =>
    request<T>(`/medical${endpoint}`, { method: "PUT", body: JSON.stringify(data) }),
  delete: <T>(endpoint: string) => request<T>(`/medical${endpoint}`, { method: "DELETE" }),
};