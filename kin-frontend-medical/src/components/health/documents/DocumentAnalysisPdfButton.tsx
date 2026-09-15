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

    const PAGE_HEIGHT = doc.internal.pageSize.getHeight();
    const PAGE_WIDTH = doc.internal.pageSize.getWidth();
    const MARGIN_TOP = 20;
    const MARGIN_BOTTOM = 20;
    const MARGIN_LEFT = 15;
    const MARGIN_RIGHT = 15;
    const LINE_HEIGHT = 6;

    let y = MARGIN_TOP;

    /**
     * Verifica si el contenido cabe en la página actual.
     * Si no cabe, agrega una nueva página y reinicia y = MARGIN_TOP.
     * @param linesToAdd Número de líneas que se van a dibujar
     */
    function checkPageBreak(linesToAdd: number) {
        const requiredSpace = linesToAdd * LINE_HEIGHT;
        const availableSpace = PAGE_HEIGHT - MARGIN_BOTTOM;
        const willBreak = y + requiredSpace > availableSpace;
        console.log(`[PDF] checkPageBreak: y=${y}, required=${requiredSpace}, available=${availableSpace}, willBreak=${willBreak}, linesToAdd=${linesToAdd}`);
        if (willBreak) {
            console.log(`[PDF] ADDING PAGE. New y=${MARGIN_TOP}, page=${doc.getNumberOfPages() + 1}`);
            doc.addPage();
            y = MARGIN_TOP;
        }
    }

    /**
     * Dibuja un array de líneas respetando saltos de página.
     * @param lines Array de strings (ya procesados con splitTextToSize)
     * @param x Posición X
     */
    function drawLines(lines: string[], x: number = MARGIN_LEFT) {
        for (const line of lines) {
            checkPageBreak(1);
            doc.text(line, x, y);
            y += LINE_HEIGHT;
        }
    }

    /**
     * Dibuja un texto con wrap automático respetando saltos de página.
     */
    function drawText(text: string, x: number = MARGIN_LEFT, maxWidth?: number) {
        const w = maxWidth ?? (PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
        const lines = doc.splitTextToSize(text, w);
        drawLines(lines, x);
    }
    const reportDate = new Date().toLocaleDateString("es-ES", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });

    doc.setFillColor(245, 245, 245);
    doc.rect(0, 0, PAGE_WIDTH, 40, "F");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(22);
    doc.setTextColor(26, 26, 26);
    doc.text("KIN Salud", MARGIN_LEFT, 18);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    doc.setTextColor(107, 114, 128);
    doc.text(`Análisis de Documento Clínico  •  ${reportDate}`, MARGIN_LEFT, 26);
    doc.setDrawColor(220, 220, 220);
    doc.line(MARGIN_LEFT, 32, PAGE_WIDTH - MARGIN_RIGHT, 32);

    y += 10;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(15);
    doc.setTextColor(26, 26, 26);
    console.log(`[PDF] Before title: y=${y}`);
    drawText(document.fileName, MARGIN_LEFT);
    y += 5;
    console.log(`[PDF] After title: y=${y}`);

    doc.setFont("helvetica", "normal");
    doc.setFontSize(9);
    doc.setTextColor(107, 114, 128);
    const meta = `${formatSize(document.fileSize)}  •  ${formatDate(document.uploadedAt)}`;
    checkPageBreak(1);
    console.log(`[PDF] Before meta: y=${y}`);
    doc.text(meta, MARGIN_LEFT, y);
    y += LINE_HEIGHT;
    if (document.description) {
      console.log(`[PDF] Before description: y=${y}`);
      drawText(`Descripción: ${document.description}`, MARGIN_LEFT);
      y += 5;
      console.log(`[PDF] After description: y=${y}`);
    }

    doc.setFont("helvetica", "italic");
    doc.setFontSize(8);
    doc.setTextColor(180, 83, 9);
    const disclaimerLines = [
      "Analisis procesado por KIN Medical.",
      "",
      "Las decisiones clinicas las toma un motor de reglas fijas que aplica guias",
      "medicas verificables. Trabajamos con datos reales de fuentes publicas y",
      "confiables: la Organizacion Mundial de la Salud (OMS), clasificaciones",
      "internacionales de enfermedades (CIE-10), rangos de referencia de",
      "laboratorios y protocolos clinicos.",
      "",
      "Que significa esto? Que cada vez que analizamos tus documentos medicos,",
      "aplicamos las mismas reglas medicas validadas, sin improvisaciones ni",
      "variaciones. La inteligencia artificial solo se encarga de redactar el",
      "resultado en palabras sencillas para ti.",
      "",
      "Este informe es de apoyo informativo y no sustituye la evaluacion ni el",
      "diagnostico de un profesional de la salud. Siempre consulta a tu medico.",
    ];
    console.log(`[PDF] Before disclaimer: y=${y}, lines=${disclaimerLines.length}`);
    for (const line of disclaimerLines) {
      if (line === "") {
        y += 3;
        continue;
      }
      checkPageBreak(1);
      doc.text(line, MARGIN_LEFT, y);
      y += LINE_HEIGHT;
    }
    console.log(`[PDF] After disclaimer: y=${y}`);
    y += 8;

    doc.setDrawColor(230, 230, 230);
    checkPageBreak(1);
    console.log(`[PDF] Before conversation header: y=${y}`);
    doc.line(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y);
    y += 10;

    doc.setFont("helvetica", "bold");
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 26);
    checkPageBreak(1);
    doc.text("Conversación de análisis", MARGIN_LEFT, y);
    y += 2;
    doc.setDrawColor(16, 163, 42);
    doc.setLineWidth(1.2);
    checkPageBreak(1);
    doc.line(MARGIN_LEFT, y, MARGIN_LEFT + 40, y);
    y += 10;
    console.log(`[PDF] After conversation header: y=${y}`);

    if (messages.length === 0) {
      doc.setFont("helvetica", "italic");
      doc.setFontSize(9);
      doc.setTextColor(156, 163, 175);
      checkPageBreak(1);
      doc.text("(aún no hay mensajes en esta conversación)", MARGIN_LEFT, y);
      y += 10;
    } else {
      const cw = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
      for (let msgIdx = 0; msgIdx < messages.length; msgIdx++) {
        const msg = messages[msgIdx];
        const isUser = msg.role === "USER";
        const label = isUser ? "TÚ" : "ASISTENTE KIN";
        const time = formatDate(msg.createdAt);
        const prefix = `[${time}] ${label}:`;
        const sanitized = sanitizeForPdf(msg.content || "");
        const contentLines = doc.splitTextToSize(sanitized, cw - 6);
        // Post-sanitize: jspdf corrompe Unicode, limpiamos las líneas resultantes
        const cleanLines = sanitizeLinesAfterJspdf(contentLines);

        console.log(`[PDF] Message ${msgIdx} (${label}): y=${y}, lines=${cleanLines.length}, boxH=${cleanLines.length * LINE_HEIGHT + 15}`);

        const boxH = cleanLines.length * LINE_HEIGHT + 15;
        checkPageBreak(cleanLines.length + 2);

        doc.setFillColor(isUser ? 239 : 249, isUser ? 244 : 250, isUser ? 255 : 251);
        doc.roundedRect(MARGIN_LEFT, y, cw, boxH, 2, 2, "F");

        doc.setFont("helvetica", "bold");
        doc.setFontSize(7);
        doc.setTextColor(isUser ? 37 : 22, isUser ? 99 : 101, isUser ? 235 : 52);
        checkPageBreak(1);
        doc.text(prefix, MARGIN_LEFT + 3, y + 5);

        doc.setFont("helvetica", "normal");
        doc.setFontSize(8.5);
        doc.setTextColor(55, 65, 81);
        const boxY = y + 12;
        for (let i = 0; i < cleanLines.length; i++) {
          doc.text(cleanLines[i], MARGIN_LEFT + 3, boxY + i * LINE_HEIGHT);
        }

        y += boxH + 4;
        console.log(`[PDF] After message ${msgIdx}: y=${y}`);
      }
    }

    const totalPages = doc.getNumberOfPages();
    console.log(`[PDF] Before footer: totalPages=${totalPages}, final y=${y}`);
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      doc.setDrawColor(220, 220, 220);
      doc.line(MARGIN_LEFT, PAGE_HEIGHT - 15, PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 15);
      doc.setFont("helvetica", "normal");
      doc.setFontSize(7);
      doc.setTextColor(156, 163, 175);
      doc.text("Generado por KIN Platform — Knowledge, Innovation & Navigation", MARGIN_LEFT, PAGE_HEIGHT - 8);
      doc.text(`Página ${i} de ${totalPages}  •  ${reportDate}`, PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 8, { align: "right" });
    }
    console.log(`[PDF] After footer: done`);

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