import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InvestmentMemoButton } from "./InvestmentMemoButton";
import { investmentMemoService } from "@/services/investmentMemo";

vi.mock("@/services/investmentMemo", () => ({
  investmentMemoService: {
    get: vi.fn(),
  },
}));

describe("InvestmentMemoButton", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("carga el documento, lo renderiza y reutiliza el resultado al reabrir", async () => {
    vi.mocked(investmentMemoService.get).mockResolvedValue("# Oportunidad\n\nTexto del memo");
    const user = userEvent.setup();

    render(<InvestmentMemoButton projectId="p1" projectTitle="Proyecto" />);

    await user.click(
      screen.getByRole("button", { name: /ver documento de inversión/i })
    );

    expect(
      await screen.findByRole("dialog", {}, { timeout: 5000 })
    ).toBeInTheDocument();
    expect(
      await screen.findByRole("heading", { name: "Oportunidad" }, { timeout: 5000 })
    ).toBeInTheDocument();
    expect(investmentMemoService.get).toHaveBeenCalledTimes(1);
    expect(investmentMemoService.get).toHaveBeenCalledWith("p1");

    await user.click(
      screen.getByRole("button", { name: /cerrar documento de inversión/i })
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: /ver documento de inversión/i })
    );
    expect(
      await screen.findByRole("dialog", {}, { timeout: 5000 })
    ).toBeInTheDocument();
    expect(investmentMemoService.get).toHaveBeenCalledTimes(1);
  });

  it("muestra el error y permite reintentar", async () => {
    vi.mocked(investmentMemoService.get)
      .mockRejectedValueOnce(new Error("Cuota de IA agotada"))
      .mockResolvedValueOnce("# Recuperado");
    const user = userEvent.setup();

    render(<InvestmentMemoButton projectId="p2" projectTitle="Proyecto 2" />);

    await user.click(
      screen.getByRole("button", { name: /ver documento de inversión/i })
    );

    expect(await screen.findByRole("alert", {}, { timeout: 5000 })).toHaveTextContent(
      "Cuota de IA agotada"
    );

    await user.click(screen.getByRole("button", { name: /reintentar/i }));

    expect(
      await screen.findByRole("heading", { name: "Recuperado" }, { timeout: 5000 })
    ).toBeInTheDocument();
    expect(investmentMemoService.get).toHaveBeenCalledTimes(2);
  });
});
