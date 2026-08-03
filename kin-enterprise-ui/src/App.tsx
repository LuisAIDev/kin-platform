import { useCallback, useEffect, useState } from "react";
import { EnterpriseScoreCard } from "./components/EnterpriseScoreCard";
import { DocumentList } from "./components/DocumentList";
import { ExportButtons, type ExportFormat } from "./components/ExportButtons";
import { LiveLog } from "./components/LiveLog";
import { ProgressBar } from "./components/ProgressBar";
import { StatusBadge } from "./components/StatusBadge";
import { Timeline } from "./components/Timeline";
import { VersionSelector } from "./components/VersionSelector";
import { useEnterpriseProgress } from "./hooks/useEnterpriseProgress";
import { downloadBlob, enterpriseApi } from "./services/enterpriseApi";
import type { EnterpriseDashboard } from "./types/enterprise";

interface AppProps {
  /** Identificador del proyecto (o null para leerlo de la URL). */
  projectId?: string | null;
  /** Versión inicial del proyecto. */
  version?: number;
}

function readProjectIdFromUrl(): string | null {
  return new URLSearchParams(window.location.search).get("projectId");
}

/** Pantalla Enterprise Dashboard. */
export function App({
  projectId: projectIdProp,
  version: initialVersion = 1,
}: AppProps = {}) {
  const projectId = projectIdProp ?? readProjectIdFromUrl();
  const [selectedVersion, setSelectedVersion] = useState(initialVersion);
  const [dashboard, setDashboard] = useState<EnterpriseDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);
  const { events, connected, error: sseError, terminal } =
    useEnterpriseProgress(projectId, selectedVersion);

  useEffect(() => {
    if (!projectId) {
      return;
    }
    let cancelled = false;
    setDashboard(null);
    setError(null);
    enterpriseApi
      .getDashboard(projectId, selectedVersion)
      .then((data) => {
        if (!cancelled) setDashboard(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      });
    return () => {
      cancelled = true;
    };
  }, [projectId, selectedVersion]);

  const handleDownloadDocument = useCallback(
    async (type: string, format: ExportFormat) => {
      if (!projectId) return;
      setDownloading(true);
      try {
        const blob = await enterpriseApi.downloadDocument(
          projectId,
          selectedVersion,
          type,
          format,
        );
        downloadBlob(blob, `${type.toLowerCase()}.${format.toLowerCase()}`);
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      } finally {
        setDownloading(false);
      }
    },
    [projectId, selectedVersion],
  );

  const handleDownloadBundle = useCallback(
    async (format: ExportFormat) => {
      if (!projectId) return;
      setDownloading(true);
      try {
        const blob = await enterpriseApi.downloadBundle(
          projectId,
          selectedVersion,
          format,
        );
        downloadBlob(blob, `enterprise-${selectedVersion}-${format.toLowerCase()}.zip`);
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      } finally {
        setDownloading(false);
      }
    },
    [projectId, selectedVersion],
  );

  if (!projectId) {
    return (
      <div className="app">
        <p className="hint">
          Falta el parámetro <code>projectId</code> en la URL (?projectId=…).
        </p>
      </div>
    );
  }

  const status = dashboard?.status ?? "REQUESTED";
  const progress = dashboard?.progress ?? 0;

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1 className="app-title">Enterprise Dashboard</h1>
          <p className="app-subtitle">
            Proyecto {projectId} · v{selectedVersion}
          </p>
        </div>
        <StatusBadge status={status} />
      </header>

      {error ? (
        <div className="card" style={{ borderColor: "#fecaca", color: "#b91c1c" }}>
          Error: {error}
        </div>
      ) : null}
      {sseError && !terminal ? (
        <p className="hint">SSE: {sseError} — reconectando…</p>
      ) : null}

      <div className="status-row">
        <span
          data-testid="connection-dot"
          className={`connection-dot ${
            connected ? "connection-dot-on" : "connection-dot-off"
          }`}
        />
        <span className="hint">
          {connected ? "Conectado" : terminal ? "Finalizado" : "Desconectado"}
        </span>
      </div>

      <div className="card" style={{ marginBottom: 16 }}>
        <div className="card-title">Generación</div>
        <ProgressBar progress={progress} status={status} />
      </div>

      <div className="grid">
        <div className="card">
          <EnterpriseScoreCard score={dashboard?.score} />
        </div>
        <div className="card">
          <VersionSelector
            versions={dashboard?.versions ?? []}
            selected={selectedVersion}
            onSelect={setSelectedVersion}
          />
        </div>
      </div>

      <div className="grid">
        <div className="card">
          <div className="card-title">Documentos ({dashboard?.documentCount ?? 0})</div>
          <DocumentList documents={dashboard?.documents ?? []} />
        </div>
        <div className="card">
          <ExportButtons
            documents={dashboard?.documents ?? []}
            onDownloadDocument={handleDownloadDocument}
            onDownloadBundle={handleDownloadBundle}
            busy={downloading}
          />
        </div>
      </div>

      <div className="grid">
        <div className="card">
          <div className="card-title">Estadísticas</div>
          {dashboard ? (
            <ul className="doc-list">
              {Object.entries(dashboard.statistics).map(([key, value]) => (
                <li key={key} className="doc-item">
                  <span className="doc-name">{key}</span>
                  <span className="doc-meta">{value}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="hint">Cargando…</p>
          )}
        </div>
        <div className="card">
          <div className="card-title">Línea de tiempo</div>
          <Timeline events={events} />
        </div>
      </div>

      <div className="card">
        <LiveLog events={events} connected={connected} />
      </div>
    </div>
  );
}
