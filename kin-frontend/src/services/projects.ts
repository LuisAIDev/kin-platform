import { api } from "./api";

export interface Project {
  id: string;
  userId: string;
  title: string;
  description: string | null;
  category: string;
  status: string;
  viabilityScore: number | null;
  aiSummary: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  title: string;
  description?: string;
  category: string;
}

export const projectsService = {
  getAll: () => api.get<Project[]>("/projects"),

  getById: (id: string) => api.get<Project>(`/projects/${id}`),

  create: (data: CreateProjectRequest) =>
    api.post<Project>("/projects", data),

  update: (id: string, data: Partial<CreateProjectRequest>) =>
    api.put<Project>(`/projects/${id}`, data),

  delete: (id: string) => api.delete<void>(`/projects/${id}`),
};
