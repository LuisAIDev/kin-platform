"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { authService } from "@/services/auth";
import { getPendingEmail } from "@/services/session";

function VerifyEmailContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const token = searchParams.get("token");
  const queryEmail = searchParams.get("email");

  const [email, setEmail] = useState<string>(() => queryEmail ?? getPendingEmail() ?? "");
  const [status, setStatus] = useState<"verifying" | "verified" | "error" | "idle">(
    token ? "verifying" : "idle"
  );
  const [error, setError] = useState("");
  const [resendState, setResendState] = useState<"idle" | "sending" | "sent" | "error">("idle");
  const [resendError, setResendError] = useState("");
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (!token || status !== "verifying") return;
    let cancelled = false;

    authService
      .verifyEmail(token)
      .then((res) => {
        if (cancelled) return;
        if (res.error) {
          setStatus("error");
          setError(res.error);
          return;
        }
        setStatus("verified");
      })
      .catch((e) => {
        if (cancelled) return;
        setStatus("error");
        setError((e as Error).message ?? "El enlace de verificación no es válido.");
      });

    return () => {
      cancelled = true;
    };
  }, [token, status]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [cooldown]);

  const handleResend = async () => {
    if (cooldown > 0 || !email.trim()) return;
    setResendState("sending");
    setResendError("");
    try {
      const res = await authService.resendVerification(email.trim());
      if (res.error) {
        setResendState("error");
        setResendError(res.error);
        return;
      }
      setResendState("sent");
      setCooldown(60);
    } catch {
      setResendState("error");
      setResendError("No se pudo reenviar el correo. Intenta de nuevo en unos minutos.");
    }
  };

  if (status === "verifying") {
    return (
      <main className="flex-1 flex items-center justify-center px-6">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <p className="text-neutral-500">Verificando tu correo electrónico...</p>
        </div>
      </main>
    );
  }

  if (status === "verified") {
    return (
      <main className="flex-1 flex items-center justify-center px-6">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-green-100 text-green-700 flex items-center justify-center text-2xl">
            ✓
          </div>
          <h1 className="text-2xl font-bold">Correo verificado correctamente</h1>
          <p className="text-sm text-neutral-600">
            Tu cuenta está activa. Ya puedes iniciar sesión.
          </p>
          <button
            onClick={() => router.push("/login")}
            className="rounded-lg bg-primary-600 py-2 px-6 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11"
          >
            Iniciar sesión
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 flex items-center justify-center px-6">
      <div className="w-full max-w-sm flex flex-col gap-4">
        <h1 className="text-2xl font-bold text-center mb-2">
          Verifica tu correo electrónico
        </h1>

        {status === "error" && (
          <div className="text-sm text-red-600 bg-red-50 px-4 py-3 rounded-lg">
            <p className="font-medium">No pudimos verificar tu correo.</p>
            <p className="mt-1">{error}</p>
            <p className="mt-2 text-neutral-600">
              Puedes solicitar un nuevo enlace de verificación:
            </p>
          </div>
        )}

        {status === "idle" && (
          <p className="text-sm text-neutral-600">
            Te hemos enviado un correo de verificación a{" "}
            <span className="font-medium">{email || "tu correo"}</span>. Revisa tu bandeja de
            entrada y haz clic en el enlace para activar tu cuenta. Si no lo ves, revisa la
            carpeta de spam.
          </p>
        )}

        <input
          type="email"
          placeholder="Tu correo electrónico"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
        />

        {resendState === "sent" && (
          <p className="text-sm text-green-700 bg-green-50 px-4 py-2 rounded-lg">
            Si existe una cuenta asociada a este correo y necesita verificación, recibirás un
            nuevo mensaje.
          </p>
        )}

        {resendState === "error" && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{resendError}</p>
        )}

        <button
          onClick={handleResend}
          disabled={cooldown > 0 || resendState === "sending" || !email.trim()}
          className="rounded-lg bg-primary-600 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {cooldown > 0
            ? `Reenviar en ${cooldown}s`
            : resendState === "sending"
              ? "Enviando..."
              : "Reenviar correo de verificación"}
        </button>

        <p className="text-center text-sm text-neutral-500">
          <Link href="/login" className="text-primary-600 underline hover:text-primary-700">
            Volver al inicio de sesión
          </Link>
        </p>
      </div>
    </main>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense
      fallback={
        <main className="flex-1 flex items-center justify-center">
          <p className="text-neutral-500">Cargando...</p>
        </main>
      }
    >
      <VerifyEmailContent />
    </Suspense>
  );
}
