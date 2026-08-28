package com.kinplatform.kin.infrastructure.outbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring del Transactional Outbox (ADR-026).
 *
 * <p>Registra {@link OutboxRelayProperties} para que {@link OutboxRelay}
 * (auto-descubierto por componente) pueda inyectarla. El resto de beans del
 * outbox (publicador transaccional, relé) se auto-descubren o se componen en
 * la infraestructura existente. No altera la lógica del relé ni el contrato
 * de propiedades ({@code kin.outbox.*}).</p>
 */
@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class OutboxConfig {
}
