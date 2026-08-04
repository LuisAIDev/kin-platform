import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Timeline } from "@/components/enterprise/Timeline";
import type { EnterpriseProgressEvent } from "@/types/enterprise";

const events: EnterpriseProgressEvent[] = [
  {
    projectId: "p1",
    version: 1,
    state: "REQUESTED",
    timestamp: "2026-08-02T10:00:00Z",
    message: "Generación solicitada",
  },
  {
    projectId: "p1",
    version: 1,
    state: "DOCUMENT_GENERATED",
    timestamp: "2026-08-02T10:01:00Z",
    documentType: "LEAN_CANVAS",
  },
  {
    projectId: "p1",
    version: 1,
    state: "COMPLETED",
    timestamp: "2026-08-02T10:02:00Z",
  },
];

describe("Timeline", () => {
  it("muestra un aviso cuando no hay eventos", () => {
    render(<Timeline events={[]} />);
    expect(screen.getByText(/Esperando eventos/)).toBeInTheDocument();
  });

  it("muestra los eventos en orden", () => {
    render(<Timeline events={events} />);
    const items = screen.getByTestId("timeline").children;
    expect(items).toHaveLength(3);
    expect(items[0]).toHaveTextContent("REQUESTED");
    expect(items[0]).toHaveTextContent("Generación solicitada");
    expect(items[1]).toHaveTextContent("DOCUMENT_GENERATED");
    expect(items[1]).toHaveTextContent("LEAN_CANVAS");
    expect(items[2]).toHaveTextContent("COMPLETED");
  });
});
