"use client";

import { authService } from "@/services/auth";
import { isHealthRole } from "@/utils/roles";
import OnboardingChecklist from "@/components/onboarding/OnboardingChecklist";

/**
 * Muestra el checklist de bienvenida (vertical Empresa). En Empresas no existe
 * la vertical Salud, por lo que se muestra siempre que la cuenta no sea de
 * salud (compatibilidad).
 */
export default function MaybeOnboarding() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  if (isHealthRole(user?.role)) return null;
  return <OnboardingChecklist />;
}
