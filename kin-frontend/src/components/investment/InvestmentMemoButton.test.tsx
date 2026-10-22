import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InvestmentMemoButton } from "./InvestmentMemoButton";
import { investmentMemoService } from "@/services/investmentMemo";

const { pdfSaveMock, pdfFromMock, pdfSetMock } = vi.hoisted(() => {
  const pdfSaveMock = vi.fn<() => Promise<void>>();
  const pdfFromMock = vi.fn<(node: HTMLElement) => { save: typeof pdfSaveMock }>(
    () => ({ save: pdfSaveMock })
  );
  const pdfSetMock = vi.fn<
    (options: unknown) => { from: typeof pdfFromMock }
  >(() => ({ from: pdfFromMock }));
  return { pdfSaveMock, pdfFromMock, pdfSetMock };
});

vi.mock("@/services/investmentMemo", () => ({
  investmentMemoService: {
    get: vi.fn(),
  },
}));

vi.mock("html2pdf.js", () => ({
  default: vi.fn(() => ({ set: pdfSetMock })),
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

  it("genera el PDF con fondo blanco y sin los controles del modal", async () => {
    vi.mocked(investmentMemoService.get).mockResolvedValue(
      "# Oportunidad\n\nTexto del memo"
    );
    let resolveSave: (() => void) | undefined;
    pdfSaveMock.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveSave = resolve;
        })
    );
    const user = userEvent.setup();

    render(<InvestmentMemoButton projectId="p3" projectTitle="Mi Proyecto" />);
    await user.click(
      screen.getByRole("button", { name: /ver documento de inversión/i })
    );
    await screen.findByRole("heading", { name: "Oportunidad" }, { timeout: 5000 });

    const pdfButton = screen.getByRole("button", {
      name: /guardar documento de inversión en pdf/i,
    });
    await user.click(pdfButton);

    expect(pdfButton).toHaveTextContent("Generando PDF...");
    await waitFor(() => expect(pdfSetMock).toHaveBeenCalledTimes(1));

    const options = pdfSetMock.mock.calls[0][0] as {
      filename: string;
      html2canvas: { backgroundColor: string };
    };
    expect(options.filename).toBe("investment-memo-mi-proyecto.pdf");
    expect(options.html2canvas.backgroundColor).toBe("#ffffff");

    const node = pdfFromMock.mock.calls[0][0];
    expect(node.id).toBe("investment-memo-pdf");
    expect(node.querySelector("#investment-memo-content")).toBeNull();
    expect(node.querySelector("button")).toBeNull();
    expect(node.textContent).toContain("Oportunidad");
    expect(node.style.backgroundColor).toMatch(/^(#ffffff|rgb\(255, 255, 255\))$/);
    expect(node.style.color).toMatch(/^(#000000|rgb\(0, 0, 0\))$/);

    resolveSave?.();
    await waitFor(() => expect(pdfButton).toHaveTextContent("Guardar en PDF"));
    expect(pdfSaveMock).toHaveBeenCalledTimes(1);
  });
});
