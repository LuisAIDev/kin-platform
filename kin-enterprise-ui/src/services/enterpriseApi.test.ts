import { afterEach, describe, expect, it, vi } from "vitest";
import { enterpriseApi, downloadBlob } from "./enterpriseApi";
import type { EnterpriseDashboard } from "../types/enterprise";

const dashboard: EnterpriseDashboard = {
  projectId: "p1",
  version: 1,
  status: "COMPLETED",
  progress: 100,
  documentCount: 1,
  versionsCount: 1,
  createdAt: "2026-08-02T10:00:00Z",
  updatedAt: "2026-08-02T10:01:00Z",
  completedAt: "2026-08-02T10:01:00Z",
  failedReason: null,
  generationDurationMillis: 60000,
  score: null,
  documents: [],
  versions: [],
  statistics: { documentCount: 1 },
};

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

describe("enterpriseApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("obtiene el dashboard de una versión", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(dashboard));
    const result = await enterpriseApi.getDashboard("p1", 1);
    expect(result.status).toBe("COMPLETED");
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/enterprise/p1/1/dashboard");
  });

  it("lanza un error cuando el dashboard falla", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("", { status: 404 }));
    await expect(enterpriseApi.getDashboard("p1", 1)).rejects.toThrow("404");
  });

  it("descarga un documento en un formato", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(new Blob(["pdf-bytes"]), { status: 200 }),
    );
    const blob = await enterpriseApi.downloadDocument("p1", 1, "LEAN_CANVAS", "PDF");
    expect(blob.size).toBeGreaterThan(0);
    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/enterprise/p1/1/export/LEAN_CANVAS/PDF");
  });

  it("descarga el bundle ZIP de un formato", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(new Blob(["zip-bytes"]), { status: 200 }),
    );
    const blob = await enterpriseApi.downloadBundle("p1", 1, "PDF");
    expect(blob.size).toBeGreaterThan(0);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/enterprise/p1/1/export/PDF");
  });

  it("incluye la cabecera Authorization cuando existe token", async () => {
    localStorage.setItem("kin_token_v2", "tok");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(dashboard));
    await enterpriseApi.getDashboard("p1", 1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toBe("Bearer tok");
    localStorage.removeItem("kin_token_v2");
  });

  it("no incluye Authorization sin token", async () => {
    localStorage.removeItem("kin_token_v2");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(dashboard));
    await enterpriseApi.getDashboard("p1", 1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it("lanza un error cuando la descarga falla", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("", { status: 403 }));
    await expect(enterpriseApi.downloadDocument("p1", 1, "KPI", "PDF")).rejects.toThrow(
      "403",
    );
    await expect(enterpriseApi.downloadBundle("p1", 1, "PDF")).rejects.toThrow("403");
  });
});

describe("downloadBlob", () => {
  it("crea un enlace de descarga y lo revoca", () => {
    const create = vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:mock");
    const revoke = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => undefined);
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, "click")
      .mockImplementation(() => undefined);

    downloadBlob(new Blob(["x"]), "doc.pdf");

    expect(create).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalled();
  });
});
