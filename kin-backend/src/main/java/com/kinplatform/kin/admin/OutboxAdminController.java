package com.kinplatform.kin.admin;

import com.kinplatform.common.eventbus.domain.OutboxRecord;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de administración para la Dead Letter Queue del Outbox (PR 4).
 *
 * <p>Permiten listar, reencolar, eliminar eventos en estado DEAD_LETTER.
 * Solo accesibles por usuarios con rol ADMIN.</p>
 */
@RestController
@RequestMapping("/admin/outbox/dead-letter")
@PreAuthorize("hasRole('ADMIN')")
public class OutboxAdminController {

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public OutboxAdminController(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Lista eventos en DEAD_LETTER con paginación y filtros.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listDeadLetter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID aggregateId,
            @RequestParam(required = false) String eventType) {

        StringBuilder sql = new StringBuilder(
                """
            SELECT id, aggregate_id, event_type, payload, metadata, status,
                   retry_count, created_at, published_at, last_error
            FROM domain_event_outbox
            WHERE status = 'DEAD_LETTER'
            """);

        if (aggregateId != null) {
            sql.append(" AND aggregate_id = ?");
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type = ?");
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        List<Object> params = new java.util.ArrayList<>();
        if (aggregateId != null) params.add(aggregateId);
        if (eventType != null && !eventType.isBlank()) params.add(eventType);
        params.add(size);
        params.add(page * size);

        List<OutboxRecord> records = jdbcTemplate.query(
                sql.toString(),
                new com.kinplatform.kin.infrastructure.outbox.OutboxRecordRowMapper(),
                params.toArray());

        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_event_outbox WHERE status = 'DEAD_LETTER'", Long.class);

        return ResponseEntity.ok(Map.of(
                "content", records,
                "totalElements", total,
                "totalPages", (total + size - 1) / size,
                "page", page,
                "size", size));
    }

    /**
     * Reencola un evento en DEAD_LETTER: cambia status a PENDING y resetea retry_count.
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, String>> retryDeadLetter(@PathVariable UUID id) {
        int updated = jdbcTemplate.update(
                """
            UPDATE domain_event_outbox
            SET status = 'PENDING', retry_count = 0, last_error = NULL
            WHERE id = ? AND status = 'DEAD_LETTER'
            """,
                id);

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Evento reencolado exitosamente", "id", id.toString()));
    }

    /**
     * Elimina un evento específico de DEAD_LETTER.
     */
    @PostMapping("/{id}/delete")
    public ResponseEntity<Map<String, String>> deleteDeadLetter(@PathVariable UUID id) {
        int deleted =
                jdbcTemplate.update("DELETE FROM domain_event_outbox WHERE id = ? AND status = 'DEAD_LETTER'", id);

        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("message", "Evento eliminado de DEAD_LETTER", "id", id.toString()));
    }

    /**
     * Elimina todos los eventos en DEAD_LETTER.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAllDeadLetter(
            @RequestParam(defaultValue = "false") boolean confirm) {

        if (!confirm) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe confirmar la eliminación con confirm=true"));
        }

        int deleted = jdbcTemplate.update("DELETE FROM domain_event_outbox WHERE status = 'DEAD_LETTER'");

        return ResponseEntity.ok(
                Map.of("message", "Eventos en DEAD_LETTER eliminados", "deletedCount", String.valueOf(deleted)));
    }

    /**
     * Estadísticas de la DLQ.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDeadLetterStats() {
        long deadLetterCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_event_outbox WHERE status = 'DEAD_LETTER'", Long.class);

        var byType = jdbcTemplate.queryForList(
                """
            SELECT event_type, COUNT(*) as count
            FROM domain_event_outbox
            WHERE status = 'DEAD_LETTER'
            GROUP BY event_type
            ORDER BY count DESC
            """);

        var byAggregate = jdbcTemplate.queryForList(
                """
            SELECT aggregate_id, COUNT(*) as count
            FROM domain_event_outbox
            WHERE status = 'DEAD_LETTER'
            GROUP BY aggregate_id
            ORDER BY count DESC
            LIMIT 10
            """);

        return ResponseEntity.ok(Map.of(
                "deadLetterCount", deadLetterCount,
                "byEventType", byType,
                "topAggregates", byAggregate));
    }
}

