package com.kinplatform.pricing.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.SupportLevel;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.ViabilityScoringDetail;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRole;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PricingDtosTest {

    private PricingPlan plan() {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .name("Plan")
                .description("Desc")
                .price(new BigDecimal("5.00"))
                .features("[\"f1\",\"f2\"]")
                .maxProjects(3)
                .messagesPerMonth(20)
                .advancedAI(true)
                .pdfExport(true)
                .supportLevel(SupportLevel.PREMIUM)
                .viabilityScoringDetail(ViabilityScoringDetail.DETAILED)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private PricingPlan planWithInvalidFeatures() {
        return PricingPlan.builder()
                .id(UUID.randomUUID())
                .name("Plan")
                .price(new BigDecimal("5.00"))
                .features("not-json")
                .isActive(true)
                .build();
    }

    @Test
    void pricingPlanDto_fromEntity_conFeaturesValidas() {
        var dto = PricingPlanDTO.fromEntity(plan());

        assertEquals(List.of("f1", "f2"), dto.getFeatures());
        assertEquals("Plan", dto.getName());
        assertEquals(3, dto.getMaxProjects());
        assertEquals(20, dto.getMessagesPerMonth());
        assertTrue(dto.getAdvancedAI());
        assertTrue(dto.getPdfExport());
        assertEquals(SupportLevel.PREMIUM, dto.getSupportLevel());
        assertEquals(ViabilityScoringDetail.DETAILED, dto.getViabilityScoringDetail());
        assertTrue(dto.getIsActive());
    }

    @Test
    void pricingPlanDto_fromEntity_conFeaturesInvalidas_deberiaDevolverListaVacia() {
        var dto = PricingPlanDTO.fromEntity(planWithInvalidFeatures());

        assertTrue(dto.getFeatures().isEmpty());
    }

    @Test
    void pricingPlanDto_builderYAcessores() {
        var dto = PricingPlanDTO.builder()
                .id(UUID.randomUUID())
                .name("X")
                .price(new BigDecimal("1.00"))
                .build();
        dto.setDescription("d");

        assertEquals("d", dto.getDescription());
        assertEquals("X", dto.getName());
    }

    @Test
    void pricingPlanResponse_fromEntity_conFeaturesValidas() {
        var response = PricingPlanResponse.fromEntity(plan());

        assertEquals(List.of("f1", "f2"), response.getFeatures());
        assertTrue(response.getCreatedAt() != null);
        assertTrue(response.getUpdatedAt() != null);
    }

    @Test
    void pricingPlanResponse_fromEntity_conFeaturesInvalidas_deberiaDevolverListaVacia() {
        var response = PricingPlanResponse.fromEntity(planWithInvalidFeatures());

        assertTrue(response.getFeatures().isEmpty());
    }

    @Test
    void subscriptionResponse_fromEntity_deberiaMapear() {
        var user = User.builder()
                .id(UUID.randomUUID())
                .email("u@kin.com")
                .fullName("U")
                .role(UserRole.FREE)
                .build();
        var subscription = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(plan())
                .startDate(OffsetDateTime.now())
                .endDate(OffsetDateTime.now().plusDays(30))
                .status(SubscriptionStatus.ACTIVE)
                .messagesUsed(3)
                .lastResetDate(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        var response = SubscriptionResponse.fromEntity(subscription);

        assertEquals(user.getId(), response.getUserId());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(3, response.getMessagesUsed());
        assertEquals(20, response.getMessagesPerMonth());
        assertEquals("Plan", response.getPlan().getName());
    }

    @Test
    void createPricingPlanRequest_accesores() {
        var request = new CreatePricingPlanRequest();
        request.setName("N");
        request.setPrice(new BigDecimal("2.00"));
        request.setFeatures(List.of("a"));
        request.setIsActive(false);

        assertEquals("N", request.getName());
        assertEquals(List.of("a"), request.getFeatures());
        assertFalse(request.getIsActive());
    }

    @Test
    void updatePricingPlanRequest_accesores() {
        var request = new UpdatePricingPlanRequest();
        request.setName("N");
        request.setPrice(new BigDecimal("2.00"));
        request.setFeatures(List.of("a"));
        request.setIsActive(true);

        assertEquals("N", request.getName());
        assertTrue(request.getIsActive());
    }

    @Test
    void subscriptionRequest_accesores() {
        var request = new SubscriptionRequest();
        var id = UUID.randomUUID();
        request.setPlanId(id);

        assertEquals(id, request.getPlanId());
    }

    @Test
    void subscriptionStatusResponse_builderYAccesores() {
        var response = SubscriptionStatusResponse.builder()
                .isActive(true)
                .planName("Plan")
                .planDescription("Desc")
                .remainingMessages(10)
                .canCreateProject(true)
                .aiLevel("PRO")
                .messagesPerMonth(20)
                .maxProjects(3)
                .advancedAI(true)
                .pdfExport(true)
                .supportLevel("PREMIUM")
                .build();

        assertTrue(response.isActive());
        assertEquals("Plan", response.getPlanName());
        assertEquals(10, response.getRemainingMessages());
        assertTrue(response.isCanCreateProject());
        assertEquals("PRO", response.getAiLevel());
        assertEquals(20, response.getMessagesPerMonth());
        assertEquals(3, response.getMaxProjects());
        assertTrue(response.getAdvancedAI());
        assertTrue(response.getPdfExport());
        assertEquals("PREMIUM", response.getSupportLevel());
    }
}
