import { afterEach, describe, expect, it, vi } from "vitest";
import { hceApi } from "@/lib/hce/api/hce.api";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("hceApi encounters (TD-HCE-1)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("createEncounter: POST /medical/hce/encounters con el payload", async () => {
    const fetchMock = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ id: "e1" }, 201));

    await hceApi.createEncounter({
      patientId: "p1",
      encounterType: "OUTPATIENT",
      chiefComplaint: "Dolor abdominal",
    });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/medical/hce/encounters");
    expect(String(url)).not.toContain("/api/v1/api/v1/");
    expect((init as RequestInit).method).toBe("POST");
    expect(JSON.parse(String((init as RequestInit).body))).toEqual({
      patientId: "p1",
      encounterType: "OUTPATIENT",
      chiefComplaint: "Dolor abdominal",
    });
  });

  it("listEncountersByPatient: GET con ?patientId=", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse([]));

    await hceApi.listEncountersByPatient("p-42");

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/medical/hce/encounters?patientId=p-42");
    expect(url).not.toContain("/api/v1/api/v1/");
  });

  it("getEncounter: GET /medical/hce/encounters/{id}", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ id: "e-9" }));

    await hceApi.getEncounter("e-9");

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/medical/hce/encounters/e-9");
    expect(url).not.toContain("/api/v1/api/v1/");
  });

  it("closeEncounter: POST /medical/hce/encounters/{id}/close", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ id: "e-9" }));

    await hceApi.closeEncounter("e-9");

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain("/medical/hce/encounters/e-9/close");
    expect((init as RequestInit).method).toBe("POST");
  });
});
