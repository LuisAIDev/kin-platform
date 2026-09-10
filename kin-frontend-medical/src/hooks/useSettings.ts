"use client";

import { useCallback, useState } from "react";
import { settingsService, type UserSettings } from "@/services/settings";

export function useSettings() {
  const [settings, setSettings] = useState<UserSettings>(() => settingsService.load());

  const update = useCallback((patch: Partial<UserSettings>) => {
    setSettings((prev) => {
      const next = { ...prev, ...patch };
      settingsService.save(next);
      return next;
    });
  }, []);

  return { settings, update };
}
