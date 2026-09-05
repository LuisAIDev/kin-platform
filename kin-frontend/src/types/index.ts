export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: "FREE" | "PREMIUM" | "FACILITADOR" | "PATIENT" | "PHYSICIAN" | "ADMIN";
  credits: number;
  avatarUrl: string | null;
  createdAt: string;
  emailVerified?: boolean;
  verificationStatus?: string | null;
  /** Capacidad profesional derivada por el backend (PhysicianAccess). Nunca se infiere en el frontend. */
  physicianCapability?: boolean;
}

export interface ChatMessage {
  id: string;
  projectId: string;
  userId: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  metadata: string | null;
  tokensUsed: number;
  createdAt: string;
}
