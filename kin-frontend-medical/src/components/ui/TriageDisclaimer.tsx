"use client";

import { ShieldCheck } from "lucide-react";

interface TriageDisclaimerProps {
  variant?: "ui" | "pdf";
}

export function TriageDisclaimer({ variant = "ui" }: TriageDisclaimerProps) {
  if (variant === "pdf") {
    const contentPdf = (
      <>
        <p className="font-semibold mb-2">Triaje procesado por KIN Medical.</p>
        <p className="mb-2">
          Las decisiones clinicas las toma un motor de reglas fijas que aplica guias
          medicas verificables. Trabajamos con datos reales de fuentes publicas y
          confiables: la Organizacion Mundial de la Salud (OMS), clasificaciones
          internacionales de enfermedades (CIE-10), rangos de referencia de
          laboratorios y protocolos clinicos.
        </p>
        <p className="mb-2">
          Que significa esto? Que cada vez que analizamos tus sintomas, aplicamos
          las mismas reglas medicas validadas, sin improvisaciones ni variaciones.
          La inteligencia artificial solo se encarga de redactar el resultado en
          palabras sencillas para ti.
        </p>
        <p>
          Este triaje es de apoyo informativo y no sustituye la evaluacion ni el
          diagnostico de un profesional de la salud. Siempre consulta a tu medico.
        </p>
      </>
    );
    return (
      <div className="text-sm text-neutral-700 leading-relaxed">{contentPdf}</div>
    );
  }

  const contentUi = (
    <>
      <p className="font-semibold mb-2">Triaje procesado por KIN Medical.</p>
      <p className="mb-2">
        Las decisiones clínicas las toma un motor de reglas fijas que aplica
        guías médicas verificables. Trabajamos con datos reales de fuentes
        públicas y confiables: la Organización Mundial de la Salud (OMS),
        clasificaciones internacionales de enfermedades (CIE-10), rangos de
        referencia de laboratorios y protocolos clínicos.
      </p>
      <p className="mb-2">
        ¿Qué significa esto? Que cada vez que analizamos tus síntomas, aplicamos
        las mismas reglas médicas validadas, sin improvisaciones ni variaciones.
        La inteligencia artificial solo se encarga de redactar el resultado en
        palabras sencillas para ti.
      </p>
      <p>
        Este triaje es de apoyo informativo y no sustituye la evaluación ni el
        diagnóstico de un profesional de la salud. Siempre consulta a tu médico.
      </p>
    </>
  );

  return (
    <div className="rounded-lg bg-medical-50 border border-medical-200 p-4 mb-6">
      <div className="flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-medical-600 flex-shrink-0 mt-0.5" />
        <div className="text-sm text-medical-900 leading-relaxed">{contentUi}</div>
      </div>
    </div>
  );
}