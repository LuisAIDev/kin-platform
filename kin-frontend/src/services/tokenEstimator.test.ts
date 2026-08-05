import { describe, expect, it } from "vitest";
import { tokenEstimator } from "@/services/tokenEstimator";

describe("tokenEstimator", () => {
  it("estima tokens con la heurística chars/4", () => {
    expect(tokenEstimator.estimateTokens(null)).toBe(0);
    expect(tokenEstimator.estimateTokens("")).toBe(0);
    expect(tokenEstimator.estimateTokens("abcd")).toBe(1);
    expect(tokenEstimator.estimateTokens("abcdefgh")).toBe(2);
  });

  it("estima el coste según la tarifa por 1000 tokens", () => {
    expect(tokenEstimator.estimateCost(0)).toBe(0);
    expect(tokenEstimator.estimateCost(1000, 0.0005)).toBe(0.0005);
    expect(tokenEstimator.estimateCost(2000, 0.001)).toBe(0.002);
  });

  it("usa tarifa por defecto y devuelve tokens + coste", () => {
    const result = tokenEstimator.estimate("a".repeat(400));

    expect(result.tokens).toBe(100);
    expect(result.cost).toBeGreaterThan(0);
  });
});
