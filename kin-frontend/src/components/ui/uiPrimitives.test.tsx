import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import EmptyState from "@/components/ui/EmptyState";
import Skeleton from "@/components/ui/Skeleton";
import ErrorState from "@/components/ui/ErrorState";

describe("EmptyState", () => {
  it("renderiza título, descripción y acción", () => {
    render(
      <EmptyState
        title="Sin proyectos"
        description="Crea uno para empezar"
        action={<button>Crear</button>}
      />
    );

    expect(screen.getByRole("heading", { name: "Sin proyectos" })).toBeInTheDocument();
    expect(screen.getByText("Crea uno para empezar")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Crear" })).toBeInTheDocument();
  });

  it("funciona sin descripción ni acción", () => {
    render(<EmptyState title="Vacío" />);
    expect(screen.getByRole("heading", { name: "Vacío" })).toBeInTheDocument();
  });
});

describe("Skeleton", () => {
  it("es accesible con rol status y aria-label", () => {
    render(<Skeleton />);
    const el = screen.getByRole("status");
    expect(el).toHaveAttribute("aria-label", "Cargando");
  });

  it("acepta clases personalizadas", () => {
    render(<Skeleton className="h-10 w-40" />);
    expect(screen.getByRole("status").className).toContain("h-10");
  });
});

describe("ErrorState", () => {
  it("muestra mensaje con rol alert", () => {
    render(<ErrorState message="Error al cargar" />);
    expect(screen.getByRole("alert")).toHaveTextContent("Error al cargar");
  });

  it("ejecuta retry al pulsar el botón", () => {
    const onRetry = vi.fn();
    render(<ErrorState message="Error" onRetry={onRetry} />);

    screen.getByRole("button", { name: "Reintentar" }).click();

    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});
