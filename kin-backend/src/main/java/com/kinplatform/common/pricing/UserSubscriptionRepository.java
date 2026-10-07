package com.kinplatform.common.pricing;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    Optional<UserSubscription> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserSubscription> findByUserIdAndStatusAndEndDateAfter(
            UUID userId, SubscriptionStatus status, OffsetDateTime date);

    /**
     * Suscripción de un usuario con un estado dado y cuyo plan pertenece a la
     * vertical indicada. Usa {@code @Query} explícita porque {@code vertical}
     * vive en {@link PricingPlan} (relación {@code plan}), no en
     * {@link UserSubscription}: una derived query como
     * {@code findByUserIdAndVerticalAndStatus} no se puede resolver.
     */
    @Query("SELECT s FROM UserSubscription s JOIN s.plan p "
            + "WHERE s.user.id = :userId AND p.vertical = :vertical AND s.status = :status")
    Optional<UserSubscription> findByUserAndPlanVerticalAndStatus(
            @Param("userId") UUID userId,
            @Param("vertical") ProductVertical vertical,
            @Param("status") SubscriptionStatus status);
}

