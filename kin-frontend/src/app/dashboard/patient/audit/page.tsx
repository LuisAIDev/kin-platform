"use client";

import { useEffect, useState } from "react";
import AuditLogTable from "@/components/audit/AuditLogTable";
import { auditService } from "@/services/audit";
import type { AuditLogEntry } from "@/services/audit";
import type { PageResponse } from "@/types";

export default function PatientAuditPage() {
  const [page, setPage] = useState<PageResponse<AuditLogEntry> | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    auditService
      .myLogs(0, 20)
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

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-4xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Historial de accesos</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Quién ha accedido a tus datos de salud y cuándo (transparencia, ADR-035).
          </p>
        </div>

        {error && <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>}

        {page && <AuditLogTable entries={page.content} />}
      </div>
    </main>
  );
}
