package com.kinplatform.billing.rips.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SisproModalidadPagoRepository extends JpaRepository<SisproModalidadPago, String> {
    Optional<SisproModalidadPago> findByCodigoAndActivoTrue(String codigo);
    boolean existsByCodigoAndActivoTrue(String codigo);
}