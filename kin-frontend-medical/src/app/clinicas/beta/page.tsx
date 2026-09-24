"use client";

import { useState } from "react";
import Link from "next/link";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

const EMPTY_FORM = {
  ipsName: "",
  nit: "",
  city: "",
  contactName: "",
  email: "",
  phone: "",
  beds: "",
  comments: "",
};

type FormFields = typeof EMPTY_FORM;

export default function BetaAccessPage() {
  const [form, setForm] = useState<FormFields>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function update(field: keyof FormFields) {
    return (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm((prev) => ({ ...prev, [field]: event.target.value }));
  }

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await fetch(`${API_URL}/institutional/inquiries`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...form,
          beds: form.beds ? Number(form.beds) : null,
        }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(data?.error || "No se pudo enviar la solicitud. Intenta de nuevo.");
      }
      setSuccess(
        data?.message ??
          "Hemos recibido tu solicitud. Te contactaremos en 48h para agendar una demo.",
      );
      setForm(EMPTY_FORM);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error inesperado al enviar la solicitud.");
    } finally {
      setSubmitting(false);
    }
  }

  const inputClass =
    "w-full rounded-lg border border-neutral-300 px-3 py-2 text-neutral-900 outline-none focus:border-medical-500 focus:ring-2 focus:ring-medical-100";

  return (
    <main className="mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:px-8">
      <Link href="/" className="text-sm font-medium text-medical-600 hover:text-medical-700">
        ← Volver
      </Link>

      <span className="mt-6 inline-flex items-center rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700 ring-1 ring-emerald-200">
        Beta Cerrada
      </span>
      <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">
        Solicitar Acceso Beta para clínicas y hospitales
      </h1>
      <p className="mt-4 text-lg text-neutral-600">
        Estamos seleccionando IPS para el programa beta. Completa el formulario y te contactaremos
        en 48h para agendar una demo.
      </p>

      {success && (
        <div className="mt-6 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-emerald-800">
          {success}
        </div>
      )}
      {error && (
        <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">{error}</div>
      )}

      {!success && (
        <form onSubmit={onSubmit} className="mt-8 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="ipsName">
              Nombre de la IPS *
            </label>
            <input id="ipsName" className={inputClass} value={form.ipsName} onChange={update("ipsName")} required />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="nit">
                NIT *
              </label>
              <input id="nit" className={inputClass} value={form.nit} onChange={update("nit")} required />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="city">
                Ciudad
              </label>
              <input id="city" className={inputClass} value={form.city} onChange={update("city")} />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="contactName">
                Nombre de contacto *
              </label>
              <input
                id="contactName"
                className={inputClass}
                value={form.contactName}
                onChange={update("contactName")}
                required
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="email">
                Email *
              </label>
              <input
                id="email"
                type="email"
                className={inputClass}
                value={form.email}
                onChange={update("email")}
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="phone">
                Teléfono
              </label>
              <input id="phone" className={inputClass} value={form.phone} onChange={update("phone")} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="beds">
                Número de camas
              </label>
              <input
                id="beds"
                type="number"
                min="0"
                className={inputClass}
                value={form.beds}
                onChange={update("beds")}
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-neutral-700" htmlFor="comments">
              Comentarios
            </label>
            <textarea
              id="comments"
              rows={4}
              className={inputClass}
              value={form.comments}
              onChange={update("comments")}
            />
          </div>

          <button type="submit" disabled={submitting} className="btn-primary w-full px-6 py-3 disabled:opacity-60">
            {submitting ? "Enviando..." : "Solicitar Acceso Beta"}
          </button>
          <p className="text-xs text-neutral-500">
            Usaremos tus datos únicamente para contactarte sobre el programa beta.
          </p>
        </form>
      )}
    </main>
  );
}
