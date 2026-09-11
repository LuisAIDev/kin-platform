import { afterEach, describe, expect, it, vi } from "vitest";
import { documentsService } from "@/services/documents";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("documentsService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("myUpload: envía FormData a /health/documents/my/upload y devuelve el documento", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      jsonResponse({
        id: "d1",
        fileName: "glucosa.pdf",
        fileSize: 10,
        mimeType: "application/pdf",
        patientId: "u1",
        physicianId: null,
        description: "",
        status: "ACTIVE",
        uploadedAt: "2026-08-01T00:00:00Z",
        analyzable: true,
      }),
    );

    const file = new File(["x"], "glucosa.pdf", { type: "application/pdf" });
    const result = await documentsService.myUpload(file, "Examen de glucosa");

    expect(result.id).toBe("d1");
    expect(result.analyzable).toBe(true);
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/medical/documents/my/upload");
    expect((init as RequestInit).method).toBe("POST");
    expect((init as RequestInit).credentials).toBe("include");
    expect((init as RequestInit).body).toBeInstanceOf(FormData);
  });

  it("myUpload: propaga el error del backend", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ error: "El archivo supera el tamaño máximo" }), { status: 400 }),
    );

    const file = new File(["x".repeat(1000)], "grande.pdf", { type: "application/pdf" });

    await expect(documentsService.myUpload(file)).rejects.toThrow(
      "El archivo supera el tamaño máximo",
    );
  });

  it("myDocuments: GET de los documentos del paciente", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));
    await documentsService.myDocuments();
    expect(String(fetchMock.mock.calls[0][0])).toContain("/medical/documents/my");
  });
});
