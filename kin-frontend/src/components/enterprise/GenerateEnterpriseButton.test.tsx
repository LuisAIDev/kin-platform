import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GenerateEnterpriseButton } from "@/components/enterprise/GenerateEnterpriseButton";
import { enterpriseApi } from "@/services/enterpriseApi";

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock("@/services/enterpriseApi", async () => {
  const actual = await vi.importActual<typeof import("@/services/enterpriseApi")>(
    "@/services/enterpriseApi",
  );
  return {
    ...actual,
    enterpriseApi: { ...actual.enterpriseApi, generate: vi.fn() },
  };
});

describe("GenerateEnterpriseButton", () => {
  beforeEach(() => {
    mockPush.mockReset();
    vi.mocked(enterpriseApi.generate).mockResolvedValue(202);
  });

  it("solicita la generación y navega al dashboard Enterprise", async () => {
    render(<GenerateEnterpriseButton projectId="p1" />);

    await userEvent.click(
      screen.getByRole("button", { name: "Generar Proyecto Empresarial" }),
    );

    expect(enterpriseApi.generate).toHaveBeenCalledWith("p1", true);
    expect(mockPush).toHaveBeenCalledWith("/dashboard/projects/p1/enterprise");
  });

  it("navega al dashboard cuando ya hay una generación en curso (409)", async () => {
    vi.mocked(enterpriseApi.generate).mockResolvedValue(409);

    render(<GenerateEnterpriseButton projectId="p1" />);

    await userEvent.click(
      screen.getByRole("button", { name: "Generar Proyecto Empresarial" }),
    );

    expect(mockPush).toHaveBeenCalledWith("/dashboard/projects/p1/enterprise");
  });

  it("muestra el error del backend junto al botón", async () => {
    vi.mocked(enterpriseApi.generate).mockRejectedValue(
      new Error("Sin contexto de conversación"),
    );

    render(<GenerateEnterpriseButton projectId="p1" />);

    await userEvent.click(
      screen.getByRole("button", { name: "Generar Proyecto Empresarial" }),
    );

    expect(await screen.findByTestId("generate-error")).toHaveTextContent(
      "Sin contexto de conversación",
    );
    expect(mockPush).not.toHaveBeenCalled();
  });
});
