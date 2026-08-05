export interface AnalyticsEvent {
  name: string;
  props?: Record<string, unknown>;
  timestamp: string;
}

const STORAGE_KEY = "kin_analytics_events_v1";
const MAX_EVENTS = 100;

/**
 * Analytics de producto (Fase 14): registra eventos de uso en localStorage y
 * consola. NO envía datos externos (sin red, sin trackers de terceros).
 */
export const analytics = {
  track(name: string, props?: Record<string, unknown>): void {
    const event: AnalyticsEvent = { name, props, timestamp: new Date().toISOString() };
    if (typeof window !== "undefined") {
      try {
        const stored = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "[]") as AnalyticsEvent[];
        stored.push(event);
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(stored.slice(-MAX_EVENTS)));
      } catch {
        // almacenamiento no disponible
      }
    }
    // eslint-disable-next-line no-console
    console.info("[analytics]", event.name, event.props ?? {});
  },

  events(): AnalyticsEvent[] {
    if (typeof window === "undefined") return [];
    try {
      return JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "[]") as AnalyticsEvent[];
    } catch {
      return [];
    }
  },

  clear(): void {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.removeItem(STORAGE_KEY);
    } catch {
      // almacenamiento no disponible
    }
  },
};
