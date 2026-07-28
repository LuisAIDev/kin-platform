"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { checkForceLogout, clearSession } from "@/services/session";

export default function SessionGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  useEffect(() => {
    if (checkForceLogout()) {
      clearSession();
      router.replace("/login");
      return;
    }

    if (!authService.getToken()) {
      router.replace("/login");
    }
  }, [router]);

  return <>{children}</>;
}
