package com.kinplatform.kin.health.documents.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El documento clínico no existe o no está activo.
 */
public class DocumentNotFoundException extends ResponseStatusException {

    public DocumentNotFoundException(UUID documentId) {
        super(HttpStatus.NOT_FOUND, "Documento no encontrado: " + documentId);
    }
}
