export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

let _forceLogoutInProgress = false;

const COOKIE_OPTIONS = "path=/; max-age=86400; SameSite=Lax";

function clearAllCookies() {
  const cookies = document.cookie.split("; ");
  for (const cookie of cookies) {
    const eqPos = cookie.indexOf("=");
    const name = eqPos > -1 ? cookie.substring(0, eqPos) : cookie;
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
  }
}

export function checkForceLogout(): boolean {
  if (typeof document === "undefined") return false;
  for (const cookie of document.cookie.split("; ")) {
    const [name] = cookie.split("=");
    if (name === "kin_force_logout") {
      document.cookie = "kin_force_logout=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
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
}) {
  localStorage.setItem("kin_user_v2", JSON.stringify({
    email: res.email,
    fullName: res.fullName,
    role: res.role,
    emailVerified: res.emailVerified ?? false,
    verificationStatus: res.verificationStatus ?? null,
  }));
  // El token se almacena SOLO en la cookie HttpOnly (kin_token_v2) gestionada por el backend.
  // No guardamos el token en localStorage/sessionStorage para evitar exposición a XSS.
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
    await fetch(`${API_URL}/auth/logout`, {
      method: "POST",
      credentials: "include",
    });
  } catch {
    // best-effort
  }

  clearSession();

  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}
