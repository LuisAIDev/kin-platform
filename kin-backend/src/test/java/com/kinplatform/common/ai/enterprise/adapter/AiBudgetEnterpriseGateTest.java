package com.kinplatform.platform.ai_enterprise.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.ai.usage.AiBudgetControlService;
import com.kinplatform.common.ai.usage.ReservationContext;
import com.kinplatform.common.usage.AiBudgetExceededException;
import com.kinplatform.common.usage.AiReservation;
import com.kinplatform.common.usage.CostEstimator;
import com.kinplatform.common.usage.HeuristicCostEstimator;
import com.kinplatform.common.usage.UsagePeriod;
import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.service.SubscriptionValidatorService;
import com.kinplatform.platform.project.Project;
import com.kinplatform.platform.project.ProjectRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRole;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifica que la generación Enterprise pasa por la MISMA autoridad de
 * presupuesto (reserva atómica) y que, sin presupuesto, no se invoca a la IA.
 */
@ExtendWith(MockitoExtension.class)
class AiBudgetEnterpriseGateTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SubscriptionValidatorService subscriptionValidator;

    @Mock
    private AiBudgetControlService budgetControlService;

    private ReservationContext reservationContext;
    private AiBudgetEnterpriseGate gate;

    @BeforeEach
    void setUp() {
        reservationContext = new ReservationContext();
        reservationContext.clear();
        CostEstimator estimator = new HeuristicCostEstimator(new BigDecimal("0.14"), new BigDecimal("0.28"), 4096);
        gate = new AiBudgetEnterpriseGate(
                projectRepository, subscriptionValidator, budgetControlService, estimator, reservationContext);
    }

    @AfterEach
    void tearDown() {
        reservationContext.clear();
    }

    private Project project() {
        return Project.builder()
                .id(PROJECT_ID)
                .user(User.builder()
                        .id(USER_ID)
                        .email("u@t.com")
                        .role(UserRole.FREE)
                        .build())
                .title("P")
                .build();
    }

    private PricingPlan plan() {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .code("FREE")
                .name("GRATIS")
                .price(BigDecimal.ZERO)
                .maxProjects(3)
                .aiBudgetUsd(new BigDecimal("0.50"))
                .build();
    }

    @Test
    void reserve_conPresupuesto_disponible_dejaReservaVigente() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        var reservation = new AiReservation(USER_ID, UsagePeriod.current(), new BigDecimal("0.01"));
        when(budgetControlService.reserveEstimate(eq(USER_ID), any(), any())).thenReturn(reservation);

        assertTrue(gate.reserve(PROJECT_ID));
        assertNotNull(reservationContext.current());
    }

    @Test
    void reserve_sinPresupuesto_deniegaYNoDejaReserva() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(budgetControlService.reserveEstimate(eq(USER_ID), any(), any()))
                .thenThrow(new AiBudgetExceededException(AiBudgetControlService.BUDGET_EXCEEDED_MESSAGE));

        assertFalse(gate.reserve(PROJECT_ID));
        assertNull(reservationContext.current());
        verify(budgetControlService).reserveEstimate(eq(USER_ID), any(), any());
    }

    @Test
    void reserve_controlDesactivado_sinGate() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(budgetControlService.reserveEstimate(eq(USER_ID), any(), any())).thenReturn(null);

        assertTrue(gate.reserve(PROJECT_ID));
        assertNull(reservationContext.current());
    }

    @Test
    void reserve_proyectoInexistente_deniega() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertFalse(gate.reserve(PROJECT_ID));
        verify(budgetControlService, never()).reserveEstimate(any(), any(), any());
    }

    @Test
    void clear_liberaElContexto() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(subscriptionValidator.getCurrentPlan(USER_ID)).thenReturn(plan());
        when(budgetControlService.reserveEstimate(eq(USER_ID), any(), any()))
                .thenReturn(new AiReservation(USER_ID, UsagePeriod.current(), BigDecimal.ONE));

        gate.reserve(PROJECT_ID);
        gate.clear();

        assertNull(reservationContext.current());
    }
}






