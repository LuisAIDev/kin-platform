"use client";

import { useEffect, useState } from "react";

type Member = {
  id: string;
  userId: string;
  branchId?: string | null;
  role: string;
  status: string;
};

export function MemberList({ refreshKey = 0 }: { refreshKey?: number }) {
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    try {
      const res = await fetch("/api/v1/institutional/members", { credentials: "include" });
      if (!res.ok) throw new Error("No se pudieron cargar los miembros");
      setMembers(await res.json());
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error");
    } finally {
      setLoading(false);
    }
  }

  async function accept(id: string) {
    await fetch(`/api/v1/institutional/members/${id}/accept`, { method: "POST", credentials: "include" });
    load();
  }

  async function remove(id: string) {
    if (!confirm("¿Remover este miembro?")) return;
    await fetch(`/api/v1/institutional/members/${id}`, { method: "DELETE", credentials: "include" });
    load();
  }

  useEffect(() => {
    load();
  }, [refreshKey]);

  if (loading) return <p className="text-sm text-neutral-500">Cargando miembros…</p>;
  if (error) return <p className="text-sm text-red-600">{error}</p>;
  if (members.length === 0) return <p className="text-sm text-neutral-500">Sin miembros registrados.</p>;

  return (
    <ul className="divide-y divide-neutral-200 rounded-xl border border-neutral-200 bg-white">
      {members.map((m) => (
        <li key={m.id} className="flex items-center justify-between gap-4 p-4">
          <div>
            <p className="font-medium text-neutral-900">{m.role}</p>
            <p className="text-xs text-neutral-500">
              {m.status} · {m.branchId ? `sede ${m.branchId}` : "sin sede"}
            </p>
          </div>
          <div className="flex gap-3">
            {m.status === "INVITED" && (
              <button onClick={() => accept(m.id)} className="text-sm text-medical-600 hover:text-medical-700">
                Aceptar
              </button>
            )}
            <button onClick={() => remove(m.id)} className="text-sm text-red-600 hover:text-red-700">
              Remover
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
