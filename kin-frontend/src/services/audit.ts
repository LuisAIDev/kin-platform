import { api } from "./api";
import type { PageResponse } from "@/types";

export interface AuditLogEntry {
  id: string;
  userId: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  patientId: string | null;
  timestamp: string;
  ipAddress: string;
  userAgent: string;
  details: Record<string, unknown>;
}

export interface AuditFilters {
  userId?: string;
  patientId?: string;
  action?: string;
  startDate?: string;
  endDate?: string;
}

export const auditService = {
  adminLogs: (filters: AuditFilters, page = 0, size = 20) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (filters.userId) params.set("userId", filters.userId);
    if (filters.patientId) params.set("patientId", filters.patientId);
    if (filters.action) params.set("action", filters.action);
    if (filters.startDate) params.set("startDate", filters.startDate);
    if (filters.endDate) params.set("endDate", filters.endDate);
    return api.get<PageResponse<AuditLogEntry>>(`/admin/health/audit/logs?${params.toString()}`);
  },

  myLogs: (page = 0, size = 20) =>
    api.get<PageResponse<AuditLogEntry>>(`/health/audit/my-logs?page=${page}&size=${size}`),
};
