package com.kinplatform.kin.medical.billing.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface CopayRuleRepository extends JpaRepository<CopayRule, UUID> {

    List<CopayRule> findByContractIdOrderByPriorityDesc(UUID contractId);

    @Query("""
        SELECT r FROM CopayRule r
        WHERE r.contractId = :contractId
        AND (r.cupsCode IS NULL OR r.cupsCode = :cupsCode)
        AND (r.cupsCategory IS NULL OR r.cupsCategory = :cupsCategory)
        AND (r.patientRegimen IS NULL OR r.patientRegimen = :patientRegimen)
        AND (r.patientAgeMin IS NULL OR r.patientAgeMin <= :patientAge)
        AND (r.patientAgeMax IS NULL OR r.patientAgeMax >= :patientAge)
        ORDER BY r.priority DESC
        """)
    List<CopayRule> findApplicableRules(
            @Param("contractId") UUID contractId,
            @Param("cupsCode") String cupsCode,
            @Param("cupsCategory") TariffCups.CupsCategory cupsCategory,
            @Param("patientRegimen") EpsContract.Regimen patientRegimen,
            @Param("patientAge") Integer patientAge);
}
