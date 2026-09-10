import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import HistoryList from "@/components/health/HistoryList";

const ENTRY = {
  id: "c1",
  symptoms: ["fiebre", "tos"],
  results: [
    {
      conditionId: "x1",
      condition: "Gripe",
      description: "",
      probability: 0.8,
      severity: "MODERADO" as const,
      urgency: "MEDIA" as const,
      recommendation: "Consulta médica.",
      matchedSymptoms: ["fiebre"],
    },
  ],
  createdAt: "2026-08-20T10:00:00Z",
};

const PAGE = {
  content: [ENTRY],
  totalElements: 1,
  totalPages: 1,
  currentPage: 0,
  size: 10,
};

describe("HistoryList", () => {
  it("muestra las consultas y sus condiciones", () => {
    render(<HistoryList page={PAGE} onPageChange={vi.fn()} onSelect={vi.fn()} />);

    expect(screen.getByText("fiebre, tos")).toBeInTheDocument();
    expect(screen.getByText("Gripe")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Ver detalle" })).toBeInTheDocument();
  });

  it("abre el detalle al pulsar Ver detalle", async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<HistoryList page={PAGE} onPageChange={vi.fn()} onSelect={onSelect} />);

    await user.click(screen.getByRole("button", { name: "Ver detalle" }));

    expect(onSelect).toHaveBeenCalledWith(ENTRY);
  });

  it("muestra estado vacío sin consultas", () => {
    render(
      <HistoryList
        page={{ content: [], totalElements: 0, totalPages: 0, currentPage: 0, size: 10 }}
        onPageChange={vi.fn()}
        onSelect={vi.fn()}
      />,
    );

    expect(screen.getByText(/Aún no hay consultas de triaje/)).toBeInTheDocument();
  });
});
