import type { EnterpriseProgressEvent } from "@/types/enterprise";

interface TimelineProps {
  /** Eventos de progreso ordenados cronológicamente. */
  events: EnterpriseProgressEvent[];
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString("es-ES", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

/** Línea de tiempo del ciclo de generación Enterprise. */
export function Timeline({ events }: TimelineProps) {
  if (events.length === 0) {
    return <p className="hint">Conectado. Esperando eventos de generación.</p>;
  }
  return (
    <ul className="timeline" data-testid="timeline">
      {events.map((event, index) => (
        <li key={`${event.state}-${index}`} className="timeline-item">
          <span className="timeline-state">{event.state}</span>
          {event.documentType ? (
            <span className="timeline-doc"> · {event.documentType}</span>
          ) : null}
          <div className="timeline-time">
            {formatTime(event.timestamp)}
            {event.message ? ` · ${event.message}` : ""}
          </div>
        </li>
      ))}
    </ul>
  );
}
