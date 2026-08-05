import { describe, expect, it } from "vitest";
import { ConversationInsights } from "@/services/intelligence/ConversationInsights";

function event(name: string, timestamp: string, props?: Record<string, unknown>) {
  return { name, props, timestamp };
}

describe("ConversationInsights", () => {
  it("calcula longitud promedio, duración y temas", () => {
    const insights = ConversationInsights.analyze([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "Quiero abrir una panadería en Cartagena" }),
      event("ai_message", "2026-08-05T10:10:00Z", { content: "El mercado de panaderías crece en la costa" }),
    ]);

    expect(insights.averageLength).toBeGreaterThan(0);
    expect(insights.durationMs).toBe(600_000);
    expect(insights.frequentTopics.length).toBeGreaterThan(0);
    expect(insights.frequentTopics).toContain("panadería".replace("í", "i"));
  });

  it("detecta intención predominante", () => {
    const insights = ConversationInsights.analyze([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "Qué permisos necesito para constituir una SAS" }),
    ]);

    expect(insights.predominantIntent).toBe("regulatorio");
  });

  it("calcula preguntas y calidad de respuestas", () => {
    const insights = ConversationInsights.analyze([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "¿Cómo inicio una SAS?" }),
      event("ai_message", "2026-08-05T10:01:00Z", { content: "a".repeat(200) }),
    ]);

    expect(insights.questionsPerSession).toBe(50);
    expect(insights.responseQuality).toBe(50);
  });

  it("calcula satisfacción desde feedback", () => {
    const insights = ConversationInsights.analyze([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "hola" }),
      event("ai_feedback", "2026-08-05T10:01:00Z", { feedback: "up" }),
      event("ai_feedback", "2026-08-05T10:02:00Z", { feedback: "up" }),
      event("ai_feedback", "2026-08-05T10:03:00Z", { feedback: "down" }),
    ]);

    expect(insights.satisfaction).toBe(67);
  });

  it("devuelve valores neutros sin mensajes", () => {
    const insights = ConversationInsights.analyze([]);

    expect(insights.averageLength).toBe(0);
    expect(insights.durationMs).toBe(0);
    expect(insights.predominantIntent).toBe("general");
  });
});
