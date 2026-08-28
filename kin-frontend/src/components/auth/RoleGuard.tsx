"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { getSelectedVertical, storeSession } from "@/services/session";
import { canAccessPath, homePathForRole, isAccountUnderReview } from "@/utils/roles";
import AccountReviewScreen from "@/components/auth/AccountReviewScreen";

/**
 * Guard de rutas por rol/vertical (client-side).
 *
 * Separa tres conceptos: (1) ¿está autenticado?, (2) ¿qué rol tiene?, (3) ¿qué
 * vertical de navegación seleccionó?. La vertical seleccionada (`kin_vertical`)
 * determina el hub/módulo; el rol sigue limitando sub-áreas específicas
 * (portal médico, admin).
 *
 * El middleware (`src/proxy.ts`) resuelve la sesión cuando la cookie HttpOnly
 * es visible en el origen del frontend (mismo-origen); en despliegues
 * cross-origin la cookie pertenece al backend y este guard la resuelve vía
 * `/auth/me`. Si el espejo local (`kin_user_v2`) se perdió pero la sesión
 * existe, se re-sincroniza desde `/auth/me`.
 */
export default function RoleGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  // Forza un re-render tras re-sincronizar el espejo local (storeSession).
  const [, setResynced] = useState(false);
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const role = user?.role;
  const selected = typeof window !== "undefined" ? getSelectedVertical() : null;

  useEffect(() => {
    if (typeof window === "undefined") return;

    if (user) {
      if (!canAccessPath(role, pathname, selected)) {
        router.replace(homePathForRole(role, selected));
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
        setResynced(true);
        if (!canAccessPath(me.role, pathname, selected)) {
          router.replace(homePathForRole(me.role, selected));
        }
      })
      .catch(() => {
        if (!cancelled) router.replace("/login");
      });

    return () => {
      cancelled = true;
    };
  }, [role, pathname, router, selected, user]);

  if (!user) {
    // Sin espejo local: mostramos una pantalla de carga mientras resolvemos la
    // sesión contra el servidor (evita parpadear el dashboard con contenido de
    // una sesión inexistente o lanzar llamadas 401 redundantes).
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-neutral-500">Verificando sesión...</p>
      </div>
    );
  }

  if (isAccountUnderReview(user)) {
    return <AccountReviewScreen />;
  }

  return <>{children}</>;
}
