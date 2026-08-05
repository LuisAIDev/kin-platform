export interface TokenCostEstimate {
  tokens: number;
  cost: number;
}

const CHARS_PER_TOKEN = 4;
const DEFAULT_RATE_PER_1K = 0.0005;

/**
 * Estimación de tokens y coste (Fase 15 — cost intelligence). Heurística
 * determinista (chars/4) sin dependencias externas ni llamadas a APIs.
 */
export const tokenEstimator = {
  estimateTokens(text: string | null | undefined): number {
    if (!text) return 0;
    return Math.max(1, Math.ceil(text.length / CHARS_PER_TOKEN));
  },

  estimateCost(tokens: number, ratePer1k: number = DEFAULT_RATE_PER_1K): number {
    if (tokens <= 0) return 0;
    return Number(((tokens / 1000) * ratePer1k).toFixed(6));
  },

  estimate(text: string | null | undefined, ratePer1k: number = DEFAULT_RATE_PER_1K): TokenCostEstimate {
    const tokens = this.estimateTokens(text);
    return { tokens, cost: this.estimateCost(tokens, ratePer1k) };
  },
};
