package com.kinplatform.kin.health.aiassist.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AIAssistDisabledException extends ResponseStatusException {

    public AIAssistDisabledException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "El modulo de IA asistencial esta deshabilitado");
    }
}
