import type { AnalyticsEvent } from "@/services/analytics";
import { tokenEstimator } from "@/services/tokenEstimator";
import type { UsageStatistics as UsageStatisticsData } from "./types";

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Estadísticas de uso (Fase 16 — Product Intelligence). Agrega eventos de
 * analytics de forma determinista (offline, sin IA).
 */
export const UsageStatistics = {
  aggregate(events: AnalyticsEvent[], now: Date = new Date()): UsageStatisticsData {
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const activeDays = new Set<number>();
    let dailyCount = 0;
    let weeklyCount = 0;
    let monthlyCount = 0;
    let messagesSent = 0;
    let sessions = 0;
    let conversations = 0;
    let projectsCreated = 0;
    let positiveFeedback = 0;
    let negativeFeedback = 0;
    let tokens = 0;

    for (const event of events ?? []) {
      const ts = new Date(event.timestamp).getTime();
      if (Number.isNaN(ts)) continue;
      activeDays.add(Math.floor(ts / DAY_MS));

      const isToday = ts >= startOfToday;
      const isWeek = ts >= startOfToday - 7 * DAY_MS;
      const isMonth = ts >= startOfToday - 30 * DAY_MS;
      if (isToday) dailyCount++;
      if (isWeek) weeklyCount++;
      if (isMonth) monthlyCount++;

      switch (event.name) {
        case "ai_message":
          messagesSent++;
          tokens += tokenEstimator.estimateTokens(
            typeof event.props?.content === "string" ? event.props.content : undefined);
          break;
        case "session_start":
          sessions++;
          break;
        case "conversation_started":
          conversations++;
          break;
        case "project_created":
          projectsCreated++;
          break;
        case "ai_feedback":
          if (event.props?.feedback === "up") positiveFeedback++;
          if (event.props?.feedback === "down") negativeFeedback++;
          break;
      }
    }

    return {
      dailyCount,
      weeklyCount,
      monthlyCount,
      messagesSent,
      sessions,
      activeDays: activeDays.size,
      estimatedTokens: tokens,
      estimatedCost: tokenEstimator.estimateCost(tokens),
      positiveFeedback,
      negativeFeedback,
      projectsCreated,
      conversations,
    };
  },
};
