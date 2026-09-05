export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

let _forceLogoutInProgress = false;

const COOKIE_OPTIONS = "path=/; max-age=86400; SameSite=Lax";

/**
 * Vertical de navegación seleccionada por el usuario (contexto, no identidad).
 * Solo se permiten "empresa" y "salud". El rol (JWT) sigue siendo la autoridad
 * de autorización; esta selección SOLO determina el módulo/hub actual.
 */
export const SELECTED_VERTICALS = ["empresa", "salud"] as const;
export type SelectedVertical = (typeof SELECTED_VERTICALS)[number];

const VERTICAL_KEY = "kin_vertical";

/** Valida que un valor sea una vertical seleccionable (función pura). */
export function isSelectedVertical(value: unknown): value is SelectedVertical {
  return value === "empresa" || value === "salud";
}

/** Vertical de navegación seleccionada, o null si no hay selección previa. */
export function getSelectedVertical(): SelectedVertical | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(VERTICAL_KEY);
  return isSelectedVertical(raw) ? raw : null;
}

/** Persiste la vertical seleccionada (localStorage + cookie para el middleware). */
export function setSelectedVertical(vertical: SelectedVertical): void {
  if (!isSelectedVertical(vertical)) return;
  if (typeof window === "undefined") return;
  window.localStorage.setItem(VERTICAL_KEY, vertical);
  document.cookie = `${VERTICAL_KEY}=${vertical}; ${COOKIE_OPTIONS}`;
}

/** Limpia la selección de vertical (localStorage + cookie). */
export function clearSelectedVertical(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(VERTICAL_KEY);
  document.cookie = `${VERTICAL_KEY}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; SameSite=Lax`;
}

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
  clearSelectedVertical();
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
  localStorage.setItem("kin_user_v2", JSON.stringify({
    email: res.email,
    fullName: res.fullName,
    role: res.role,
    emailVerified: res.emailVerified ?? false,
    verificationStatus: res.verificationStatus ?? null,
    physicianCapability: res.physicianCapability ?? false,
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

  if (typeof window !== "undefined" && window.location.pathname !== "/login") {
    // Evita un bucle de recarga si ya estamos en /login (p. ej. un 401 del
    // wrapper api durante la verificación de sesión).
    window.location.href = "/login";
  }
}
