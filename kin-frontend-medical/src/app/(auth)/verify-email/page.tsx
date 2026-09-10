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
  const pendingReview = searchParams.get("pending") === "1";

  const [email, setEmail] = useState<string>(() => queryEmail ?? getPendingEmail() ?? "");
  const [status, setStatus] = useState<"verifying" | "verified" | "error" | "idle">(
    token ? "verifying" : "idle"
  );
  const [error, setError] = useState("");
  const [resendState, setResendState] = useState<
    "idle" | "sending" | "sent" | "error" | "already_verified"
  >("idle");
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
      // El backend indica el resultado REAL: nunca afirmamos envío si no lo hubo.
      const resendStatus = res.data?.status;
      if (resendStatus === "ALREADY_VERIFIED") {
        setResendState("already_verified");
        return;
      }
      if (resendStatus === "COOLDOWN") {
        setResendState("error");
        setResendError("Ya has solicitado un reenvío recientemente. Intenta de nuevo en unos segundos.");
        return;
      }
      if (resendStatus === "NO_ACCOUNT") {
        setResendState("error");
        setResendError(
          "Si existe una cuenta asociada a este correo y necesita verificación, recibirás un nuevo mensaje.",
        );
        return;
      }
      // SENT (o ausencia de status por compatibilidad): sí hubo envío.
      setResendState("sent");
      setCooldown(60);
    } catch {
      setResendState("error");
      setResendError("No se pudo reenviar el correo. Intenta de nuevo en unos minutos.");
    }
  };

  if (status === "verifying") {
    return (
      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-medical-100 flex items-center justify-center">
            <svg className="animate-spin w-6 h-6 text-medical-600" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
          </div>
          <p className="text-neutral-500">Verificando tu correo electrónico...</p>
        </div>
      </main>
    );
  }

  if (status === "verified") {
    return (
      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-sm flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-medical-100 text-medical-700 flex items-center justify-center">
            <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          {pendingReview ? (
            <>
              <h1 className="text-2xl font-bold">Correo verificado</h1>
              <p className="text-sm text-neutral-600">
                Tu cuenta de médico está <strong>en revisión</strong>. Un
                administrador verificará tu cédula profesional y recibirás un
                correo cuando sea aprobada. Mientras tanto no podrás iniciar
                sesión.
              </p>
            </>
          ) : (
            <>
              <h1 className="text-2xl font-bold">Correo verificado correctamente</h1>
              <p className="text-sm text-neutral-600">
                Tu cuenta está activa. Ya puedes iniciar sesión.
              </p>
            </>
          )}
          <button
            onClick={() => router.push("/login")}
            className="rounded-lg bg-medical-600 py-2 px-6 text-sm font-medium text-white hover:bg-medical-700 transition min-h-11"
          >
            Ir a iniciar sesión
          </button>
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 flex items-center justify-center px-6 py-16">
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
            Para activar tu cuenta necesitas verificar tu correo{" "}
            <span className="font-medium">{email || "asociado"}</span>. Si el correo fue enviado,
            revisa tu bandeja de entrada (y la carpeta de spam) y haz clic en el enlace. Si no lo
            recibiste, puedes solicitar un nuevo enlace.
          </p>
        )}

        <input
          type="email"
          placeholder="Tu correo electrónico"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-medical-500 min-h-11"
        />

        {resendState === "sent" && (
          <p className="text-sm text-green-700 bg-green-50 px-4 py-2 rounded-lg">
            Te hemos enviado un nuevo correo de verificación. Revisa tu bandeja de entrada.
          </p>
        )}

        {resendState === "already_verified" && (
          <p className="text-sm text-emerald-700 bg-emerald-50 px-4 py-2 rounded-lg">
            Tu cuenta ya está verificada. Inicia sesión.
          </p>
        )}

        {resendState === "error" && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2 rounded-lg">{resendError}</p>
        )}

        <button
          onClick={handleResend}
          disabled={cooldown > 0 || resendState === "sending" || !email.trim()}
          className="rounded-lg bg-medical-600 py-2 text-sm font-medium text-white hover:bg-medical-700 transition min-h-11 disabled:opacity-50 disabled:cursor-not-allowed"
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