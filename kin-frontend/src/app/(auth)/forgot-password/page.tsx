"use client";

import { useState } from "react";
import Link from "next/link";
import { api } from "@/services/api";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    setLoading(true);
    try {
      const res = await api.post<{ message: string }>("/auth/forgot-password", { email });
      setMessage(res.message);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="flex-1 flex items-center justify-center px-6">
      <form onSubmit={handleSubmit} className="w-full max-w-sm flex flex-col gap-4">
        <h1 className="text-2xl font-bold text-center mb-2">Recuperar contraseña</h1>
        <p className="text-center text-sm text-neutral-500">
          Ingresa el correo de tu cuenta y te enviaremos un enlace para restablecer tu contraseña.
        </p>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{error}</p>
        )}
        {message && (
          <p className="text-sm text-emerald-600 bg-emerald-50 px-4 py-2 rounded-lg">{message}</p>
        )}

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
        />

        <button
          type="submit"
          disabled={loading}
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50"
        >
          {loading ? "Enviando..." : "Enviar enlace de recuperación"}
        </button>

        <p className="text-center text-sm text-neutral-500">
          ¿Recordaste tu contraseña?{" "}
          <Link href="/login" className="text-primary-600 underline hover:text-primary-700">
            Inicia sesión
          </Link>
        </p>
      </form>
    </main>
  );
}
