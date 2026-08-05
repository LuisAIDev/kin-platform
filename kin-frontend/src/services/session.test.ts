import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { checkForceLogout, clearSession, forceLogout, storeSession } from "@/services/session";

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

  it("storeSession: guarda token y usuario en localStorage y cookies", () => {
    storeSession({ token: "t", email: "a@b.c", fullName: "Ana", role: "USER" });

    expect(localStorage.getItem("kin_token_v2")).toBe("t");
    expect(localStorage.getItem("kin_user_v2")).toContain("Ana");
    expect(document.cookie).toContain("kin_session_v2=active");
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

  it("forceLogout: limpia sesión, redirige a /login y es idempotente", () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 200 }));
    const location = { href: "" };
    Object.defineProperty(window, "location", { value: location, writable: true });
    localStorage.setItem("kin_token_v2", "t");

    forceLogout();
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
    expect(location.href).toBe("/login");

    const hrefAfterFirst = location.href;
    forceLogout();
    expect(location.href).toBe(hrefAfterFirst);
  });
});
