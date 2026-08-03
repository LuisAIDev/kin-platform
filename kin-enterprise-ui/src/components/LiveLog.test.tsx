import { render, screen } from "@testing-library/react";
import { LiveLog } from "./LiveLog";
import type { EnterpriseProgressEvent } from "../types/enterprise";

const events: EnterpriseProgressEvent[] = [
  {
    projectId: "p1",
    version: 1,
    state: "RUNNING",
    timestamp: "2026-08-02T10:00:00Z",
  },
  {
    projectId: "p1",
    version: 1,
    state: "FAILED",
    timestamp: "2026-08-02T10:01:00Z",
    message: "Generación fallida: motor",
  },
];

describe("LiveLog", () => {
  it("muestra el estado de conexión", () => {
    render(<LiveLog events={[]} connected />);
    expect(screen.getByText(/conectado/i)).toBeInTheDocument();
  });

  it("muestra un marcador cuando no hay eventos", () => {
    render(<LiveLog events={[]} connected={false} />);
    expect(screen.getByText(/Esperando eventos/)).toBeInTheDocument();
  });

  it("muestra los eventos y el estado de desconexión", () => {
    render(<LiveLog events={events} connected={false} />);
    expect(screen.getByText(/desconectado/i)).toBeInTheDocument();
    expect(screen.getByText(/RUNNING/)).toBeInTheDocument();
    expect(screen.getByText(/FAILED/)).toBeInTheDocument();
    expect(screen.getByText(/Generación fallida: motor/)).toBeInTheDocument();
  });

  it("aplica la clase de color para estados terminales", () => {
    render(<LiveLog events={events} connected={false} />);
    expect(document.querySelector(".log-line-FAILED")).toBeInTheDocument();
  });
});
