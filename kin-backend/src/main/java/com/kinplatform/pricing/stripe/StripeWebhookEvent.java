package com.kinplatform.pricing.stripe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro de eventos de webhook ya procesados, para garantizar idempotencia:
 * Stripe puede reentregar el mismo evento (retry por timeout/fallo), y este
 * registro impide que {@code checkout.session.completed} se aplique dos veces.
 * La columna {@code event_id} es �nica (constraint en Flyway V11 y ddl-auto en
 * dev), de modo que el guardado de un duplicado lanza
 * {@code DataIntegrityViolationException} y la transacci�n se revierte.
 */
@Entity
@Table(name = "webhook_events")
public class StripeWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected StripeWebhookEvent() {
    }

    public StripeWebhookEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
