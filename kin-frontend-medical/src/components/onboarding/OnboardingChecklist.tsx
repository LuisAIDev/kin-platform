"use client";

import Link from "next/link";
import { ONBOARDING_ITEMS, useOnboarding } from "@/hooks/useOnboarding";

export default function OnboardingChecklist() {
  const { dismissed, completed, markDone, dismiss, doneCount, isComplete } = useOnboarding();

  if (dismissed || isComplete) return null;

  return (
    <section aria-label="Checklist de bienvenida" className="mx-6 mt-6 rounded-2xl border border-primary-200 bg-gradient-to-b from-primary-50 to-white p-5 sm:p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-primary-900">Bienvenido a KIN 🎉</h2>
          <p className="mt-1 text-sm text-neutral-600">
            Sigue estos pasos para aprovechar al máximo la plataforma
            ({doneCount}/{ONBOARDING_ITEMS.length} completados).
          </p>
        </div>
        <button
          onClick={dismiss}
          aria-label="Cerrar bienvenida"
          className="flex h-8 w-8 items-center justify-center rounded-lg text-neutral-400 hover:bg-neutral-100 hover:text-neutral-600 transition"
        >
          ✕
        </button>
      </div>

      <ul className="mt-4 space-y-2">
        {ONBOARDING_ITEMS.map((item) => {
          const done = completed.includes(item.key);
          return (
            <li key={item.key}>
              <Link
                href={item.href}
                onClick={() => markDone(item.key)}
                className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition ${
                  done
                    ? "text-neutral-400 line-through"
                    : "text-neutral-700 hover:bg-primary-50"
                }`}
              >
                <span
                  aria-hidden="true"
                  className={`flex h-5 w-5 items-center justify-center rounded-full border text-xs ${
                    done ? "border-emerald-500 bg-emerald-500 text-white" : "border-neutral-300"
                  }`}
                >
                  {done ? "✓" : ""}
                </span>
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
