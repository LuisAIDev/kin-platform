"use client";

import { useCallback, useEffect, useState } from "react";
import { enterpriseApi } from "@/services/enterpriseApi";
import { projectInfoService } from "@/services/projectInfo";
import type { EnterpriseInformation, InfoValue } from "@/types/enterprise";

const STATE_LABEL: Record<string, string> = {
  CONFIRMED: "Confirmado",
  IMPORTED: "Importado",
  CALCULATED: "Calculado",
  ESTIMATED: "Estimado",
  AI_SUGGESTED: "Sugerido por IA",
  PENDING: "Pendiente",
  NOT_AVAILABLE: "No disponible",
};

const STATE_BADGE: Record<string, string> = {
  CONFIRMED: "bg-emerald-100 text-emerald-700",
  IMPORTED: "bg-blue-100 text-blue-700",
  CALCULATED: "bg-violet-100 text-violet-700",
  ESTIMATED: "bg-amber-100 text-amber-700",
  AI_SUGGESTED: "bg-purple-100 text-purple-700",
  PENDING: "bg-neutral-100 text-neutral-500",
  NOT_AVAILABLE: "bg-red-100 text-red-700",
};

function valueText(value: InfoValue | null | undefined): string {
  if (!value) return "Por definir";
  if (value.value != null && value.value !== "") return value.value;
  return value.state === "NOT_AVAILABLE" ? "No disponible" : "Por definir";
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleDateString("es-ES", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  } catch {
    return "—";
  }
}

interface InfoRow {
  dato: string;
  valor: string;
  origen: string;
  estado: string;
  state: string;
  section?: string;
  key?: string;
}

interface EnterpriseInformationPanelProps {
  projectId: string;
}

