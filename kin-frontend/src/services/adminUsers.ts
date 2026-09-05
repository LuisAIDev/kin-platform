import { api } from "./api";

export interface PendingPhysician {
  id: string;
  email: string;
  fullName: string;
  /** Persona/rol ORIGINAL del solicitante (FREE/PREMIUM/PATIENT/PHYSICIAN). */
  role: string | null;
  licenseNumber: string | null;
  specialty: string | null;
  country: string | null;
  phone: string | null;
  createdAt: string;
}

/** Administración de usuarios (vertical Salud): verificación de médicos. */
export const adminUsersService = {
  pendingPhysicians: () => api.get<PendingPhysician[]>("/admin/users/physicians/pending"),

  approvePhysician: (userId: string) =>
    api.post<void>(`/admin/users/physicians/${userId}/approve`, {}),

  rejectPhysician: (userId: string, reason?: string) =>
    api.post<void>(`/admin/users/physicians/${userId}/reject`, { reason: reason ?? null }),
};
