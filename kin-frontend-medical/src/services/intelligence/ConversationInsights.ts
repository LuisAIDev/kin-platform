import type { AnalyticsEvent } from "@/services/analytics";
import type { ConversationInsight } from "./types";

const STOPWORDS = new Set(["el", "la", "los", "las", "de", "del", "que", "y", "en", "a", "es",
  "mi", "me", "para", "una", "un", "con", "por", "como", "tengo", "quiero"]);

function normalize(text: string): string {
  return text.toLowerCase()
    .normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

function tokensOf(text: string): string[] {
  return normalize(text).split(/[^a-z0-9]+/).filter((w) => w.length > 3 && !STOPWORDS.has(w));
}

function topTopics(events: AnalyticsEvent[], limit: number): string[] {
  const freq = new Map<string, number>();
  for (const event of events ?? []) {
    if (event.name !== "ai_message") continue;
    for (const token of tokensOf(String(event.props?.content ?? ""))) {
      freq.set(token, (freq.get(token) ?? 0) + 1);
    }
  }
  return [...freq.entries()].sort((a, b) => b[1] - a[1]).slice(0, limit).map(([w]) => w);
}

function predominantIntent(events: AnalyticsEvent[]): string {
  const intents: Record<string, RegExp> = {
    regulatorio: /sas|tramite|permiso|licencia|constituir|impuesto/,
    financiero: /inversion|costo|presupuesto|rentabilidad|capital/,
    mercado: /mercado|sector|demanda|clientes|competencia/,
    documento: /pdf|documento|analiza|archivo/,
  };
  const counts: Record<string, number> = {};
  for (const event of events ?? []) {
    if (event.name !== "ai_message") continue;
    const content = normalize(String(event.props?.content ?? ""));
    for (const [intent, pattern] of Object.entries(intents)) {
      if (pattern.test(content)) counts[intent] = (counts[intent] ?? 0) + 1;
    }
  }
  return Object.entries(counts).sort((a, b) => b[1] - a[1])[0]?.[0] ?? "general";
}

/**
 * Análisis determinista de conversaciones (Fase 16 — Product Intelligence).
 * Funciona offline, sin IA.
 */
export const ConversationInsights = {
  analyze(events: AnalyticsEvent[]): ConversationInsight {
    const messages = (events ?? []).filter((e) => e.name === "ai_message");
    const lengths = messages.map((e) => String(e.props?.content ?? "").length);
    const averageLength = lengths.length
      ? Math.round(lengths.reduce((a, b) => a + b, 0) / lengths.length) : 0;

    const timestamps = messages.map((e) => new Date(e.timestamp).getTime())
      .filter((t) => !Number.isNaN(t)).sort((a, b) => a - b);
    const durationMs = timestamps.length > 1 ? timestamps[timestamps.length - 1] - timestamps[0] : 0;

    const questionRegex = /[?¿]|\bqu[eé]\b|\bc[oó]mo\b|\bcu[aá]ndo\b/i;
    const questions = messages.filter((e) => questionRegex.test(String(e.props?.content ?? ""))).length;

    const positive = (events ?? []).filter((e) => e.name === "ai_feedback" && e.props?.feedback === "up").length;
    const negative = (events ?? []).filter((e) => e.name === "ai_feedback" && e.props?.feedback === "down").length;
    const feedbackTotal = positive + negative;
    const satisfaction = feedbackTotal ? Math.round((positive / feedbackTotal) * 100) : 0;

    const goodLength = lengths.filter((l) => l > 100).length;
    const responseQuality = lengths.length ? Math.round((goodLength / lengths.length) * 100) : 0;

    return {
      averageLength,
      questionsPerSession: messages.length ? Math.round((questions / messages.length) * 100) : 0,
      durationMs,
      predominantIntent: predominantIntent(events ?? []),
      frequentTopics: topTopics(events ?? [], 5),
      responseQuality,
      satisfaction,
    };
  },
};
