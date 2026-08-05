import { beforeEach, describe, expect, it, vi } from "vitest";
import { feedbackService } from "@/services/feedback";
import { analytics } from "@/services/analytics";

describe("feedbackService", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(console, "info").mockImplementation(() => {});
  });

  it("registra feedback 👍 con analytics local", () => {
    feedbackService.submit("up", "msg-1");

    const events = analytics.events();
    expect(events).toHaveLength(1);
    expect(events[0].name).toBe("ai_feedback");
    expect(events[0].props?.feedback).toBe("up");
    expect(events[0].props?.assistantMessageId).toBe("msg-1");
  });

  it("registra feedback 👎", () => {
    feedbackService.submit("down");

    expect(analytics.events()[0].props?.feedback).toBe("down");
  });

  it("reporta una respuesta marcada", () => {
    feedbackService.report("Respuesta incorrecta");

    const events = analytics.events();
    expect(events[0].name).toBe("ai_feedback_report");
    expect(events[0].props?.message).toBe("Respuesta incorrecta");
  });
});
