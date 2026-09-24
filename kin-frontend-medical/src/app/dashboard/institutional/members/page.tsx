"use client";

import { useState } from "react";
import { MemberInviteForm } from "../../../../components/institutional/MemberInviteForm";
import { MemberList } from "../../../../components/institutional/MemberList";

export default function MembersPage() {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <main className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold tracking-tight text-neutral-900">Equipo médico</h1>
      <p className="mt-1 text-sm text-neutral-600">Invita y administra los roles de tu equipo.</p>
      <div className="mt-6 space-y-6">
        <MemberInviteForm onInvited={() => setRefreshKey((k) => k + 1)} />
        <MemberList refreshKey={refreshKey} />
      </div>
    </main>
  );
}
