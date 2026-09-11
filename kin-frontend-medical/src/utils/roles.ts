/**
 * Utilidades de roles para KIN Medical.
 *
 * Medical es la vertical "salud" en exclusiva: no existen "empresa" ni
 * "admin". El acceso al portal médico se decide por `physicianCapability`
 * (fuente de verdad del backend), no por `role === "PHYSICIAN"`.
 */

export interface PhysicianContext {
  role?: string | null;
  verificationStatus?: string | null;
  physicianCapability?: boolean;
}

/**
 * Determina si el usuario tiene CAPACIDAD profesional (médico).
 *
 * Java decide: el valor proviene de `physicianCapability` (calculado por
 * `PhysicianAccess` en el backend). FREE/PREMIUM/PATIENT + APPROVED también
 * son médicos. Fallback: sesiones antiguas sin el campo usan role === PHYSICIAN.
 */
export function hasPhysicianCapability(user?: PhysicianContext | null): boolean {
  if (!user) return false;
  if (user.physicianCapability !== undefined) {
    return user.physicianCapability === true;
  }
  return user.role === "PHYSICIAN";
}

export function isPhysicianRole(roleOrUser?: string | null | PhysicianContext): boolean {
  if (typeof roleOrUser === "string" || roleOrUser == null) {
    return roleOrUser === "PHYSICIAN";
  }
  return hasPhysicianCapability(roleOrUser);
}

export function isPatientRole(role?: string | null): boolean {
  return role === "PATIENT";
}

export function isAdminRole(role?: string | null): boolean {
  return role === "ADMIN";
}

export function isHealthRole(role?: string | null): boolean {
  return role === "PATIENT" || role === "PHYSICIAN";
}

/** En Medical no existe la vertical empresarial. */
export function isBusinessRole(_role?: string | null): boolean {
  return false;
}

export function isAccountPendingReview(user?: PhysicianContext | null): boolean {
  return user?.role === "PHYSICIAN" && user?.verificationStatus === "PENDING";
}

export function isAccountRejected(user?: PhysicianContext | null): boolean {
  return user?.role === "PHYSICIAN" && user?.verificationStatus === "REJECTED";
}

export function isAccountUnderReview(user?: PhysicianContext | null): boolean {
  return isAccountPendingReview(user) || isAccountRejected(user);
}

/** Página de inicio según rol: médicos al portal, el resto a paciente. */
export function homePathForRole(roleOrUser?: string | null | PhysicianContext, _selected?: unknown): string {
  const role =
    typeof roleOrUser === "string" || roleOrUser == null ? roleOrUser : roleOrUser.role;
  if (isPhysicianRole(roleOrUser)) return "/dashboard/physician";
  if (role === "ADMIN") return "/dashboard/patient";
  return "/dashboard/patient";
}

/**
 * Determina si un usuario puede acceder a un pathname del dashboard.
 *
 * - `/dashboard/patient/*` → accesible a cualquier usuario autenticado
 *   (el backend autoriza; un usuario sin consentimiento ve invitaciones).
 * - `/dashboard/physician/*` → requiere capacidad profesional.
 * - `/dashboard/settings` → común.
 */
export function canAccessPath(
  roleOrUser: string | null | undefined | PhysicianContext,
  pathname: string
): boolean {
  const role =
    typeof roleOrUser === "string" || roleOrUser == null ? roleOrUser : roleOrUser.role;

  if (!role) return false;

  if (pathname.startsWith("/dashboard/physician")) {
    const user =
      typeof roleOrUser === "string" || roleOrUser == null
        ? { role: roleOrUser }
        : roleOrUser;
    return hasPhysicianCapability(user);
  }

  if (pathname.startsWith("/dashboard/patient")) return true;
  if (pathname.startsWith("/dashboard/settings")) return true;
  if (pathname === "/dashboard" || pathname === "/dashboard/") return true;

  return false;
}
// ------------------------------------------------------------------
// Compatibilidad con el código migrado de kin-frontend: en Medical la
// vertical es siempre "salud" (no hay selector).
// ------------------------------------------------------------------
export type Vertical = "salud";

export function verticalForRole(_role?: string | null): Vertical {
  return "salud";
}

export function resolveVertical(_role?: string | null, _selected?: unknown): Vertical {
  return "salud";
}

export function isSelectedVertical(value: unknown): value is "salud" {
  return value === "salud";
}
