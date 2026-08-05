import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { useSettings } from "@/hooks/useSettings";
import { DEFAULT_SETTINGS, settingsService } from "@/services/settings";

describe("settingsService", () => {
  beforeEach(() => localStorage.clear());

  it("devuelve valores por defecto sin datos guardados", () => {
    expect(settingsService.load()).toEqual(DEFAULT_SETTINGS);
  });

  it("guarda y recupera ajustes", () => {
    settingsService.save({ ...DEFAULT_SETTINGS, displayName: "Ana", theme: "dark" });

    const loaded = settingsService.load();
    expect(loaded.displayName).toBe("Ana");
    expect(loaded.theme).toBe("dark");
  });

  it("normaliza valores inválidos al cargar", () => {
    localStorage.setItem("kin_user_settings_v1", JSON.stringify({ displayName: "X", theme: "nope" }));

    const loaded = settingsService.load();
    expect(loaded.theme).toBe("system");
    expect(loaded.displayName).toBe("X");
  });

  it("aplica preferencias IA con valores válidos", () => {
    settingsService.save({ ...DEFAULT_SETTINGS, aiProvider: "deepseek", temperature: 0.4, aiLength: "long" });

    const loaded = settingsService.load();
    expect(loaded.aiProvider).toBe("deepseek");
    expect(loaded.temperature).toBe(0.4);
    expect(loaded.aiLength).toBe("long");
  });

  it("acota la temperatura a 0..1", () => {
    localStorage.setItem("kin_user_settings_v1", JSON.stringify({ temperature: 1.8 }));

    expect(settingsService.load().temperature).toBe(1);
  });

  it("tolera JSON corrupto", () => {
    localStorage.setItem("kin_user_settings_v1", "{broken");

    expect(settingsService.load()).toEqual(DEFAULT_SETTINGS);
  });
});

describe("useSettings", () => {
  beforeEach(() => localStorage.clear());

  it("expone ajustes y los actualiza guardando en localStorage", () => {
    const { result } = renderHook(() => useSettings());

    act(() => result.current.update({ language: "en" }));

    expect(result.current.settings.language).toBe("en");
    expect(JSON.parse(localStorage.getItem("kin_user_settings_v1") ?? "{}").language).toBe("en");
  });
});
