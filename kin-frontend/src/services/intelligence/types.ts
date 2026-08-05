import type { AnalyticsEvent } from "@/services/analytics";

export interface UsageStatistics {
  dailyCount: number;
  weeklyCount: number;
  monthlyCount: number;
  messagesSent: number;
  sessions: number;
  activeDays: number;
  estimatedTokens: number;
  estimatedCost: number;
  positiveFeedback: number;
  negativeFeedback: number;
  projectsCreated: number;
  conversations: number;
}

export interface ConversationInsight {
  averageLength: number;
  questionsPerSession: number;
  durationMs: number;
  predominantIntent: string;
  frequentTopics: string[];
  responseQuality: number;
  satisfaction: number;
}

export interface FeatureUsage {
  feature: string;
  uses: number;
  lastUsed: string | null;
}

export interface Recommendation {
  id: string;
  type: "next-step" | "related" | "documentation" | "feature" | "tip";
  title: string;
  description: string;
  href?: string;
}

export interface TimelineEvent {
  id: string;
  timestamp: string;
  name: string;
  details?: Record<string, unknown>;
}

export interface ProductMetrics {
  retention: number;
  activation: number;
  engagement: number;
  sessions: number;
  onboardingCompletion: number;
  aiUsage: number;
  dashboardUsage: number;
  projectsUsage: number;
}

export interface ProductIntelligence {
  usage: UsageStatistics;
  insights: ConversationInsight;
  metrics: ProductMetrics;
  features: FeatureUsage[];
  recommendations: Recommendation[];
  timeline: TimelineEvent[];
}

export type { AnalyticsEvent };
