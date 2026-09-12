export const API_URL = "/api/v1";

let _forceLogoutInProgress = false;

/**
 * KIN Medical es la vertical "salud" en exclusiva: no hay selector de vertical.
 */
export type SelectedVertical = "salud";

export function isSelectedVertical(value: unknown): value is "salud" {
  return value === "salud";
}

export function getSelectedVertical(): "salud" {
  return "salud";
}

export function setSelectedVertical(_vertical: "salud"): void {
  // No-op: Medical no tiene selector de vertical.
}

export function clearSelectedVertical(): void {
  // No-op
}

function clearAllCookies() {
  const cookies = document.cookie.split("; ");
  for (const cookie of cookies) {
    const eqPos = cookie.indexOf("=");
    const name = eqPos > -1 ? cookie.substring(0, eqPos) : cookie;
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; SameSite=Lax`;
  }
}

export function checkForceLogout(): boolean {
  if (typeof document === "undefined") return false;
  for (const cookie of document.cookie.split("; ")) {
    const [name] = cookie.split("=");
    if (name === "kin_force_logout") {
      document.cookie =
        "kin_force_logout=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; SameSite=Lax";
      return true;
    }
  }
  return false;
}

export function clearSession() {
  localStorage.clear();
  sessionStorage.clear();
  clearAllCookies();
}

export function getToken(): string | null {
  return null;
}

export function storeSession(res: {
  token: string | null;
  email: string;
  fullName: string;
  role: string;
  emailVerified?: boolean;
  verificationStatus?: string | null;
  physicianCapability?: boolean;
}) {
  localStorage.setItem(
    "kin_user_v2",
    JSON.stringify({
      email: res.email,
      fullName: res.fullName,
      role: res.role,
      emailVerified: res.emailVerified ?? false,
      verificationStatus: res.verificationStatus ?? null,
      physicianCapability: res.physicianCapability ?? false,
    })
  );
  // El token se almacena SOLO en la cookie HttpOnly gestionada por el backend.
}

export function setPendingEmail(email: string) {
  sessionStorage.setItem("kin_pending_email", email);
}

export function getPendingEmail(): string | null {
  if (typeof sessionStorage === "undefined") return null;
  return sessionStorage.getItem("kin_pending_email");
}

export function clearPendingEmail() {
  sessionStorage.removeItem("kin_pending_email");
}

export async function forceLogout() {
  if (_forceLogoutInProgress) return;
  _forceLogoutInProgress = true;

  try {
    await fetch("/api/v1/auth/logout", { method: "POST", credentials: "include" });
  } catch {
    // best-effort
  }

  clearSession();

  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}