package com.kinplatform.pricing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, UUID> {

    List<PricingPlan> findByIsActiveTrueOrderByPriceAsc();

    java.util.Optional<PricingPlan> findByName(String name);

    java.util.Optional<PricingPlan> findByCode(String code);

    java.util.Optional<PricingPlan> findFirstByIsActiveTrueOrderByPriceAsc();
}
