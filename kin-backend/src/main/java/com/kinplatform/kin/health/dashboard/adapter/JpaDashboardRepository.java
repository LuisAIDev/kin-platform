package com.kinplatform.kin.health.dashboard.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.dashboard.port.DashboardRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA del puerto {@link DashboardRepository} (ADR-030).
 *
 * <p>Persiste el perfil del paciente como JSONB en {@code patient_profiles} y
 * los recordatorios en {@code reminders}. El aislamiento por usuario se
 * garantiza en el servicio/controller (el userId siempre viene de la
 * autenticación).</p>
 */
@Component
public class JpaDashboardRepository implements DashboardRepository {

    private final PatientProfileJpaRepository profileRepository;
    private final ReminderJpaRepository reminderRepository;
    private final ObjectMapper objectMapper;

    public JpaDashboardRepository(
            PatientProfileJpaRepository profileRepository,
            ReminderJpaRepository reminderRepository,
            ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.reminderRepository = reminderRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientProfile> findProfileByUserId(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return profileRepository
                .findById(userId)
                .map(e -> DashboardMapper.toProfile(
                        e.getUserId(),
                        DashboardMapper.fromJson(objectMapper, e.getProfileData()),
                        e.getUpdatedAt(),
                        objectMapper));
    }

    @Override
    @Transactional
    public PatientProfile saveProfile(PatientProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile no puede ser null");
        }
        var entity = profileRepository.findById(profile.userId()).orElseGet(PatientProfileEntity::new);
        entity.setUserId(profile.userId());
        entity.setProfileData(DashboardMapper.toJson(objectMapper, DashboardMapper.toData(profile)));
        entity.setUpdatedAt(java.time.OffsetDateTime.now());
        profileRepository.save(entity);
        return profile;
    }

    @Override
    @Transactional
    public Reminder saveReminder(Reminder reminder) {
        if (reminder == null) {
            throw new IllegalArgumentException("reminder no puede ser null");
        }
        ReminderEntity saved = reminderRepository.save(DashboardMapper.toEntity(reminder));
        return DashboardMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reminder> findActiveRemindersByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return reminderRepository.findByUserIdAndActiveTrueOrderByScheduledAtAsc(userId).stream()
                .map(DashboardMapper::toDomain)
                .toList();
    }
}
