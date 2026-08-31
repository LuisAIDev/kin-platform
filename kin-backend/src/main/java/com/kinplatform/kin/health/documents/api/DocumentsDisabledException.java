package com.kinplatform.kin.health.documents.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * El módulo de documentos clínicos está deshabilitado.
 */
public class DocumentsDisabledException extends ResponseStatusException {

    public DocumentsDisabledException() {
        super(HttpStatus.NOT_FOUND, "El módulo de documentos está deshabilitado");
    }
}
