import { api } from "./api";
import { storeSession, clearSession, setPendingEmail } from "./session";

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface PatientRegisterRequest extends RegisterRequest {
  dateOfBirth: string; // yyyy-MM-dd
  sex: string;
  phone?: string;
  healthDataConsent: boolean;
}

export interface PhysicianRegisterRequest extends RegisterRequest {
  licenseNumber: string;
  specialty: string;
  country: string;
  phone?: string;
  healthDataConsent: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string | null;
  email: string;
  fullName: string;
  role: string;
  emailVerified: boolean;
  verificationStatus?: string | null;
  /** Capacidad profesional derivada por el backend (PhysicianAccess). */
  physicianCapability?: boolean;
  state?: string | null;
}

export type ResendVerificationStatus =
  | "SENT"
  | "ALREADY_VERIFIED"
  | "COOLDOWN"
  | "NO_ACCOUNT";

export interface ResendVerificationResponse {
  message: string;
  status: ResendVerificationStatus;
}

export const authService = {
  async register(data: RegisterRequest) {
    try {
      const res = await api.post<AuthResponse>("/auth/register", data);
      setPendingEmail(res.email);
      return { data: res, error: null };
    } catch (err) {
      return { data: null, error: (err as Error).message };
    }
  },

  async registerPatient(data: PatientRegisterRequest) {
    try {
      const res = await api.post<AuthResponse>("/auth/register/patient", data);
      setPendingEmail(res.email);
      return { data: res, error: null };
    } catch (err) {
      return { data: null, error: (err as Error).message };
    }
  },

  async registerPhysician(data: PhysicianRegisterRequest) {
    try {
      const res = await api.post<AuthResponse>("/auth/register/physician", data);
      setPendingEmail(res.email);
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
      const e = err as Error & { code?: string };
      return {
        data: null,
        error: e?.message || "No se pudo conectar con el servidor",
        code: e?.code,
      };
    }
  },

  async verifyEmail(token: string) {
    try {
      const res = await api.get<{ message: string }>(
        `/auth/verify-email?token=${encodeURIComponent(token)}`
      );
      return { data: res, error: null, code: null };
    } catch (err) {
      const e = err as Error & { code?: string };
      return { data: null, error: e.message, code: e.code ?? null };
    }
  },

  async resendVerification(email: string) {
    try {
      const res = await api.post<ResendVerificationResponse>("/auth/resend-verification", {
        email,
      });
      return { data: res, error: null };
    } catch (err) {
      return { data: null, error: (err as Error).message };
    }
  },

  async logout(): Promise<void> {
    try {
      await fetch(
        `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/auth/logout`,
        { method: "POST", credentials: "include" }
      );
    } catch {
      // best-effort
    }
    clearSession();
  },

  getUser(): AuthResponse | null {
    if (typeof window === "undefined") return null;
    const raw = localStorage.getItem("kin_user_v2");
    return raw ? JSON.parse(raw) : null;
  },

  async fetchCurrentUser(): Promise<AuthResponse | null> {
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/auth/me`,
        { credentials: "include" }
      );
      if (res.status === 401) {
        const body = await res.json().catch(() => null);
        throw new Error("unauthorized");
      }
      if (!res.ok) return null;
      const body = await res.json();
      if (!body?.role) return null;
      return {
        token: null,
        email: body.email ?? "",
        fullName: body.fullName ?? "",
        role: body.role,
        emailVerified: body.emailVerified ?? true,
        verificationStatus: body.verificationStatus ?? null,
        physicianCapability: body.physicianCapability === true,
      };
    } catch (err: any) {
      if (err?.message === "unauthorized") {
        throw err;
      }
      return null;
    }
  },
};