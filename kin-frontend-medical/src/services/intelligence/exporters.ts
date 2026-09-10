import type { ProductIntelligence } from "./types";

function csvValue(value: unknown): string {
  const text = String(value ?? "");
  return `"${text.replace(/"/g, '""')}"`;
}

function rowsFor(pi: ProductIntelligence): Array<Array<string>> {
  const rows: Array<Array<string>> = [];
  for (const [key, value] of Object.entries(pi.usage)) {
    rows.push([`usage.${key}`, csvValue(value)]);
  }
  for (const [key, value] of Object.entries(pi.insights)) {
    rows.push([`insights.${key}`, csvValue(Array.isArray(value) ? value.join(" | ") : value)]);
  }
  for (const [key, value] of Object.entries(pi.metrics)) {
    rows.push([`metrics.${key}`, csvValue(value)]);
  }
  return rows;
}

/**
 * Exportadores de métricas (Fase 16): JSON, CSV y PDF (jsPDF).
 */
export const Exporters = {
  toJson(pi: ProductIntelligence): string {
    return JSON.stringify(pi, null, 2);
  },

  toCsv(pi: ProductIntelligence): string {
    const header = "metric,value";
    const lines = rowsFor(pi).map(([k, v]) => `${csvValue(k)},${v}`);
    return [header, ...lines].join("\n");
  },

  toPdf(pi: ProductIntelligence): void {
    // Carga diferida de jsPDF para no aumentar el bundle inicial.
    void import("jspdf").then(({ default: JsPDF }) => {
      const doc = new JsPDF();
      doc.setFontSize(16);
      doc.text("KIN - Product Intelligence Report", 14, 20);
      doc.setFontSize(10);
      let y = 32;
      for (const [key, value] of Object.entries(pi.usage)) {
        doc.text(`${key}: ${String(value)}`, 14, y);
        y += 6;
        if (y > 280) {
          doc.addPage();
          y = 20;
        }
      }
      doc.save("KIN_Product_Intelligence_Report.pdf");
    });
  },

  download(filename: string, content: string, mime: string): void {
    if (typeof document === "undefined") return;
    const blob = new Blob([content], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },
};
