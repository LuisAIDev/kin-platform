package com.kinplatform.kin.health.hce.repository;

import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientIdentificationRepository extends JpaRepository<PatientIdentification, UUID> {

    Optional<PatientIdentification> findByUserId(UUID userId);

    Optional<PatientIdentification> findByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    @Query("SELECT p FROM PatientIdentification p WHERE p.documentType = :documentType AND p.documentNumber = :documentNumber")
    Optional<PatientIdentification> findByDocumentTypeAndDocumentNumberIgnoreCase(
            @Param("documentType") DocumentType documentType,
            @Param("documentNumber") String documentNumber);

    List<PatientIdentification> findByEpsCode(String epsCode);

    List<PatientIdentification> findByRegimen(String regimen);

    boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);
}