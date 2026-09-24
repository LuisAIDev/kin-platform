"use client";

import { useEffect, useState } from "react";

export type Branch = {
  id: string;
  name: string;
  address?: string | null;
  phone?: string | null;
  city?: string | null;
  servicesEnabled?: string | null;
};

export function BranchList({ refreshKey = 0 }: { refreshKey?: number }) {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    try {
      const res = await fetch("/api/v1/institutional/branches", { credentials: "include" });
      if (!res.ok) throw new Error("No se pudieron cargar las sedes");
      setBranches(await res.json());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error");
    } finally {
      setLoading(false);
    }
  }

  async function remove(id: string) {
    if (!confirm("¿Eliminar esta sede?")) return;
    const res = await fetch(`/api/v1/institutional/branches/${id}`, {
      method: "DELETE",
      credentials: "include",
    });
    if (res.ok) load();
  }

  useEffect(() => {
    load();
  }, [refreshKey]);

  if (loading) return <p className="text-sm text-neutral-500">Cargando sedes…</p>;
  if (error) return <p className="text-sm text-red-600">{error}</p>;
  if (branches.length === 0) return <p className="text-sm text-neutral-500">Sin sedes registradas.</p>;

  return (
    <ul className="divide-y divide-neutral-200 rounded-xl border border-neutral-200 bg-white">
      {branches.map((b) => (
        <li key={b.id} className="flex items-center justify-between p-4">
          <div>
            <p className="font-medium text-neutral-900">{b.name}</p>
            <p className="text-sm text-neutral-500">
              {[b.city, b.address, b.phone].filter(Boolean).join(" · ") || "—"}
            </p>
          </div>
          <button onClick={() => remove(b.id)} className="text-sm text-red-600 hover:text-red-700">
            Eliminar
          </button>
        </li>
      ))}
    </ul>
  );
}
