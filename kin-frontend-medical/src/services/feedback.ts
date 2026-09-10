import { analytics } from "@/services/analytics";

export type Feedback = "up" | "down";

/**
 * Feedback de respuestas IA (Fase 15): 👍/👎 y reporte de respuesta.
 * Solo almacenamiento local (analytics) — sin envío a terceros.
 */
export const feedbackService = {
  submit(feedback: Feedback, assistantMessageId?: string): void {
    analytics.track("ai_feedback", { feedback, assistantMessageId });
  },

  report(message?: string): void {
    analytics.track("ai_feedback_report", { message });
  },
};
