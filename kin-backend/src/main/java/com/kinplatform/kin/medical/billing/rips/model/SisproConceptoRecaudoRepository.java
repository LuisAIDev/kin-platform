package com.kinplatform.kin.medical.billing.rips.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SisproConceptoRecaudoRepository extends JpaRepository<SisproConceptoRecaudo, String> {
    Optional<SisproConceptoRecaudo> findByCodigoAndActivoTrue(String codigo);
    boolean existsByCodigoAndActivoTrue(String codigo);
}
