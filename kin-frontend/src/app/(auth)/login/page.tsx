"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";
import { api } from "@/services/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [checking, setChecking] = useState(() => {
    if (typeof window === "undefined") return true;
    return !!authService.getToken();
  });

  useEffect(() => {
    if (!checking) return;

    api.get("/auth/me")
      .then(() => router.push("/dashboard/projects"))
      .catch(() => {
        authService.logout();
        setChecking(false);
      });
  }, [checking, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const result = await authService.login({ email, password });
    if (result.error) {
      setError(result.error);
      return;
    }

    router.push("/dashboard/projects");
  };

  if (checking) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-500">Verificando sesion...</p>
      </main>
    );
  }

  return (
    <main className="flex-1 flex items-center justify-center px-6">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm flex flex-col gap-4"
      >
        <h1 className="text-2xl font-bold text-center mb-2">Iniciar sesion</h1>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">
            {error}
          </p>
        )}

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
        />

        <input
          type="password"
          placeholder="Contrasena"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
        />

        <button
          type="submit"
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11"
        >
          Entrar
        </button>

        <p className="text-center text-sm text-neutral-500">
          No tienes cuenta?{" "}
          <Link href="/register" className="text-primary-600 underline hover:text-primary-700">
            Registrate
          </Link>
        </p>
      </form>
    </main>
  );
}
