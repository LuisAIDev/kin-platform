package com.kinplatform.kin.medical.billing.rips.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SisproCoberturaPlanRepository extends JpaRepository<SisproCoberturaPlan, String> {
    Optional<SisproCoberturaPlan> findByCodigoAndActivoTrue(String codigo);
    boolean existsByCodigoAndActivoTrue(String codigo);
}
