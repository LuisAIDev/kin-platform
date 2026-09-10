import { describe, expect, it } from "vitest";
import { UsageStatistics } from "@/services/intelligence/UsageStatistics";

const now = new Date("2026-08-05T12:00:00Z");

function event(name: string, timestamp: string, props?: Record<string, unknown>) {
  return { name, props, timestamp };
}

describe("UsageStatistics", () => {
  it("agrega eventos por periodo y cuenta por tipo", () => {
    const stats = UsageStatistics.aggregate([
      event("ai_message", "2026-08-05T10:00:00Z", { content: "hola mundo" }),
      event("ai_message", "2026-08-04T10:00:00Z", { content: "a".repeat(100) }),
      event("project_created", "2026-07-20T10:00:00Z"),
      event("session_start", "2026-08-05T10:00:00Z"),
      event("ai_feedback", "2026-08-05T11:00:00Z", { feedback: "up" }),
      event("ai_feedback", "2026-08-05T11:05:00Z", { feedback: "down" }),
    ], now);

    expect(stats.dailyCount).toBe(4); // hoy: 4 eventos
    expect(stats.weeklyCount).toBe(5); // últimos 7 días: 5
    expect(stats.monthlyCount).toBe(6);
    expect(stats.messagesSent).toBe(2);
    expect(stats.projectsCreated).toBe(1);
    expect(stats.sessions).toBe(1);
    expect(stats.positiveFeedback).toBe(1);
    expect(stats.negativeFeedback).toBe(1);
    expect(stats.estimatedTokens).toBeGreaterThan(0);
    expect(stats.activeDays).toBeGreaterThanOrEqual(1);
  });

  it("maneja eventos vacíos y timestamps inválidos", () => {
    const stats = UsageStatistics.aggregate([
      event("ai_message", "no-es-fecha"),
      ...[],
    ], now);

    expect(stats.messagesSent).toBe(0);
    expect(stats.dailyCount).toBe(0);
    expect(stats.activeDays).toBe(0);
    expect(UsageStatistics.aggregate([], now).dailyCount).toBe(0);
  });

  it("ignora feedback sin tipo", () => {
    const stats = UsageStatistics.aggregate([
      event("ai_feedback", "2026-08-05T10:00:00Z", { feedback: "meh" }),
    ], now);

    expect(stats.positiveFeedback).toBe(0);
    expect(stats.negativeFeedback).toBe(0);
  });
});
