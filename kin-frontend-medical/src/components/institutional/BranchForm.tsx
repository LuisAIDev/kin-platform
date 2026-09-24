"use client";

import { useState } from "react";

export function BranchForm({ onCreated }: { onCreated?: () => void }) {
  const [form, setForm] = useState({ name: "", address: "", phone: "", city: "", services: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const input = "w-full rounded-lg border border-neutral-300 px-3 py-2 text-neutral-900 focus:border-medical-500 focus:ring-2 focus:ring-medical-100";

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const servicesEnabled = form.services
        ? JSON.stringify(form.services.split(",").map((s) => s.trim()).filter(Boolean))
        : null;
      const res = await fetch("/api/v1/institutional/branches", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: form.name,
          address: form.address || null,
          phone: form.phone || null,
          city: form.city || null,
          servicesEnabled,
        }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data?.error || "No se pudo crear la sede");
      }
      setForm({ name: "", address: "", phone: "", city: "", services: "" });
      onCreated?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-3 rounded-xl border border-neutral-200 bg-white p-4">
      <h3 className="font-semibold text-neutral-900">Nueva sede</h3>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <input className={input} placeholder="Nombre *" required value={form.name}
        onChange={(e) => setForm({ ...form, name: e.target.value })} />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <input className={input} placeholder="Ciudad" value={form.city}
          onChange={(e) => setForm({ ...form, city: e.target.value })} />
        <input className={input} placeholder="Teléfono" value={form.phone}
          onChange={(e) => setForm({ ...form, phone: e.target.value })} />
      </div>
      <input className={input} placeholder="Dirección" value={form.address}
        onChange={(e) => setForm({ ...form, address: e.target.value })} />
      <input className={input} placeholder="Servicios (separados por coma)" value={form.services}
        onChange={(e) => setForm({ ...form, services: e.target.value })} />
      <button type="submit" disabled={saving}
        className="rounded-lg bg-medical-600 px-4 py-2 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-60">
        {saving ? "Guardando…" : "Crear sede"}
      </button>
    </form>
  );
}
