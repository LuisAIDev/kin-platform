"use client";

import { useState } from "react";
import { physicianService } from "@/services/physician";

/**
 * Modal para que un médico invite a un paciente (por email) a vincularse a su
 * cartera (ciclo de vida de relación V30). La invitación queda PENDING hasta
 * que el paciente la acepte.
 */
export default function InvitePatientModal({
  onClose,
  onInvited,
}: {
  onClose: () => void;
  onInvited: () => void;
}) {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [invitedEmail, setInvitedEmail] = useState("");

  const handleSubmit = async () => {
    const normalized = email.trim();
    if (!normalized) return;
    setSaving(true);
    setError("");
    try {
      const invitation = await physicianService.invitePatient(normalized, message.trim());
      setInvitedEmail(invitation.patientEmail);
      onInvited();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Invitar paciente"
    >
      <div
        className="bg-white rounded-2xl max-w-md w-full p-6 flex flex-col gap-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-lg font-semibold">Invitar paciente</h2>
            <p className="text-sm text-neutral-500 mt-0.5">
              Envía una invitación por correo a un paciente registrado en KIN.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="text-neutral-400 hover:text-neutral-600 text-xl leading-none"
          >
            ×
          </button>
        </div>

        {invitedEmail ? (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
            Invitación enviada a <strong>{invitedEmail}</strong>. El paciente
            debe aceptarla para que la relación quede activa.
          </div>
        ) : (
          <>
            <div className="flex flex-col gap-1.5">
              <label htmlFor="invite-email" className="text-sm font-medium">
                Correo del paciente
              </label>
              <input
                id="invite-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="paciente@correo.com"
                autoComplete="off"
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="invite-message" className="text-sm font-medium">
                Mensaje (opcional)
              </label>
              <textarea
                id="invite-message"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                rows={3}
                maxLength={500}
                placeholder="Un breve mensaje de bienvenida..."
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
              />
            </div>

            {error && (
              <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
            )}

            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-100 transition"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={saving || !email.trim()}
                className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
              >
                {saving ? "Enviando..." : "Enviar invitación"}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
