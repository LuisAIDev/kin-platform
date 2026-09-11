"use client";

import { authService } from "@/services/auth";
import { getSelectedVertical } from "@/services/session";
import { isHealthRole, resolveVertical } from "@/utils/roles";
import OnboardingChecklist from "@/components/onboarding/OnboardingChecklist";

/**
 * Muestra el checklist de bienvenida (orientado a la vertical Empresa) solo
 * cuando el usuario está en la vertical Empresa. Se oculta en la vertical
 * Salud (tanto por rol como por selección de vertical).
 */
export default function MaybeOnboarding() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const selected = typeof window !== "undefined" ? getSelectedVertical() : null;
  const vertical = resolveVertical(user?.role, selected);
  if (isHealthRole(user?.role) || vertical === "salud") return null;
  return <OnboardingChecklist />;
}
