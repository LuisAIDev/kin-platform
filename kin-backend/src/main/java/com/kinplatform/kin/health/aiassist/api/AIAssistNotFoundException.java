package com.kinplatform.kin.health.aiassist.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AIAssistNotFoundException extends ResponseStatusException {

    public AIAssistNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Solicitud de IA no encontrada: " + id);
    }
}
