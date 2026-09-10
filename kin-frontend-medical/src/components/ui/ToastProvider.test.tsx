import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import ToastProvider, { useToast } from "@/components/ui/ToastProvider";
import { useState } from "react";

function Harness() {
  const { success, error, info } = useToast();
  const [count, setCount] = useState(0);
  return (
    <div>
      <button onClick={() => { success("Guardado"); setCount(count + 1); }}>ok</button>
      <button onClick={() => error("Falló")}>fail</button>
      <button onClick={() => info("Info")}>info</button>
    </div>
  );
}

function Broken() {
  useToast();
  return null;
}

describe("ToastProvider / useToast", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("useToast fuera del provider lanza error", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    expect(() => render(<Broken />)).toThrow("useToast");
    spy.mockRestore();
  });

  it("muestra toasts de éxito con rol status", () => {
    vi.useFakeTimers();
    render(<ToastProvider><Harness /></ToastProvider>);

    act(() => { screen.getByRole("button", { name: "ok" }).click(); });

    expect(screen.getByRole("status")).toHaveTextContent("Guardado");
  });

  it("muestra toasts de error con rol alert y los descarta tras el timeout", () => {
    vi.useFakeTimers();
    render(<ToastProvider><Harness /></ToastProvider>);

    act(() => { screen.getByRole("button", { name: "fail" }).click(); });
    expect(screen.getByRole("alert")).toHaveTextContent("Falló");

    act(() => { vi.advanceTimersByTime(4000); });
    expect(screen.queryByRole("alert")).toBeNull();
  });

  it("muestra múltiples toasts", () => {
    render(<ToastProvider><Harness /></ToastProvider>);

    act(() => { screen.getByRole("button", { name: "info" }).click(); });
    act(() => { screen.getByRole("button", { name: "ok" }).click(); });

    expect(screen.getAllByRole("status").length).toBeGreaterThanOrEqual(2);
  });
});
