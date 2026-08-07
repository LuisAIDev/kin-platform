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
});
