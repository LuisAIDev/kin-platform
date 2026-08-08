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

  it("get: agrega Authorization con token y devuelve JSON", async () => {
    localStorage.setItem("kin_token_v2", "tok-1");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ ok: true }));

    const result = await api.get<{ ok: boolean }>("/ping");

    expect(result.ok).toBe(true);
    const [, init] = fetchMock.mock.calls[0];
    expect((init?.headers as Record<string, string>).Authorization).toBe("Bearer tok-1");
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
    localStorage.setItem("kin_token_v2", "current-tok");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ error: "x" }, 401));

    await expect(api.get("/x")).rejects.toThrow("Unauthorized");
    expect(mockedForceLogout).toHaveBeenCalled();
  });

  it("401 de una petición con token antiguo no fuerza logout ni borra el token nuevo", async () => {
    localStorage.setItem("kin_token_v2", "old-tok");
    let resolveFetch!: (r: Response) => void;
    const pending = new Promise<Response>((r) => (resolveFetch = r));
    vi.spyOn(globalThis, "fetch").mockReturnValue(pending as Promise<Response>);

    const p = api.get("/x");
    localStorage.setItem("kin_token_v2", "new-tok");
    resolveFetch(jsonResponse({ error: "Token inválido" }, 401));

    await expect(p).rejects.toThrow("Unauthorized");
    expect(mockedForceLogout).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBe("new-tok");
  });

  it("400 authenticated user de una petición con token antiguo no fuerza logout", async () => {
    localStorage.setItem("kin_token_v2", "old-tok");
    let resolveFetch!: (r: Response) => void;
    const pending = new Promise<Response>((r) => (resolveFetch = r));
    vi.spyOn(globalThis, "fetch").mockReturnValue(pending as Promise<Response>);

    const p = api.get("/x");
    localStorage.setItem("kin_token_v2", "new-tok");
    resolveFetch(jsonResponse({ error: "No authenticated user" }, 400));

    await expect(p).rejects.toThrow("No authenticated user");
    expect(mockedForceLogout).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBe("new-tok");
  });

  it("200 en /auth/me mantiene la sesión activa", async () => {
    localStorage.setItem("kin_token_v2", "tok");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ email: "a@b.c" }, 200));

    const result = await api.get<{ email: string }>("/auth/me");

    expect(result).toEqual({ email: "a@b.c" });
    expect(mockedForceLogout).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBe("tok");
  });

  it("200 en /subscriptions/status mantiene la sesión activa", async () => {
    localStorage.setItem("kin_token_v2", "tok");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ isActive: true }, 200));

    const result = await api.get<{ isActive: boolean }>("/subscriptions/status");

    expect(result).toEqual({ isActive: true });
    expect(mockedForceLogout).not.toHaveBeenCalled();
    expect(localStorage.getItem("kin_token_v2")).toBe("tok");
  });
});
