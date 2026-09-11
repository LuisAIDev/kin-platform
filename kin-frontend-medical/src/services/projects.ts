import { api } from "./api";
import type { PageResponse } from "@/types";

export interface Category {
  id: string;
  code: string;
  name: string;
  description: string | null;
  displayOrder: number;
  icon: string | null;
  color: string | null;
  active: boolean;
}

export interface Project {
  id: string;
  userId: string;
  title: string;
  description: string | null;
  category: string;
  categoryName: string | null;
  categoryColor: string | null;
  status: string;
  viabilityScore: number | null;
  aiSummary: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  progressPercentage: number | null;
}

export interface CreateProjectRequest {
  title: string;
  description?: string;
  category: string;
}

export const projectsService = {
  getAll: (page = 0, size = 12) =>
    api.get<PageResponse<Project>>(`/projects?page=${page}&size=${size}`),

  getById: (id: string) => api.get<Project>(`/projects/${id}`),

  create: (data: CreateProjectRequest) =>
    api.post<Project>("/projects", data),

  update: (id: string, data: Partial<CreateProjectRequest>) =>
    api.put<Project>(`/projects/${id}`, data),

  delete: (id: string) => api.delete<void>(`/projects/${id}`),

  getCategories: () => api.get<Category[]>("/categories"),
};
