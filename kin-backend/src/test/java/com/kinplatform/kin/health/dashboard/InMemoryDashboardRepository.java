package com.kinplatform.kin.health.dashboard;

import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.dashboard.port.DashboardRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación en memoria del puerto {@link DashboardRepository} para tests
 * (ADR-030).
 */
public class InMemoryDashboardRepository implements DashboardRepository {

    private PatientProfile profile;
    private final List<Reminder> reminders = new ArrayList<>();

    @Override
    public Optional<PatientProfile> findProfileByUserId(UUID userId) {
        return Optional.ofNullable(profile).filter(p -> p.userId().equals(userId));
    }

    @Override
    public PatientProfile saveProfile(PatientProfile profile) {
        this.profile = profile;
        return profile;
    }

    @Override
    public Reminder saveReminder(Reminder reminder) {
        reminders.add(reminder);
        return reminder;
    }

    @Override
    public List<Reminder> findActiveRemindersByUserId(UUID userId) {
        return reminders.stream()
                .filter(r -> r.userId().equals(userId) && r.active())
                .toList();
    }
}
