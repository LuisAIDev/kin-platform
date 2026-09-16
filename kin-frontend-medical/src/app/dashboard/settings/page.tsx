"use client";

import { useEffect, useState } from "react";
import { useSettings } from "@/hooks/useSettings";
import { useToast } from "@/components/ui/ToastProvider";
import { useTheme } from "@/components/providers/ThemeProvider";
import { analytics } from "@/services/analytics";
import { authService } from "@/services/auth";
import { MessageCircle, Save } from "lucide-react";
import type { Language, Theme } from "@/services/settings";

export default function SettingsPage() {
  const { settings, update } = useSettings();
  const { theme, setTheme } = useTheme();
  const { success } = useToast();
  const [saved, setSaved] = useState(false);

  // WhatsApp notifications (solo para médicos)
  const [whatsappEnabled, setWhatsappEnabled] = useState(false);
  const [phone, setPhone] = useState('');
  const [whatsappSaving, setWhatsappSaving] = useState(false);
  const [whatsappLoaded, setWhatsappLoaded] = useState(false);

  // Obtener usuario actual
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const isPhysician = Boolean(user?.physicianCapability === true || user?.role === "PHYSICIAN");

  // Cargar preferencias de WhatsApp del médico
  useEffect(() => {
    if (!isPhysician) return;
    fetch('/api/v1/physician/preferences', { credentials: 'include' })
      .then((r) => r.json())
      .then((data) => {
        setWhatsappEnabled(data.whatsappNotificationsEnabled);
        setPhone(data.phone || '');
        setWhatsappLoaded(true);
      })
      .catch(() => {
        setWhatsappLoaded(true);
      });
  }, [isPhysician]);

  // Guardar preferencias de WhatsApp
  const handleSaveWhatsApp = async () => {
    if (!isPhysician) return;
    setWhatsappSaving(true);
    try {
      const res = await fetch('/api/v1/physician/preferences', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          whatsappNotificationsEnabled: whatsappEnabled,
          phone,
        }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Error al guardar');
      }
      success("Avisos de WhatsApp guardados");
    } catch (err) {
      alert((err as Error).message);
    } finally {
      setWhatsappSaving(false);
    }
  };

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
        {isPhysician && whatsappLoaded && (
          <section className="rounded-xl border border-neutral-200 p-5">
            <div className="flex items-start gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
                <MessageCircle className="w-5 h-5 text-green-600" />
              </div>
              <div>
                <h2 className="font-semibold text-neutral-900">Avisos por WhatsApp</h2>
                <p className="text-sm text-neutral-500 mt-1">
                  Recibe un aviso cuando un paciente te envíe un mensaje por KIN Medical. 
                  El aviso no incluye el contenido del mensaje por seguridad.
                </p>
              </div>
            </div>

            <label className="flex items-center gap-3 py-3 cursor-pointer">
              <input
                type="checkbox"
                checked={whatsappEnabled}
                onChange={(e) => setWhatsappEnabled(e.target.checked)}
                className="w-4 h-4 rounded border-neutral-300 text-medical-600 focus:ring-medical-500"
              />
              <span className="text-sm text-neutral-700">Activar avisos por WhatsApp</span>
            </label>

            {whatsappEnabled && (
              <div className="mt-4">
                <label className="block text-sm font-medium text-neutral-700 mb-2">
                  Número de WhatsApp
                </label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+57 318 619 7995"
                  className="w-full px-4 py-2 rounded-lg border border-neutral-300 focus:ring-2 focus:ring-medical-500 focus:border-transparent"
                />
                <p className="text-xs text-neutral-500 mt-2">
                  Usa formato internacional (ej. +573186197995)
                </p>
              </div>
            )}

            <button
              onClick={handleSaveWhatsApp}
              disabled={whatsappSaving}
              className="mt-4 inline-flex items-center gap-2 rounded-lg bg-medical-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              {whatsappSaving ? 'Guardando...' : 'Guardar avisos WhatsApp'}
            </button>
          </section>
        )}

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
                value={theme}
                onChange={(e) => setTheme(e.target.value as Theme)}
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
