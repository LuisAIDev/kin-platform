package com.kinplatform.kin.health.followup.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.followup.domain.PatientEvolution;
import com.kinplatform.kin.health.followup.port.PatientEvolutionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link PatientEvolutionRepository} (ADR-033).
 *
 * <p>Los signos vitales se persisten como JSONB (String JSON en la entidad,
 * patrón {@code patient_profiles}); el dominio usa un {@code Map} flexible.</p>
 */
@Component
public class JpaPatientEvolutionRepository implements PatientEvolutionRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaPatientEvolutionRepository.class);

    private final PatientEvolutionJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaPatientEvolutionRepository(PatientEvolutionJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PatientEvolution save(PatientEvolution evolution) {
        if (evolution == null) {
            throw new IllegalArgumentException("evolution no puede ser null");
        }
        PatientEvolutionEntity entity = repository.findById(evolution.id()).orElseGet(PatientEvolutionEntity::new);
        entity.setId(evolution.id());
        entity.setPatientId(evolution.patientId());
        entity.setPhysicianId(evolution.physicianId());
        entity.setRecordedAt(evolution.recordedAt());
        entity.setSymptoms(evolution.symptoms());
        entity.setVitals(toJson(evolution.vitals()));
        entity.setMedicationAdherence(evolution.medicationAdherence());
        entity.setNotes(evolution.notes());
        entity.setCreatedAt(evolution.createdAt());
        repository.save(entity);
        return evolution;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientEvolution> findByPatientIdAndPhysicianId(UUID patientId, UUID physicianId) {
        if (patientId == null || physicianId == null) {
            return List.of();
        }
        return repository.findByPatientIdAndPhysicianIdOrderByRecordedAtDesc(patientId, physicianId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientEvolution> findByPatientId(UUID patientId) {
        if (patientId == null) {
            return List.of();
        }
        return repository.findByPatientIdOrderByRecordedAtDesc(patientId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientEvolution> findLatestByPatientId(UUID patientId) {
        if (patientId == null) {
            return Optional.empty();
        }
        return repository.findTopByPatientIdOrderByRecordedAtDesc(patientId).map(this::toDomain);
    }

    private PatientEvolution toDomain(PatientEvolutionEntity e) {
        return PatientEvolution.of(
                e.getId(),
                e.getPatientId(),
                e.getPhysicianId(),
                e.getRecordedAt(),
                e.getSymptoms(),
                fromJson(e.getVitals()),
                e.getMedicationAdherence(),
                e.getNotes(),
                e.getCreatedAt());
    }

    private String toJson(Map<String, Object> vitals) {
        if (vitals == null || vitals.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(vitals);
        } catch (Exception ex) {
            log.warn("JpaPatientEvolutionRepository: no se pudo serializar vitals; se guarda vacío", ex);
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("JpaPatientEvolutionRepository: no se pudo deserializar vitals; se devuelve vacío", ex);
            return Map.of();
        }
    }
}
