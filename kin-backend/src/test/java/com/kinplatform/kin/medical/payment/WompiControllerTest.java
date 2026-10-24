package com.kinplatform.kin.medical.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.common.pricing.PricingPlan;
import com.kinplatform.common.pricing.PricingPlanRepository;
import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

/**
 * Wompi debe poder cobrar planes de EMPRESAS (KIN Platform) además de los de
 * salud, usando siempre {@code price_cop} y la vertical real del plan.
 */
@ExtendWith(MockitoExtension.class)
class WompiControllerTest {

    @Mock
    private WompiService wompiService;

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private WompiController controller;
    private UUID userId;
    private UUID planId;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new WompiController(wompiService, planRepository, userRepository);
        userId = UUID.randomUUID();
        planId = UUID.randomUUID();
        user = User.builder().id(userId).email("empresa@kin.com").build();

        when(authentication.getName()).thenReturn("empresa@kin.com");
        when(userRepository.findByEmail("empresa@kin.com")).thenReturn(Optional.of(user));
    }

    @Test
    void createCheckout_empresas_deberiaCrearSesionConPriceCop() {
        var plan = plan(ProductVertical.EMPRESAS, new BigDecimal("190000"));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(wompiService.createCheckoutSession(any()))
                .thenReturn(new PaymentGateway.CheckoutSession("KIN_ref", "https://checkout.wompi.co/p/?x=1"));

        var response = controller.createCheckout(
                authentication,
                new WompiController.CreateCheckoutRequestDto(
                        planId, "https://app/dashboard/plans?success=true", "https://app/dashboard/plans?cancelled=true"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("https://checkout.wompi.co/p/?x=1", response.getBody().url());
        assertEquals("KIN_ref", response.getBody().sessionId());

        ArgumentCaptor<PaymentGateway.CreateCheckoutRequest> captor =
                ArgumentCaptor.forClass(PaymentGateway.CreateCheckoutRequest.class);
        verify(wompiService).createCheckoutSession(captor.capture());
        assertEquals("EMPRESAS", captor.getValue().vertical());
        assertEquals("COP", captor.getValue().currency());
        assertEquals(new BigDecimal("190000"), captor.getValue().amount());
        assertEquals(userId, captor.getValue().userId());

        assertEquals("WOMPI", user.getPreferredPaymentGateway());
        verify(userRepository).save(user);
    }

    @Test
    void createCheckout_saludPersonal_sigueFuncionando() {
        var plan = plan(ProductVertical.SALUD_PERSONAL, new BigDecimal("79000"));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(wompiService.createCheckoutSession(any()))
                .thenReturn(new PaymentGateway.CheckoutSession("KIN_ref", "https://checkout.wompi.co/p/?x=1"));

        var response = controller.createCheckout(
                authentication,
                new WompiController.CreateCheckoutRequestDto(planId, null, null));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ArgumentCaptor<PaymentGateway.CreateCheckoutRequest> captor =
                ArgumentCaptor.forClass(PaymentGateway.CreateCheckoutRequest.class);
        verify(wompiService).createCheckoutSession(captor.capture());
        assertEquals("SALUD_PERSONAL", captor.getValue().vertical());
    }

    @Test
    void createCheckout_empresas_sinPriceCop_deberiaFallar() {
        var plan = plan(ProductVertical.EMPRESAS, null);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThrows(IllegalArgumentException.class, () -> controller.createCheckout(
                authentication,
                new WompiController.CreateCheckoutRequestDto(planId, null, null)));
    }

    private PricingPlan plan(ProductVertical vertical, BigDecimal priceCop) {
        return PricingPlan.builder()
                .id(planId)
                .code("STANDARD")
                .name("Plan")
                .price(new BigDecimal("49.00"))
                .priceCop(priceCop)
                .features("[]")
                .vertical(vertical)
                .isActive(true)
                .build();
    }
}
