"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { storeSession, clearSession } from "@/services/session";
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
  const [status, setStatus] = useState<"loading" | "authenticated" | "unauthenticated">(
    "loading"
  );

  useEffect(() => {
    if (typeof window === "undefined") return;

    if (user) {
      setStatus("authenticated");
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
          setStatus("unauthenticated");
          router.replace("/login");
          return;
        }
        storeSession(me);
        setStatus("authenticated");
        setResynced(true);
        if (!canAccessPath(me, pathname)) {
          router.replace(homePathForRole(me));
        }
      })
      .catch((err: any) => {
        if (cancelled) return;
        const status = err?.status ?? err?.response?.status;
        if (status === 401) {
          setStatus("unauthenticated");
          clearSession();
          router.replace("/login");
        } else {
          // Error transitorio: reintentar una vez después de 2s, luego logout
          const retry = async () => {
            try {
              const me = await authService.fetchCurrentUser();
              if (cancelled) return;
              if (!me?.role) {
                setStatus("unauthenticated");
                router.replace("/login");
                return;
              }
              storeSession(me);
              setStatus("authenticated");
              setResynced(true);
              if (!canAccessPath(me, pathname)) {
                router.replace(homePathForRole(me));
              }
            } catch {
              if (!cancelled) {
                setStatus("unauthenticated");
                router.replace("/login");
              }
            }
          };
          setTimeout(retry, 2000);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [role, pathname, router, user, status]);

  if (status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-neutral-500">Verificando sesión...</p>
      </div>
    );
  }

  if (status === "unauthenticated") {
    router.replace("/login");
  }

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