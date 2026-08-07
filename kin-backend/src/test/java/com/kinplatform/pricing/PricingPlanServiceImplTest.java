package com.kinplatform.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.pricing.dto.CreatePricingPlanRequest;
import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;
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
class PricingPlanServiceImplTest {

    private static final UUID PLAN_ID = UUID.randomUUID();

    @Mock
    private PricingPlanRepository repository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private PricingPlanServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PricingPlanServiceImpl(repository, objectMapper);
    }

    private PricingPlan plan() {
        return PricingPlan.builder()
                .id(PLAN_ID)
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
                .build();
    }

    @Test
    void getAllActive_deberiaMapearPlanes() {
        when(repository.findByIsActiveTrueOrderByPriceAsc()).thenReturn(List.of(plan()));

        var plans = service.getAllActive();

        assertEquals(1, plans.size());
        assertEquals("Plan", plans.get(0).getName());
        assertEquals(List.of("f1", "f2"), plans.get(0).getFeatures());
    }

    @Test
    void getActivePlans_deberiaDevolverEntidades() {
        when(repository.findByIsActiveTrueOrderByPriceAsc()).thenReturn(List.of(plan()));

        var plans = service.getActivePlans();

        assertEquals(1, plans.size());
        assertTrue(plans.contains(plan()));
    }

    @Test
    void getPlanByName_deberiaDevolverElPlan() {
        when(repository.findByName("Premium")).thenReturn(Optional.of(plan()));

        assertTrue(service.getPlanByName("Premium").isPresent());
    }

    @Test
    void getById_deberiaDevolverElPlan() {
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));

        PricingPlanResponse response = service.getById(PLAN_ID);

        assertEquals(PLAN_ID, response.getId());
    }

    @Test
    void getById_noEncontrado_deberiaFallar() {
        when(repository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getById(PLAN_ID));
    }

    @Test
    void create_conTodosLosCampos_deberiaGuardar() throws Exception {
        var request = new CreatePricingPlanRequest();
        request.setName("Nuevo");
        request.setDescription("Desc");
        request.setPrice(new BigDecimal("9.99"));
        request.setFeatures(List.of("a", "b"));
        request.setMaxProjects(5);
        request.setMessagesPerMonth(50);
        request.setAdvancedAI(true);
        request.setPdfExport(true);
        request.setSupportLevel(SupportLevel.PREMIUM);
        request.setViabilityScoringDetail(ViabilityScoringDetail.DETAILED);
        request.setIsActive(true);
        when(repository.save(any(PricingPlan.class))).thenAnswer(i -> {
            var p = (PricingPlan) i.getArgument(0);
            p.setId(PLAN_ID);
            return p;
        });

        var response = service.create(request);

        assertEquals("Nuevo", response.getName());
        assertEquals(List.of("a", "b"), response.getFeatures());
    }

    @Test
    void create_soloCamposObligatorios_deberiaAplicarDefaults() {
        var request = new CreatePricingPlanRequest();
        request.setName("Basico");
        request.setPrice(BigDecimal.ZERO);
        request.setFeatures(List.of());
        when(repository.save(any(PricingPlan.class))).thenAnswer(i -> {
            var p = (PricingPlan) i.getArgument(0);
            p.setId(PLAN_ID);
            return p;
        });

        var response = service.create(request);

        assertFalse(response.getAdvancedAI());
        assertFalse(response.getPdfExport());
        assertEquals(SupportLevel.BASIC, response.getSupportLevel());
        assertEquals(ViabilityScoringDetail.BASIC, response.getViabilityScoringDetail());
        assertTrue(response.getIsActive());
    }

    @Test
    void create_serializacionFallida_deberiaLanzarRuntime() throws Exception {
        objectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
            }
        };
        service = new PricingPlanServiceImpl(repository, objectMapper);
        var request = new CreatePricingPlanRequest();
        request.setName("X");
        request.setPrice(BigDecimal.ZERO);
        request.setFeatures(List.of("a"));

        assertThrows(RuntimeException.class, () -> service.create(request));
    }

    @Test
    void update_deberiaActualizarElPlan() {
        var request = new UpdatePricingPlanRequest();
        request.setName("Actualizado");
        request.setDescription("Nueva");
        request.setPrice(new BigDecimal("15.00"));
        request.setFeatures(List.of("x"));
        request.setMaxProjects(10);
        request.setMessagesPerMonth(100);
        request.setAdvancedAI(true);
        request.setPdfExport(false);
        request.setSupportLevel(SupportLevel.BASIC);
        request.setViabilityScoringDetail(ViabilityScoringDetail.BASIC);
        request.setIsActive(true);
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
        when(repository.save(any(PricingPlan.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.update(PLAN_ID, request);

        assertEquals("Actualizado", response.getName());
        assertEquals(new BigDecimal("15.00"), response.getPrice());
        assertEquals(List.of("x"), response.getFeatures());
    }

    @Test
    void update_noEncontrado_deberiaFallar() {
        when(repository.findById(PLAN_ID)).thenReturn(Optional.empty());

        var request = new UpdatePricingPlanRequest();
        assertThrows(IllegalArgumentException.class, () -> service.update(PLAN_ID, request));
    }

    @Test
    void update_serializacionFallida_deberiaLanzarRuntime() throws Exception {
        var request = new UpdatePricingPlanRequest();
        request.setName("X");
        request.setPrice(BigDecimal.ZERO);
        request.setFeatures(List.of("a"));
        request.setIsActive(true);
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(plan()));
        objectMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {};
            }
        };
        service = new PricingPlanServiceImpl(repository, objectMapper);

        assertThrows(RuntimeException.class, () -> service.update(PLAN_ID, request));
    }

    @Test
    void deactivate_deberiaDesactivarElPlan() {
        var p = plan();
        when(repository.findById(PLAN_ID)).thenReturn(Optional.of(p));
        when(repository.save(any(PricingPlan.class))).thenAnswer(i -> i.getArgument(0));

        service.deactivate(PLAN_ID);

        assertFalse(p.getIsActive());
    }

    @Test
    void deactivate_noEncontrado_deberiaFallar() {
        when(repository.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deactivate(PLAN_ID));
    }
}
