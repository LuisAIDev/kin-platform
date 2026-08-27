/**
 * Utilidades de segmentación por rol y vertical.
 *
 * Dos verticales de negocio: **Empresa** (estructuración de proyectos) y
 * **Salud** (triaje, portal médico, telemedicina), más la vertical **Admin**
 * que agrupa a los administradores con acceso a todo.
 *
 * Roles del backend (UserRole): FREE, PREMIUM, FACILITADOR (empresarial),
 * PATIENT, PHYSICIAN (salud) y ADMIN.
 */

export type UserRole =
  | "FREE"
  | "PREMIUM"
  | "FACILITADOR"
  | "PATIENT"
  | "PHYSICIAN"
  | "ADMIN"
  | "USER";

export type Vertical = "empresa" | "salud" | "admin";

const HEALTH_ROLES: string[] = ["PATIENT", "PHYSICIAN"];

/** Vertical a la que pertenece un rol. Sin rol (o legacy "USER") → empresa. */
export function verticalForRole(role?: string | null): Vertical {
  if (!role) return "empresa";
  if (role === "ADMIN") return "admin";
  if (HEALTH_ROLES.includes(role)) return "salud";
  return "empresa";
}

/** Página de inicio del usuario según su rol/vertical. */
export function homePathForRole(role?: string | null): string {
  const vertical = verticalForRole(role);
  if (vertical === "admin") return "/dashboard/admin";
  if (vertical === "salud") return "/dashboard/salud";
  return "/dashboard/empresa";
}

export function isAdminRole(role?: string | null): boolean {
  return role === "ADMIN";
}

export function isHealthRole(role?: string | null): boolean {
  return verticalForRole(role) === "salud";
}

export function isBusinessRole(role?: string | null): boolean {
  return verticalForRole(role) === "empresa";
}

export function isPatientRole(role?: string | null): boolean {
  return role === "PATIENT";
}

export function isPhysicianRole(role?: string | null): boolean {
  return role === "PHYSICIAN";
}

/**
 * Determina si un usuario puede acceder a un pathname del dashboard.
 * Reglas:
 * - ADMIN accede a todo.
 * - `/dashboard/settings` es común a todas las verticales.
 * - Hubs: `/dashboard/empresa` (empresarial), `/dashboard/salud` (salud), `/dashboard/admin` (admin).
 * - `/dashboard/patient/*` solo PATIENT; `/dashboard/physician/*` solo PHYSICIAN.
 * - Segmentos empresariales (projects, analytics, insights, recommendations,
 *   reports, pricing, subscription) solo para la vertical empresa.
 */
export function canAccessPath(role: string | null | undefined, pathname: string): boolean {
  const vertical = verticalForRole(role);
  if (vertical === "admin") return true;

  const rest = pathname.replace(/^\/dashboard\/?/, "");
  const segment = rest.split("/")[0] ?? "";

  if (segment === "") return true; // /dashboard → el hub decide el redirect
  if (segment === "settings") return true;
  if (segment === "empresa") return vertical === "empresa";
  if (segment === "salud") return vertical === "salud";
  if (segment === "patient") return vertical === "salud" && role === "PATIENT";
  if (segment === "physician") return vertical === "salud" && role === "PHYSICIAN";
  if (segment === "admin") return false;

  // Segmentos empresariales (projects, analytics, insights, ...).
  return vertical === "empresa";
}
