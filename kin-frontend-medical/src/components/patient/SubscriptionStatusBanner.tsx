"use client";

import { useEffect, useState } from "react";
import { subscriptionApi } from "@/services/subscriptionApi";
import { patientPlansService } from "@/services/patientPlans";
import type { PatientSubscriptionStatusResponse } from "@/services/patientPlans";

export function SubscriptionStatusBanner() {
  const [status, setStatus] = useState<PatientSubscriptionStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    subscriptionApi
      .getPatientStatus()
      .then((data) => {
        if (!cancelled) setStatus(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return (
      <div className="rounded-lg border border-neutral-200 bg-neutral-50 px-4 py-3 text-sm text-neutral-500">
        Cargando información de suscripción...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        Error al cargar el estado de la suscripción: {error}
      </div>
    );
  }

  if (!status) {
    return null;
  }

  const { planName, planCode, isActive: isActiveFromStatus, maxTriagesPerMonth, triagesUsed, triagesRemaining, maxStorageMb, storageUsedMb, storageRemainingMb, aiBudgetUsd, aiBudgetUsed, aiBudgetRemaining, aiLevel, pdfExport, triageSharing, advancedAI, supportLevel, subscriptionEndDate } = status;

  const isFreePlan = planCode === "FREE";
  const isActive = isActiveFromStatus && subscriptionEndDate ? new Date(subscriptionEndDate) > new Date() : isActiveFromStatus;

  return (
    <section className="rounded-xl border border-neutral-200 bg-white p-5">
      <div className="flex items-start justify-between gap-4 mb-4">
        <div>
          <h2 className="text-base font-semibold">{planName}</h2>
          <p className="text-xs text-neutral-500 mt-0.5">
            {isActive ? "Suscripción activa" : "Sin suscripción activa"} · {planCode}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
            isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
          }`}>
            <span className={`w-1.5 h-1.5 rounded-full ${isActive ? "bg-green-500" : "bg-red-500"}`} />
            {isActive ? "Activa" : "Inactiva"}
          </span>
          {subscriptionEndDate && (
            <span className="text-xs text-neutral-500">
              Renueva: {new Date(subscriptionEndDate).toLocaleDateString("es-ES")}
            </span>
          )}
        </div>
      </div>

      <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
        {/* Triajes */}
        <div className="rounded-lg border border-neutral-200 bg-neutral-50 p-4">
          <div className="flex items-center gap-1.5 text-xs text-neutral-500 mb-1">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.613 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
            <span>Triajes</span>
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-bold text-neutral-900">{triagesUsed ?? 0}</span>
            <span className="text-sm text-neutral-500">/ {maxTriagesPerMonth !== null ? maxTriagesPerMonth : "∞"}</span>
            <span className="text-xs text-neutral-400">triajes usados</span>
          </div>
          {triagesRemaining !== null && (
            <div className="mt-2 h-2 rounded-full bg-neutral-200 overflow-hidden">
              <div className="h-full bg-primary-600 transition-all" style={{ width: `${maxTriagesPerMonth ? Math.min(100, (triagesUsed ?? 0) / maxTriagesPerMonth * 100) : 0}%` }} />
            </div>
          )}
        </div>

        {/* Almacenamiento */}
        <div className="rounded-lg border border-neutral-200 bg-neutral-50 p-4">
          <div className="flex items-center gap-1.5 text-xs text-neutral-500 mb-1">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2 2v2a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v2a2 2 0 002 2h10a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v2a2 2 0 002 2z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
            <span>Almacenamiento</span>
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-bold text-neutral-900">{storageUsedMb ?? 0} MB</span>
            <span className="text-sm text-neutral-500">/ {maxStorageMb !== null ? `${maxStorageMb} MB` : "∞"}</span>
          </div>
          {maxStorageMb !== null && (
            <div className="mt-2 h-2 rounded-full bg-neutral-200 overflow-hidden">
              <div className="h-full bg-primary-600 transition-all" style={{ width: `${maxStorageMb ? Math.min(100, (storageUsedMb ?? 0) / maxStorageMb * 100) : 0}%` }} />
            </div>
          )}
        </div>

        {/* IA */}
        <div className="rounded-lg border border-neutral-200 bg-neutral-50 p-4">
          <div className="flex items-center gap-1.5 text-xs text-neutral-500 mb-1">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.355c-.59-.35-.09-1.29.8-1.29h.473c1.29 0 2.54.338 3.51.944l.528.527A9 9 0 0021 12c0-4.97-4.03-9-9-9s-9 4.03-9 9v.53a2 2 0 002 2h.394" /></svg>
            <span>IA</span>
          </div>
          <div className="flex items-baseline gap-1 mt-1">
            <span className="text-2xl font-bold text-neutral-900">{aiLevel ?? "FLASH"}</span>
            <span className="text-xs text-neutral-500">({aiBudgetUsd ? `$${aiBudgetUsd.toFixed(2)}` : "Sin límite"})</span>
          </div>
          {aiBudgetUsd !== null && aiBudgetUsd > 0 && (
            <div className="mt-2 h-2 rounded-full bg-neutral-200 overflow-hidden">
              <div className="h-full bg-primary-600 transition-all" style={{ width: `${Math.min(100, Math.max(0, (Number(aiBudgetRemaining) / Number(aiBudgetUsd)) * 100))}%` }} />
            </div>
          )}
        </div>

        {/* Extras */}
        <div className="rounded-lg border border-neutral-200 bg-neutral-50 p-4">
          <div className="flex items-center gap-1.5 text-xs text-neutral-500 mb-1">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2z" /></svg>
            <span>Extras</span>
          </div>
          <div className="grid grid-cols-2 gap-2 text-sm">
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${pdfExport ? "bg-green-500" : "bg-neutral-300"}`} />
              <span className="text-sm text-neutral-600">Exportar PDF</span>
            </div>
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${triageSharing ? "bg-green-500" : "bg-neutral-300"}`} />
              <span className="text-sm text-neutral-600">Compartir triajes</span>
            </div>
            <div className="flex items-center gap-2">
              <span className={`w-2 h-2 rounded-full ${advancedAI ? "bg-green-500" : "bg-neutral-300"}`} />
              <span className="text-sm text-neutral-600">IA Avanzada</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-primary-500" />
              <span className="text-sm text-neutral-600">Soporte {supportLevel}</span>
            </div>
          </div>
        </div>
      </div>

      {subscriptionEndDate && new Date(subscriptionEndDate) < new Date() && (
        <div className="mt-4 rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3 text-sm text-yellow-800">
          Tu suscripción ha expirado. <button className="underline hover:text-yellow-900 ml-1">Renovar ahora</button>
        </div>
      )}
    </section>
  );
}