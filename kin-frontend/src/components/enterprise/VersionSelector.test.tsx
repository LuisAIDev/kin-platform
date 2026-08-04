import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { VersionSelector } from "@/components/enterprise/VersionSelector";
import type { EnterpriseVersion } from "@/types/enterprise";

const versions: EnterpriseVersion[] = [
  {
    version: 1,
    status: "COMPLETED",
    createdAt: "2026-08-02T10:00:00Z",
    updatedAt: "2026-08-02T10:05:00Z",
    documentCount: 3,
  },
  {
    version: 2,
    status: "RUNNING",
    createdAt: "2026-08-02T11:00:00Z",
    updatedAt: "2026-08-02T11:01:00Z",
    documentCount: 0,
  },
];

describe("VersionSelector", () => {
  it("muestra las versiones disponibles", () => {
    render(
      <VersionSelector versions={versions} selected={1} onSelect={() => undefined} />,
    );
    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(2);
    expect(options[0]).toHaveTextContent("v1");
    expect(options[1]).toHaveTextContent("v2");
  });

  it("invoca onSelect al cambiar de versión", async () => {
    const onSelect = vi.fn();
    render(<VersionSelector versions={versions} selected={1} onSelect={onSelect} />);
    await userEvent.selectOptions(screen.getByRole("combobox"), "2");
    expect(onSelect).toHaveBeenCalledWith(2);
  });

  it("muestra la versión seleccionada", () => {
    render(
      <VersionSelector versions={versions} selected={2} onSelect={() => undefined} />,
    );
    expect(screen.getByRole("combobox")).toHaveValue("2");
  });
});
