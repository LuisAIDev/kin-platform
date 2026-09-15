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
    const d = new Date(iso);
    if (isNaN(d.getTime())) return "(fecha invalida)";
    
    const day = String(d.getDate()).padStart(2, "0");
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, "0");
    const minutes = String(d.getMinutes()).padStart(2, "0");
    
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  } catch {
    return "(fecha invalida)";
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
 * Sanitiza el texto ANTES de pasarlo a jspdf: elimina markdown y TODO Unicode no-ASCII.
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
    // Reemplazos específicos con equivalente ASCII
    .replace(/[≥]/g, ">=")
    .replace(/[≤]/g, "<=")
    .replace(/[→]/g, "->")
    .replace(/[←]/g, "<-")
    .replace(/[×]/g, "x")
    .replace(/[•●▪◦]/g, "-")
    .replace(/[—–]/g, "-")
    .replace(/[…]/g, "...")
    .replace(/[""]/g, '"')
    .replace(/['']/g, "'")
    // Tildes -> sin tilde (conservar la letra base)
    .replace(/[áàäâã]/gi, (c) => c === c.toUpperCase() ? "A" : "a")
    .replace(/[éèëê]/gi, (c) => c === c.toUpperCase() ? "E" : "e")
    .replace(/[íìïî]/gi, (c) => c === c.toUpperCase() ? "I" : "i")
    .replace(/[óòöôõ]/gi, (c) => c === c.toUpperCase() ? "O" : "o")
    .replace(/[úùüû]/gi, (c) => c === c.toUpperCase() ? "U" : "u")
    .replace(/[ñ]/g, "n")
    .replace(/[Ñ]/g, "N")
    .replace(/[ç]/g, "c")
    .replace(/[Ç]/g, "C")
    // Eliminar cualquier otro carácter fuera de ASCII imprimible
    .replace(/[^\x20-\x7E\n]/g, "")
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
      .replace(/[^\x20-\x7E\n]/g, "") // Solo ASCII imprimible
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
        if (y + requiredSpace > availableSpace) {
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
        const sanitized = sanitizeForPdf(text);
        const w = maxWidth ?? (PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT);
        const lines = doc.splitTextToSize(sanitized, w);
        drawLines(lines, x);
    }
    const _d = new Date();
    const reportDate = `${String(_d.getDate()).padStart(2, "0")}/${String(_d.getMonth() + 1).padStart(2, "0")}/${_d.getFullYear()}`;

    doc.setFillColor(245, 245, 245);
    doc.rect(0, 0, PAGE_WIDTH, 40, "F");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(22);
    doc.setTextColor(26, 26, 26);
    doc.text(sanitizeForPdf("KIN Salud"), MARGIN_LEFT, 18);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    doc.setTextColor(107, 114, 128);
    doc.text(sanitizeForPdf(`Análisis de Documento Clínico  •  ${reportDate}`), MARGIN_LEFT, 26);
    doc.setDrawColor(220, 220, 220);
    doc.line(MARGIN_LEFT, 32, PAGE_WIDTH - MARGIN_RIGHT, 32);

    y += 10;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(15);
    doc.setTextColor(26, 26, 26);
    drawText(document.fileName, MARGIN_LEFT);
    y += 5;

    doc.setFont("helvetica", "normal");
    doc.setFontSize(9);
    doc.setTextColor(107, 114, 128);
    const meta = `${formatSize(document.fileSize)}  •  ${formatDate(document.uploadedAt)}`;
    checkPageBreak(1);
    doc.text(sanitizeForPdf(meta), MARGIN_LEFT, y);
    y += LINE_HEIGHT;
    if (document.description) {
      drawText(`Descripción: ${document.description}`, MARGIN_LEFT);
      y += 5;
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
    for (const line of disclaimerLines) {
      if (line === "") {
        y += 3;
        continue;
      }
      checkPageBreak(1);
      doc.text(sanitizeForPdf(line), MARGIN_LEFT, y);
      y += LINE_HEIGHT;
    }
    y += 8;

    doc.setDrawColor(230, 230, 230);
    checkPageBreak(1);
    doc.line(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y);
    y += 10;

    doc.setFont("helvetica", "bold");
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 26);
    checkPageBreak(1);
    doc.text(sanitizeForPdf("Conversación de análisis"), MARGIN_LEFT, y);
    y += 2;
    doc.setDrawColor(16, 163, 42);
    doc.setLineWidth(1.2);
    checkPageBreak(1);
    doc.line(MARGIN_LEFT, y, MARGIN_LEFT + 40, y);
    y += 10;

    if (messages.length === 0) {
      doc.setFont("helvetica", "italic");
      doc.setFontSize(9);
      doc.setTextColor(156, 163, 175);
      checkPageBreak(1);
      doc.text(sanitizeForPdf("(aún no hay mensajes en esta conversación)"), MARGIN_LEFT, y);
      y += 10;
    } else {
      const cw = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
      for (let msgIdx = 0; msgIdx < messages.length; msgIdx++) {
        const msg = messages[msgIdx];
        const isUser = msg.role === "USER";
        const label = isUser ? "TU" : "ASISTENTE KIN";
        const time = sanitizeForPdf(formatDate(msg.createdAt));
        const prefix = `[${time}] ${label}:`;
        const sanitized = sanitizeForPdf(msg.content || "");
        const contentLines = doc.splitTextToSize(sanitized, cw - 6);
        // Post-sanitize: jspdf corrompe Unicode, limpiamos las líneas resultantes
        const cleanLines = sanitizeLinesAfterJspdf(contentLines);

        const boxH = cleanLines.length * LINE_HEIGHT + 15;
        checkPageBreak(cleanLines.length + 2);

        doc.setFont("helvetica", "bold");
        doc.setFontSize(7);
        doc.setTextColor(isUser ? 37 : 22, isUser ? 99 : 101, isUser ? 235 : 52);
        checkPageBreak(1);
        doc.text(prefix, MARGIN_LEFT + 3, y + 5);

        doc.setFont("helvetica", "normal");
        doc.setFontSize(8.5);
        doc.setTextColor(55, 65, 81);
        let lineY = y + 12;
        for (let i = 0; i < cleanLines.length; i++) {
          checkPageBreak(1);
          lineY = y + 12;
          doc.text(cleanLines[i], MARGIN_LEFT + 3, lineY);
          y += LINE_HEIGHT;
          lineY += LINE_HEIGHT;
        }

        y += 4;
      }
    }

    const totalPages = doc.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      doc.setPage(i);
      doc.setDrawColor(220, 220, 220);
      doc.line(MARGIN_LEFT, PAGE_HEIGHT - 15, PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 15);
      doc.setFont("helvetica", "normal");
      doc.setFontSize(7);
      doc.setTextColor(156, 163, 175);
      doc.text(sanitizeForPdf("Generado por KIN Platform — Knowledge, Innovation & Navigation"), MARGIN_LEFT, PAGE_HEIGHT - 8);
      doc.text(sanitizeForPdf(`Página ${i} de ${totalPages}  •  ${reportDate}`), PAGE_WIDTH - MARGIN_RIGHT, PAGE_HEIGHT - 8, { align: "right" });
    }

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