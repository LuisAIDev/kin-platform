package com.kinplatform.common.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import com.kinplatform.test.PostgresTestSupport;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regresión del 500 {@code cannot execute UPDATE in a read-only transaction} en
 * {@code GET /subscriptions/status}. Ejercita el flujo real (controller →
 * validator → rollover nativo en PostgreSQL) sin {@code @Transactional(readOnly
 * = true)} en el controller y con el plan cargado por {@code JOIN FETCH}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SubscriptionStatusEndpointIntegrationTest extends PostgresTestSupport {

    private static final String EMAIL = "subscription-status-it@kin.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @Autowired
    private PricingPlanRepository planRepository;

    @Test
    @WithMockUser(username = EMAIL)
    void status_conRolloverPendiente_devuelve200YReiniciaContador() throws Exception {
        var plan = planRepository.saveAndFlush(PricingPlan.builder()
                .code("STATUS_IT")
                .vertical(ProductVertical.EMPRESAS)
                .name("Status IT")
                .description("Plan de prueba")
                .price(new BigDecimal("15.00"))
                .priceCop(new BigDecimal("60000.00"))
                .features("[]")
                .maxProjects(5)
                .messagesPerMonth(100)
                .advancedAI(true)
                .pdfExport(true)
                .supportLevel(SupportLevel.PREMIUM)
                .viabilityScoringDetail(ViabilityScoringDetail.DETAILED)
                .build());

        var user = userRepository.saveAndFlush(User.builder()
                .email(EMAIL)
                .passwordHash("hash")
                .fullName("Status IT")
                .role(UserRole.FREE)
                .platform("EMPRESAS")
                .completedProjects(7)
                .build());

        subscriptionRepository.saveAndFlush(UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(OffsetDateTime.now().minusDays(1))
                .endDate(OffsetDateTime.now().plusDays(30))
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(0)
                .build());

        mockMvc.perform(get("/subscriptions/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("STATUS_IT"))
                .andExpect(jsonPath("$.completedProjects").value(0))
                .andExpect(jsonPath("$.maxProjects").value(5))
                .andExpect(jsonPath("$.canCompleteProject").value(true));

        var refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.getCompletedProjects()).isZero();
        assertThat(refreshed.getCompletedProjectsPeriodStart()).isNotNull();
    }
}
