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
  role: "FREE" | "PREMIUM" | "FACILITADOR" | "ADMIN";
  credits: number;
  avatarUrl: string | null;
  createdAt: string;
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
