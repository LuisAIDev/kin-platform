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

import {
  isSelectedVertical,
  type SelectedVertical,
} from "@/services/session";

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

/** Vertical por defecto según el rol (cuando no hay selección explícita). */
export function verticalForRole(role?: string | null): Vertical {
  if (!role) return "empresa";
  if (role === "ADMIN") return "admin";
  if (HEALTH_ROLES.includes(role)) return "salud";
  return "empresa";
}

/**
 * Resuelve la vertical de NAVEGACIÓN a partir del rol y la vertical
 * seleccionada por el usuario.
 *
 * ROLE ≠ VERTICAL: el rol determina autorización; la selección determina el
 * módulo/hub en el que trabaja el usuario.
 * - ADMIN → admin (ignora selección; mantiene su hub).
 * - Roles de salud (PATIENT/PHYSICIAN) → salud (ignoran selección).
 * - Roles empresariales → respetan la selección explícita (empresa/salud);
 *   sin selección conservan el comportamiento previo (empresa).
 */
export function resolveVertical(
  role?: string | null,
  selected?: SelectedVertical | null,
): Vertical {
  const base = verticalForRole(role);
  if (base === "admin" || base === "salud") return base;
  return isSelectedVertical(selected) ? selected : "empresa";
}

/** Página de inicio según rol y vertical de navegación seleccionada. */
export function homePathForRole(
  role?: string | null,
  selected?: SelectedVertical | null,
): string {
  const vertical = resolveVertical(role, selected);
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

export function isPhysicianRole(
  roleOrUser?: string | null | PhysicianContext,
): boolean {
  if (typeof roleOrUser === "string" || roleOrUser == null) {
    // Compatibilidad con llamadas existentes que pasan el role (p. ej.
    // dashboard/salud, Sidebar). Para sesiones nuevas se prefiere el contexto.
    return roleOrUser === "PHYSICIAN";
  }
  return hasPhysicianCapability(roleOrUser);
}

/** Contexto de rol/capacidad devuelto por el backend (/auth/me, login). */
export interface PhysicianContext {
  role?: string | null;
  verificationStatus?: string | null;
  /** Capacidad profesional derivada por el backend. Fuente de verdad. */
  physicianCapability?: boolean;
}

/**
 * Determina si el usuario tiene CAPACIDAD profesional (médico).
 *
 * Java decide: el valor proviene de {@code physicianCapability} (calculado por
 * {@code PhysicianAccess} en el backend), NO de {@code role === "PHYSICIAN"}.
 * FREE/PREMIUM/PATIENT + APPROVED también son médicos.
 *
 * Fallback de compatibilidad: si el contexto no trae {@code physicianCapability}
 * (sesiones antiguas en localStorage), se conserva el comportamiento previo
 * ({@code role === "PHYSICIAN"}) para no romper sesiones existentes.
 */
export function hasPhysicianCapability(user?: PhysicianContext | null): boolean {
  if (!user) return false;
  if (user.physicianCapability !== undefined) {
    return user.physicianCapability === true;
  }
  return user.role === "PHYSICIAN";
}

/** Estado de verificación de identidad de un médico (auto-registro). */
export type PhysicianVerificationStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface RoleContext {
  role?: string | null;
  verificationStatus?: string | null;
}

/** Un médico con la cuenta pendiente de revisión por un administrador. */
export function isAccountPendingReview(user?: RoleContext | null): boolean {
  return user?.role === "PHYSICIAN" && user?.verificationStatus === "PENDING";
}

/** Un médico con la cuenta rechazada. */
export function isAccountRejected(user?: RoleContext | null): boolean {
  return user?.role === "PHYSICIAN" && user?.verificationStatus === "REJECTED";
}

/** Cuenta médica no habilitada (pendiente o rechazada). */
export function isAccountUnderReview(user?: RoleContext | null): boolean {
  return isAccountPendingReview(user) || isAccountRejected(user);
}

/**
 * Determina si un usuario puede acceder a un pathname del dashboard.
 *
 * Acepta el role como string (compatibilidad con llamadas existentes) o un
 * contexto de usuario ({@link PhysicianContext}) que puede incluir
 * {@code physicianCapability} (fuente de verdad del backend).
 *
 * Reglas:
 * - ADMIN accede a todo.
 * - `/dashboard/settings` es común a todas las verticales.
 * - Hubs: `/dashboard/empresa` (empresarial), `/dashboard/salud` (salud), `/dashboard/admin` (admin).
 * - `/dashboard/patient/*` solo para la persona paciente (no PHYSICIAN legado).
 * - `/dashboard/physician/*` requiere CAPACIDAD profesional (physicianCapability),
 *   no solo `role === "PHYSICIAN"` (FREE/PREMIUM/PATIENT + APPROVED también acceden).
 * - Segmentos empresariales (projects, analytics, insights, recommendations,
 *   reports, pricing, subscription) solo para la vertical empresa.
 */
export function canAccessPath(
  roleOrUser: string | null | undefined | PhysicianContext,
  pathname: string,
  selected?: SelectedVertical | null,
): boolean {
  const role = typeof roleOrUser === "string" || roleOrUser == null ? roleOrUser : roleOrUser.role;
  const vertical = resolveVertical(role, selected);
  if (vertical === "admin") return true;

  const rest = pathname.replace(/^\/dashboard\/?/, "");
  const segment = rest.split("/")[0] ?? "";

  if (segment === "") return true; // /dashboard → el hub decide el redirect
  if (segment === "settings") return true;
  if (segment === "empresa") return vertical === "empresa";
  if (segment === "salud") return vertical === "salud";
  // Subárea de paciente: permitida en la vertical Salud a todo rol que no sea
  // el rol PHYSICIAN legado (el backend ya la autoriza para roles empresariales).
  if (segment === "patient") return vertical === "salud" && role !== "PHYSICIAN";
  // Portal médico: requiere CAPACIDAD profesional (no solo role PHYSICIAN).
  // Un FREE/PREMIUM/PATIENT con physicianCapability=true (APPROVED) también
  // accede, independientemente de la vertical de persona seleccionada.
  if (segment === "physician") return hasPhysicianCapability(
      typeof roleOrUser === "string" || roleOrUser == null ? { role: roleOrUser } : roleOrUser,
    );
  if (segment === "admin") return false;

  // Segmentos empresariales (projects, analytics, insights, ...).
  return vertical === "empresa";
}
