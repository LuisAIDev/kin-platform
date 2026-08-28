"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { getSelectedVertical } from "@/services/session";
import { homePathForRole } from "@/utils/roles";

/**
 * Redirige `/dashboard` (raíz) al home de la vertical del usuario:
 * `/dashboard/empresa`, `/dashboard/salud` o `/dashboard/admin`.
 * Respeta la vertical de navegación seleccionada (`kin_vertical`) cuando
 * existe; sin selección conserva el comportamiento previo (home del rol).
 * El middleware (`src/proxy.ts`) ya aplica esta lógica en el servidor; esta
 * página cubre las navegaciones que no pasan por él.
 */
export default function DashboardIndexRedirect() {
  const router = useRouter();

  useEffect(() => {
    const user = typeof window !== "undefined" ? authService.getUser() : null;
    const selected = getSelectedVertical();
    router.replace(homePathForRole(user?.role, selected));
  }, [router]);

  return (
    <main className="flex-1 flex items-center justify-center">
      <p className="text-neutral-500">Redirigiendo...</p>
    </main>
  );
}
