"use client";

import { useState } from "react";
import type { ProductIntelligence } from "@/services/intelligence/types";
import { Exporters } from "@/services/intelligence/exporters";
import { analytics } from "@/services/analytics";

/**
 * Generador de reportes Enterprise (Fase 16). Componente separado del
 * PdfReportButton existente; genera JSON/CSV/PDF de las métricas de producto.
 */
export default function EnterpriseReportGenerator({ intelligence }: { intelligence: ProductIntelligence }) {
  const [exporting, setExporting] = useState(false);

  const handleExport = async (format: "json" | "csv" | "pdf") => {
    setExporting(true);
    try {
      if (format === "json") {
        Exporters.download("KIN_metrics.json", Exporters.toJson(intelligence), "application/json");
      } else if (format === "csv") {
        Exporters.download("KIN_metrics.csv", Exporters.toCsv(intelligence), "text/csv");
      } else {
        await import("jspdf");
        Exporters.toPdf(intelligence);
      }
      analytics.track("report_generated", { format });
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="flex flex-wrap items-center gap-3">
      <button
        onClick={() => handleExport("json")}
        disabled={exporting}
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition min-h-11 disabled:opacity-50"
      >
        Exportar JSON
      </button>
      <button
        onClick={() => handleExport("csv")}
        disabled={exporting}
        className="rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition min-h-11 disabled:opacity-50"
      >
        Exportar CSV
      </button>
      <button
        onClick={() => handleExport("pdf")}
        disabled={exporting}
        className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 transition min-h-11 disabled:opacity-50"
      >
        {exporting ? "Generando..." : "Exportar PDF"}
      </button>
    </div>
  );
}
