package com.kinplatform.kin.health.common;

import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.kin.health.telemedicine.event.TelemedicineEvent;
import com.kinplatform.kin.health.triage.event.TriagePerformedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Métricas de salud (fase de producción).
 *
 * <p>Registra contadores Micrometer para los eventos clave de KIN Health y los
 * expone en {@code /actuator/metrics}: total de triajes, diagnósticos
 * diferenciales, mensajes y citas. Se suscribe al {@link DomainEventBus} en su
 * construcción; aditivo y sin acoplar los módulos al bus de métricas.</p>
 */
@Component
public class HealthMetricsService {

    private static final Logger log = LoggerFactory.getLogger(HealthMetricsService.class);

    private final MeterRegistry meterRegistry;
    private final DomainEventBus eventBus;

    private Counter triages;
    private Counter highUrgencyTriages;
    private Counter differentials;
    private Counter messages;
    private Counter appointments;

    public HealthMetricsService(MeterRegistry meterRegistry, DomainEventBus eventBus) {
        this.meterRegistry = meterRegistry;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void registerMetrics() {
        triages = Counter.builder("kin.health.triages.total")
                .description("Total de triajes realizados")
                .register(meterRegistry);
        highUrgencyTriages = Counter.builder("kin.health.triages.high_urgency")
                .description("Triajes con urgencia máxima ALTA")
                .register(meterRegistry);
        differentials = Counter.builder("kin.health.differentials.total")
                .description("Total de diagnósticos diferenciales")
                .register(meterRegistry);
        messages = Counter.builder("kin.health.telemedicine.messages.total")
                .description("Total de mensajes de telemedicina")
                .register(meterRegistry);
        appointments = Counter.builder("kin.health.telemedicine.appointments.total")
                .description("Total de citas de telemedicina")
                .register(meterRegistry);

        eventBus.subscribe(TriagePerformedEvent.class, this::onTriage);
        eventBus.subscribe(TelemedicineEvent.class, this::onTelemedicine);
        log.info("HealthMetricsService: métricas de salud registradas");
    }

    private void onTriage(TriagePerformedEvent event) {
        triages.increment();
        if (event.maxUrgency() == com.kinplatform.kin.health.triage.domain.Urgency.ALTA) {
            highUrgencyTriages.increment();
        }
        differentials.increment();
    }

    private void onTelemedicine(TelemedicineEvent event) {
        if (event.type().startsWith("telemedicine_message")) {
            messages.increment();
        } else if (event.type().startsWith("telemedicine_appointment")) {
            appointments.increment();
        }
    }
}

