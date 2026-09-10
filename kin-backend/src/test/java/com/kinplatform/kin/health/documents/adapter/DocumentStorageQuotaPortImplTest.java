package com.kinplatform.kin.health.documents.adapter;

import com.kinplatform.kin.health.documents.domain.ClinicalDocument;
import com.kinplatform.kin.health.documents.domain.DocumentStatus;
import com.kinplatform.kin.health.documents.port.DocumentStorageQuotaPort;
import com.kinplatform.pricing.PricingPlan;
import com.kinplatform.pricing.PricingPlanRepository;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.pricing.UserSubscription;
import com.kinplatform.pricing.UserSubscriptionRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentStorageQuotaPortImpl — pruebas unitarias")
class DocumentStorageQuotaPortImplTest {

    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private PricingPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClinicalDocumentJpaRepository documentRepository;

    private DocumentStorageQuotaPortImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DocumentStorageQuotaPortImpl(subscriptionRepository, planRepository, userRepository, documentRepository);

        var user = User.builder()
                .id(USER_ID)
                .email("u@t.com")
                .fullName("U")
                .role(UserRole.PATIENT)
                .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Paciente con plan Free y 0 documentos → 0 MB usados, canUpload = true dentro de límite")
    void freePlanZeroDocuments_canUploadWithinLimit() {
        var freePlan = createPlan("FREE", ProductVertical.SALUD_PERSONAL, 50, 0);
        setupSubscription(freePlan, 0);

        var canUpload = service.canUpload(USER_ID, 10 * 1024 * 1024); // 10 MB

        assertThat(canUpload).isTrue();
    }

    @Test
    @DisplayName("Paciente con plan Free y documentos que superan 50 MB → canUpload devuelve false")
    void freePlanExceedsLimit_canUploadReturnsFalse() {
        var freePlan = createPlan("FREE", ProductVertical.SALUD_PERSONAL, 50, 60);
        setupSubscription(freePlan, 60 * 1024 * 1024); // 60 MB usados

        var canUpload = service.canUpload(USER_ID, 1024 * 1024); // 1 MB

        assertThat(canUpload).isFalse();
    }

    @Test
    @DisplayName("Paciente con plan Personal+ y documentos → canUpload devuelve true (hasta 5 GB)")
    void personalPlusPlan_canUploadWithinLimit() {
        var plusPlan = createPlan("PERSONAL_PLUS", ProductVertical.SALUD_PERSONAL, null, 0); // ilimitado
        setupSubscription(plusPlan, 4 * 1024 * 1024 * 1024L); // 4 GB usados

        var canUpload = service.canUpload(USER_ID, 2 * 1024 * 1024 * 1024L); // 2 GB

        assertThat(canUpload).isTrue();
    }

    @Test
    @DisplayName("Paciente sin suscripción → usa plan Free por defecto")
    void noSubscription_usesFreePlan() {
        var freePlan = createPlan("FREE", ProductVertical.SALUD_PERSONAL, 50, 0);
        when(planRepository.findByCodeAndVertical("FREE", ProductVertical.SALUD_PERSONAL))
                .thenReturn(Optional.of(freePlan));
        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.empty());

        long storageUsed = 10L * 1024 * 1024; // 10 MB
        when(documentRepository.sumFileSizeByUserIdAndStatus(any(), any()))
                .thenReturn(storageUsed);

        var used = service.getStorageUsedBytes(USER_ID);
        var limit = service.getStorageLimitBytes(USER_ID);
        var canUpload = service.canUpload(USER_ID, 1024 * 1024);

        assertThat(used).isEqualTo(storageUsed);
        assertThat(limit).isEqualTo(50L * 1024 * 1024);
        assertThat(canUpload).isTrue();
    }

    @Test
    @DisplayName("Plan Personal+ ilimitado → canUpload siempre true")
    void personalPlusUnlimited_alwaysCanUpload() {
        var plusPlan = createPlan("PERSONAL_PLUS", ProductVertical.SALUD_PERSONAL, null, 0);
        setupSubscription(plusPlan, 4 * 1024 * 1024 * 1024L); // 4 GB usados

        var canUpload = service.canUpload(USER_ID, 2 * 1024 * 1024 * 1024L);

        assertThat(canUpload).isTrue();
    }

    private void setupSubscription(PricingPlan plan, long storageUsedBytes) {
        var subscription = UserSubscription.builder()
                .id(UUID.randomUUID())
                .user(createUser())
                .plan(plan)
                .startDate(OffsetDateTime.now().minusDays(5))
                .endDate(OffsetDateTime.now().plusMonths(1))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        when(subscriptionRepository.findByUserIdAndStatusAndEndDateAfter(any(), any(), any()))
                .thenReturn(Optional.of(subscription));
        when(documentRepository.sumFileSizeByUserIdAndStatus(any(), any()))
                .thenReturn(storageUsedBytes);
    }

    private User createUser() {
        return User.builder()
                .id(USER_ID)
                .email("u@t.com")
                .fullName("U")
                .role(UserRole.PATIENT)
                .build();
    }

    private PricingPlan createPlan(String code, ProductVertical vertical, Integer maxStorageMb, int trialDays) {
        return PricingPlan.builder()
                .id(PLAN_ID)
                .code(code)
                .vertical(vertical)
                .maxStorageMb(maxStorageMb)
                .trialDays(trialDays)
                .isActive(true)
                .build();
    }
}