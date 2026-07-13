"use client";

import type { Project } from "@/services/projects";
import type { ChatMessage } from "@/services/chat";

type Props = {
  project: Project;
  messages: ChatMessage[];
};

const SCORE_REGEX = /###\s*Scoring\s+de\s+Viabilidad\s+Estimado:\s*\*\*(\d+)\/100\*\*/;

function formatDate(iso: string | undefined | null): string {
  if (!iso) return "(fecha desconocida)";
  try {
    return new Date(iso).toLocaleDateString("es-ES", {
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

export default function PdfReportButton({ project, messages }: Props) {
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

    // === HEADER ===
    doc.setFillColor(245, 245, 245);
    doc.rect(0, 0, pw, 40, "F");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(24);
    doc.setTextColor(26, 26, 26);
    doc.text("KIN", ml, 18);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    doc.setTextColor(107, 114, 128);
    doc.text(`Reporte de Proyecto  •  ${reportDate}`, ml, 26);
    doc.setDrawColor(220, 220, 220);
    doc.line(ml, 32, pw - mr, 32);

    // === PROJECT INFO ===
    y = 52;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(16);
    doc.setTextColor(26, 26, 26);
    const titleLines = doc.splitTextToSize(project.title, cw);
    doc.text(titleLines, ml, y);
    y += titleLines.length * 7 + 4;

    if (project.description) {
      doc.setFont("helvetica", "normal");
      doc.setFontSize(9);
      doc.setTextColor(107, 114, 128);
      const descLines = doc.splitTextToSize(project.description, cw);
      doc.text(descLines, ml, y);
      y += descLines.length * 5 + 4;
    }

    // Metadata row
    doc.setFont("helvetica", "normal");
    doc.setFontSize(9);
    doc.setTextColor(55, 65, 81);
    const meta = `Categoría: ${project.category}  |  Estado: ${project.status}  |  Creado: ${formatDate(project.createdAt)}`;
    doc.text(meta, ml, y);
    y += 8;

    doc.setDrawColor(230, 230, 230);
    doc.line(ml, y, pw - mr, y);
    y += 10;

    // === SCORES SECTION ===
    doc.setFont("helvetica", "bold");
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 26);
    doc.text("Métricas del Proyecto", ml, y);
    y += 2;
    doc.setDrawColor(16, 163, 42);
    doc.setLineWidth(1.2);
    doc.line(ml, y, ml + 36, y);
    y += 10;

    // Progress bar
    const progress = project.progressPercentage ?? 0;
    doc.setFont("helvetica", "normal");
    doc.setFontSize(10);
    doc.setTextColor(55, 65, 81);
    doc.text("Progreso del proyecto", ml, y);
    y += 2;
    doc.setFillColor(230, 230, 230);
    doc.roundedRect(ml, y, cw, 12, 3, 3, "F");
    const progColor = progress >= 80 ? "#16a34a" : progress >= 50 ? "#d97706" : "#dc2626";
    doc.setFillColor(progColor);
    doc.roundedRect(ml, y, Math.max((progress / 100) * cw, 4), 12, 3, 3, "F");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(11);
    doc.setTextColor(255, 255, 255);
    doc.text(`${progress}%`, ml + 6, y + 9);
    y += 20;

    // Viability score
    let parsedScore: number | null = null;
    for (const msg of messages) {
      const m = msg.content?.match(SCORE_REGEX);
      if (m) {
        parsedScore = parseInt(m[1], 10);
        break;
      }
    }
    const displayScore = project.viabilityScore != null ? project.viabilityScore : parsedScore;
    doc.setFont("helvetica", "normal");
    doc.setFontSize(10);
    doc.setTextColor(55, 65, 81);
    doc.text("Score de Viabilidad", ml, y);
    y += 2;
    doc.setFillColor(230, 230, 230);
    doc.roundedRect(ml, y, cw, 12, 3, 3, "F");
    if (displayScore != null) {
      const scoreVal = typeof displayScore === "number" ? displayScore : Number(displayScore);
      if (!isNaN(scoreVal)) {
        const scColor = scoreVal >= 70 ? "#16a34a" : scoreVal >= 40 ? "#d97706" : "#dc2626";
        doc.setFillColor(scColor);
        doc.roundedRect(ml, y, Math.max((scoreVal / 100) * cw, 4), 12, 3, 3, "F");
        doc.setFont("helvetica", "bold");
        doc.setFontSize(11);
        doc.setTextColor(255, 255, 255);
        doc.text(`${scoreVal}/100`, ml + 6, y + 9);
      } else {
        doc.setFont("helvetica", "italic");
        doc.setFontSize(9);
        doc.setTextColor(156, 163, 175);
        doc.text("(aún no generado)", ml + 4, y + 9);
      }
    } else {
      doc.setFont("helvetica", "italic");
      doc.setFontSize(9);
      doc.setTextColor(156, 163, 175);
      doc.text("(aún no generado)", ml + 4, y + 9);
    }
    y += 24;

    // === CHAT HISTORY ===
    if (y > 240) {
      doc.addPage();
      y = 20;
    }
    doc.setDrawColor(230, 230, 230);
    doc.line(ml, y, pw - mr, y);
    y += 8;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 26);
    doc.text("Historial de Conversación", ml, y);
    y += 2;
    doc.setDrawColor(16, 163, 42);
    doc.setLineWidth(1.2);
    doc.line(ml, y, ml + 36, y);
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
        const label = isUser ? "USUARIO" : "KIN";
        const time = formatDate(msg.createdAt);
        const prefix = `[${time}] ${label}:`;
        const contentLines = doc.splitTextToSize(msg.content || "", cw - 4);

        const boxH = Math.max(10, 8 + 5 + contentLines.length * 5);
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

    // === FOOTER ===
    const fy = 285;
    doc.setDrawColor(220, 220, 220);
    doc.line(ml, fy, pw - mr, fy);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7);
    doc.setTextColor(156, 163, 175);
    doc.text("Generado por KIN Platform — Knowledge, Innovation & Navigation", ml, fy + 5);
    doc.text(reportDate, pw - mr, fy + 5, { align: "right" });

    doc.save(`KIN_Reporte_${project.title.replace(/\s+/g, "_")}.pdf`);
  };

  return (
    <div className="flex justify-center mt-3 mb-2">
      <button
        onClick={handleClick}
        className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium text-white transition cursor-pointer bg-primary-600 hover:bg-primary-700"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="16"
          height="16"
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
        Descargar Reporte PDF
      </button>
    </div>
  );
}
