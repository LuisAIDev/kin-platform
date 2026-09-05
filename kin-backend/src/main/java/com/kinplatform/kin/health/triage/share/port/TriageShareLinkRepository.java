package com.kinplatform.kin.health.triage.share.port;

import com.kinplatform.kin.health.triage.share.domain.TriageShareLink;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de enlaces de compartición de triaje.
 *
 * <p>Guarda y consulta los enlaces temporales generados por el paciente.
 * La infraestructura lo implementa con JPA (tabla {@code triage_share_links}).</p>
 */
public interface TriageShareLinkRepository {

    TriageShareLink save(TriageShareLink link);

    Optional<TriageShareLink> findByToken(String token);

    /** Enlace vigente (no revocado) más reciente de un triaje, si existe. */
    Optional<TriageShareLink> findActiveByTriageId(UUID triageId);
}
