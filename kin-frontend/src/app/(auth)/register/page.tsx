"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";
import { PasswordInput } from "@/components/auth/PasswordInput";

export default function RegisterPage() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!acceptedTerms) {
      setError("Debe aceptar los Términos y Condiciones y la Política de Privacidad");
      return;
    }

    const result = await authService.register({ fullName, email, password });
    if (result.error) {
      setError(result.error);
      return;
    }

    router.push(`/verify-email?email=${encodeURIComponent(email)}`);
  };

  return (
    <main className="flex-1 flex items-center justify-center px-6">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm flex flex-col gap-4"
      >
        <h1 className="text-2xl font-bold text-center mb-2">
          Crear cuenta
        </h1>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">
            {error}
          </p>
        )}

        <input
          type="text"
          placeholder="Nombre completo"
          value={fullName}
          onChange={(e) => setFullName(e.target.value)}
          required
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
        />

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
        />

        <PasswordInput
          value={password}
          onChange={setPassword}
          placeholder="Contraseña (mín. 8 caracteres)"
          minLength={8}
          autoComplete="new-password"
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 w-full pr-10"
        />

        <label className="flex items-start gap-2 text-sm text-neutral-600">
          <input
            type="checkbox"
            checked={acceptedTerms}
            onChange={(e) => setAcceptedTerms(e.target.checked)}
            required
            className="mt-1 h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-500"
          />
          <span className="leading-relaxed">
            Acepto los{" "}
            <Link href="/terminos" className="text-primary-600 underline hover:text-primary-700">
              Términos y Condiciones
            </Link>{" "}
            y la{" "}
            <Link href="/privacidad" className="text-primary-600 underline hover:text-primary-700">
              Política de Privacidad
            </Link>
          </span>
        </label>

        <button
          type="submit"
          disabled={!acceptedTerms}
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Crear cuenta
        </button>

        <p className="text-center text-sm text-neutral-500">
          ¿Ya tienes cuenta?{" "}
          <Link href="/login" className="text-primary-600 underline hover:text-primary-700">
            Inicia sesión
          </Link>
        </p>
      </form>
    </main>
  );
}
