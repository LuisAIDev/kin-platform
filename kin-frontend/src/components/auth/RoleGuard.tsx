"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { storeSession } from "@/services/session";
import { canAccessPath, homePathForRole, isAccountUnderReview } from "@/utils/roles";
import AccountReviewScreen from "@/components/auth/AccountReviewScreen";

/**
 * Guard de rutas por rol/vertical (client-side).
 *
 * El middleware (`src/proxy.ts`) es la autoridad de autenticación (cookie
 * HttpOnly). Este guard aplica la segmentación por rol:
 * - Médico pendiente/rechazado → pantalla de espera "en revisión".
 * - Ruta fuera de la vertical → home del rol.
 *
 * Si el espejo local (`kin_user_v2`) se perdió pero la cookie existe, se
 * **re-sincroniza** desde `/auth/me` en lugar de rebotar a `/login`; esto evita
 * el bucle de redirección (middleware /login ⇄ guard) que hace parpadear la
 * página de login. Solo se redirige a `/login` si el servidor confirma que no
 * hay sesión.
 */
export default function RoleGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const role = user?.role;

  useEffect(() => {
    if (typeof window === "undefined") return;

    if (user) {
      if (!canAccessPath(role, pathname)) {
        router.replace(homePathForRole(role));
      }
      return;
    }

    // Sin espejo local: verificar contra el servidor antes de decidir.
    // Se usa un fetch raw (authService.fetchCurrentUser): un 401 aquí significa
    // que no hay sesión (no debe disparar forceLogout ni recargar en bucle).
    let cancelled = false;
    authService
      .fetchCurrentUser()
      .then((me) => {
        if (cancelled) return;
        if (!me?.role) {
          router.replace("/login");
          return;
        }
        storeSession(me);
        if (!canAccessPath(me.role, pathname)) {
          router.replace(homePathForRole(me.role));
        }
      })
      .catch(() => {
        if (!cancelled) router.replace("/login");
      });

    return () => {
      cancelled = true;
    };
  }, [role, pathname, router, user]);

  if (typeof window !== "undefined" && user && isAccountUnderReview(user)) {
    return <AccountReviewScreen />;
  }

  return <>{children}</>;
}
