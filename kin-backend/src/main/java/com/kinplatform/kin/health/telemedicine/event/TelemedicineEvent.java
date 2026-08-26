package com.kinplatform.kin.health.telemedicine.event;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.HasUserId;
import java.util.UUID;

/**
 * Evento de dominio de telemedicina (ADR-032, fase de producción).
 *
 * <p>Emitido por la capa de aplicación al enviar mensajes y al gestionar citas;
 * alimenta las métricas de salud ({@code HealthMetricsService}) y futuras
 * notificaciones sin acoplar el dominio al bus de métricas.</p>
 */
public record TelemedicineEvent(UUID userId, String eventName, UUID relatedId) implements DomainEvent, HasUserId {

    public TelemedicineEvent {
        eventName = eventName == null ? "" : eventName;
    }

    @Override
    public String type() {
        return "telemedicine_" + eventName;
    }

    @Override
    public Object aggregateId() {
        return relatedId;
    }

    @Override
    public UUID userId() {
        return userId;
    }

    public static TelemedicineEvent message(UUID userId, UUID messageId) {
        return new TelemedicineEvent(userId, "message_sent", messageId);
    }

    public static TelemedicineEvent appointment(UUID userId, UUID appointmentId) {
        return new TelemedicineEvent(userId, "appointment_status", appointmentId);
    }
}
