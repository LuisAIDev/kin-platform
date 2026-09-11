import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import EvolutionForm from "@/components/followup/EvolutionForm";

vi.mock("@/services/followup", () => ({
  followUpService: {
    recordEvolution: vi.fn(),
  },
}));

import { followUpService } from "@/services/followup";

describe("EvolutionForm", () => {
  it("registra la evolución con síntomas y signos vitales", async () => {
    const onRecorded = vi.fn();
    const user = userEvent.setup();
    (followUpService.recordEvolution as ReturnType<typeof vi.fn>).mockResolvedValue({});

    render(<EvolutionForm patientId="p1" onRecorded={onRecorded} />);

    await user.type(screen.getByLabelText("Síntomas"), "Mejoría notable");
    await user.type(screen.getByLabelText("Presión arterial"), "120/80");
    await user.click(screen.getByRole("button", { name: "Registrar evolución" }));

    await waitFor(() =>
      expect(followUpService.recordEvolution).toHaveBeenCalledWith(
        "p1",
        "Mejoría notable",
        expect.objectContaining({ presion: "120/80" }),
        true,
        "",
      ),
    );
    expect(onRecorded).toHaveBeenCalled();
  });

  it("muestra error si el servicio falla", async () => {
    const user = userEvent.setup();
    (followUpService.recordEvolution as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("La relación no está activa"),
    );

    render(<EvolutionForm patientId="p1" onRecorded={vi.fn()} />);

    await user.type(screen.getByLabelText("Síntomas"), "Dolor");
    await user.click(screen.getByRole("button", { name: "Registrar evolución" }));

    await waitFor(() => expect(screen.getByText(/La relación no está activa/)).toBeInTheDocument());
  });
});
