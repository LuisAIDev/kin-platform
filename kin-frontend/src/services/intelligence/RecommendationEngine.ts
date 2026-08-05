import type { AnalyticsEvent } from "@/services/analytics";
import type { FeatureUsage, Recommendation } from "./types";
import { KNOWN_FEATURES } from "./FeatureUsageTracker";

interface RecommendationContext {
  features: FeatureUsage[];
  settings?: {
    displayName?: string;
    aiLevel?: string;
  };
  negativeFeedback: number;
  projectsCreated: number;
}

let sequence = 0;

function nextId(): string {
  return `rec-${++sequence}`;
}

/**
 * Motor de recomendaciones por reglas (Fase 16 — Product Intelligence).
 * Sin IA: solo reglas basadas en analytics, historial y preferencias.
 */
export const RecommendationEngine = {
  recommend(events: AnalyticsEvent[], context: Partial<RecommendationContext> = {}): Recommendation[] {
    const recs: Recommendation[] = [];
    const negativeFeedback = context.negativeFeedback ?? 0;
    const projectsCreated = context.projectsCreated ?? 0;
    const usedFeatures = new Set((context.features ?? []).map((f) => f.feature));

    if (projectsCreated === 0) {
      recs.push({
        id: nextId(),
        type: "next-step",
        title: "Crea tu primer proyecto",
        description: "Empieza a validar tu idea con el asistente de KIN.",
        href: "/dashboard/projects/new",
      });
    }

    const unused = KNOWN_FEATURES.filter((f) => !usedFeatures.has(f));
    if (unused.length > 0) {
      recs.push({
        id: nextId(),
        type: "feature",
        title: `Descubre: ${unused[0].replace("dashboard_", "").replace("_", " ")}`,
        description: `Aún no usas esta funcionalidad: ${unused[0]}.`,
        href: `/${unused[0] === "settings" ? "dashboard/settings" : `dashboard/${unused[0].replace("dashboard_", "")}`}`,
      });
    }

    if (negativeFeedback > 0) {
      recs.push({
        id: nextId(),
        type: "tip",
        title: "Revisa tus respuestas marcadas",
        description: `Tienes ${negativeFeedback} respuesta(s) con feedback negativo. Revisa el historial para ajustar tus consultas.`,
      });
    }

    const aiLevel = context.settings?.aiLevel;
    if (aiLevel === "FLASH") {
      recs.push({
        id: nextId(),
        type: "feature",
        title: "Mejora a IA Pro",
        description: "Activa el nivel Pro en Configuración para respuestas más detalladas.",
        href: "/dashboard/settings",
      });
    }

    if (recs.length === 0) {
      recs.push({
        id: nextId(),
        type: "related",
        title: "Explora el módulo Enterprise",
        description: "Genera reportes y documentos de negocio desde tus proyectos.",
        href: "/dashboard/projects",
      });
    }

    return recs;
  },
};
