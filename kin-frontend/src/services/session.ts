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

export function clearSession() {
  localStorage.clear();
  sessionStorage.clear();
  clearAllCookies();
}

function setCookie(name: string, value: string) {
  document.cookie = `${name}=${value}; ${COOKIE_OPTIONS}`;
}

export function storeSession(res: { token: string; email: string; fullName: string; role: string }) {
  localStorage.setItem("kin_token_v2", res.token);
  localStorage.setItem("kin_user_v2", JSON.stringify(res));
  setCookie("kin_session_v2", "active");
  setCookie("kin_token_v2", res.token);
}

export function forceLogout() {
  if (_forceLogoutInProgress) return;
  _forceLogoutInProgress = true;

  const token = localStorage.getItem("kin_token_v2");
  if (token) {
    fetch(`${API_URL}/auth/logout`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {});
  }

  clearSession();

  if (typeof window !== "undefined") {
    window.location.href = "/login";
  }
}
