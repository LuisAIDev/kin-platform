import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useNotificationCounts } from "@/hooks/useNotificationCounts";
import { notificationService } from "@/services/notifications";

vi.mock("@/services/notifications", () => ({
  notificationService: {
    counts: vi.fn(),
  },
}));

describe("useNotificationCounts", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("consulta los contadores para un paciente", async () => {
    (notificationService.counts as ReturnType<typeof vi.fn>).mockResolvedValue({
      invitations: 3,
      unreadMessages: 2,
      pendingAppointments: 0,
      upcomingAppointments: 1,
      highUrgencyAlerts: 0,
    });

    const { result } = renderHook(() => useNotificationCounts("PATIENT"));

    await waitFor(() => expect(result.current?.invitations).toBe(3));
    expect(notificationService.counts).toHaveBeenCalledTimes(1);
  });

  it("consulta los contadores para un médico", async () => {
    (notificationService.counts as ReturnType<typeof vi.fn>).mockResolvedValue({
      invitations: 0,
      unreadMessages: 5,
      pendingAppointments: 2,
      upcomingAppointments: 0,
      highUrgencyAlerts: 1,
    });

    const { result } = renderHook(() => useNotificationCounts("PHYSICIAN"));

    await waitFor(() => expect(result.current?.pendingAppointments).toBe(2));
    expect(result.current?.highUrgencyAlerts).toBe(1);
  });

  it("no consulta para roles no de salud", () => {
    renderHook(() => useNotificationCounts("ADMIN"));

    expect(notificationService.counts).not.toHaveBeenCalled();
  });

  it("devuelve null ante errores transitorios", async () => {
    (notificationService.counts as ReturnType<typeof vi.fn>).mockRejectedValue(new Error("401"));

    const { result } = renderHook(() => useNotificationCounts("PATIENT"));

    await waitFor(() => expect(notificationService.counts).toHaveBeenCalledTimes(1));
    expect(result.current).toBeNull();
  });
});
