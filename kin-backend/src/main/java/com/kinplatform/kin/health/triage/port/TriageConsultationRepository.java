package com.kinplatform.kin.health.triage.port;

import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de persistencia de consultas de triaje (ADR-028).
 *
 * <p>Guarda y consulta el historial de triaje por usuario. La infraestructura
 * lo implementa (JPA/BD); el dominio nunca persiste directamente.
 * {@code findByIdAndUserId} permite que el diagnóstico diferencial (ADR-029) y
 * el dashboard (ADR-030) recuperen una consulta existente del paciente
 * autenticado. {@code findByUserId} paginado alimenta el historial del
 * dashboard.</p>
 */
public interface TriageConsultationRepository {

    TriageConsultation save(TriageConsultation consultation);

    List<TriageConsultation> findByUserId(UUID userId);

    Page<TriageConsultation> findByUserId(UUID userId, Pageable pageable);

    Optional<TriageConsultation> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndCreatedAtBetween(UUID userId, java.time.OffsetDateTime start, java.time.OffsetDateTime end);
}
