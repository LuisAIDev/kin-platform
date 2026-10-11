package com.kinplatform.kin.medical.admission.repository;

import com.kinplatform.kin.medical.admission.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByDocumentTypeAndDocumentNumber(
            Patient.DocumentType documentType, String documentNumber);

    boolean existsByDocumentTypeAndDocumentNumber(
            Patient.DocumentType documentType, String documentNumber);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.middleName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.secondLastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " p.documentNumber LIKE CONCAT('%', :search, '%'))")
    Page<Patient> searchByNameOrDocument(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND " +
           "p.documentType = :documentType AND p.documentNumber = :documentNumber")
    Optional<Patient> findActiveByDocument(@Param("documentType") Patient.DocumentType documentType,
                                            @Param("documentNumber") String documentNumber);

    List<Patient> findByBirthDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND p.eps = :eps")
    List<Patient> findByEps(String eps);

    @Query("SELECT p FROM Patient p WHERE p.deletedAt IS NULL AND p.regime = :regime")
    List<Patient> findByRegime(Patient.Regime regime);
}