"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { checkForceLogout, clearSession } from "@/services/session";

export default function SessionGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  useEffect(() => {
    if (checkForceLogout()) {
      clearSession();
      router.replace("/login");
      return;
    }
    // Con cookie HttpOnly, la autenticación la maneja el middleware (proxy.ts)
    // No podemos leer la cookie HttpOnly desde JavaScript
  }, [router]);

  return <>{children}</>;
}
