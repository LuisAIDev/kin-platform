"use client";

import Link from "next/link";

const CARDS = [
  { href: "/dashboard/institutional/branches", title: "Sedes", desc: "Gestiona las sedes de la IPS." },
  { href: "/dashboard/institutional/members", title: "Equipo médico", desc: "Invita y administra roles." },
  { href: "/dashboard/institutional/kpis", title: "KPIs", desc: "Facturación, cartera y glosas." },
];

export function InstitutionalDashboard() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {CARDS.map((c) => (
        <Link key={c.href} href={c.href}
          className="rounded-xl border border-neutral-200 bg-white p-5 transition hover:border-medical-300 hover:shadow-sm">
          <p className="text-lg font-semibold text-neutral-900">{c.title}</p>
          <p className="mt-1 text-sm text-neutral-600">{c.desc}</p>
        </Link>
      ))}
    </div>
  );
}
