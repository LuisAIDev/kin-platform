import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ConfirmDialog from "@/components/ConfirmDialog";

describe("ConfirmDialog", () => {
  it("no renderiza nada cuando está cerrado", () => {
    const { container } = render(
      <ConfirmDialog open={false} title="T" message="M" onConfirm={vi.fn()} onCancel={vi.fn()} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("renderiza título, mensaje y botones con roles accesibles", () => {
    render(
      <ConfirmDialog open title="Eliminar proyecto" message="¿Seguro?" onConfirm={vi.fn()} onCancel={vi.fn()} />
    );

    expect(screen.getByRole("heading", { name: "Eliminar proyecto" })).toBeInTheDocument();
    expect(screen.getByText("¿Seguro?")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Eliminar" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeInTheDocument();
  });

  it("confirma y cancela al pulsar los botones", async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(
      <ConfirmDialog open title="T" message="M" onConfirm={onConfirm} onCancel={onCancel} />
    );

    await user.click(screen.getByRole("button", { name: "Eliminar" }));
    await user.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("muestra estado de carga y deshabilita botones", () => {
    render(
      <ConfirmDialog open title="T" message="M" loading onConfirm={vi.fn()} onCancel={vi.fn()} />
    );

    expect(screen.getByRole("button", { name: "Eliminando..." })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeDisabled();
  });

  it("cierra al hacer clic en el fondo", async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <ConfirmDialog open title="T" message="M" onConfirm={vi.fn()} onCancel={onCancel} />
    );

    await user.click(document.querySelector(".fixed.inset-0.bg-black\\/40") as HTMLElement);
    expect(onCancel).toHaveBeenCalled();
  });
});
