"use client";

import { authService } from "@/services/auth";
import { isHealthRole } from "@/utils/roles";
import OnboardingChecklist from "@/components/onboarding/OnboardingChecklist";

/**
 * Muestra el checklist de bienvenida (orientado a la vertical Empresa) solo
 * para usuarios empresariales o administradores. Las verticales de Salud lo
 * ocultan por completo.
 */
export default function MaybeOnboarding() {
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  if (isHealthRole(user?.role)) return null;
  return <OnboardingChecklist />;
}
