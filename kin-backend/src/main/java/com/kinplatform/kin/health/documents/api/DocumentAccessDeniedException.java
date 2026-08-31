package com.kinplatform.kin.health.documents.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El usuario no tiene permiso para operar sobre este documento clínico.
 */
public class DocumentAccessDeniedException extends ResponseStatusException {

    public DocumentAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
