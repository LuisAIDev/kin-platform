import { describe, expect, it } from "vitest";
import { statusBadge } from "@/utils/badgeColors";

describe("badgeColors", () => {
  it("devuelve clases para cada estado conocido", () => {
    expect(statusBadge("DRAFT")).toContain("bg-neutral-100");
    expect(statusBadge("IN_PROGRESS")).toContain("bg-primary-100");
    expect(statusBadge("COMPLETED")).toContain("bg-emerald-100");
    expect(statusBadge("ARCHIVED")).toContain("bg-neutral-200");
  });

  it("usa el color por defecto para estados desconocidos", () => {
    expect(statusBadge("CANCELLED")).toBe("bg-neutral-100 text-neutral-600");
    expect(statusBadge("")).toBe("bg-neutral-100 text-neutral-600");
  });
});
