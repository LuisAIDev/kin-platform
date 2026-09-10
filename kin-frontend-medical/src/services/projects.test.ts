import { afterEach, describe, expect, it, vi } from "vitest";
import { projectsService } from "@/services/projects";

function jsonResponse(body: unknown, status = 200): Response {
  if (status === 204) {
    return new Response(null, { status });
  }
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("projectsService", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it("getAll: usa paginación", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ content: [], page: 0 }));

    await projectsService.getAll(2, 10);

    const url = String(fetchMock.mock.calls[0][0]);
    expect(url).toContain("/projects?page=2&size=10");
  });

  it("getById: GET al proyecto", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse({ id: "p1", title: "Proyecto" }));

    const project = await projectsService.getById("p1");

    expect(project.title).toBe("Proyecto");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/projects/p1");
  });

  it("create/update: POST y PUT con body", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(jsonResponse({ id: "p1" }))
      .mockResolvedValueOnce(jsonResponse({ id: "p1" }));

    await projectsService.create({ title: "Nuevo", category: "cat-1" });
    await projectsService.update("p1", { title: "Editado" });

    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("POST");
    expect((fetchMock.mock.calls[1][1] as RequestInit).method).toBe("PUT");
  });

  it("delete: DELETE", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse(null, 204));

    await projectsService.delete("p1");

    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("DELETE");
  });

  it("getCategories: GET a /categories", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(jsonResponse([{ id: "c1", code: "sas", name: "SAS" }]));

    const categories = await projectsService.getCategories();

    expect(categories).toHaveLength(1);
    expect(String(fetchMock.mock.calls[0][0])).toContain("/categories");
  });
});
