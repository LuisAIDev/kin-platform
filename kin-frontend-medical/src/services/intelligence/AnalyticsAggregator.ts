import type { AnalyticsEvent } from "@/services/analytics";
import type { ProductIntelligence } from "./types";
import { UsageStatistics } from "./UsageStatistics";
import { ConversationInsights } from "./ConversationInsights";
import { FeatureUsageTracker } from "./FeatureUsageTracker";
import { RecommendationEngine } from "./RecommendationEngine";
import { ProductMetrics } from "./ProductMetrics";

/**
 * Fachada de inteligencia de producto (Fase 16): agrega todas las métricas a
 * partir de eventos de analytics y uso local de funciones. Offline y sin IA.
 */
export const AnalyticsAggregator = {
  compute(events: AnalyticsEvent[], now: Date = new Date()): ProductIntelligence {
    const usage = UsageStatistics.aggregate(events, now);
    const insights = ConversationInsights.analyze(events);
    const metrics = ProductMetrics.compute(events, now);
    const features = FeatureUsageTracker.mergeWithEvents(events);
    const recommendations = RecommendationEngine.recommend(events, {
      features,
      negativeFeedback: usage.negativeFeedback,
      projectsCreated: usage.projectsCreated,
    });
    const timeline = ProductMetrics.buildTimeline(events);

    return { usage, insights, metrics, features, recommendations, timeline };
  },
};
