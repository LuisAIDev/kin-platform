import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  checkForceLogout,
  clearSelectedVertical,
  clearSession,
  forceLogout,
  getSelectedVertical,
  isSelectedVertical,
  setSelectedVertical,
  storeSession,
} from "@/services/session";

describe("session", () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    document.cookie.split("; ").forEach((c) => {
      const name = c.split("=")[0];
      document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("storeSession: guarda usuario en localStorage y cookies (NO el token, que va en cookie HttpOnly)", () => {
    storeSession({ token: "secret-token-abc123", email: "a@b.c", fullName: "Ana", role: "USER" });

    // Token NO se guarda en localStorage (protección contra XSS)
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
    expect(localStorage.getItem("kin_user_v2")).not.toContain("secret-token-abc123");
    // Solo datos de usuario no sensibles
    expect(localStorage.getItem("kin_user_v2")).toContain("Ana");
    // Nota: document.cookie no es fiable en jsdom; la cookie HttpOnly se verifica en tests E2E
  });

  it("clearSession: limpia localStorage, sessionStorage y cookies", () => {
    localStorage.setItem("a", "1");
    sessionStorage.setItem("b", "2");
    document.cookie = "x=1";

    clearSession();

    expect(localStorage.getItem("a")).toBeNull();
    expect(sessionStorage.getItem("b")).toBeNull();
  });

  it("checkForceLogout: detecta y limpia la cookie de fuerza de logout", () => {
    document.cookie = "kin_force_logout=1";
    expect(checkForceLogout()).toBe(true);
    expect(checkForceLogout()).toBe(false);
  });

  it("checkForceLogout: sin cookie devuelve false", () => {
    expect(checkForceLogout()).toBe(false);
  });

  it("forceLogout: limpia sesión, redirige a /login y es idempotente", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 200 }));
    const location = { href: "" };
    Object.defineProperty(window, "location", { value: location, writable: true });
    // No pre-seed localStorage token (ya no se usa)

    await forceLogout();
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
    expect(location.href).toBe("/login");

    const hrefAfterFirst = location.href;
    await forceLogout();
    expect(location.href).toBe(hrefAfterFirst);
  });

  describe("vertical de navegación (kin_vertical)", () => {
    it("isSelectedVertical solo acepta 'empresa' y 'salud'", () => {
      expect(isSelectedVertical("empresa")).toBe(true);
      expect(isSelectedVertical("salud")).toBe(true);
      expect(isSelectedVertical("admin")).toBe(false);
      expect(isSelectedVertical("")).toBe(false);
      expect(isSelectedVertical(null)).toBe(false);
      expect(isSelectedVertical(undefined)).toBe(false);
      expect(isSelectedVertical("EMPRESA")).toBe(false);
    });

    it("setSelectedVertical guarda empresa y la recupera", () => {
      setSelectedVertical("empresa");
      expect(getSelectedVertical()).toBe("empresa");
    });

    it("setSelectedVertical guarda salud y la recupera", () => {
      setSelectedVertical("salud");
      expect(getSelectedVertical()).toBe("salud");
    });

    it("sin selección previa devuelve null", () => {
      expect(getSelectedVertical()).toBeNull();
    });

    it("rechaza valores inválidos (no guarda)", () => {
      setSelectedVertical("admin" as never);
      expect(getSelectedVertical()).toBeNull();
      expect(localStorage.getItem("kin_vertical")).toBeNull();
    });

    it("clearSelectedVertical limpia la selección", () => {
      setSelectedVertical("salud");
      clearSelectedVertical();
      expect(getSelectedVertical()).toBeNull();
    });

    it("clearSession limpia también la vertical", () => {
      setSelectedVertical("salud");
      clearSession();
      expect(getSelectedVertical()).toBeNull();
    });
  });
});
