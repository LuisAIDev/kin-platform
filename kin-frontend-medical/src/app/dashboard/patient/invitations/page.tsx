"use client";

import FeedbackButton from "@/components/health/FeedbackButton";
import InvitationsList from "@/components/health/InvitationsList";

export default function PatientInvitationsPage() {
  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-3xl flex flex-col gap-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Invitaciones</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Médicos que te han invitado a vincularte. Acepta para activar la
              comunicación y las citas.
            </p>
          </div>
          <FeedbackButton label="Dar feedback" />
        </div>

        <InvitationsList />
      </div>
    </main>
  );
}
