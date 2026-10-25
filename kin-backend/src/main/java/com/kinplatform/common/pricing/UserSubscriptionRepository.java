package com.kinplatform.common.pricing;

import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * Suscripción activa con su {@link PricingPlan} ya cargado mediante
     * {@code JOIN FETCH}. El plan se consume fuera de la transacción del
     * repositorio (p. ej. {@code SubscriptionController#getSubscriptionStatus}),
     * así que debe venir inicializado para evitar {@code LazyInitializationException}.
     */
    @Query("SELECT s FROM UserSubscription s JOIN FETCH s.plan p "
            + "WHERE s.user.id = :userId AND s.status = :status AND p.vertical = :vertical "
            + "AND (s.endDate IS NULL OR s.endDate > :now) ORDER BY s.createdAt DESC")
    List<UserSubscription> findActiveByUserAndVertical(
            @Param("userId") UUID userId,
            @Param("status") SubscriptionStatus status,
            @Param("vertical") ProductVertical vertical,
            @Param("now") OffsetDateTime now);

    Optional<UserSubscription> findTopByUserIdAndPlan_VerticalOrderByCreatedAtDesc(
            UUID userId, ProductVertical vertical);
}
