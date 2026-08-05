import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import PricingSection from "@/components/pricing/PricingSection";

const { getAll } = vi.hoisted(() => ({ getAll: vi.fn() }));

vi.mock("@/services/pricing", () => ({
  pricingService: { getAll },
}));
vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

const plans = [
  { id: "free", name: "Free", description: "Básico", price: 0, features: ["1 proyecto"],
    maxProjects: 1, messagesPerMonth: 100, advancedAI: false, pdfExport: false,
    supportLevel: "BASIC", viabilityScoringDetail: "BASIC", isActive: true },
  { id: "pro", name: "Pro", description: "Avanzado", price: 29, features: ["10 proyectos"],
    maxProjects: 10, messagesPerMonth: 1000, advancedAI: true, pdfExport: true,
    supportLevel: "PREMIUM", viabilityScoringDetail: "DETAILED", isActive: true },
];

describe("PricingSection", () => {
  it("renderiza los planes cuando cargan", async () => {
    getAll.mockResolvedValue(plans);

    render(<PricingSection />);

    expect(await screen.findByRole("heading", { name: "Free" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Pro" })).toBeInTheDocument();
    expect(screen.getByText("Más popular")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Comenzar gratis" })).toHaveAttribute("href", "/register");
    expect(screen.getByRole("link", { name: "Ir a Premium" })).toHaveAttribute("href", "/register");
  });

  it("no renderiza nada si hay error", async () => {
    getAll.mockRejectedValue(new Error("boom"));

    const { container } = render(<PricingSection />);
    await vi.waitFor(() => expect(container).toBeEmptyDOMElement());
  });
});
