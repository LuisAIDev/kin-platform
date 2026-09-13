"use client";

import { ShieldCheck } from "lucide-react";

interface TriageDisclaimerProps {
  variant?: "ui" | "pdf";
}

export function TriageDisclaimer({ variant = "ui" }: TriageDisclaimerProps) {
  const content = (
    <>
      <p className="font-semibold mb-2">Triaje procesado por KIN Medical.</p>
      <p className="mb-2">
        Las decisiones cl\u00EDnicas las toma un motor de reglas fijas basado en gu\u00EDas
        m\u00E9dicas verificables (CIE-10, rangos de referencia est\u00E1ndar y protocolos
        cl\u00EDnicos). Siempre aplica las mismas reglas, sin improvisaciones. La
        inteligencia artificial solo redacta el resultado en lenguaje sencillo.
      </p>
      <p>
        Este triaje es de apoyo informativo y no sustituye la evaluaci\u00F3n ni el
        diagn\u00F3stico de un profesional de la salud.
      </p>
    </>
  );

  if (variant === "pdf") {
    return (
      <div className="text-sm text-neutral-700 leading-relaxed">{content}</div>
    );
  }

  return (
    <div className="rounded-lg bg-medical-50 border border-medical-200 p-4 mb-6">
      <div className="flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-medical-600 flex-shrink-0 mt-0.5" />
        <div className="text-sm text-medical-900 leading-relaxed">{content}</div>
      </div>
    </div>
  );
}