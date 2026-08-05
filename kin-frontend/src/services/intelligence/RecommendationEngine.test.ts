import { describe, expect, it } from "vitest";
import { RecommendationEngine } from "@/services/intelligence/RecommendationEngine";

describe("RecommendationEngine", () => {
  it("sugiere crear el primer proyecto cuando no hay ninguno", () => {
    const recs = RecommendationEngine.recommend([], { projectsCreated: 0, features: [] });

    expect(recs.some((r) => r.type === "next-step" && r.title.includes("primer proyecto"))).toBe(true);
  });

  it("sugiere funciones no usadas", () => {
    const recs = RecommendationEngine.recommend([], {
      projectsCreated: 1,
      features: [{ feature: "dashboard_analytics", uses: 1, lastUsed: null }],
    });

    expect(recs.some((r) => r.type === "feature" && r.title.startsWith("Descubre"))).toBe(true);
  });

  it("sugiere revisar feedback negativo", () => {
    const recs = RecommendationEngine.recommend([], {
      projectsCreated: 1,
      features: [],
      negativeFeedback: 2,
    });

    expect(recs.some((r) => r.type === "tip" && r.title.includes("marcadas"))).toBe(true);
  });

  it("sugiere mejorar a IA Pro cuando el nivel es FLASH", () => {
    const recs = RecommendationEngine.recommend([], {
      projectsCreated: 1,
      features: [],
      settings: { aiLevel: "FLASH" },
    });

    expect(recs.some((r) => r.title.includes("IA Pro"))).toBe(true);
  });

  it("cae a recomendación de módulo Enterprise cuando no hay nada más", () => {
    const recs = RecommendationEngine.recommend([], {
      projectsCreated: 1,
      features: ["dashboard_analytics", "dashboard_insights", "dashboard_reports",
        "dashboard_recommendations", "enterprise", "settings"].map((f) => ({
        feature: f, uses: 1, lastUsed: null,
      })),
      settings: { aiLevel: "PRO" },
    });

    expect(recs.some((r) => r.type === "related" && r.title.includes("Enterprise"))).toBe(true);
  });
});
