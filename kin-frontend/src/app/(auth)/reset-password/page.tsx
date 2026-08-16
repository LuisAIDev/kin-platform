"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { api } from "@/services/api";
import { PasswordInput } from "@/components/auth/PasswordInput";

function ResetPasswordForm() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    if (newPassword !== confirmPassword) {
      setError("Las contraseñas no coinciden.");
      return;
    }
    if (newPassword.length < 8) {
      setError("La contraseña debe tener al menos 8 caracteres.");
      return;
    }
    if (!token) {
      setError("El enlace de recuperación no es válido.");
      return;
    }
    setLoading(true);
    try {
      const res = await api.post<{ message: string }>("/auth/reset-password", {
        token,
        newPassword,
      });
      setMessage(res.message);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  if (message) {
    return (
      <div className="w-full max-w-sm flex flex-col gap-4 text-center">
        <h1 className="text-2xl font-bold">Contraseña actualizada</h1>
        <p className="text-sm text-emerald-600 bg-emerald-50 px-4 py-2 rounded-lg">{message}</p>
        <Link
          href="/login"
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition text-center"
        >
          Volver a iniciar sesión
        </Link>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-sm flex flex-col gap-4">
      <h1 className="text-2xl font-bold text-center mb-2">Nueva contraseña</h1>

      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{error}</p>
      )}

      <PasswordInput
        value={newPassword}
        onChange={setNewPassword}
        placeholder="Nueva contraseña (mín. 8 caracteres)"
        minLength={8}
        autoComplete="new-password"
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 w-full pr-10"
      />

      <PasswordInput
        value={confirmPassword}
        onChange={setConfirmPassword}
        placeholder="Confirmar nueva contraseña"
        minLength={8}
        autoComplete="new-password"
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 w-full pr-10"
      />

      <button
        type="submit"
        disabled={loading}
        className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50"
      >
        {loading ? "Guardando..." : "Actualizar contraseña"}
      </button>

      <p className="text-center text-sm text-neutral-500">
        <Link href="/login" className="text-primary-600 underline hover:text-primary-700">
          Volver a iniciar sesión
        </Link>
      </p>
    </form>
  );
}

export default function ResetPasswordPage() {
  return (
    <main className="flex-1 flex items-center justify-center px-6">
      <Suspense
        fallback={
          <p className="text-neutral-500 text-center">Cargando recuperación de contraseña...</p>
        }
      >
        <ResetPasswordForm />
      </Suspense>
    </main>
  );
}
