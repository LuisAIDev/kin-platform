"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { outboxAdminApi, OutboxRecord, DeadLetterStats, type DeadLetterPage } from "@/services/outbox";

export default function DeadLetterAdminPage() {
  const router = useRouter();
  const [records, setRecords] = useState<OutboxRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<{
    deadLetterCount: number;
    byEventType: Array<{ event_type: string; count: number }>;
    topAggregates: Array<{ aggregate_id: string; count: number }>;
  } | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filterEventType, setFilterEventType] = useState("");
  const [filterAggregateId, setFilterAggregateId] = useState("");
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Cargar lista y estadísticas
  const loadData = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      const params: Record<string, string | number> = {
        page: page,
        size: pageSize,
      };
      if (filterEventType) {
        (params as Record<string, string>)["eventType"] = filterEventType;
      }
      if (filterAggregateId) {
        (params as Record<string, string>)["aggregateId"] = filterAggregateId;
      }

      const [pageData, statsData] = await Promise.all([
        outboxAdminApi.listDeadLetter({
          page,
          size: pageSize,
          eventType: filterEventType || undefined,
          aggregateId: filterAggregateId || undefined,
        }),
        outboxAdminApi.getDeadLetterStats(),
      ]);

      setRecords(pageData.content);
      setTotalElements(pageData.totalElements);
      setTotalPages(pageData.totalPages);
      setStats(statsData);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Error al cargar eventos";
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, filterEventType, filterAggregateId]);

  useEffect(() => {
    const user = authService.getUser();
    if (!user || user.role !== "ADMIN") {
      router.replace("/dashboard");
      return;
    }
    loadData();
  }, [router, loadData]);

  const handleRetry = async (id: string) => {
    setActionLoading(id);
    try {
      await outboxAdminApi.retryDeadLetter(id);
      setMessage({ type: "success", text: "Evento reencolado correctamente" });
      loadData();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Error al reencolar";
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm("¿Eliminar este evento de la cola de muertos? Esta acción no se puede deshacer.")) {
      return;
    }
    setActionLoading(id);
    try {
      await outboxAdminApi.deleteDeadLetter(id);
      setMessage({ type: "success", text: "Evento eliminado correctamente" });
      loadData();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Error al eliminar";
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeleteAll = async () => {
    if (!window.confirm("¿Eliminar TODOS los eventos en DEAD_LETTER? Esta acción no se puede deshacer.")) {
      return;
    }
    if (!window.confirm("CONFIRMACIÓN FINAL: Se eliminarán TODOS los eventos en DEAD_LETTER permanentemente.")) {
      return;
    }
    try {
      await outboxAdminApi.deleteAllDeadLetter();
      setMessage({ type: "success", text: "Todos los eventos eliminados" });
      loadData();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Error al eliminar";
      setMessage({ type: "error", text: errorMessage });
    }
  };

  const formatDate = (iso: string) => {
    try {
      return new Date(iso).toLocaleString("es-ES", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
    } catch {
      return iso;
    }
  };

  const truncate = (str: string, len = 60) => (str.length > len ? str.slice(0, len) + "…" : str);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh] text-neutral-500">
        Cargando cola de eventos fallidos...
      </div>
    );
  }

  const eventTypes = [...new Set(records.map((r) => r.event_type))].sort();

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">Cola de Eventos Fallidos (DLQ)</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Gestiona eventos en estado DEAD_LETTER: reencola, elimina o elimina todo.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleDeleteAll}
            disabled={totalElements === 0}
            className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-11"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v12m-6 0h12m-6 0h6" />
            </svg>
            Eliminar todo
          </button>
        </div>
      </div>

      {message && (
        <div
          className={`mb-6 rounded-lg px-4 py-3 text-sm font-medium ${
            message.type === "success"
              ? "bg-green-50 text-green-700 border border-green-200"
              : "bg-red-50 text-red-700 border border-red-200"
          }`}
        >
          {message.text}
        </div>
      )}

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div className="rounded-xl border border-neutral-200 bg-white p-4">
            <p className="text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
              Eventos en DLQ
            </p>
            <p className="text-2xl font-bold text-neutral-900">{stats.deadLetterCount}</p>
          </div>
          <div className="rounded-xl border border-neutral-200 bg-white p-4">
            <p className="text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
              Tipos de evento
            </p>
            <p className="text-2xl font-bold text-neutral-900">{stats.byEventType.length}</p>
          </div>
          <div className="rounded-xl border border-neutral-200 bg-white p-4">
            <p className="text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
              Proyectos afectados
            </p>
            <p className="text-2xl font-bold text-neutral-900">{stats.topAggregates.length}</p>
          </div>
        </div>
      )}

      {/* Filtros */}
      <div className="rounded-xl border border-neutral-200 bg-white p-4 mb-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
              Filtrar por event_type
            </label>
            <select
              value={filterEventType}
              onChange={(e) => {
                setFilterEventType(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11 bg-white"
            >
              <option value="">Todos los tipos</option>
              {eventTypes.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
              Filtrar por aggregate_id (UUID)
            </label>
            <input
              type="text"
              placeholder="UUID del proyecto..."
              value={filterAggregateId}
              onChange={(e) => setFilterAggregateId(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  setPage(0);
                }
              }}
              className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={() => {
                setFilterEventType("");
                setFilterAggregateId("");
                setPage(0);
              }}
              className="w-full rounded-lg border border-neutral-300 bg-white px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition min-h-11"
            >
              Limpiar filtros
            </button>
          </div>
        </div>
      </div>

      {/* Tabla */}
      <div className="rounded-xl border border-neutral-200 bg-white overflow-hidden">
        {loading && records.length === 0 ? (
          <div className="flex items-center justify-center min-h-[40vh] text-neutral-500">
            Cargando eventos...
          </div>
        ) : records.length === 0 ? (
          <div className="text-center py-12 text-neutral-500">
            <svg className="w-12 h-12 mx-auto mb-3 text-neutral-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 11a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="text-lg font-medium">No hay eventos en DEAD_LETTER</p>
            <p className="text-sm mt-1">¡La cola está limpia! 🎉</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-neutral-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                      ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                      Aggregate ID
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                      Tipo
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                      Creado
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                      Reintentos
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                      Último error
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-semibold text-neutral-500 uppercase tracking-wider pr-4">
                      Acciones
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-200">
                  {records.map((record) => (
                    <tr key={record.id} className="hover:bg-neutral-50">
                      <td className="px-4 py-3 text-sm font-mono text-neutral-600 truncate max-w-[120px]">
                        {record.id}
                      </td>
                      <td className="px-4 py-3 text-sm font-mono text-neutral-600 truncate max-w-[120px]">
                        {record.aggregate_id}
                      </td>
                      <td className="px-4 py-3">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-primary-50 text-primary-700">
                          {record.event_type}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-neutral-600">
                        {record.created_at ? new Date(record.created_at).toLocaleString("es-ES") : "—"}
                      </td>
                      <td className="px-4 py-3 text-sm text-neutral-600">
                        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${
                          record.retry_count >= 5
                            ? "bg-red-50 text-red-700"
                            : record.retry_count > 0
                            ? "bg-amber-50 text-amber-700"
                            : "bg-green-50 text-green-700"
                        }`}>
                          {record.retry_count}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-sm text-neutral-500 truncate max-w-[200px]">
                        {record.last_error ? truncate(record.last_error, 60) : "—"}
                      </td>
                      <td className="px-4 py-3 text-right pr-4">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleRetry(record.id)}
                            disabled={actionLoading === record.id}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white shadow-sm hover:bg-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-8"
                          >
                            {actionLoading === record.id ? (
                              <>
                                <svg className="animate-spin h-3.5 w-3.5" viewBox="0 0 24 24">
                                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                                </svg>
                                Reencolar
                              </>
                            ) : (
                              "Reencolar"
                            )}
                          </button>
                          <button
                            onClick={() => handleDelete(record.id)}
                            disabled={actionLoading === record.id}
                            className="inline-flex items-center gap-1.5 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-medium text-white shadow-sm hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-8"
                          >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v12m-6 0h12m-6 0h6" />
                            </svg>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="border-t border-neutral-200 px-4 py-3 flex items-center justify-between">
                <p className="text-sm text-neutral-500">
                  Página {page + 1} de {totalPages} — {totalElements} eventos
                </p>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-8"
                  >
                    Anterior
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-8"
                  >
                    Siguiente
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}