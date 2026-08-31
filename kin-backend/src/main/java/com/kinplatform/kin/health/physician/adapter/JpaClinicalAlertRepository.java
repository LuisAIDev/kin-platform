package com.kinplatform.kin.health.physician.adapter;

import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link ClinicalAlertRepository} (ADR-031).
 *
 * <p>Persiste y consulta las alertas del portal de médicos (tabla
 * {@code clinical_alerts}).</p>
 */
@Component
public class JpaClinicalAlertRepository implements ClinicalAlertRepository {

    private final ClinicalAlertJpaRepository repository;

    public JpaClinicalAlertRepository(ClinicalAlertJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ClinicalAlert save(ClinicalAlert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("alert no puede ser null");
        }
        ClinicalAlertEntity entity = repository.findById(alert.id()).orElseGet(ClinicalAlertEntity::new);
        entity.setId(alert.id());
        entity.setPatientId(alert.patientId());
        entity.setPhysicianId(alert.physicianId());
        entity.setType(alert.type());
        entity.setSeverity(alert.severity());
        entity.setMessage(alert.message());
        entity.setStatus(alert.status());
        entity.setCreatedAt(alert.createdAt());
        entity.setAcknowledgedAt(alert.acknowledgedAt());
        ClinicalAlertEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClinicalAlert> findByIdAndPhysician(UUID id, UUID physicianId) {
        if (id == null || physicianId == null) {
            return Optional.empty();
        }
        return repository
                .findById(id)
                .filter(e -> physicianId.equals(e.getPhysicianId()))
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalAlert> findActiveByPhysician(UUID physicianId) {
        if (physicianId == null) {
            return List.of();
        }
        return repository
                .findByPhysicianIdAndStatusOrderByCreatedAtDesc(physicianId, ClinicalAlert.AlertStatus.PENDING)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveHighUrgencyByPhysician(UUID physicianId) {
        if (physicianId == null) {
            return 0;
        }
        return repository.countByPhysicianIdAndStatusAndSeverity(
                physicianId, ClinicalAlert.AlertStatus.PENDING, ClinicalAlert.AlertSeverity.ALTA);
    }

    private ClinicalAlert toDomain(ClinicalAlertEntity entity) {
        if (entity == null) {
            return null;
        }
        return ClinicalAlert.of(
                entity.getId(),
                entity.getPatientId(),
                entity.getPhysicianId(),
                entity.getType(),
                entity.getSeverity(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getAcknowledgedAt());
    }
}
