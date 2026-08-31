import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import NotificationBadge from "@/components/layout/NotificationBadge";
import { countForNavItem } from "@/components/layout/Sidebar";
import type { NotificationCounts } from "@/services/notifications";

describe("NotificationBadge", () => {
  it("no renderiza nada con contador cero o ausente", () => {
    const { container } = render(<NotificationBadge count={0} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("muestra el contador cuando hay novedades", () => {
    render(<NotificationBadge count={3} />);
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByLabelText("3 novedades")).toBeInTheDocument();
  });

  it("acota el contador a 99+", () => {
    render(<NotificationBadge count={150} />);
    expect(screen.getByText("99+")).toBeInTheDocument();
  });
});

const COUNTS: NotificationCounts = {
  invitations: 3,
  unreadMessages: 2,
  pendingAppointments: 1,
  upcomingAppointments: 4,
  highUrgencyAlerts: 5,
  pendingTasks: 6,
  overdueTasks: 7,
};

describe("countForNavItem", () => {
  it("mapea invitaciones a la página del paciente", () => {
    expect(countForNavItem("/dashboard/patient/invitations", COUNTS)).toBe(3);
  });

  it("mapea mensajes no leídos a las páginas de mensajes de ambos roles", () => {
    expect(countForNavItem("/dashboard/patient/messages", COUNTS)).toBe(2);
    expect(countForNavItem("/dashboard/physician/messages", COUNTS)).toBe(2);
  });

  it("mapea citas próximas al paciente y pendientes al médico", () => {
    expect(countForNavItem("/dashboard/patient/appointments", COUNTS)).toBe(4);
    expect(countForNavItem("/dashboard/physician/appointments", COUNTS)).toBe(1);
  });

  it("mapea alertas ALTA al portal médico", () => {
    expect(countForNavItem("/dashboard/physician", COUNTS)).toBe(5);
  });

  it("mapea tareas de seguimiento al menú de ambos roles", () => {
    expect(countForNavItem("/dashboard/patient/followup", COUNTS)).toBe(6);
    expect(countForNavItem("/dashboard/physician/followup", COUNTS)).toBe(7);
  });

  it("devuelve cero sin contadores o para ítems sin novedades", () => {
    expect(countForNavItem("/dashboard/patient/triage", COUNTS)).toBe(0);
    expect(countForNavItem("/dashboard/physician", null)).toBe(0);
  });
});
