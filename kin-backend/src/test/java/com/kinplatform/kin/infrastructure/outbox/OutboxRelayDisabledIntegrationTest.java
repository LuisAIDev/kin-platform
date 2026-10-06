package com.kinplatform.kin.infrastructure.outbox;

import com.kinplatform.common.event.ReportGeneratedEvent;
import com.kinplatform.common.eventbus.port.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:tc:postgresql:18:///kin_test",
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
@ActiveProfiles("test")
@Transactional
class OutboxRelayDisabledIntegrationTest {

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxEventPublisher publisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM domain_event_outbox");
    }

    @Test
    @DisplayName("Relay desactivado: no procesa eventos aunque existan pendientes")
    void disabledRelay_doesNotProcessPendingEvents() {
        UUID projectId = UUID.randomUUID();
        var event = new ReportGeneratedEvent(projectId, "PDF");

        // Publicar evento en outbox
        publisher.publish(event);

        // Verificar que hay evento pendiente
        int pendingBefore = countPending();
        assertThat(pendingBefore).isEqualTo(1);

        // Ejecutar relé (debería estar desactivado en perfil test)
        relay.processPendingEvents();

        // Verificar que NO se procesó (el relay no debería hacer nada si está desactivado)
        int pendingAfter = countPending();
        assertThat(pendingAfter).isEqualTo(1);
    }

    private int countPending() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_event_outbox WHERE status = 'PENDING'", Integer.class);
    }
}
