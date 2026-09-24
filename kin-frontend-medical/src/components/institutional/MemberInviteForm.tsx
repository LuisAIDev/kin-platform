"use client";

import { useEffect, useState } from "react";

const ROLES = ["IPS_ADMIN", "IPS_MEDICO", "IPS_ENFERMERA", "IPS_FACTURADOR", "IPS_AUDITOR"];

export function MemberInviteForm({ onInvited }: { onInvited?: () => void }) {
  const [form, setForm] = useState({ userId: "", role: "IPS_MEDICO", branchId: "" });
  const [branches, setBranches] = useState<{ id: string; name: string }[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const input = "w-full rounded-lg border border-neutral-300 px-3 py-2 text-neutral-900 focus:border-medical-500 focus:ring-2 focus:ring-medical-100";

  useEffect(() => {
    fetch("/api/v1/institutional/branches", { credentials: "include" })
      .then((r) => (r.ok ? r.json() : []))
      .then(setBranches)
      .catch(() => setBranches([]));
  }, []);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const res = await fetch("/api/v1/institutional/members", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: form.userId,
          role: form.role,
          branchId: form.branchId || null,
        }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data?.error || "No se pudo invitar al miembro");
      }
      setForm({ userId: "", role: "IPS_MEDICO", branchId: "" });
      onInvited?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-3 rounded-xl border border-neutral-200 bg-white p-4">
      <h3 className="font-semibold text-neutral-900">Invitar miembro</h3>
      {error && <p className="text-sm text-red-600">{error}</p>}
      <input className={input} placeholder="ID de usuario (UUID) *" required value={form.userId}
        onChange={(e) => setForm({ ...form, userId: e.target.value })} />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <select className={input} value={form.role}
          onChange={(e) => setForm({ ...form, role: e.target.value })}>
          {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
        <select className={input} value={form.branchId}
          onChange={(e) => setForm({ ...form, branchId: e.target.value })}>
          <option value="">Sin sede</option>
          {branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
        </select>
      </div>
      <button type="submit" disabled={saving}
        className="rounded-lg bg-medical-600 px-4 py-2 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-60">
        {saving ? "Enviando…" : "Invitar"}
      </button>
    </form>
  );
}
