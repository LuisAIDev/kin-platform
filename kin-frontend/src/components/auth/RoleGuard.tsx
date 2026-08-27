"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { canAccessPath, homePathForRole, isAccountUnderReview } from "@/utils/roles";
import AccountReviewScreen from "@/components/auth/AccountReviewScreen";

/**
 * Guard de rutas por rol/vertical (client-side).
 *
 * Complementa al middleware (`src/proxy.ts`) que aplica la misma lógica en el
 * servidor:
 * - Sin sesión → `/login`.
 * - Médico con la cuenta pendiente/rechazada → pantalla de espera "en revisión".
 * - Ruta fuera de la vertical → home del rol (p. ej. paciente no puede abrir
 *   `/dashboard/empresa/analytics`).
 */
export default function RoleGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const role = user?.role;

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (!user) {
      router.replace("/login");
      return;
    }
    if (!canAccessPath(role, pathname)) {
      router.replace(homePathForRole(role));
    }
  }, [role, pathname, router, user]);

  if (typeof window !== "undefined" && user && isAccountUnderReview(user)) {
    return <AccountReviewScreen />;
  }

  return <>{children}</>;
}
