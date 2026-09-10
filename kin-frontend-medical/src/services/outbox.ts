import { API_URL } from "@/services/session";

/** Estado de un evento en el outbox (refleja el enum OutboxStatus del backend). */
export type OutboxStatus = "PENDING" | "PUBLISHED" | "FAILED" | "DEAD_LETTER";

/** Registro de un evento en el outbox (refleja OutboxRecord del backend). */
export interface OutboxRecord {
  id: string;
  aggregate_id: string;
  event_type: string;
  payload: string;
  metadata: string | null;
  status: OutboxStatus;
  retry_count: number;
  created_at: string;
  published_at: string | null;
  last_error: string | null;
}

/** Respuesta paginada para listar eventos DEAD_LETTER. */
export interface DeadLetterPage {
  content: OutboxRecord[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

/** Estadísticas de la Dead Letter Queue. */
export interface DeadLetterStats {
  deadLetterCount: number;
  byEventType: Array<{ event_type: string; count: number }>;
  topAggregates: Array<{ aggregate_id: string; count: number }>;
}

/** Respuesta de acción (retry/delete). */
export interface DeadLetterActionResponse {
  message: string;
  id?: string;
  deletedCount?: string;
}

/** Error de API tipado. */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly body?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/** Objeto de ayuda para peticiones JSON con credenciales incluidas. */
async function jsonRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    headers: { Accept: "application/json", ...options.headers },
    credentials: "include",
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new ApiError(
      body?.error ?? `Request failed (${res.status})`,
      res.status,
      body,
    );
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

/** API para la gestión de la Dead Letter Queue del Outbox (admin). */
export const outboxAdminApi = {
  /**
   * Lista eventos en DEAD_LETTER con paginación y filtros.
   */
  listDeadLetter(params: {
    page?: number;
    size?: number;
    aggregateId?: string;
    eventType?: string;
  } = {}): Promise<DeadLetterPage> {
    const searchParams = new URLSearchParams();
    if (params.page !== undefined) searchParams.set("page", String(params.page));
    if (params.size !== undefined) searchParams.set("size", String(params.size));
    if (params.aggregateId) searchParams.set("aggregateId", params.aggregateId);
    if (params.eventType) searchParams.set("eventType", params.eventType);

    return jsonRequest<DeadLetterPage>(
      `/admin/outbox/dead-letter?${searchParams.toString()}`,
    );
  },

  /**
   * Obtiene estadísticas de la DLQ.
   */
  getDeadLetterStats(): Promise<{
    deadLetterCount: number;
    byEventType: Array<{ event_type: string; count: number }>;
    topAggregates: Array<{ aggregate_id: string; count: number }>;
  }> {
    return jsonRequest("/admin/outbox/dead-letter/stats");
  },

  /**
   * Reencola un evento en DEAD_LETTER (cambia status a PENDING y resetea retry_count).
   */
  retryDeadLetter(id: string): Promise<{ message: string; id: string }> {
    return jsonRequest(`/admin/outbox/dead-letter/${id}/retry`, {
      method: "POST",
    });
  },

  /**
   * Elimina un evento específico de DEAD_LETTER.
   */
  deleteDeadLetter(id: string): Promise<{ message: string; id: string }> {
    return jsonRequest(`/admin/outbox/dead-letter/${id}/delete`, {
      method: "POST",
    });
  },

  /**
   * Elimina todos los eventos en DEAD_LETTER (requiere confirm=true).
   */
  deleteAllDeadLetter(confirm = true): Promise<{ message: string; deletedCount: string }> {
    return jsonRequest("/admin/outbox/dead-letter", {
      method: "DELETE",
      body: new URLSearchParams({ confirm: "true" }).toString(),
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
    });
  },
};