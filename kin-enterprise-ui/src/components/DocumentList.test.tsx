import { render, screen } from "@testing-library/react";
import { DocumentList } from "./DocumentList";
import type { EnterpriseDocument } from "../types/enterprise";

const documents: EnterpriseDocument[] = [
  {
    id: "a",
    type: "LEAN_CANVAS",
    size: 2048,
    createdAt: "2026-08-02T10:00:00Z",
    generatedBy: "BusinessModelEngine",
    engineVersion: "1.0.0",
    version: 1,
    inputHash: "hash-a",
  },
  {
    id: "b",
    type: "KPI",
    size: 512,
    createdAt: "2026-08-02T10:00:00Z",
    generatedBy: "KpiEngine",
    engineVersion: "1.0.0",
    version: 1,
    inputHash: "hash-b",
  },
];

describe("DocumentList", () => {
  it("muestra un aviso cuando no hay documentos", () => {
    render(<DocumentList documents={[]} />);
    expect(screen.getByText(/Aún no se han generado documentos/)).toBeInTheDocument();
  });

  it("muestra los documentos con su tamaño y motor", () => {
    render(<DocumentList documents={documents} />);
    expect(screen.getByTestId("document-list").children).toHaveLength(2);
    expect(screen.getByText("LEAN CANVAS")).toBeInTheDocument();
    expect(screen.getByText("KPI")).toBeInTheDocument();
    expect(screen.getByText(/2.0 KB/)).toBeInTheDocument();
    expect(screen.getByText(/512 B/)).toBeInTheDocument();
  });
});
