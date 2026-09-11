import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import AuditLogTable from "@/components/audit/AuditLogTable";
import type { AuditLogEntry } from "@/services/audit";

const ENTRIES: AuditLogEntry[] = [
  {
    id: "log1",
    userId: "u1",
    action: "VIEW_HISTORY",
    resourceType: "PACIENTE",
    resourceId: "p1",
    patientId: "p1",
    timestamp: "2026-08-30T10:00:00Z",
    ipAddress: "1.2.3.4",
    userAgent: "test",
    details: {},
  },
];

describe("AuditLogTable", () => {
  it("muestra las acciones de acceso", () => {
    render(<AuditLogTable entries={ENTRIES} />);

    expect(screen.getByText("VIEW_HISTORY")).toBeInTheDocument();
    expect(screen.getByText("PACIENTE")).toBeInTheDocument();
    expect(screen.getByText("1.2.3.4")).toBeInTheDocument();
  });

  it("muestra estado vacío sin registros", () => {
    render(<AuditLogTable entries={[]} />);

    expect(screen.getByText("Sin registros de auditoría para estos criterios.")).toBeInTheDocument();
  });
});
