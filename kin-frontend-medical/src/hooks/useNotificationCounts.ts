"use client";

import { useEffect, useState } from "react";
import { EMPTY_COUNTS, type NotificationCounts } from "@/services/notifications";

/**
 * Contadores de novedades del sidebar (mensajes, citas, tareas, invitaciones).
 * Hace polling cada 30 s contra /health/notifications/counts (ruta canónica
 * del backend; el backend autoriza por identidad/rol).
 */
export function useNotificationCounts(_role?: string | null): NotificationCounts | null {
  const [counts, setCounts] = useState<NotificationCounts | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchCounts() {
      try {
        const res = await fetch("/api/v1/health/notifications/counts", { credentials: "include" });
        if (!res.ok) return;
        const data = (await res.json()) as NotificationCounts;
        if (!cancelled) setCounts(data);
      } catch {
        if (!cancelled) setCounts(EMPTY_COUNTS);
      }
    }

    fetchCounts();
    const interval = setInterval(fetchCounts, 30000);

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [_role]);

  return counts;
}