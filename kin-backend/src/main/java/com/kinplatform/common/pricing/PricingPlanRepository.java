package com.kinplatform.common.pricing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, UUID> {

    List<PricingPlan> findByIsActiveTrueOrderByPriceAsc();

    List<PricingPlan> findByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical vertical);

    java.util.Optional<PricingPlan> findByName(String name);

    java.util.Optional<PricingPlan> findByCode(String code);

    /**
     * Busca un plan por {@code code} dentro de una vertical concreta.
     *
     * <p>Desde V37 la unicidad de {@code code} es compuesta
     * ({@code uq_pricing_plan_code_vertical (code, vertical)}), no global:
     * {@code FREE} existe tanto en EMPRESAS como en SALUD_PERSONAL. Los
     * componentes que gestionan planes de una vertical específica (p. ej.
     * {@code DataInitializer} con los planes de EMPRESAS) deben usar este
     * método en lugar de {@link #findByCode(String)} para no obtener
     * {@code NonUniqueResultException}.</p>
     */
    java.util.Optional<PricingPlan> findByCodeAndVertical(String code, ProductVertical vertical);

    /**
     * Busca un plan por {@code name} dentro de una vertical concreta. El nombre
     * no es único global entre verticales (los planes de salud pueden compartir
     * denominaciones comerciales), por lo que la búsqueda debe filtrar por
     * vertical.
     */
    java.util.Optional<PricingPlan> findByNameAndVertical(String name, ProductVertical vertical);

    java.util.Optional<PricingPlan> findFirstByIsActiveTrueOrderByPriceAsc();

    java.util.Optional<PricingPlan> findFirstByVerticalAndIsActiveTrueOrderByPriceAsc(ProductVertical vertical);
}

