package com.kinplatform.common.eventbus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Servicio de idempotencia para eventos de dominio.
 *
 * <p>Permite a los listeners verificar si un evento ya fue procesado exitosamente
 * y registrar su procesamiento exitoso, evitando duplicados cuando el relé del
 * outbox reintenta la entrega (semántica at-least-once).</p>
 *
 * <p>La tabla {@code processed_events} se consulta e inserta dentro de la misma
 * transacción que el procesamiento del evento, garantizando consistencia.</p>
 */
@Service
public class IdempotencyService {

    private static final String CHECK_SQL = """
        SELECT 1 FROM processed_events WHERE event_id = ?
        """;

    private static final String INSERT_SQL = """
        INSERT INTO processed_events (event_id, aggregate_id, event_type, processed_at)
        VALUES (?, ?, ?, ?)
        """;

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verifica si un evento ya fue procesado.
     *
     * @param eventId identificador único del evento (hash o correlationId)
     * @return {@code true} si ya fue procesado, {@code false} en caso contrario
     */
    public boolean isProcessed(String eventId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(CHECK_SQL, Boolean.class, eventId));
    }

    /**
     * Registra un evento como procesado exitosamente.
     *
     * @param eventId identificador único del evento
     * @param aggregateId ID del agregado origen
     * @param eventType FQCN del tipo de evento
     */
    public void markProcessed(String eventId, UUID aggregateId, String eventType) {
        jdbcTemplate.update(INSERT_SQL, eventId, aggregateId, eventType, OffsetDateTime.now());
    }

    /**
     * Verifica y registra atómicamente: si el evento no fue procesado, lo marca
     * como procesado y retorna {@code true} (proceder con el procesamiento).
     * Si ya fue procesado, retorna {@code false} (saltarse el procesamiento).
     *
     * @param eventId identificador único del evento
     * @param aggregateId ID del agregado origen
     * @param eventType FQCN del tipo de evento
     * @return {@code true} si el listener debe procesar el evento, {@code false} si ya fue procesado
     */
    public boolean tryMarkProcessed(String eventId, UUID aggregateId, String eventType) {
        if (isProcessed(eventId)) {
            return false;
        }
        markProcessed(eventId, aggregateId, eventType);
        return true;
    }
}
