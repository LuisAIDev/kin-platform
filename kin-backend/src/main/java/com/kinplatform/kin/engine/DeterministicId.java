package com.kinplatform.kin.engine;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utilidad para generar identificadores deterministas (UUID v3 derivado de
 * contenido). Compartida por todos los motores para que un mismo elemento
 * produzca siempre el mismo id — trazabilidad reproducible sin estado.
 */
public final class DeterministicId {

    private DeterministicId() {
    }

    public static UUID from(String category, String title, String description) {
        return UUID.nameUUIDFromBytes((category + "|" + title + "|" + description)
            .getBytes(StandardCharsets.UTF_8));
    }
}
