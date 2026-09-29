import Link from "next/link";
import type { EncounterResponse } from "@/lib/hce/api/hce.api";

const TYPE_LABELS: Record<string, string> = {
  OUTPATIENT: "Consulta externa",
  INPATIENT: "Hospitalización",
  EMERGENCY: "Urgencias",
  TELEMEDICINE: "Telemedicina",
  HOME_CARE: "Atención domiciliaria",
  DAY_SURGERY: "Cirugía ambulatoria",
};

const STATUS_LABELS: Record<string, string> = {
  OPEN: "Abierta",
  IN_PROGRESS: "En curso",
  CLOSED: "Cerrada",
  COMPLETED: "Completada",
  CANCELLED: "Cancelada",
};

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("es-CO", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Lista read-only de encuentros clínicos de un paciente. Cada fila enlaza al
 * asistente HCE para editar el encuentro. Sin encuentros muestra un estado vacío.
 */
export default function EncounterList({ encounters }: { encounters: EncounterResponse[] }) {
  if (encounters.length === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-500">
        Sin consultas previas
      </div>
    );
  }

  return (
    <ul className="flex flex-col gap-3" aria-label="Consultas del paciente">
      {encounters.map((encounter) => (
        <li
          key={encounter.id}
          className="flex flex-col gap-3 rounded-xl border border-neutral-200 bg-white p-4 sm:flex-row sm:items-center sm:justify-between"
        >
          <div className="flex flex-col gap-1">
            <p className="text-sm font-semibold text-neutral-800">
              {encounter.chiefComplaint || "Sin motivo registrado"}
            </p>
            <div className="flex flex-wrap items-center gap-2 text-xs text-neutral-500">
              <span>{formatDateTime(encounter.startedAt)}</span>
              <span aria-hidden="true">·</span>
              <span>{TYPE_LABELS[encounter.encounterType] ?? encounter.encounterType}</span>
              <span className="rounded-full bg-neutral-100 px-2.5 py-0.5 font-medium text-neutral-600">
                {STATUS_LABELS[encounter.status] ?? encounter.status}
              </span>
            </div>
          </div>
          <Link
            href={`/dashboard/physician/hce/${encounter.id}/edit`}
            className="self-start rounded-lg border border-primary-200 px-3 py-1.5 text-xs font-medium text-primary-700 hover:bg-primary-50 transition sm:self-auto"
          >
            Abrir
          </Link>
        </li>
      ))}
    </ul>
  );
}
