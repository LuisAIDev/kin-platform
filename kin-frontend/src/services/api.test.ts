import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/services/api";

const { mockedForceLogout } = vi.hoisted(() => ({ mockedForceLogout: vi.fn() }));
vi.mock("@/services/session", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/services/session")>();
  return { ...actual, forceLogout: mockedForceLogout };
});

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    mockedForceLogout.mockReset();
  });

  it("get: usa credentials:include y devuelve JSON", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ ok: true }));

    const result = await api.get<{ ok: boolean }>("/ping");

    expect(result.ok).toBe(true);
    const [, init] = fetchMock.mock.calls[0];
    expect((init?.headers as Record<string, string> | undefined)?.Authorization).toBeUndefined();
    expect((init as RequestInit).credentials).toBe("include");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/ping");
  });

  it("post/put/delete: usan método y body JSON", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse({ id: 1 }))
      .mockResolvedValueOnce(jsonResponse({ id: 2 }))
      .mockResolvedValueOnce(jsonResponse(null, 204));

    await api.post("/create", { a: 1 });
    await api.put("/update", { b: 2 });
    await api.delete("/remove");

    expect(String(fetchMock.mock.calls[0][0])).toContain("/create");
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("POST");
    expect((fetchMock.mock.calls[1][1] as RequestInit).method).toBe("PUT");
    expect((fetchMock.mock.calls[2][1] as RequestInit).method).toBe("DELETE");
  });

  it("error: extrae message del body y fuerza logout en 401", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Token inválido" }, 401));

    await expect(api.get("/x")).rejects.toThrow("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
  });

  it("error: mensaje por defecto sin body", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("boom", { status: 500 }));

    await expect(api.get("/x")).rejects.toThrow("Request failed (500)");
  });

  it("error 400 authenticated user fuerza logout", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ error: "No authenticated user" }, 400));

    await expect(api.get("/x")).rejects.toThrow("No authenticated user");
    expect(mockedForceLogout).toHaveBeenCalled();
  });

  it("error 500 no fuerza logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "boom" }, 500));

    await expect(api.get("/x")).rejects.toThrow("boom");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("error 503 no fuerza logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "unavailable" }, 503));

    await expect(api.get("/x")).rejects.toThrow("unavailable");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("error 403 no fuerza logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Forbidden" }, 403));

    await expect(api.get("/x")).rejects.toThrow("Forbidden");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("401 con el token actual fuerza logout", async () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ email: "a@b.c", fullName: "Ana", role: "FREE" }));
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "x" }, 401));

    await expect(api.get("/x")).rejects.toThrow("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
    // forceLogout está mockeado, no limpia localStorage real
  });

  it("401 siempre fuerza logout (sin token en localStorage)", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Token inválido" }, 401));

    await expect(api.get("/x")).rejects.toThrow("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
  });

  it("400 authenticated user fuerza logout", async () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ email: "a@b.c", fullName: "Ana", role: "FREE" }));
    let resolveFetch!: (r: Response) => void;
    const pending = new Promise<Response>((r) => (resolveFetch = r));
    vi.spyOn(globalThis, "fetch").mockReturnValue(pending as Promise<Response>);

    const p = api.get("/x");
    resolveFetch(jsonResponse({ error: "No authenticated user" }, 400));

    await expect(p).rejects.toThrow("No authenticated user");
    expect(mockedForceLogout).toHaveBeenCalled();
    // forceLogout está mockeado, no limpia localStorage real
  });

  it("200 en /auth/me mantiene la sesión activa", async () => {
    localStorage.setItem("kin_user_v2", JSON.stringify({ email: "a@b.c", fullName: "Ana", role: "FREE" }));
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ email: "a@b.c" }, 200));

    const result = await api.get<{ email: string }>("/auth/me");

    expect(result).toEqual({ email: "a@b.c" });
    expect(mockedForceLogout).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_user_v2")).toContain("a@b.c");
  });

  it("200 en /subscriptions/status mantiene la sesión activa", async () => {
    localStorage.setItem("kin_token_v2", "tok");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ isActive: true }, 200));

    const result = await api.get<{ isActive: boolean }>("/subscriptions/status");

    expect(result).toEqual({ isActive: true });
    expect(mockedForceLogout).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBe("tok");
  });

  it("401 en /auth/login NO fuerza logout y propaga el mensaje (error de credenciales)", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Invalid email or password" }, 401));

    await expect(api.post("/auth/login", { email: "a@b.c", password: "x" }))
      .rejects.toThrow("Invalid email or password");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("400 en /auth/login NO fuerza logout y propaga el mensaje", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Invalid email or password" }, 400));

    await expect(api.post("/auth/login", { email: "a@b.c", password: "x" }))
      .rejects.toThrow("Invalid email or password");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("403 en /auth/login con code EMAIL_VERIFICATION_REQUIRED propaga el código", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ error: "correo no verificado", code: "EMAIL_VERIFICATION_REQUIRED" }, 403));

    const err = await api.post("/auth/login", { email: "a@b.c", password: "x" }).catch((e: Error & { code?: string }) => e);

    expect(err.code).toBe("EMAIL_VERIFICATION_REQUIRED");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("401 en /auth/register/physician NO fuerza logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "boom" }, 401));

    await expect(api.post("/auth/register/physician", {})).rejects.toThrow("boom");
    expect(mockedForceLogout).not.toHaveBeenCalled();
  });

  it("401 en endpoints NO-auth SÍ fuerza logout (sesión expirada)", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "Token inválido" }, 401));

    await expect(api.get("/projects")).rejects.toThrow("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
  });
});
