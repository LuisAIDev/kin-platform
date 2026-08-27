"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { canAccessPath, homePathForRole } from "@/utils/roles";

/**
 * Guard de rutas por rol/vertical (client-side).
 *
 * Complementa al middleware (`src/proxy.ts`) que aplica la misma lógica en el
 * servidor: si el usuario no tiene permiso para el pathname actual, se le
 * redirige al home de su vertical. Bloquea, por ejemplo, el acceso de un
 * paciente a `/dashboard/empresa/analytics`.
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

  return <>{children}</>;
}
