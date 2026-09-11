import type { AnalyticsEvent } from "@/services/analytics";
import type { FeatureUsage } from "./types";

export const KNOWN_FEATURES = [
  "dashboard_analytics",
  "dashboard_insights",
  "dashboard_reports",
  "dashboard_recommendations",
  "enterprise",
  "settings",
];

/**
 * Seguimiento de adopción de funciones (Fase 16 — Product Intelligence).
 * Registra y agrega uso de funciones desde eventos de analytics (offline).
 */
export const FeatureUsageTracker = {
  record(feature: string): void {
    if (typeof window === "undefined") return;
    try {
      const list = JSON.parse(window.localStorage.getItem("kin_feature_usage_v1") ?? "[]") as FeatureUsage[];
      const existing = list.find((f) => f.feature === feature);
      if (existing) {
        existing.uses += 1;
        existing.lastUsed = new Date().toISOString();
      } else {
        list.push({ feature, uses: 1, lastUsed: new Date().toISOString() });
      }
      window.localStorage.setItem("kin_feature_usage_v1", JSON.stringify(list.slice(-200)));
    } catch {
      // almacenamiento no disponible
    }
  },

  list(): FeatureUsage[] {
    if (typeof window === "undefined") return [];
    try {
      return JSON.parse(window.localStorage.getItem("kin_feature_usage_v1") ?? "[]") as FeatureUsage[];
    } catch {
      return [];
    }
  },

  mergeWithEvents(events: AnalyticsEvent[]): FeatureUsage[] {
    const merged = new Map<string, FeatureUsage>();
    for (const usage of this.list()) {
      merged.set(usage.feature, { ...usage });
    }
    for (const event of events ?? []) {
      if (event.name !== "feature_used") continue;
      const feature = String(event.props?.feature ?? "");
      if (!feature) continue;
      const current = merged.get(feature);
      if (current) {
        current.uses += 1;
        if (!current.lastUsed || event.timestamp > current.lastUsed) {
          current.lastUsed = event.timestamp;
        }
      } else {
        merged.set(feature, { feature, uses: 1, lastUsed: event.timestamp });
      }
    }
    return [...merged.values()];
  },

  unused(): string[] {
    const used = new Set(this.list().map((f) => f.feature));
    return KNOWN_FEATURES.filter((feature) => !used.has(feature));
  },
};
