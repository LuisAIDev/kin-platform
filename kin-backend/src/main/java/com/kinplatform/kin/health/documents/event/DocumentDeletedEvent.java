package com.kinplatform.kin.health.documents.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un documento clínico se elimina (soft delete).
 */
public record DocumentDeletedEvent(UUID documentId, UUID patientId, UUID physicianId, UUID deletedBy)
        implements DomainEvent, HasUserId {

    @Override
    public String type() {
        return "document_deleted";
    }

    @Override
    public Object aggregateId() {
        return documentId;
    }

    @Override
    public UUID userId() {
        return patientId;
    }
}

