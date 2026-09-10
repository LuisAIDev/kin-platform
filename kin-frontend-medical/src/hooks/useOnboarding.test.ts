import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { ONBOARDING_ITEMS, useOnboarding } from "@/hooks/useOnboarding";

describe("useOnboarding", () => {
  beforeEach(() => localStorage.clear());

  it("empieza sin completar y sin descartar", () => {
    const { result } = renderHook(() => useOnboarding());

    expect(result.current.dismissed).toBe(false);
    expect(result.current.doneCount).toBe(0);
    expect(result.current.isComplete).toBe(false);
  });

  it("marca items como completados y persiste en localStorage", () => {
    const { result } = renderHook(() => useOnboarding());

    act(() => result.current.markDone(ONBOARDING_ITEMS[0].key));
    act(() => result.current.markDone(ONBOARDING_ITEMS[0].key));

    expect(result.current.doneCount).toBe(1);
    expect(JSON.parse(localStorage.getItem("kin_onboarding_v1") ?? "[]")).toEqual([ONBOARDING_ITEMS[0].key]);
  });

  it("completa el checklist al marcar todos los items", () => {
    const { result } = renderHook(() => useOnboarding());

    ONBOARDING_ITEMS.forEach((item) => act(() => result.current.markDone(item.key)));

    expect(result.current.isComplete).toBe(true);
    expect(result.current.doneCount).toBe(ONBOARDING_ITEMS.length);
  });

  it("dismiss y reset funcionan", () => {
    const { result } = renderHook(() => useOnboarding());

    act(() => result.current.dismiss());
    expect(result.current.dismissed).toBe(true);

    act(() => result.current.reset());
    expect(result.current.dismissed).toBe(false);
    expect(result.current.doneCount).toBe(0);
  });

  it("recupera el estado previo de localStorage", () => {
    localStorage.setItem("kin_onboarding_v1", JSON.stringify([ONBOARDING_ITEMS[0].key]));

    const { result } = renderHook(() => useOnboarding());

    expect(result.current.doneCount).toBe(1);
  });
});
