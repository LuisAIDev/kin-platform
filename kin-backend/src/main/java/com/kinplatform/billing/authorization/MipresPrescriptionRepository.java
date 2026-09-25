package com.kinplatform.billing.authorization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MipresPrescriptionRepository extends JpaRepository<MipresPrescription, UUID> {

    Optional<MipresPrescription> findByPrescriptionNumber(String prescriptionNumber);

    List<MipresPrescription> findByOrganizationId(UUID organizationId);

    List<MipresPrescription> findByOrganizationIdAndStatus(UUID organizationId, MipresPrescription.PrescriptionStatus status);

    List<MipresPrescription> findByPatientId(UUID patientId);

    @Query("""
        SELECT p FROM MipresPrescription p
        WHERE p.organizationId = :orgId
        AND p.prescriptionDate BETWEEN :start AND :end
        ORDER BY p.prescriptionDate DESC
        """)
    List<MipresPrescription> findByOrganizationIdAndDateRange(
            @Param("orgId") UUID organizationId,
            @Param("start") java.time.LocalDate start,
            @Param("end") java.time.LocalDate end);
}