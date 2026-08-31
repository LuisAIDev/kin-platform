"use client";

import { useEffect, useState } from "react";
import AuditLogTable from "@/components/audit/AuditLogTable";
import { auditService } from "@/services/audit";
import type { AuditLogEntry, AuditFilters } from "@/services/audit";
import type { PageResponse } from "@/types";

export default function AdminAuditPage() {
  const [page, setPage] = useState<PageResponse<AuditLogEntry> | null>(null);
  const [filters, setFilters] = useState<AuditFilters>({});
  const [currentPage, setCurrentPage] = useState(0);
  const [error, setError] = useState("");

  const load = async (filtersToApply: AuditFilters, pageNumber: number) => {
    try {
      setPage(await auditService.adminLogs(filtersToApply, pageNumber, 20));
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    auditService
      .adminLogs({}, 0)
      .then((data) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const applyFilters = () => {
    setCurrentPage(0);
    load(filters, 0);
  };

  const handlePageChange = (next: number) => {
    if (next < 0 || (page && next >= page.totalPages)) return;
    setCurrentPage(next);
    load(filters, next);
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-6xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Auditoría de accesos</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Registro de accesos a datos de salud de la plataforma (ADR-035).
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

        <section className="rounded-xl border border-neutral-200 bg-white p-5 flex flex-col gap-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
            <input
              type="text"
              value={filters.userId ?? ""}
              onChange={(e) => setFilters((f) => ({ ...f, userId: e.target.value || undefined }))}
              placeholder="User ID"
              aria-label="Filtro por usuario"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="text"
              value={filters.patientId ?? ""}
              onChange={(e) => setFilters((f) => ({ ...f, patientId: e.target.value || undefined }))}
              placeholder="Patient ID"
              aria-label="Filtro por paciente"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="text"
              value={filters.action ?? ""}
              onChange={(e) => setFilters((f) => ({ ...f, action: e.target.value || undefined }))}
              placeholder="Acción (ej. VIEW_HISTORY)"
              aria-label="Filtro por acción"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="datetime-local"
              value={filters.startDate ?? ""}
              onChange={(e) => setFilters((f) => ({ ...f, startDate: e.target.value ? new Date(e.target.value).toISOString() : undefined }))}
              aria-label="Desde"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="datetime-local"
              value={filters.endDate ?? ""}
              onChange={(e) => setFilters((f) => ({ ...f, endDate: e.target.value ? new Date(e.target.value).toISOString() : undefined }))}
              aria-label="Hasta"
              className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <button
              type="button"
              onClick={applyFilters}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition"
            >
              Filtrar
            </button>
          </div>
        </section>

        {page && <AuditLogTable entries={page.content} showUser />}

        {page && (
          <div className="flex items-center justify-between text-sm text-neutral-500">
            <span>
              {page.totalElements} registro{page.totalElements === 1 ? "" : "s"} · Página{" "}
              {currentPage + 1} de {Math.max(1, page.totalPages)}
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100 disabled:opacity-40"
              >
                Anterior
              </button>
              <button
                type="button"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={page && currentPage + 1 >= page.totalPages}
                className="rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-100 disabled:opacity-40"
              >
                Siguiente
              </button>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
