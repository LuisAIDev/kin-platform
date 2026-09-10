package com.kinplatform.kin.health.documents.adapter;

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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DocumentStorageQuotaPortImpl implements DocumentStorageQuotaPort {

    private final UserSubscriptionRepository subscriptionRepository;
    private final PricingPlanRepository planRepository;
    private final UserRepository userRepository;
    private final com.kinplatform.kin.health.documents.adapter.ClinicalDocumentJpaRepository documentRepository;

    public DocumentStorageQuotaPortImpl(
            UserSubscriptionRepository subscriptionRepository,
            PricingPlanRepository planRepository,
            UserRepository userRepository,
            com.kinplatform.kin.health.documents.adapter.ClinicalDocumentJpaRepository documentRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    @Override
    public long getStorageUsedBytes(UUID userId) {
        return documentRepository.sumFileSizeByUserIdAndStatus(userId, DocumentStatus.ACTIVE);
    }

    @Override
    public long getStorageLimitBytes(UUID userId) {
        var plan = resolvePlan(userId);
        var maxStorageMb = plan.getMaxStorageMb();
        if (maxStorageMb == null) {
            return Long.MAX_VALUE; // Ilimitado
        }
        return (long) maxStorageMb * 1024L * 1024L; // MB a bytes
    }

    @Override
    public boolean canUpload(UUID userId, long fileSizeBytes) {
        long used = getStorageUsedBytes(userId);
        long limit = getStorageLimitBytes(userId);
        return used + fileSizeBytes <= limit;
    }

    /**
     * Resuelve el plan que aplica a un usuario.
     * Si tiene una suscripción ACTIVE vigente se usa el plan de esa suscripción.
     * Si no, se cae al plan más barato ACTIVO de la vertical que corresponde al rol del usuario.
     */
    private PricingPlan resolvePlan(UUID userId) {
        var subscription = subscriptionRepository
                .findByUserIdAndStatusAndEndDateAfter(userId, SubscriptionStatus.ACTIVE, OffsetDateTime.now())
                .orElse(null);
        if (subscription != null) {
            return subscription.getPlan();
        }
        ProductVertical vertical = verticalForRole(userId);
        return planRepository.findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(vertical)
                .orElseThrow(() -> new RuntimeException("No active pricing plan found for vertical " + vertical));
    }

    private ProductVertical verticalForRole(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getRole() == UserRole.PATIENT) {
            return ProductVertical.SALUD_PERSONAL;
        }
        if (user != null && user.getRole() == UserRole.PHYSICIAN) {
            return ProductVertical.SALUD_PROFESIONAL;
        }
        return ProductVertical.EMPRESAS;
    }
}