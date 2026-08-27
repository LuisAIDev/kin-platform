"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { homePathForRole } from "@/utils/roles";

/**
 * Redirige `/dashboard` (raíz) al home de la vertical del usuario:
 * `/dashboard/empresa`, `/dashboard/salud` o `/dashboard/admin`.
 * El middleware (`src/proxy.ts`) ya aplica esta lógica en el servidor; esta
 * página cubre las navegaciones que no pasan por él.
 */
export default function DashboardIndexRedirect() {
  const router = useRouter();

  useEffect(() => {
    const user = typeof window !== "undefined" ? authService.getUser() : null;
    router.replace(homePathForRole(user?.role));
  }, [router]);

  return (
    <main className="flex-1 flex items-center justify-center">
      <p className="text-neutral-500">Redirigiendo...</p>
    </main>
  );
}
