import { api } from "./api";
import { API_URL, storeSession, clearSession } from "./session";

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: string;
}

export const authService = {
  async register(data: RegisterRequest) {
    try {
      const res = await api.post<AuthResponse>("/auth/register", data);
      storeSession(res);
      return { data: res, error: null };
    } catch (err) {
      return { data: null, error: (err as Error).message };
    }
  },

  async login(data: LoginRequest) {
    try {
      const res = await api.post<AuthResponse>("/auth/login", data);
      storeSession(res);
      return { data: res, error: null };
    } catch (err) {
      return { data: null, error: (err as Error).message };
    }
  },

  logout() {
    const token = localStorage.getItem("kin_token_v2");
    if (token) {
      fetch(`${API_URL}/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {});
    }
    clearSession();
  },

  getToken(): string | null {
    return localStorage.getItem("kin_token_v2");
  },

  getUser(): AuthResponse | null {
    const raw = localStorage.getItem("kin_user_v2");
    return raw ? JSON.parse(raw) : null;
  },
};
