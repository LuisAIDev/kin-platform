"use client";

import { useEffect, useState } from "react";
import { useSettings } from "@/hooks/useSettings";
import { useToast } from "@/components/ui/ToastProvider";
import { analytics } from "@/services/analytics";
import type { Language, Theme } from "@/services/settings";

export default function SettingsPage() {
  const { settings, update } = useSettings();
  const { success } = useToast();
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!saved) return;
    const timer = setTimeout(() => setSaved(false), 2000);
    return () => clearTimeout(timer);
  }, [saved]);

  const handleSave = () => {
    update({ ...settings });
    setSaved(true);
    success("Preferencias guardadas");
    analytics.track("settings_saved", { theme: settings.theme, language: settings.language });
  };

  return (
    <main className="flex-1 px-6 py-8 max-w-3xl mx-auto w-full">
      <h1 className="text-2xl font-bold tracking-tight">Configuración</h1>
      <p className="mt-1 text-sm text-neutral-500">Personaliza tu experiencia en KIN.</p>

      <div className="mt-8 space-y-6">
        <section className="rounded-xl border border-neutral-200 p-5">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-neutral-500">Perfil</h2>
          <label className="mt-3 block">
            <span className="text-sm font-medium text-neutral-700">Nombre a mostrar</span>
            <input
              type="text"
              value={settings.displayName}
              onChange={(e) => update({ displayName: e.target.value })}
              className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none"
              placeholder="Tu nombre"
            />
          </label>
        </section>

        <section className="rounded-xl border border-neutral-200 p-5">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-neutral-500">Preferencias</h2>
          <div className="mt-3 grid gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="text-sm font-medium text-neutral-700">Tema</span>
              <select
                value={settings.theme}
                onChange={(e) => update({ theme: e.target.value as Theme })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm"
              >
                <option value="system">Sistema</option>
                <option value="light">Claro</option>
                <option value="dark">Oscuro</option>
              </select>
            </label>
            <label className="block">
              <span className="text-sm font-medium text-neutral-700">Idioma</span>
              <select
                value={settings.language}
                onChange={(e) => update({ language: e.target.value as Language })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm"
              >
                <option value="es">Español</option>
                <option value="en">English</option>
              </select>
            </label>
          </div>
        </section>

        <section className="rounded-xl border border-neutral-200 p-5">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-neutral-500">Asistente IA</h2>
          <div className="mt-3 grid gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="text-sm font-medium text-neutral-700">Nivel de IA</span>
              <select
                value={settings.aiLevel}
                onChange={(e) => update({ aiLevel: e.target.value as "FLASH" | "PRO" })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm"
              >
                <option value="FLASH">Flash</option>
                <option value="PRO">Pro</option>
              </select>
            </label>
            <label className="block">
              <span className="text-sm font-medium text-neutral-700">Proveedor preferido</span>
              <select
                value={settings.aiProvider}
                onChange={(e) => update({ aiProvider: e.target.value as "auto" | "deepseek" | "openai" })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm"
              >
                <option value="auto">Automático</option>
                <option value="deepseek">DeepSeek</option>
                <option value="openai">OpenAI</option>
              </select>
            </label>
            <label className="block">
              <span className="text-sm font-medium text-neutral-700">Longitud de respuesta</span>
              <select
                value={settings.aiLength}
                onChange={(e) => update({ aiLength: e.target.value as "short" | "balanced" | "long" })}
                className="mt-1 block w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm"
              >
                <option value="short">Breve</option>
                <option value="balanced">Equilibrada</option>
                <option value="long">Extensa</option>
              </select>
            </label>
            <label className="block">
              <span className="text-sm font-medium text-neutral-700">
                Creatividad ({settings.temperature.toFixed(1)})
              </span>
              <input
                type="range"
                min={0}
                max={1}
                step={0.1}
                value={settings.temperature}
                onChange={(e) => update({ temperature: Number(e.target.value) })}
                className="mt-3 w-full"
              />
            </label>
          </div>
          <label className="mt-4 flex items-center gap-2 text-sm text-neutral-700">
            <input
              type="checkbox"
              checked={settings.notificationsEnabled}
              onChange={(e) => update({ notificationsEnabled: e.target.checked })}
              className="h-4 w-4 rounded border-neutral-300"
            />
            Recibir notificaciones
          </label>
        </section>

        <div className="flex items-center gap-3">
          <button
            onClick={handleSave}
            className="rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none min-h-11"
          >
            Guardar preferencias
          </button>
          {saved && (
            <span role="status" className="text-sm text-emerald-600">
              Guardado ✓
            </span>
          )}
        </div>
      </div>
    </main>
  );
}
