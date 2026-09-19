"use client";

import { useEffect, useState } from "react";
import { EMPTY_COUNTS, type NotificationCounts } from "@/services/notifications";

/**
 * Contadores de novedades del sidebar (mensajes, citas, tareas, invitaciones).
 * Hace polling cada 5 min contra /health/notifications/counts.
 * Se pausa cuando la pestaña no está activa (document.hidden).
 */
export function useNotificationCounts(_role?: string | null): NotificationCounts | null {
  const [counts, setCounts] = useState<NotificationCounts | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function fetchCounts() {
      if (document.hidden) return; // Pausar si pestaña inactiva
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
    const interval = setInterval(fetchCounts, 5 * 60 * 1000); // 5 minutos

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [_role]);

  return counts;
}