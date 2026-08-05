"use client";

import { useCallback, useEffect, useState } from "react";

export type OnboardingItem = {
  key: string;
  label: string;
  href: string;
};

export const ONBOARDING_ITEMS: OnboardingItem[] = [
  { key: "create-project", label: "Crear tu primer proyecto", href: "/dashboard/projects/new" },
  { key: "chat-ai", label: "Conversar con la IA", href: "/dashboard/projects" },
  { key: "explore-enterprise", label: "Explorar el módulo Enterprise", href: "/dashboard/projects" },
];

const STORAGE_KEY = "kin_onboarding_v1";

function readCompleted(): string[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    const parsed = raw ? (JSON.parse(raw) as string[]) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function useOnboarding() {
  const [dismissed, setDismissed] = useState(false);
  const [completed, setCompleted] = useState<string[]>(() => readCompleted());

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(completed));
    } catch {
      // almacenamiento no disponible
    }
  }, [completed]);

  const markDone = useCallback((key: string) => {
    setCompleted((prev) => (prev.includes(key) ? prev : [...prev, key]));
  }, []);

  const dismiss = useCallback(() => setDismissed(true), []);
  const reset = useCallback(() => {
    setCompleted([]);
    setDismissed(false);
  }, []);

  const doneCount = ONBOARDING_ITEMS.filter((item) => completed.includes(item.key)).length;
  const isComplete = doneCount === ONBOARDING_ITEMS.length;

  return { dismissed, completed, markDone, dismiss, reset, doneCount, isComplete };
}
