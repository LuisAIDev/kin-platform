package com.kinplatform.kin.health.triage.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import com.kinplatform.kin.health.triage.domain.Urgency;
import java.util.List;
import java.util.UUID;

/**
 * Evento de dominio emitido cuando se realiza un triaje (ADR-028).
 *
 * <p>Permite logging, métricas y flujos derivados (alertas clínicas del portal
 * de médicos, ADR-031) sin acoplar el módulo al bus de eventos. Incluye la
 * urgencia máxima y los nombres de las condiciones para que los listeners
 * decidan de forma determinista (p. ej. alerta si urgencia ALTA). Implementa
 * {@link HasUserId} para enriquecer la metadata del outbox transaccional.</p>
 */
public record TriagePerformedEvent(
        UUID userId,
        UUID projectId,
        List<String> symptoms,
        int resultCount,
        Urgency maxUrgency,
        List<String> conditionNames)
        implements DomainEvent, HasUserId {

    /**
     * Constructor de compatibilidad (sin urgencia/condiciones): conserva el
     * contrato previo a ADR-031.
     */
    public TriagePerformedEvent(UUID userId, UUID projectId, List<String> symptoms, int resultCount) {
        this(userId, projectId, symptoms, resultCount, Urgency.BAJA, List.of());
    }

    @Override
    public String type() {
        return "triage_performed";
    }

    @Override
    public Object aggregateId() {
        return projectId;
    }

    @Override
    public UUID userId() {
        return userId;
    }
}
