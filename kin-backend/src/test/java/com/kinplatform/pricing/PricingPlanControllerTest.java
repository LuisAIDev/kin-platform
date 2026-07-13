package com.kinplatform.pricing;

import com.kinplatform.pricing.dto.PricingPlanResponse;
import com.kinplatform.pricing.dto.UpdatePricingPlanRequest;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingPlanControllerTest {

    @Mock
    private PricingPlanRepository pricingPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication auth;

    @InjectMocks
    private PricingPlanController controller;

    @Captor
    private ArgumentCaptor<PricingPlan> planCaptor;

    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID FREE_USER_ID = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@kin.com";
    private static final String FREE_EMAIL = "free@kin.com";
    private static final String FEATURES_JSON = "[\"Feature 1\",\"Feature 2\"]";

    private PricingPlan freePlan;
    private PricingPlan premiumPlan;
    private User adminUser;
    private User freeUser;
    private UpdatePricingPlanRequest updateRequest;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(ADMIN_USER_ID)
                .email(ADMIN_EMAIL)
                .fullName("Admin")
                .role(UserRole.ADMIN)
                .build();

        freeUser = User.builder()
                .id(FREE_USER_ID)
                .email(FREE_EMAIL)
                .fullName("Free User")
                .role(UserRole.FREE)
                .build();

        freePlan = PricingPlan.builder()
                .id(PLAN_ID)
                .name("Free")
                .price(BigDecimal.ZERO)
                .currency("USD")
                .billingPeriod("monthly")
                .features(FEATURES_JSON)
                .isPopular(false)
                .displayOrder(1)
                .build();

        premiumPlan = PricingPlan.builder()
                .id(UUID.randomUUID())
                .name("Premium")
                .price(new BigDecimal("29.99"))
                .currency("USD")
                .billingPeriod("monthly")
                .features(FEATURES_JSON)
                .isPopular(true)
                .displayOrder(2)
                .build();

        updateRequest = new UpdatePricingPlanRequest();
        updateRequest.setName("Free Updated");
        updateRequest.setPrice(new BigDecimal("9.99"));
        updateRequest.setCurrency("EUR");
        updateRequest.setBillingPeriod("yearly");
        updateRequest.setFeatures(List.of("Feature A", "Feature B"));
        updateRequest.setIsPopular(true);
        updateRequest.setDisplayOrder(3);
    }

    @Test
    void getAllPlans_deberiaRetornarListaPlanes_sinRequerirAutenticacion() {
        when(pricingPlanRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(freePlan, premiumPlan));

        ResponseEntity<List<PricingPlanResponse>> response = controller.getAll();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        PricingPlanResponse first = response.getBody().get(0);
        assertEquals("Free", first.getName());
        assertEquals(BigDecimal.ZERO, first.getPrice());
        assertEquals(1, first.getDisplayOrder());

        PricingPlanResponse second = response.getBody().get(1);
        assertEquals("Premium", second.getName());
        assertEquals(new BigDecimal("29.99"), second.getPrice());
        assertTrue(second.getIsPopular());

        verify(pricingPlanRepository).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    void updatePlan_deberiaActualizarExitosamente_cuandoUsuarioEsAdmin() {
        when(auth.getName()).thenReturn(ADMIN_EMAIL);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        when(pricingPlanRepository.findById(PLAN_ID)).thenReturn(Optional.of(freePlan));
        when(pricingPlanRepository.save(any(PricingPlan.class)))
                .thenAnswer(i -> i.getArgument(0));

        ResponseEntity<PricingPlanResponse> response = controller.update(auth, PLAN_ID, updateRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals("Free Updated", response.getBody().getName());
        assertEquals(new BigDecimal("9.99"), response.getBody().getPrice());
        assertEquals("EUR", response.getBody().getCurrency());
        assertEquals("yearly", response.getBody().getBillingPeriod());
        assertTrue(response.getBody().getIsPopular());
        assertEquals(3, response.getBody().getDisplayOrder());
        assertEquals(List.of("Feature A", "Feature B"), response.getBody().getFeatures());

        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(pricingPlanRepository).findById(PLAN_ID);
        verify(pricingPlanRepository).save(planCaptor.capture());

        PricingPlan saved = planCaptor.getValue();
        assertEquals("Free Updated", saved.getName());
        assertEquals(new BigDecimal("9.99"), saved.getPrice());
        assertEquals("EUR", saved.getCurrency());
        assertEquals("yearly", saved.getBillingPeriod());
        assertTrue(saved.getIsPopular());
        assertEquals(3, saved.getDisplayOrder());
    }

    @Test
    void updatePlan_deberiaLanzarExcepcion_cuandoUsuarioNoEsAdmin() {
        when(auth.getName()).thenReturn(FREE_EMAIL);
        when(userRepository.findByEmail(FREE_EMAIL)).thenReturn(Optional.of(freeUser));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> controller.update(auth, PLAN_ID, updateRequest));

        assertEquals("Only administrators can update pricing plans", ex.getMessage());
        verify(userRepository).findByEmail(FREE_EMAIL);
        verify(pricingPlanRepository, never()).findById(any());
        verify(pricingPlanRepository, never()).save(any());
    }

    @Test
    void updatePlan_deberiaLanzarExcepcion_cuandoPlanNoExiste() {
        when(auth.getName()).thenReturn(ADMIN_EMAIL);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser));
        when(pricingPlanRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controller.update(auth, PLAN_ID, updateRequest));

        assertTrue(ex.getMessage().contains("Pricing plan not found"));
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(pricingPlanRepository).findById(PLAN_ID);
        verify(pricingPlanRepository, never()).save(any());
    }
}
