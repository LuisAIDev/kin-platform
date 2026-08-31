"use client";

import { useEffect, useState } from "react";
import { notificationService } from "@/services/notifications";
import type { NotificationCounts } from "@/services/notifications";

const POLL_INTERVAL_MS = 30_000;

/**
 * Contadores de notificaciones para el sidebar, con polling.
 * Solo aplica a roles de salud (PATIENT/PHYSICIAN); para otros roles devuelve
 * {@code null} y no consulta el endpoint.
 */
export function useNotificationCounts(role?: string | null): NotificationCounts | null {
  const [counts, setCounts] = useState<NotificationCounts | null>(null);

  useEffect(() => {
    if (role !== "PATIENT" && role !== "PHYSICIAN") {
      return;
    }
    let cancelled = false;

    const refresh = () => {
      notificationService
        .counts()
        .then((data) => {
          if (!cancelled) setCounts(data);
        })
        .catch(() => {
          // Errores transitorios (p. ej. 403/401): sin badges, sin ruido.
        });
    };

    refresh();
    const interval = setInterval(refresh, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [role]);

  return role === "PATIENT" || role === "PHYSICIAN" ? counts : null;
}
