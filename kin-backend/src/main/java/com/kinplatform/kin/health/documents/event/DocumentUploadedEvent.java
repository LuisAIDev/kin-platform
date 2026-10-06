package com.kinplatform.kin.health.documents.event;

import com.kinplatform.common.event.DomainEvent;
import com.kinplatform.common.event.HasUserId;
import java.util.UUID;

/**
 * Evento emitido cuando un médico sube y comparte un documento con un paciente.
 */
public record DocumentUploadedEvent(UUID documentId, UUID patientId, UUID physicianId, String fileName)
        implements DomainEvent, HasUserId {

    public DocumentUploadedEvent {
        fileName = fileName == null ? "" : fileName;
    }

    @Override
    public String type() {
        return "document_uploaded";
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

