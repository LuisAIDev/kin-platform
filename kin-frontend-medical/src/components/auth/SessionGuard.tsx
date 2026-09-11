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
    }
  }, [router]);

  return <>{children}</>;
}