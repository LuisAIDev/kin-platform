import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import PatientSchedulePage from "@/app/dashboard/patient/schedule/page";

vi.mock("@/services/scheduling", () => ({
  schedulingService: {
    slotsForPhysician: vi.fn(),
    requestAppointment: vi.fn(),
    upcomingAppointments: vi.fn(),
    appointmentHistory: vi.fn(),
    cancelAppointment: vi.fn(),
  },
}));

vi.mock("@/services/telemedicine", () => ({
  telemedicineService: {
    conversations: vi.fn(),
  },
}));

import { schedulingService } from "@/services/scheduling";
import { telemedicineService } from "@/services/telemedicine";

const SLOT = "2026-09-07T09:00:00-05:00";

describe("PatientSchedulePage", () => {
  it("muestra los slots del médico y permite solicitar una cita", async () => {
    (telemedicineService.conversations as ReturnType<typeof vi.fn>).mockResolvedValue([
      { otherId: "m1", otherName: "Dr. Ana García", lastMessage: "", lastMessageAt: "", unread: 0 },
    ]);
    (schedulingService.slotsForPhysician as ReturnType<typeof vi.fn>).mockResolvedValue([SLOT]);
    (schedulingService.upcomingAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (schedulingService.appointmentHistory as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (schedulingService.requestAppointment as ReturnType<typeof vi.fn>).mockResolvedValue({});

    const user = userEvent.setup();
    render(<PatientSchedulePage />);

    await waitFor(() =>
      expect(screen.getByText("09:00")).toBeInTheDocument(),
    );

    await user.click(screen.getByText("09:00"));

    await waitFor(() =>
      expect(schedulingService.requestAppointment).toHaveBeenCalledWith(
        expect.objectContaining({ physicianId: "m1", scheduledAt: SLOT }),
      ),
    );
    await waitFor(() =>
      expect(screen.getByText(/Cita solicitada/)).toBeInTheDocument(),
    );
  });

  it("muestra estado sin horarios disponibles", async () => {
    (telemedicineService.conversations as ReturnType<typeof vi.fn>).mockResolvedValue([
      { otherId: "m1", otherName: "Dr. Test", lastMessage: "", lastMessageAt: "", unread: 0 },
    ]);
    (schedulingService.slotsForPhysician as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (schedulingService.upcomingAppointments as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    (schedulingService.appointmentHistory as ReturnType<typeof vi.fn>).mockResolvedValue([]);

    render(<PatientSchedulePage />);

    await waitFor(() =>
      expect(screen.getByText("Sin horarios disponibles para esta fecha.")).toBeInTheDocument(),
    );
  });
});
