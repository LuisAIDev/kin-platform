'use client';

import { useState, useEffect } from 'react';
import { MessageCircle, Save, Loader2, Check } from 'lucide-react';

export default function PhysicianSettingsPage() {
  const [whatsappEnabled, setWhatsappEnabled] = useState(false);
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetch('/api/v1/physician/preferences', { credentials: 'include' })
      .then((r) => {
        if (!r.ok) throw new Error('No autorizado');
        return r.json();
      })
      .then((data) => {
        setWhatsappEnabled(data.whatsappNotificationsEnabled);
        setPhone(data.phone || '');
        setLoading(false);
      })
      .catch(() => {
        setLoading(false);
      });
  }, []);

  async function handleSave() {
    setSaving(true);
    setSaved(false);
    setError('');
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
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="min-h-screen flex items-center justify-center">Cargando...</div>;

  return (
    <div className="min-h-screen bg-neutral-50 py-12 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-neutral-900">Configuración</h1>
          <p className="text-neutral-500 mt-1">Personaliza tu experiencia en KIN Medical.</p>
        </div>

        <div className="bg-white rounded-2xl border border-neutral-200 p-6 shadow-sm">
          <div className="flex items-start gap-3 mb-4">
            <div className="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
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
              className="w-5 h-5 rounded border-neutral-300 text-medical-600 focus:ring-medical-500"
            />
            <span className="text-sm text-neutral-700">Activar avisos por WhatsApp</span>
          </label>

          {whatsappEnabled && (
            <div className="mt-4">
              {error && (
                <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
                  {error}
                </div>
              )}
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
            onClick={handleSave}
            disabled={saving}
            className="mt-6 inline-flex items-center gap-2 rounded-lg bg-medical-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-50 transition"
          >
            {saving ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : saved ? (
              <>
                <Check className="w-4 h-4" />
                ¡Guardado!
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                Guardar cambios
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}