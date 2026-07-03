"use client";

type Props = {
  projectTitle: string;
  content: string;
};

const SECTIONS = ["Problema", "Solución", "Clientes", "Costos"];

function parseReport(content: string) {
  const sections: Record<string, string> = {};
  for (const s of SECTIONS) {
    const re = new RegExp(
      `\\*\\*${s}\\:\\*\\*\\s*([^]*?)(?=\\n\\*\\*|\\n---|$)`
    );
    const m = content.match(re);
    if (m) sections[s] = m[1].trim();
  }

  const scoreMatch = content.match(/\*\*(\d+)\/100\*\*/);
  const score = scoreMatch ? parseInt(scoreMatch[1], 10) : null;

  const recoLines: string[] = [];
  const recoRe = content.match(
    /recomendaría[^]*?(?:\n|$)((?:.*?\d\..*?(?:\n|$))+)/
  );
  if (recoRe) {
    for (const line of recoRe[1].split("\n")) {
      const t = line.trim();
      if (/^\d+\.\s/.test(t)) recoLines.push(t.replace(/^\d+\.\s*/, ""));
    }
  }

  return { sections, score, recoLines };
}

export default function PdfReportButton({ projectTitle, content }: Props) {
  const scoreMatch = content.match(/\*\*(\d+)\/100\*\*/);
  const score = scoreMatch ? parseInt(scoreMatch[1], 10) : null;
  const btnColor =
    score !== null && score >= 70
      ? "bg-green-600 hover:bg-green-700"
      : score !== null && score >= 40
        ? "bg-amber-500 hover:bg-amber-600"
        : "bg-red-500 hover:bg-red-600";

  const handleClick = async () => {
    const { default: jsPDF } = await import("jspdf");
    const doc = new jsPDF("p", "mm", "a4");
    const { sections, score: s, recoLines } = parseReport(content);

    const pw = doc.internal.pageSize.getWidth();
    const ml = 22;
    const mr = 22;
    const cw = pw - ml - mr;
    let y = 24;
    const accent =
      s !== null && s >= 70
        ? "#16a34a"
        : s !== null && s >= 40
          ? "#d97706"
          : "#dc2626";
    const dateStr = new Date().toLocaleDateString("es-ES", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });

    // Header bar
    doc.setFillColor(245, 245, 245);
    doc.rect(0, 0, pw, 42, "F");
    doc.setFont("helvetica", "bold");
    doc.setFontSize(22);
    doc.setTextColor(26, 26, 26);
    doc.text("KIN", ml, 18);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    doc.setTextColor(107, 114, 128);
    doc.text(`Reporte de Viabilidad  •  ${dateStr}`, ml, 26);
    doc.setDrawColor(220, 220, 220);
    doc.line(ml, 30, pw - mr, 30);

    // Project title
    y = 53;
    doc.setFont("helvetica", "normal");
    doc.setFontSize(13);
    doc.setTextColor(107, 114, 128);
    doc.text("PROYECTO", ml, y);
    y += 7;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(17);
    doc.setTextColor(26, 26, 26);
    const titleLines = doc.splitTextToSize(projectTitle, cw);
    doc.text(titleLines, ml, y);
    y += titleLines.length * 7 + 6;

    doc.setDrawColor(230, 230, 230);
    doc.line(ml, y, pw - mr, y);
    y += 10;

    // Executive Summary
    doc.setFont("helvetica", "bold");
    doc.setFontSize(12);
    doc.setTextColor(26, 26, 26);
    doc.text("Resumen Ejecutivo", ml, y);
    y += 2;
    doc.setDrawColor(accent);
    doc.setLineWidth(1.2);
    doc.line(ml, y, ml + 40, y);
    y += 8;

    const hasContent = SECTIONS.some((sec) => sections[sec]);
    if (hasContent) {
      for (const sec of SECTIONS) {
        const val = sections[sec];
        if (!val) continue;
        const lines = doc.splitTextToSize(val, cw - 48);
        const boxH = Math.max(14, 11 + lines.length * 5);
        if (y + boxH > 275) {
          doc.addPage();
          y = 24;
        }
        doc.setFillColor(249, 250, 251);
        doc.roundedRect(ml, y, cw, boxH, 2, 2, "F");
        doc.setFont("helvetica", "bold");
        doc.setFontSize(8);
        doc.setTextColor(107, 114, 128);
        doc.text(sec.toUpperCase(), ml + 4, y + 5);
        doc.setFont("helvetica", "normal");
        doc.setFontSize(9.5);
        doc.setTextColor(55, 65, 81);
        doc.text(lines, ml + 4, y + 12);
        y += boxH + 5;
      }
    } else {
      doc.setFont("helvetica", "italic");
      doc.setFontSize(9.5);
      doc.setTextColor(107, 114, 128);
      doc.text("(Los hallazgos del resumen ejecutivo se muestran en el contenido del chat)", ml, y + 2);
      y += 14;
    }

    // Score section
    if (y + 48 > 270) {
      doc.addPage();
      y = 24;
    }
    y += 4;
    doc.setDrawColor(230, 230, 230);
    doc.line(ml, y, pw - mr, y);
    y += 8;
    doc.setFont("helvetica", "bold");
    doc.setFontSize(12);
    doc.setTextColor(26, 26, 26);
    doc.text("Scoring de Viabilidad", ml, y);
    y += 2;
    doc.setDrawColor(accent);
    doc.setLineWidth(1.2);
    doc.line(ml, y, ml + 40, y);
    y += 10;

    doc.setFillColor(230, 230, 230);
    doc.roundedRect(ml, y, cw, 14, 3, 3, "F");
    if (s !== null) {
      doc.setFillColor(accent);
      doc.roundedRect(ml, y, Math.max((s / 100) * cw, 4), 14, 3, 3, "F");
    }
    doc.setFont("helvetica", "bold");
    doc.setFontSize(13);
    doc.setTextColor(255, 255, 255);
    doc.text(s !== null ? `${s}/100` : "—", ml + 6, y + 10);

    let label = "";
    let labelColor = "";
    if (s !== null) {
      if (s >= 70) {
        label = "ALTO";
        labelColor = "#16a34a";
      } else if (s >= 40) {
        label = "MEDIO";
        labelColor = "#d97706";
      } else {
        label = "BAJO";
        labelColor = "#dc2626";
      }
    }
    doc.setFont("helvetica", "bold");
    doc.setFontSize(10);
    doc.setTextColor(labelColor);
    doc.text(label, pw - mr, y + 11, { align: "right" });
    y += 26;

    // Recommendations
    if (recoLines.length > 0) {
      if (y + 12 + recoLines.length * 7 > 275) {
        doc.addPage();
        y = 24;
      }
      doc.setFont("helvetica", "bold");
      doc.setFontSize(12);
      doc.setTextColor(26, 26, 26);
      doc.text("Recomendaciones", ml, y);
      y += 2;
      doc.setDrawColor(accent);
      doc.setLineWidth(1.2);
      doc.line(ml, y, ml + 40, y);
      y += 9;
      for (let i = 0; i < recoLines.length; i++) {
        const lines = doc.splitTextToSize(
          `${i + 1}.  ${recoLines[i]}`,
          cw - 6
        );
        doc.setFont("helvetica", "normal");
        doc.setFontSize(9.5);
        doc.setTextColor(55, 65, 81);
        doc.text(lines, ml + 3, y);
        y += lines.length * 5 + 4;
      }
    }

    // Footer
    const fy = 285;
    doc.setDrawColor(220, 220, 220);
    doc.line(ml, fy, pw - mr, fy);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.5);
    doc.setTextColor(156, 163, 175);
    doc.text(
      "Generado por KIN Platform — Knowledge, Innovation & Navigation",
      ml,
      fy + 5
    );
    doc.text(dateStr, pw - mr, fy + 5, { align: "right" });

    doc.save(`KIN_Reporte_${projectTitle.replace(/\s+/g, "_")}.pdf`);
  };

  return (
    <div className="flex justify-center mt-3 mb-2">
      <button
        onClick={handleClick}
        className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium text-white transition cursor-pointer ${btnColor}`}
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
