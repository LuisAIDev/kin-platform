import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import FollowUpPlanCard from "@/components/followup/FollowUpPlanCard";
import type { FollowUpPlan } from "@/services/followup";

const PLAN: FollowUpPlan = {
  planId: "plan1",
  patientId: "p1",
  physicianId: "m1",
  title: "Control de presión",
  description: "Medir dos veces al día",
  startDate: "2026-08-26T10:00:00Z",
  endDate: null,
  frequency: "DAILY",
  status: "ACTIVE",
  createdAt: "2026-08-26T10:00:00Z",
  tasks: [
    {
      taskId: "t1",
      planId: "plan1",
      description: "Tomar medicación",
      dueDate: "2026-08-27T10:00:00Z",
      status: "PENDING",
      completedAt: null,
    },
    {
      taskId: "t2",
      planId: "plan1",
      description: "Registrar presión",
      dueDate: "2026-08-26T10:00:00Z",
      status: "COMPLETED",
      completedAt: "2026-08-26T09:00:00Z",
    },
  ],
};

describe("FollowUpPlanCard", () => {
  it("muestra el plan y sus tareas", () => {
    render(<FollowUpPlanCard plan={PLAN} mode="patient" />);

    expect(screen.getByText("Control de presión")).toBeInTheDocument();
    expect(screen.getByText("Tomar medicación")).toBeInTheDocument();
    expect(screen.getByText("Registrar presión")).toBeInTheDocument();
    expect(screen.getByText("Pendiente")).toBeInTheDocument();
    expect(screen.getByText("Completada")).toBeInTheDocument();
  });

  it("en modo paciente permite completar tareas pendientes", async () => {
    const onCompleteTask = vi.fn();
    const user = userEvent.setup();
    render(<FollowUpPlanCard plan={PLAN} mode="patient" onCompleteTask={onCompleteTask} />);

    await user.click(screen.getByRole("button", { name: "Completar" }));

    expect(onCompleteTask).toHaveBeenCalledWith("t1");
  });

  it("en modo médico muestra el formulario para añadir tareas", async () => {
    const onAddTask = vi.fn();
    const user = userEvent.setup();
    render(<FollowUpPlanCard plan={PLAN} mode="physician" onAddTask={onAddTask} />);

    await user.type(screen.getByLabelText("Nueva tarea para Control de presión"), "Nueva tarea");
    await user.type(screen.getByLabelText("Fecha de vencimiento"), "2026-08-28");
    await user.click(screen.getByRole("button", { name: "Añadir tarea" }));

    await waitFor(() =>
      expect(onAddTask).toHaveBeenCalledWith("plan1", "Nueva tarea", expect.any(String)),
    );
  });
});
