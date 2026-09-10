import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProfileEditor from "@/components/health/ProfileEditor";

const { dashboardService } = vi.hoisted(() => ({
  dashboardService: {
    profile: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

vi.mock("@/services/dashboard", async () => {
  const actual = await vi.importActual<typeof import("@/services/dashboard")>(
    "@/services/dashboard",
  );
  return {
    ...actual,
    dashboardService,
  };
});

describe("ProfileEditor", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("carga y muestra el perfil del paciente", async () => {
    dashboardService.profile.mockResolvedValue({
      userId: "u1",
      riskFactors: ["fumador"],
      chronicConditions: ["hipertensión"],
      updatedAt: "2026-08-20T10:00:00Z",
    });

    render(<ProfileEditor />);

    expect(await screen.findByRole("heading", { name: "Factores de riesgo" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "fumador" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByLabelText("Condiciones crónicas")).toHaveValue("hipertensión");
  });

  it("guarda el perfil y muestra confirmación", async () => {
    dashboardService.profile.mockResolvedValue({
      userId: "u1",
      riskFactors: [],
      chronicConditions: [],
      updatedAt: "2026-08-20T10:00:00Z",
    });
    dashboardService.updateProfile.mockResolvedValue({
      userId: "u1",
      riskFactors: ["diabetes"],
      chronicConditions: [],
      updatedAt: "2026-08-20T10:00:00Z",
    });

    const user = userEvent.setup();
    render(<ProfileEditor />);

    await user.click(await screen.findByRole("button", { name: "diabetes" }));
    await user.click(screen.getByRole("button", { name: "Guardar perfil" }));

    expect(await screen.findByText(/Perfil guardado/)).toBeInTheDocument();
    expect(dashboardService.updateProfile).toHaveBeenCalledWith(["diabetes"], []);
  });
});
