import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ExportButtons } from "./ExportButtons";
import type { EnterpriseDocument } from "../types/enterprise";

const documents: EnterpriseDocument[] = [
  {
    id: "a",
    type: "LEAN_CANVAS",
    size: 100,
    createdAt: "2026-08-02T10:00:00Z",
    generatedBy: "BusinessModelEngine",
    engineVersion: "1.0.0",
    version: 1,
    inputHash: "hash",
  },
];

describe("ExportButtons", () => {
  it("ofrece ZIP para cada formato y botones por documento", () => {
    render(
      <ExportButtons
        documents={documents}
        onDownloadDocument={() => undefined}
        onDownloadBundle={() => undefined}
      />,
    );
    expect(screen.getByRole("button", { name: "ZIP · PDF" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "ZIP · DOCX" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "ZIP · PPTX" })).toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: /^(PDF|DOCX|PPTX)$/ }).length,
    ).toBe(3);
  });

  it("deshabilita los botones ZIP sin documentos", () => {
    render(
      <ExportButtons
        documents={[]}
        onDownloadDocument={() => undefined}
        onDownloadBundle={() => undefined}
      />,
    );
    expect(screen.getByRole("button", { name: "ZIP · PDF" })).toBeDisabled();
  });

  it("invoca onDownloadDocument al pulsar un formato de documento", async () => {
    const onDownloadDocument = vi.fn();
    render(
      <ExportButtons
        documents={documents}
        onDownloadDocument={onDownloadDocument}
        onDownloadBundle={() => undefined}
      />,
    );
    await userEvent.click(screen.getByRole("button", { name: "PDF" }));
    expect(onDownloadDocument).toHaveBeenCalledWith("LEAN_CANVAS", "PDF");
  });

  it("invoca onDownloadBundle al pulsar ZIP de un formato", async () => {
    const onDownloadBundle = vi.fn();
    render(
      <ExportButtons
        documents={documents}
        onDownloadDocument={() => undefined}
        onDownloadBundle={onDownloadBundle}
      />,
    );
    await userEvent.click(screen.getByRole("button", { name: "ZIP · DOCX" }));
    expect(onDownloadBundle).toHaveBeenCalledWith("DOCX");
  });

  it("deshabilita todos los botones mientras está ocupado", () => {
    render(
      <ExportButtons
        documents={documents}
        onDownloadDocument={() => undefined}
        onDownloadBundle={() => undefined}
        busy
      />,
    );
    screen.getAllByRole("button").forEach((button) => expect(button).toBeDisabled());
  });
});
