package com.kinplatform.kin.medical.billing.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TariffCupsRepository extends JpaRepository<TariffCups, UUID> {

    Optional<TariffCups> findByContractIdAndCupsCodeAndEffectiveFrom(
            UUID contractId, String cupsCode, LocalDate effectiveFrom);

    @Query("SELECT t FROM TariffCups t WHERE t.contractId = :contractId AND t.effectiveFrom <= :date AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date)")
    List<TariffCups> findValidTariffsForDate(@Param("contractId") UUID contractId, @Param("date") LocalDate date);

    List<TariffCups> findByContractId(UUID contractId);

    long countByContractId(UUID contractId);

    void deleteByContractId(UUID contractId);
}
