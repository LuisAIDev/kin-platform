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
  if (typeof window === "undefined") return null;
  return localStorage.getItem("kin_token_v2");
}

function setCookie(name: string, value: string) {
  document.cookie = `${name}=${value}; ${COOKIE_OPTIONS}`;
}

export function storeSession(res: { token: string | null; email: string; fullName: string; role: string; emailVerified?: boolean }) {
  if (res.token) {
    localStorage.setItem("kin_token_v2", res.token);
  }
  localStorage.setItem("kin_user_v2", JSON.stringify(res));
  setCookie("kin_session_v2", "active");
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

export function forceLogout() {
  if (_forceLogoutInProgress) return;
  _forceLogoutInProgress = true;

  const token = getToken();
  if (token) {
    fetch(`${API_URL}/auth/logout`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      credentials: "include",
    }).catch(() => {});
  }

  clearSession();

  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}
