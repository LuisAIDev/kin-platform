"use client";

import { useEffect, useState } from "react";
import { EMPTY_COUNTS, type NotificationCounts } from "@/services/notifications";

/**
 * Contadores de novedades del sidebar (mensajes, citas, tareas, invitaciones).
 * Hace polling cada 30 s contra /notifications/counts (ruta legacy común a
 * ambas verticales; el backend autoriza por identidad).
 */
export function useNotificationCounts(_role?: string | null): NotificationCounts | null {
  const [counts, setCounts] = useState<NotificationCounts | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchCounts() {
      try {
        const res = await fetch(
          `${process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"}/notifications/counts`,
          { credentials: "include" }
        );
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