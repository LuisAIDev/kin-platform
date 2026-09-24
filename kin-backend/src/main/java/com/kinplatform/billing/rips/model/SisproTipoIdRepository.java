package com.kinplatform.billing.rips.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SisproTipoIdRepository extends JpaRepository<SisproTipoId, String> {
    Optional<SisproTipoId> findByCodigoAndActivoTrue(String codigo);
    boolean existsByCodigoAndActivoTrue(String codigo);
}