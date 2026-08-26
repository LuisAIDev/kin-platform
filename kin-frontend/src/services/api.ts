import { forceLogout } from "./session";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

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

    const error = new Error(message) as Error & { code?: string };
    error.code = body?.code;

    if (res.status === 401) {
      forceLogout();
      error.message = "Unauthorized";
      throw error;
    }

    if (res.status === 400 && message.toLowerCase().includes("authenticated user")) {
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
