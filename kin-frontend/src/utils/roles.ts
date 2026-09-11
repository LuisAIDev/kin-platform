/**
 * Utilidades de segmentación por rol — KIN Empresas.
 *
 * Este frontend corresponde ÚNICAMENTE a la vertical Empresa (estructuración
 * de proyectos). La vertical Salud vive en `kin-frontend-medical`.
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

/** Verticales de navegación disponibles en Empresas: empresa y admin. */
export type Vertical = "empresa" | "admin";

/** Vertical por defecto según el rol. */
export function verticalForRole(role?: string | null): Vertical {
  if (role === "ADMIN") return "admin";
  return "empresa";
}

/**
 * Resuelve la vertical de NAVEGACIÓN. En Empresas solo existen "empresa" y
 * "admin"; la vertical Salud ya no está disponible en este frontend.
 */
export function resolveVertical(_role?: string | null, _selected?: unknown): Vertical {
  return verticalForRole(_role);
}

/** Página de inicio según rol. */
export function homePathForRole(role?: string | null, _selected?: unknown): string {
  if (verticalForRole(role) === "admin") return "/dashboard/admin";
  return "/dashboard/empresa";
}

export function isAdminRole(role?: string | null): boolean {
  return role === "ADMIN";
}

/** En Empresas no existe la vertical Salud. */
export function isHealthRole(_role?: string | null): boolean {
  return false;
}

export function isBusinessRole(role?: string | null): boolean {
  return role !== "ADMIN";
}

/** En Empresas no existen subáreas de paciente/médico. */
export function isPatientRole(_role?: string | null): boolean {
  return false;
}

export function isPhysicianRole(_role?: string | null): boolean {
  return false;
}

/** Contexto de rol devuelto por el backend (/auth/me, login). */
export interface PhysicianContext {
  role?: string | null;
  verificationStatus?: string | null;
  physicianCapability?: boolean;
}

export interface RoleContext {
  role?: string | null;
  verificationStatus?: string | null;
}

/** Sin subárea de médico en Empresas: no hay cuentas "en revisión". */
export function isAccountPendingReview(_user?: RoleContext | null): boolean {
  return false;
}

export function isAccountRejected(_user?: RoleContext | null): boolean {
  return false;
}

export function isAccountUnderReview(_user?: RoleContext | null): boolean {
  return false;
}

/**
 * Determina si un usuario puede acceder a un pathname del dashboard.
 *
 * Reglas (solo Empresas):
 * - ADMIN accede a todo.
 * - `/dashboard/settings` es común.
 * - `/dashboard/empresa` es el hub empresarial.
 * - `/dashboard/admin` solo para ADMIN (ya cubierto por la regla anterior).
 * - Las subáreas de salud (patient/physician/salud) NO son accesibles.
 */
export function canAccessPath(
  roleOrUser: string | null | undefined | PhysicianContext,
  pathname: string,
  selected?: unknown,
): boolean {
  const role =
    typeof roleOrUser === "string" || roleOrUser == null ? roleOrUser : roleOrUser.role;
  const vertical = resolveVertical(role, selected);
  if (vertical === "admin") return true;

  const rest = pathname.replace(/^\/dashboard\/?/, "");
  const segment = rest.split("/")[0] ?? "";

  if (segment === "") return true;
  if (segment === "settings") return true;
  if (segment === "empresa") return vertical === "empresa";
  if (segment === "admin") return false;
  if (segment === "accept-invitation") return true;
  // La vertical Salud no existe en este frontend.
  if (segment === "patient" || segment === "physician" || segment === "salud") return false;

  return vertical === "empresa";
}
