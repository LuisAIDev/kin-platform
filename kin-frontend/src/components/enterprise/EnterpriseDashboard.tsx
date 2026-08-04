"use client";

import { useCallback, useEffect, useState } from "react";
import { EnterpriseScoreCard } from "@/components/enterprise/EnterpriseScoreCard";
import { DocumentList } from "@/components/enterprise/DocumentList";
import { ExportButtons, type ExportFormat } from "@/components/enterprise/ExportButtons";
import { LiveLog } from "@/components/enterprise/LiveLog";
import { ProgressBar } from "@/components/enterprise/ProgressBar";
import { StatusBadge } from "@/components/enterprise/StatusBadge";
import { Timeline } from "@/components/enterprise/Timeline";
import { VersionSelector } from "@/components/enterprise/VersionSelector";
import { useEnterpriseProgress } from "@/hooks/useEnterpriseProgress";
import { downloadBlob, enterpriseApi } from "@/services/enterpriseApi";
import type { EnterpriseDashboard } from "@/types/enterprise";

interface EnterpriseDashboardProps {
  /** Identificador del proyecto de KIN. */
  projectId: string;
  /** Versión inicial del proyecto (1 por defecto). */
  version?: number;
}

/** Pantalla Enterprise Dashboard integrada en kin-frontend (M3F). */
export default function EnterpriseDashboard({
  projectId,
  version: initialVersion = 1,
}: EnterpriseDashboardProps) {
  const [selectedVersion, setSelectedVersion] = useState(initialVersion);
  const [dashboard, setDashboard] = useState<EnterpriseDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const { events, connected, error: sseError, terminal } =
    useEnterpriseProgress(projectId, selectedVersion);

  useEffect(() => {
    if (!projectId) {
      return;
    }
    let cancelled = false;
    enterpriseApi
      .getDashboard(projectId, selectedVersion)
      .then((data) => {
        if (cancelled) {
          return;
        }
        setDashboard(data);
        setError(null);
        // Si hay una generación activa (REQUESTED/RUNNING), se selecciona
        // automáticamente su versión para mostrar el progreso en vivo (M3G).
        const active = data.versions.filter(
          (v) => v.status === "REQUESTED" || v.status === "RUNNING",
        );
        if (active.length > 0) {
          const maxActive = Math.max(...active.map((v) => v.version));
          if (maxActive !== selectedVersion) {
            setSelectedVersion(maxActive);
          }
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      });
    return () => {
      cancelled = true;
    };
  }, [projectId, selectedVersion]);

  /** Recarga el dashboard de la versión seleccionada (sin recargar la página). */
  const refreshDashboard = useCallback(() => {
    if (!projectId) {
      return;
    }
    enterpriseApi
      .getDashboard(projectId, selectedVersion)
      .then((data) => {
        setDashboard(data);
        setError(null);
      })
      .catch((err: unknown) =>
        setError(err instanceof Error ? err.message : String(err)),
      );
  }, [projectId, selectedVersion]);

  // Al terminar el SSE (COMPLETED/FAILED) se recarga el dashboard para reflejar
  // documentos, score y estado finales de la versión (M3G).
  useEffect(() => {
    if (terminal) {
      refreshDashboard();
    }
  }, [terminal, refreshDashboard]);

  const handleSelectVersion = useCallback((version: number) => {
    setSelectedVersion(version);
    setDashboard(null);
    setError(null);
  }, []);

  /**
   * Descubre la versión de la generación recién solicitada sondeando el listado
   * de versiones: espera a que aparezca una versión activa (REQUESTED/RUNNING)
   * o, si la generación terminó muy rápido, una versión nueva terminal.
   */
  const waitForNewVersion = useCallback(
    async (maxKnown: number): Promise<void> => {
      const deadline = Date.now() + 40_000;
      while (Date.now() < deadline) {
        try {
          const data = await enterpriseApi.getDashboard(projectId, selectedVersion);
          const active = data.versions.filter(
            (v) => v.status === "REQUESTED" || v.status === "RUNNING",
          );
          if (active.length > 0) {
            setSelectedVersion(Math.max(...active.map((v) => v.version)));
            setGenerating(false);
            return;
          }
          const newerTerminal = data.versions
            .filter((v) => v.version > maxKnown)
            .sort((a, b) => b.version - a.version)[0];
          if (newerTerminal) {
            setSelectedVersion(newerTerminal.version);
            setGenerating(false);
            return;
          }
        } catch {
          // La versión consultada puede no existir aún (primera generación).
        }
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
      setGenerating(false);
      refreshDashboard();
    },
    [projectId, selectedVersion, refreshDashboard],
  );

  /** Solicita la generación (POST async) y muestra el progreso en vivo. */
  const handleGenerate = useCallback(async () => {
    if (!projectId || generating) {
      return;
    }
    setGenerating(true);
    setError(null);
    try {
      await enterpriseApi.generate(projectId, true);
      const maxKnown = (dashboard?.versions ?? []).reduce(
        (max, v) => Math.max(max, v.version),
        0,
      );
      await waitForNewVersion(maxKnown);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setGenerating(false);
    }
  }, [projectId, generating, dashboard, waitForNewVersion]);

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
        <p className="hint">Falta el identificador del proyecto.</p>
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
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={generating}
            onClick={() => void handleGenerate()}
          >
            {generating ? "Generando..." : "Generar Proyecto Empresarial"}
          </button>
          <StatusBadge status={status} />
        </div>
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
            onSelect={handleSelectVersion}
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
