import { render, screen } from "@testing-library/react";
import { StatusBadge } from "./StatusBadge";

describe("StatusBadge", () => {
  it.each(["REQUESTED", "RUNNING", "DOCUMENT_GENERATED", "COMPLETED", "FAILED"])(
    "muestra el estado %s",
    (status) => {
      render(<StatusBadge status={status} />);
      expect(screen.getByTestId("status-badge")).toHaveTextContent(status);
      expect(screen.getByTestId("status-badge")).toHaveClass(`badge-${status}`);
    },
  );

  it("usa la clase por defecto para estados desconocidos", () => {
    render(<StatusBadge status="DESCONOCIDO" />);
    expect(screen.getByTestId("status-badge")).toHaveClass("badge-unknown");
  });
});
