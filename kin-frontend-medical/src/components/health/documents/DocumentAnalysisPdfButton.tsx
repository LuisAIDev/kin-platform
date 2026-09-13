"use client";

import type { ClinicalDocument } from "@/services/documents";
import type { DocumentChatMessage } from "@/services/documentChat";

type Props = {
  document: ClinicalDocument;
  messages: DocumentChatMessage[];
};

function formatDate(iso: string | undefined | null): string {
  if (!iso) return "(fecha desconocida)";
  try {
    return new Date(iso).toLocaleString("es-ES", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return "(fecha inválida)";
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function sanitizeName(name: string): string {
  return name.replace(/\s+/g, "_").replace(/[^a-zA-Z0-9_ÁÉÍÓÚáéíóúñÑ.\-]/g, "");
}

/**
 * Sanitiza el texto ANTES de pasarlo a jspdf: elimina markdown y Unicode problemático.
 */
function sanitizeForPdf(text: string): string {
  if (!text) return "";
  return text
    // Eliminar markdown residual
    .replace(/\*\*/g, "") // **negrita**
    .replace(/\*/g, "") // *cursiva*
    .replace(/^#+\s*/gm, "") // # encabezados
    .replace(/\|/g, "") // tablas |
    .replace(/&b/gi, "") // artefactos &b
    .replace(/&[a-z]+;/g, "") // entidades HTML
    // Reemplazar Unicode problemático por equivalentes ASCII
    .replace(/[•●▪→]/g, "-") // bullets
    .replace(/[≥]/g, ">=")
    .replace(/[≤]/g, "<=")
    .replace(/[→]/g, "->")
    .replace(/[×]/g, "x")
    .replace(/[≥]/g, ">=")
    .replace(/[≤]/g, "<=")
    .replace(/[×]/g, "x")
    .replace(/[•]/g, "-")
    .replace(/[●]/g, "-")
    .replace(/[▪]/g, "-")
    .replace(/[→]/g, "->")
    .replace(/[≥]/g, ">=")
    .replace(/[≤]/g, "<=")
    .replace(/[×]/g, "x")
    .replace(/[€]/g, "EUR")
    .replace(/[™]/g, "(TM)")
    .replace(/[©]/g, "(c)")
    .replace(/[®]/g, "(R)")
    // Eliminar artefactos &b, &p, etc.
    .replace(/&\s*[a-z]+\s*;?/gi, "")
    // Eliminar comillas raras y caracteres de control
    .replace(/['"`´`]/g, "'")
    .replace(/[\x00-\x08\x0B-\x0C\x0E-\x1F]/g, "")
    // Normalizar saltos de línea
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .trim();
}

/**
 * Sanitiza líneas DESPUÉS de que jspdf las procese con splitTextToSize.
 * jspdf corrompe Unicode (convierte ≥ a &b, • a &b, etc).
 * Esta función limpia los residuos que jspdf deja.
 */
function sanitizeLinesAfterJspdf(lines: string[]): string[] {
  return lines.map((line) =>
    line
      // Artefactos que jspdf deja al renderizar Unicode
      .replace(/&\s*b\b/gi, "") // &b
      .replace(/&\s*p\b/gi, "") // &p
      .replace(/&[a-z]{1,4}\b/gi, "") // &xxx
      // Caracteres corruptos comunes que deja jspdf
      .replace(/["'`´`]/g, "'")
      .replace(/[^\x20-\x7E\u00C0-\u017F\n]/g, "") // Solo ASCII + latinos básicos
      .trim()
  );
}

export default function DocumentAnalysisPdfButton({
  document,
  messages,
}: Props) {
  const handleClick = async () => {
    const { default: jsPDF } = await import("jspdf");
    const doc = new jsPDF("p", "mm", "a4");

    const pw = doc.internal.pageSize.getWidth();
    const ml = 20;
    const mr = 20;
    const cw = pw - ml - mr;
    let y = 20;
    const reportDate = new Date().toLocaleDateString("es-ES", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });

    doc.setFillColor(245, 245, 245);
    doc.rect(0, 0, pw, 40, "F");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(22);
    doc.setTextColor(26, 26, 26);
    doc.text("KIN Salud", ml, 18);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    doc.setTextColor(107, 114, 128);
    doc.text(`Análisis de Documento Clínico  •  ${reportDate}`, ml, 26);
    doc.setDrawColor(220, 220, 220);
    doc.line(ml, 32, pw - mr, 32);

    y = 50;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(15);
    doc.setTextColor(26, 26, 26);
    const titleLines = doc.splitTextToSize(document.fileName, cw);
    doc.text(titleLines, ml, y);
    y += titleLines.length * 7 + 4;

    doc.setFont("helvetica", "normal");
    doc.setFontSize(9);
    doc.setTextColor(107, 114, 128);
    const meta = `${formatSize(document.fileSize)}  •  ${formatDate(document.uploadedAt)}`;
    doc.text(meta, ml, y);
    y += 6;
    if (document.description) {
      const descLines = doc.splitTextToSize(`Descripción: ${document.description}`, cw);
      doc.text(descLines, ml, y);
      y += descLines.length * 5 + 4;
    }

    doc.setFont("helvetica", "italic");
    doc.setFontSize(8);
    doc.setTextColor(180, 83, 9);
    const disclaimerLines = doc.splitTextToSize(
      "Este documento es una interpretación automática generada por IA. No sustituye la evaluación ni el diagnóstico de un profesional de la salud. Consulta a tu médico para interpretar tus resultados.",
      cw,
    );
    doc.text(disclaimerLines, ml, y);
    y += disclaimerLines.length * 5 + 8;

    doc.setDrawColor(230, 230, 230);
    doc.line(ml, y, pw - mr, y);
    y += 10;

    doc.setFont("helvetica", "bold");
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 26);
    doc.text("Conversación de análisis", ml, y);
    y += 2;
    doc.setDrawColor(16, 163, 42);
    doc.setLineWidth(1.2);
    doc.line(ml, y, ml + 40, y);
    y += 10;

    if (messages.length === 0) {
      doc.setFont("helvetica", "italic");
      doc.setFontSize(9);
      doc.setTextColor(156, 163, 175);
      doc.text("(aún no hay mensajes en esta conversación)", ml, y);
      y += 10;
    } else {
      for (const msg of messages) {
        const isUser = msg.role === "USER";
        const label = isUser ? "TÚ" : "ASISTENTE KIN";
        const time = formatDate(msg.createdAt);
        const prefix = `[${time}] ${label}:`;
        const sanitized = sanitizeForPdf(msg.content || "");
        let contentLines = doc.splitTextToSize(sanitized, cw - 4);
        // Post-sanitize: jspdf corrompe Unicode, limpiamos las líneas resultantes
        contentLines = sanitizeLinesAfterJspdf(contentLines);

        const boxH = Math.max(12, 8 + 5 + contentLines.length * 5);
        if (y + boxH > 275) {
          doc.addPage();
          y = 20;
        }

        doc.setFillColor(isUser ? 239 : 249, isUser ? 244 : 250, isUser ? 255 : 251);
        doc.roundedRect(ml, y, cw, boxH, 2, 2, "F");

        doc.setFont("helvetica", "bold");
        doc.setFontSize(7);
        doc.setTextColor(isUser ? 37 : 22, isUser ? 99 : 101, isUser ? 235 : 52);
        doc.text(prefix, ml + 3, y + 5);

        doc.setFont("helvetica", "normal");
        doc.setFontSize(8.5);
        doc.setTextColor(55, 65, 81);
        doc.text(contentLines, ml + 3, y + 12);

        y += boxH + 4;
      }
    }

    const fy = 285;
    doc.setDrawColor(220, 220, 220);
    doc.line(ml, fy, pw - mr, fy);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7);
    doc.setTextColor(156, 163, 175);
    doc.text("Generado por KIN Platform — Knowledge, Innovation & Navigation", ml, fy + 5);
    doc.text(reportDate, pw - mr, fy + 5, { align: "right" });

    const base = sanitizeName(document.fileName).replace(/\.\w+$/, "") || "documento";
    doc.save(`KIN_Analisis_${base}.pdf`);
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={messages.length === 0}
      className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 px-3 py-1.5 text-xs font-medium text-neutral-700 hover:bg-neutral-50 transition disabled:cursor-not-allowed disabled:opacity-50"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <polyline points="14 2 14 8 20 8" />
        <line x1="16" y1="13" x2="8" y2="13" />
        <line x1="16" y1="17" x2="8" y2="17" />
        <polyline points="10 9 9 9 8 9" />
      </svg>
      Descargar PDF del análisis
    </button>
  );
}