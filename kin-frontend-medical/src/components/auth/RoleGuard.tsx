"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { storeSession } from "@/services/session";
import { canAccessPath, homePathForRole, isAccountUnderReview } from "@/utils/roles";
import AccountReviewScreen from "@/components/auth/AccountReviewScreen";

/**
 * Guard de rutas (client-side) para KIN Medical.
 *
 * Separa: (1) ¿autenticado?, (2) capacidad profesional. La capacidad la decide
 * el backend vía `physicianCapability`, nunca `role === "PHYSICIAN"`.
 */
export default function RoleGuard({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [, setResynced] = useState(false);
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const role = user?.role;

  useEffect(() => {
    if (typeof window === "undefined") return;

    if (user) {
      if (!canAccessPath(user, pathname)) {
        router.replace(homePathForRole(user));
      }
      return;
    }

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
        if (!canAccessPath(me, pathname)) {
          router.replace(homePathForRole(me));
        }
      })
      .catch(() => {
        if (!cancelled) router.replace("/login");
      });

    return () => {
      cancelled = true;
    };
  }, [role, pathname, router, user]);

  if (!user) {
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