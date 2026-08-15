import { afterEach, describe, expect, it, vi } from "vitest";
import { projectInfoService } from "@/services/projectInfo";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("projectInfoService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("listInfo: GET de información estructurada", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse([{ section: "FINANZAS" }]));

    const result = await projectInfoService.listInfo("p1");

    expect(result).toEqual([{ section: "FINANZAS" }]);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/projects/p1/info");
  });

  it("saveInfo: POST con los campos y sourceType", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));

    await projectInfoService.saveInfo("p1", [
      { section: "FINANZAS", key: "inversion_inicial", value: "80000000", sourceType: "USER_INPUT" },
    ]);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/projects/p1/info");
    expect((init as RequestInit).method).toBe("POST");
    const body = JSON.parse((init as RequestInit).body as string);
    expect(body.entries[0]).toEqual({
      section: "FINANZAS",
      key: "inversion_inicial",
      value: "80000000",
      sourceType: "USER_INPUT",
    });
  });

  it("listDocuments: GET de documentos", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));

    await projectInfoService.listDocuments("p1");

    expect(String(fetchMock.mock.calls[0][0])).toContain("/projects/p1/documents");
  });

  it("uploadDocument: envía FormData con Authorization y devuelve el documento", async () => {
    localStorage.setItem("kin_token_v2", "tok");
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({ id: "d1", filename: "notas.txt", status: "PROCESADO" }),
    );

    const file = new File(["contenido"], "notas.txt", { type: "text/plain" });
    const result = await projectInfoService.uploadDocument("p1", file);

    expect(result.filename).toBe("notas.txt");
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/projects/p1/documents");
    const headers = (init as RequestInit).headers as Record<string, string>;
    expect(headers.Authorization).toBe("Bearer tok");
    expect(init).toBeDefined();
    expect((init as RequestInit).body).toBeInstanceOf(FormData);
  });

  it("uploadDocument: lanza el error del backend", async () => {
    localStorage.setItem("kin_token_v2", "tok");
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ error: "Formato no permitido" }), { status: 400 }),
    );

    const file = new File(["x"], "malo.exe", { type: "application/octet-stream" });

    await expect(projectInfoService.uploadDocument("p1", file)).rejects.toThrow(
      "Formato no permitido",
    );
  });

  it("confirmInfo: POST de confirmación de un dato importado", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({
        projectId: "p1",
        section: "FINANZAS",
        key: "precio",
        value: "45000",
        sourceType: "USER_INPUT",
        originalSourceType: "IMPORTED_DOCUMENT",
        sourceDocument: "documento.pdf",
        confirmedAt: "2026-08-10T00:00:00Z",
        updatedAt: "2026-08-10T00:00:00Z",
      }),
    );

    const result = await projectInfoService.confirmInfo("p1", "FINANZAS", "precio");

    expect(result.sourceType).toBe("USER_INPUT");
    expect(result.originalSourceType).toBe("IMPORTED_DOCUMENT");
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/projects/p1/info/FINANZAS/precio/confirm");
    expect((init as RequestInit).method).toBe("POST");
  });
});
