package com.kinplatform.common.ai.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.platform.usage.AiBudgetExceededException;
import com.kinplatform.platform.usage.AiReservation;
import com.kinplatform.platform.usage.AiUsagePort;
import com.kinplatform.platform.usage.CostEstimator;
import com.kinplatform.platform.usage.HeuristicCostEstimator;
import com.kinplatform.platform.usage.UsagePeriod;
import com.kinplatform.common.pricing.PricingPlan;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiBudgetControlServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final BigDecimal INPUT_PRICE = new BigDecimal("1.00");
    private static final BigDecimal OUTPUT_PRICE = new BigDecimal("2.00");

    @Mock
    private AiUsagePort usagePort;

    private AiBudgetControlService service;

    @BeforeEach
    void setUp() {
        CostEstimator estimator = new HeuristicCostEstimator(INPUT_PRICE, OUTPUT_PRICE, 4096);
        service = new AiBudgetControlService(usagePort, estimator, true, new BigDecimal("1.00"));
    }

    private PricingPlan plan(BigDecimal budget) {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .code("STANDARD")
                .name("STANDARD")
                .price(new BigDecimal("25.00"))
                .maxProjects(5)
                .aiBudgetUsd(budget)
                .build();
    }

    @Test
    void reserve_presupuestoDisponible_devuelveReserva() {
        when(usagePort.tryReserve(eq(USER_ID), any(), eq(new BigDecimal("6.25")), any()))
                .thenReturn(true);

        AiReservation reservation = service.reserve(USER_ID, plan(new BigDecimal("6.25")), "hola", List.of());

        assertNotNull(reservation);
        assertEquals(USER_ID, reservation.userId());
        verify(usagePort).tryReserve(eq(USER_ID), any(), eq(new BigDecimal("6.25")), any());
    }

    @Test
    void reserve_presupuestoAgotado_lanza403() {
        when(usagePort.tryReserve(eq(USER_ID), any(), eq(new BigDecimal("6.25")), any()))
                .thenReturn(false);

        assertThrows(
                AiBudgetExceededException.class,
                () -> service.reserve(USER_ID, plan(new BigDecimal("6.25")), "hola", List.of()));
    }

    @Test
    void reserve_estimacionSuperaTopePorSolicitud_lanza403() {
        // 200k caracteres -> ~50k tokens input @ $1/1M = $0.05 + output 4096 @ $2/1M = $0.008
        // necesita un mensaje enorme para superar $1.00 de tope; se sube el tope para el control
        var smallCap = new AiBudgetControlService(
                usagePort, new HeuristicCostEstimator(INPUT_PRICE, OUTPUT_PRICE, 4096), true, new BigDecimal("0.01"));
        String huge = "a".repeat(1_000_000); // ~250k tokens input @ $1/1M = $0.25

        assertThrows(
                AiBudgetExceededException.class,
                () -> smallCap.reserve(USER_ID, plan(new BigDecimal("6.25")), huge, List.of()));
    }

    @Test
    void reserve_controlDesactivado_devuelveNull() {
        var disabled = new AiBudgetControlService(
                usagePort, new HeuristicCostEstimator(INPUT_PRICE, OUTPUT_PRICE, 4096), false, new BigDecimal("1.00"));

        assertNull(disabled.reserve(USER_ID, plan(new BigDecimal("6.25")), "hola", List.of()));
        verify(usagePort, never()).tryReserve(any(), any(), any(), any());
    }

    @Test
    void reserve_sinPreciosConfigurados_yControlActivado_failFast() {
        assertThrows(
                IllegalStateException.class,
                () -> new AiBudgetControlService(
                        usagePort,
                        new HeuristicCostEstimator(BigDecimal.ZERO, BigDecimal.ZERO, 4096),
                        true,
                        new BigDecimal("1.00")));
    }

    @Test
    void failFast_controlActivado_soloInputCero() {
        assertThrows(
                IllegalStateException.class,
                () -> new AiBudgetControlService(
                        usagePort,
                        new HeuristicCostEstimator(BigDecimal.ZERO, new BigDecimal("0.28"), 4096),
                        true,
                        new BigDecimal("1.00")));
    }

    @Test
    void failFast_controlActivado_soloOutputCero() {
        assertThrows(
                IllegalStateException.class,
                () -> new AiBudgetControlService(
                        usagePort,
                        new HeuristicCostEstimator(new BigDecimal("0.14"), BigDecimal.ZERO, 4096),
                        true,
                        new BigDecimal("1.00")));
    }

    @Test
    void configuracionValida_controlActivado_conPreciosPositivos() {
        var service = new AiBudgetControlService(
                usagePort,
                new HeuristicCostEstimator(new BigDecimal("0.14"), new BigDecimal("0.28"), 4096),
                true,
                new BigDecimal("1.00"));
        assertTrue(service.isCostControlEnabled());
    }

    @Test
    void configuracionValida_controlDesactivado_conPreciosCero() {
        var service = new AiBudgetControlService(
                usagePort,
                new HeuristicCostEstimator(BigDecimal.ZERO, BigDecimal.ZERO, 4096),
                false,
                new BigDecimal("1.00"));
        assertFalse(service.isCostControlEnabled());
        assertNull(service.reserve(USER_ID, plan(new BigDecimal("6.25")), "hola", List.of()));
        verify(usagePort, never()).tryReserve(any(), any(), any(), any());
    }

    @Test
    void reserveEstimate_presupuestoDisponible_reserva() {
        when(usagePort.tryReserve(eq(USER_ID), any(), eq(new BigDecimal("6.25")), any()))
                .thenReturn(true);

        AiReservation reservation =
                service.reserveEstimate(USER_ID, plan(new BigDecimal("6.25")), new BigDecimal("0.05"));

        assertNotNull(reservation);
        assertEquals(new BigDecimal("0.05"), reservation.estimatedCostUsd());
    }

    @Test
    void reserveEstimate_presupuestoAgotado_lanza403() {
        when(usagePort.tryReserve(eq(USER_ID), any(), eq(new BigDecimal("6.25")), any()))
                .thenReturn(false);

        assertThrows(
                AiBudgetExceededException.class,
                () -> service.reserveEstimate(USER_ID, plan(new BigDecimal("6.25")), new BigDecimal("0.05")));
    }

    @Test
    void recordActual_reconciliaReservaConUsoReal() {
        AiReservation reservation = new AiReservation(USER_ID, UsagePeriod.current(), new BigDecimal("0.010000"));

        service.recordActual(reservation, 10_000, 5_000);

        // 10k input @ $1/1M = $0.01 ; 5k output @ $2/1M = $0.01 -> actual = $0.02
        verify(usagePort)
                .recordActual(
                        eq(USER_ID),
                        any(),
                        eq(new BigDecimal("0.010000")),
                        eq(new BigDecimal("0.020000")),
                        eq(10_000L),
                        eq(5_000L));
    }

    @Test
    void summary_calculaUsadoReservadoDisponible() {
        var period = UsagePeriod.current();
        var record = new com.kinplatform.platform.usage.AiUsageRecord(
                UUID.randomUUID(),
                USER_ID,
                period,
                10_000L,
                5_000L,
                15_000L,
                new BigDecimal("0.020000"),
                new BigDecimal("0.010000"),
                2,
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now());
        when(usagePort.findByUserAndPeriod(USER_ID, period)).thenReturn(Optional.of(record));

        var summary = service.summary(USER_ID, plan(new BigDecimal("6.25")));

        assertEquals(new BigDecimal("0.020000"), summary.budgetUsed());
        assertEquals(new BigDecimal("0.010000"), summary.budgetReserved());
        assertEquals(new BigDecimal("6.25"), summary.budgetLimit());
        assertEquals(new BigDecimal("6.220000"), summary.budgetRemaining());
        assertEquals(15_000L, summary.totalTokens());
        assertEquals(2, summary.requestCount());
    }

    @Test
    void summary_sinRegistro_ceros() {
        when(usagePort.findByUserAndPeriod(eq(USER_ID), any())).thenReturn(Optional.empty());

        var summary = service.summary(USER_ID, plan(new BigDecimal("6.25")));

        assertTrue(summary.budgetUsed().signum() == 0);
        assertEquals(new BigDecimal("6.25"), summary.budgetLimit());
        assertEquals(new BigDecimal("6.25"), summary.budgetRemaining());
    }
}




