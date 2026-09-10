import type { AnalyticsEvent } from "@/services/analytics";
import type { ProductMetrics as ProductMetricsData, TimelineEvent } from "./types";

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Métricas de producto (Fase 16 — Product Intelligence): retención, activación,
 * engagement, sesiones, embudo de onboarding y uso por área. Offline, sin IA.
 */
export const ProductMetrics = {
  compute(events: AnalyticsEvent[], now: Date = new Date()): ProductMetricsData {
    const list = events ?? [];
    if (list.length === 0) {
      return {
        retention: 0, activation: 0, engagement: 0, sessions: 0,
        onboardingCompletion: 0, aiUsage: 0, dashboardUsage: 0, projectsUsage: 0,
      };
    }
    const days = new Set<number>();
    for (const event of list) {
      const ts = new Date(event.timestamp).getTime();
      if (!Number.isNaN(ts)) days.add(Math.floor(ts / DAY_MS));
    }
    const sortedDays = [...days].sort((a, b) => a - b);
    const first = sortedDays[0];
    const last = sortedDays[sortedDays.length - 1];
    const spanDays = first !== undefined && last !== undefined ? Math.max(1, last - first + 1) : 1;

    const sessions = list.filter((e) => e.name === "session_start").length;
    const aiMessages = list.filter((e) => e.name === "ai_message").length;
    const projectViews = list.filter((e) => e.name === "page_view" && e.props?.page === "projects").length;
    const featureUsage = list.filter((e) => e.name === "feature_used").length;

    // Retención (aproximada): días con actividad / días transcurridos
    const elapsedDays = Math.max(1, Math.floor((now.getTime() - first * DAY_MS) / DAY_MS) + 1);
    const retention = Math.min(100, Math.round((days.size / elapsedDays) * 100));

    // Activación: al menos una conversación + un proyecto
    const conversations = list.filter((e) => e.name === "conversation_started").length;
    const projects = list.filter((e) => e.name === "project_created").length;
    const activation = conversations > 0 && projects > 0 ? 100 : conversations > 0 ? 60 : 0;

    const engagement = list.length ? Math.round(list.length / spanDays) : 0;

    const onboardingSteps = ["project_created", "ai_message", "feature_used"];
    const completed = onboardingSteps.filter((step) => list.some((e) => e.name === step)).length;
    const onboardingCompletion = Math.round((completed / onboardingSteps.length) * 100);

    return {
      retention,
      activation,
      engagement,
      sessions,
      onboardingCompletion,
      aiUsage: aiMessages,
      dashboardUsage: featureUsage,
      projectsUsage: projectViews,
    };
  },

  buildTimeline(events: AnalyticsEvent[]): TimelineEvent[] {
    return (events ?? [])
      .filter((e) => new Date(e.timestamp).getTime() > 0)
      .map((e) => ({
        id: `${e.name}-${e.timestamp}`,
        timestamp: e.timestamp,
        name: e.name,
        details: e.props,
      }))
      .sort((a, b) => (a.timestamp < b.timestamp ? 1 : -1));
  },
};
