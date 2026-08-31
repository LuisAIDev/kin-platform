import { api } from "./api";

export type FollowUpFrequency = "DAILY" | "WEEKLY" | "MONTHLY";
export type FollowUpStatus = "ACTIVE" | "PAUSED" | "COMPLETED";
export type FollowUpTaskStatus = "PENDING" | "COMPLETED" | "OVERDUE";

export interface FollowUpTask {
  taskId: string;
  planId: string;
  description: string;
  dueDate: string;
  status: FollowUpTaskStatus;
  completedAt: string | null;
}

export interface FollowUpPlan {
  planId: string;
  patientId: string;
  physicianId: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string | null;
  frequency: FollowUpFrequency;
  status: FollowUpStatus;
  createdAt: string;
  tasks: FollowUpTask[];
}

export interface PatientEvolution {
  id: string;
  patientId: string;
  physicianId: string;
  recordedAt: string;
  symptoms: string;
  vitals: Record<string, unknown>;
  medicationAdherence: boolean | null;
  notes: string;
}

export interface CreatePlanRequest {
  patientId: string;
  title: string;
  description?: string;
  frequency: FollowUpFrequency;
  startDate: string;
  endDate?: string;
}

export const followUpService = {
  // Médico
  createPlan: (req: CreatePlanRequest) => api.post<FollowUpPlan>("/health/followup/plans", req),

  addTask: (planId: string, description: string, dueDate: string) =>
    api.post<FollowUpTask>(`/health/followup/plans/${planId}/tasks`, { description, dueDate }),

  plansForPatient: (patientId: string) =>
    api.get<FollowUpPlan[]>(`/health/followup/patients/${patientId}/plans`),

  recordEvolution: (
    patientId: string,
    symptoms: string,
    vitals: Record<string, unknown>,
    medicationAdherence: boolean,
    notes: string,
  ) =>
    api.post<PatientEvolution>(`/health/followup/patients/${patientId}/evolution`, {
      symptoms,
      vitals,
      medicationAdherence,
      notes,
    }),

  evolutionHistory: (patientId: string) =>
    api.get<PatientEvolution[]>(`/health/followup/patients/${patientId}/evolution`),

  overdueTasks: () => api.get<FollowUpTask[]>("/health/followup/tasks/overdue"),

  // Paciente
  activePlans: () => api.get<FollowUpPlan[]>("/health/followup/plans/active"),

  completeTask: (taskId: string) =>
    api.post<FollowUpTask>(`/health/followup/tasks/${taskId}/complete`, {}),

  ownEvolution: () => api.get<PatientEvolution[]>("/health/followup/evolution"),
};
