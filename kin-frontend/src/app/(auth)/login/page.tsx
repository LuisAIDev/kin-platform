"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { authService } from "@/services/auth";
import { checkForceLogout, setPendingEmail, storeSession } from "@/services/session";
import { PasswordInput } from "@/components/auth/PasswordInput";
import { homePathForRole } from "@/utils/roles";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const verticalSalud = searchParams.get("vertical") === "salud";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [checking, setChecking] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (checkForceLogout()) {
      localStorage.clear();
      sessionStorage.clear();
    }

    // Con cookie HttpOnly, verificamos la sesión con un fetch raw (un 401 aquí
    // es esperado si no hay sesión: NO debe disparar forceLogout ni recargar).
    authService
      .fetchCurrentUser()
      .then((me) => {
        if (me) {
          storeSession(me);
          router.push(homePathForRole(me.role));
        }
      })
      .finally(() => setChecking(false));
  }, [router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const result = await authService.login({ email, password });
      if (result.error) {
        if (result.code === "EMAIL_VERIFICATION_REQUIRED") {
          setPendingEmail(email);
          router.push(`/verify-email?email=${encodeURIComponent(email)}`);
          return;
        }
        if (result.code === "RATE_LIMITED") {
          setError("Demasiados intentos de inicio de sesión. Espera 60 segundos antes de intentar de nuevo.");
          return;
        }
        if (
          result.error === "Unauthorized" ||
          result.error === "Invalid email or password" ||
          result.error === "Bad credentials"
        ) {
          setError("Correo o contraseña incorrectos.");
        } else {
          setError(result.error);
        }
        return;
      }
      router.push(homePathForRole(result.data?.role));
    } catch (err) {
      // Defensivo: authService.login nunca lanza (devuelve {error}), pero si
      // algo inesperado ocurre, mostrarlo en vez de dejar el botón "mudo".
      console.error("Error inesperado al iniciar sesión", err);
      setError("No se pudo iniciar sesión. Inténtalo de nuevo.");
    } finally {
      setLoading(false);
    }
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

        {verticalSalud && (
          <p className="text-sm text-emerald-700 bg-emerald-50 border border-emerald-200 px-4 py-2 rounded-lg">
            Acceso al portal de salud. Las cuentas de pacientes y médicos las
            crea tu institución o el administrador del piloto.
          </p>
        )}

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

        <PasswordInput
          value={password}
          onChange={setPassword}
          placeholder="Contraseña"
          autoComplete="current-password"
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 w-full pr-10"
        />

        <div className="flex justify-end -mt-1">
          <Link
            href="/forgot-password"
            className="text-xs text-primary-600 underline hover:text-primary-700"
          >
            ¿Olvidaste tu contraseña? Recupérala aquí
          </Link>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading && (
            <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          )}
          {loading ? "Cargando..." : "Entrar"}
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

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
