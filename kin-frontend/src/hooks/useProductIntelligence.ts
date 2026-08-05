"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { analytics, type AnalyticsEvent } from "@/services/analytics";
import { AnalyticsAggregator } from "@/services/intelligence/AnalyticsAggregator";
import { FeatureUsageTracker } from "@/services/intelligence/FeatureUsageTracker";
import type { ProductIntelligence } from "@/services/intelligence/types";

/**
 * Hook de inteligencia de producto (Fase 16): agrega métricas desde analytics
 * local y registra el uso de la página actual como feature.
 */
export function useProductIntelligence(feature?: string) {
  const [events, setEvents] = useState<AnalyticsEvent[]>(() => analytics.events());
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    if (feature) {
      FeatureUsageTracker.record(feature);
    }
  }, [feature]);

  const refresh = useCallback(() => {
    setEvents(analytics.events());
    setNow(new Date());
  }, []);

  const intelligence: ProductIntelligence = useMemo(
    () => AnalyticsAggregator.compute(events, now),
    [events, now],
  );

  return { intelligence, refresh };
}
