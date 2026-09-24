"use client";

import { useState } from "react";
import { BranchForm } from "../../../../components/institutional/BranchForm";
import { BranchList } from "../../../../components/institutional/BranchList";

export default function BranchesPage() {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <main className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold tracking-tight text-neutral-900">Sedes</h1>
      <p className="mt-1 text-sm text-neutral-600">Gestiona las sedes de tu institución.</p>
      <div className="mt-6 space-y-6">
        <BranchForm onCreated={() => setRefreshKey((k) => k + 1)} />
        <BranchList refreshKey={refreshKey} />
      </div>
    </main>
  );
}
