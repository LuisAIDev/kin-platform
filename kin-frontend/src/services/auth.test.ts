import { afterEach, describe, expect, it, vi } from "vitest";
import { authService } from "@/services/auth";

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const sessionUser = { token: "t", email: "a@b.c", fullName: "Ana", role: "USER" };

const { mockedForceLogout } = vi.hoisted(() => ({ mockedForceLogout: vi.fn() }));
vi.mock("@/services/session", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/session")>();
  return { ...actual, forceLogout: mockedForceLogout };
});

describe("authService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    mockedForceLogout.mockReset();
  });

  it("register: éxito guarda sesión", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(sessionUser));

    const result = await authService.register({ fullName: "Ana", email: "a@b.c", password: "x" });

    expect(result.error).toBeNull();
    expect(result.data?.token).toBe("t");
    expect(localStorage.getItem("kin_token_v2")).toBe("t");
  });

  it("login: error devuelve mensaje sin lanzar", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Invalid email or password" }, 401));

    const result = await authService.login({ email: "a@b.c", password: "x" });

    expect(result.data).toBeNull();
    expect(result.error).toBe("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
  });

  it("logout: limpia sesión (sin token no llama a la API)", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch");

    authService.logout();

    expect(fetchMock).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
  });

  it("logout: con token llama a /auth/logout y limpia", async () => {
    localStorage.setItem("kin_token_v2", "t");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 200 }));

    authService.logout();

    expect(String(fetchMock.mock.calls[0][0])).toContain("/auth/logout");
    expect(localStorage.getItem("kin_token_v2")).toBeNull();
  });

  it("getToken/getUser: leen de localStorage", () => {
    localStorage.setItem("kin_token_v2", "tok");
    localStorage.setItem("kin_user_v2", JSON.stringify(sessionUser));

    expect(authService.getToken()).toBe("tok");
    expect(authService.getUser()?.email).toBe("a@b.c");
    expect(authService.getUser()?.fullName).toBe("Ana");
  });

  it("getUser: sin datos devuelve null", () => {
    expect(authService.getUser()).toBeNull();
  });
});
