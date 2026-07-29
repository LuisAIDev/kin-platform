package com.kinplatform.pricing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    Optional<UserSubscription> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserSubscription> findByUserIdAndStatusAndEndDateAfter(
            UUID userId, SubscriptionStatus status, OffsetDateTime date);
}
