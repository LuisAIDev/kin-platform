import type { EnterpriseProgressEvent } from "../types/enterprise";

interface LiveLogProps {
  /** Eventos de progreso registrados. */
  events: EnterpriseProgressEvent[];
  /** Estado de la conexión SSE. */
  connected: boolean;
}

/** Registro en vivo (log) del progreso de generación Enterprise. */
export function LiveLog({ events, connected }: LiveLogProps) {
  const stateClass = (state: string): string =>
    state === "COMPLETED" || state === "FAILED" ? `log-line-${state}` : "";
  return (
    <div data-testid="live-log">
      <div className="card-title">
        Registro en vivo
        {connected ? " · conectado" : " · desconectado"}
      </div>
      <div className="live-log">
        {events.length === 0 ? (
          <p className="log-line">— Esperando eventos —</p>
        ) : (
          events.map((event, index) => (
            <p
              key={`${event.state}-${event.documentType ?? ""}-${index}`}
              className={`log-line ${stateClass(event.state)}`.trim()}
            >
              [{new Date(event.timestamp).toLocaleTimeString("es-ES")}]
              {event.state}
              {event.documentType ? ` · ${event.documentType}` : ""}
              {event.message ? ` — ${event.message}` : ""}
            </p>
          ))
        )}
      </div>
    </div>
  );
}