/** Sección "Información y fuentes" del dashboard Enterprise (FASE 8/9). */
export function EnterpriseInformationPanel({ projectId }: EnterpriseInformationPanelProps) {
  const [info, setInfo] = useState<EnterpriseInformation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState<string | null>(null);

  const load = useCallback(() => {
    if (!projectId) return;
    let cancelled = false;
    enterpriseApi
      .getInformation(projectId)
      .then((data) => {
        if (!cancelled) setInfo(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  useEffect(() => {
    return load();
  }, [load]);

  const handleConfirm = async (section: string, key: string) => {
    if (!projectId) return;
    setConfirming(`${section}.${key}`);
    try {
      await projectInfoService.confirmInfo(projectId, section, key);
      load();
    } catch {
      // El error queda visible en la recarga de información del proyecto.
    } finally {
      setConfirming(null);
    }
  };

  if (error) {
    return (
      <div className="card">
        <div className="card-title">Información y fuentes</div>
        <p className="hint">Error: {error}</p>
      </div>
    );
  }

  if (!info) {
    return (
      <div className="card">
        <div className="card-title">Información y fuentes</div>
        <p className="hint">Cargando…</p>
      </div>
    );
  }

  const rows: InfoRow[] = info.resolvedDimensions.map((d) => ({
    dato: d.displayName,
    valor: valueText({
      value: d.value,
      sourceType: d.sourceType,
      state: d.state,
      origin: d.origin,
    }),
    origen: d.origin ?? "—",
    estado: STATE_LABEL[d.state] ?? d.state,
    state: d.state,
  }));

  const supplementalRows: InfoRow[] = [
    ...Object.entries(info.supplemental.financial).map(([key, v]) => ({
      dato: key.replace(/_/g, " "),
      valor: valueText(v),
      origen: v.origin ?? "—",
      estado: STATE_LABEL[v.state] ?? v.state,
      state: v.state,
      section: "FINANZAS",
      key,
    })),
    ...Object.entries(info.supplemental.market).map(([key, v]) => ({
      dato: key.replace(/_/g, " "),
      valor: valueText(v),
      origen: v.origin ?? "—",
      estado: STATE_LABEL[v.state] ?? v.state,
      state: v.state,
      section: "MERCADO",
      key,
    })),
    ...Object.entries(info.supplemental.impact).map(([key, v]) => ({
      dato: key.replace(/_/g, " "),
      valor: valueText(v),
      origen: v.origin ?? "—",
      estado: STATE_LABEL[v.state] ?? v.state,
      state: v.state,
      section: "IMPACTO",
      key,
    })),
    ...Object.entries(info.supplemental.risk).map(([key, v]) => ({
      dato: key.replace(/_/g, " "),
      valor: valueText(v),
      origen: v.origin ?? "—",
      estado: STATE_LABEL[v.state] ?? v.state,
      state: v.state,
      section: "RIESGOS",
      key,
    })),
  ];

  const breakevenRow: InfoRow = {
    dato: "Punto de equilibrio",
    valor: info.supplemental.breakevenCalculable
      ? valueText(info.supplemental.breakeven)
      : "No calculable con los datos disponibles",
    origen: info.supplemental.breakeven.origin ?? "—",
    estado: info.supplemental.breakevenCalculable
      ? "Calculado"
      : "Faltan datos",
    state: info.supplemental.breakevenCalculable ? "CALCULATED" : "NOT_AVAILABLE",
  };

  const allRows = [...rows, ...supplementalRows, breakevenRow];

  return (
    <div className="card" data-testid="info-sources-panel">
      <div className="card-title">Información y fuentes</div>

      {info.conflicts.length > 0 && (
        <div className="mb-3">
          {info.conflicts.map((conflict) => (
            <p key={conflict} className="text-xs text-amber-700 mb-1">
              ⚠ {conflict}
            </p>
          ))}
        </div>
      )}

      {info.supplemental.breakevenMissing.length > 0 && (
        <p className="text-xs text-neutral-500 mb-3">
          Faltan para el punto de equilibrio: {info.supplemental.breakevenMissing.join(", ")}
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead>
            <tr className="text-left text-neutral-400 uppercase tracking-wide">
              <th className="py-1 pr-2 font-semibold">Dato</th>
              <th className="py-1 pr-2 font-semibold">Valor</th>
              <th className="py-1 pr-2 font-semibold">Origen</th>
              <th className="py-1 pr-2 font-semibold">Estado</th>
              <th className="py-1 font-semibold">Acción</th>
            </tr>
          </thead>
          <tbody>
            {allRows.map((row) => (
              <tr key={`${row.dato}-${row.origen}`} className="border-t border-neutral-100">
                <td className="py-1.5 pr-2 text-neutral-700 capitalize">{row.dato}</td>
                <td className="py-1.5 pr-2 text-neutral-800">{row.valor}</td>
                <td className="py-1.5 pr-2 text-neutral-500">{row.origen}</td>
                <td className="py-1.5">
                  <span
                    className={`text-[10px] font-medium px-1.5 py-0.5 rounded-full ${
                      STATE_BADGE[row.state] ?? STATE_BADGE.PENDING
                    }`}
                  >
                    {row.estado}
                  </span>
                </td>
                <td className="py-1.5">
                  {row.state === "IMPORTED" && row.section && row.key ? (
                    <button
                      type="button"
                      onClick={() => void handleConfirm(row.section!, row.key!)}
                      disabled={confirming === `${row.section}.${row.key}`}
                      data-testid={`confirm-${row.section}-${row.key}`}
                      className="text-[10px] font-medium text-primary-600 hover:text-primary-800 disabled:opacity-50"
                    >
                      {confirming === `${row.section}.${row.key}` ? "Confirmando..." : "Confirmar"}
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {info.documents.length > 0 && (
        <div className="mt-4">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-neutral-400 mb-1">
            Documentos del proyecto
          </p>
          <ul className="space-y-2">
            {info.documents.map((doc) => (
              <li key={doc.id} className="border border-neutral-200 rounded-lg p-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-neutral-800 break-all">
                    📄 {doc.filename}
                  </span>
                  <span
                    className={`text-[10px] font-medium px-1.5 py-0.5 rounded-full ${
                      doc.status === "PROCESADO"
                        ? "bg-emerald-100 text-emerald-700"
                        : doc.status === "ERROR"
                          ? "bg-red-100 text-red-700"
                          : "bg-amber-100 text-amber-700"
                    }`}
                  >
                    {doc.status}
                  </span>
                </div>
                <div className="text-neutral-400 mt-0.5">
                  {doc.mimeType} · {formatBytes(doc.size)} · {formatDate(doc.createdAt)}
                </div>
                {doc.summary && (
                  <p className="text-neutral-600 mt-1 line-clamp-2">{doc.summary}</p>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
