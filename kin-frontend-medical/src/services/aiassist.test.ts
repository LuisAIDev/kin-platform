import { afterEach, describe, expect, it, vi } from "vitest";
import { aiassistService } from "@/services/aiassist";

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("aiassistService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("getMyHistory: genera /health/aiassist/my/history (sin doble /api/v1)", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));

    await aiassistService.getMyHistory();

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/medical/aiassist/my/history");
    expect(url).not.toContain("/api/v1/api/v1/");
  });

  it("getHistory: genera /health/aiassist/patients/{patientId}/history", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));

    await aiassistService.getHistory("patient-123");

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/medical/aiassist/patients/patient-123/history");
    expect(url).not.toContain("/api/v1/api/v1/");
  });

  it("generateSummary: recibe patientId y genera la ruta correcta", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({}));

    await aiassistService.generateSummary("patient-456");

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/medical/aiassist/patients/patient-456/summary");
    expect(url).not.toContain("/api/v1/api/v1/");
  });

  it("prepareConsultation: genera la ruta correcta con patientId", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({}));

    await aiassistService.prepareConsultation("patient-789");

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/medical/aiassist/patients/patient-789/consultation-prep");
    expect(url).not.toContain("/api/v1/api/v1/");
  });

  it("draftMessage: genera la ruta correcta con patientId y recommendation", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({}));

    await aiassistService.draftMessage("patient-999", "tome reposo");

    const url = String(fetchMock.mock.calls[0][0]);
    const [, init] = fetchMock.mock.calls[0];
    expect(url).toContain("/medical/aiassist/patients/patient-999/draft-message");
    const body = JSON.parse(String((init as RequestInit).body));
    expect(body.recommendation).toBe("tome reposo");
    expect(url).not.toContain("/api/v1/api/v1/");
  });
});
