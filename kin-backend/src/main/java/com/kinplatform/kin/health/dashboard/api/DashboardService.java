package com.kinplatform.kin.health.dashboard.api;

import com.kinplatform.kin.health.dashboard.config.DashboardProperties;
import com.kinplatform.kin.health.dashboard.domain.CarePlan;
import com.kinplatform.kin.health.dashboard.domain.CareRecommendationRegistry;
import com.kinplatform.kin.health.dashboard.domain.HealthSummary;
import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.domain.Reminder;
import com.kinplatform.kin.health.dashboard.port.DashboardRepository;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del dashboard de salud (ADR-030).
 *
 * <p>Consolida la experiencia del paciente: resumen de salud (estadísticas
 * agregadas), historial cronológico paginado de consultas de triaje, perfil con
 * factores de riesgo (que el {@code DifferentialEngine} consume en futuras
 * consultas) y plan de cuidado determinista generado por
 * {@link CareRecommendationRegistry}. El {@code userId} siempre proviene de la
 * autenticación (aislamiento por paciente).</p>
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final DashboardRepository dashboardRepository;
    private final TriageConsultationRepository consultationRepository;
    private final CareRecommendationRegistry careRegistry;
    private final DashboardProperties properties;

    public DashboardService(
            DashboardRepository dashboardRepository,
            TriageConsultationRepository consultationRepository,
            CareRecommendationRegistry careRegistry,
            DashboardProperties properties) {
        this.dashboardRepository = dashboardRepository;
        this.consultationRepository = consultationRepository;
        this.careRegistry = careRegistry;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public HealthSummary summary(UUID userId) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        List<TriageConsultation> consultations = consultationRepository.findByUserId(userId);
        List<HealthSummary.FrequentCondition> top = topConditions(consultations, 3);
        OffsetDateTime last = consultations.stream()
                .map(TriageConsultation::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        int differentials = (int)
                consultations.stream().filter(c -> c.results().size() >= 2).count();
        int activeReminders =
                dashboardRepository.findActiveRemindersByUserId(userId).size();
        return new HealthSummary(consultations.size(), differentials, top, last, activeReminders);
    }

    @Transactional(readOnly = true)
    public Page<TriageConsultation> history(UUID userId, Pageable pageable) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        return consultationRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public TriageConsultation consultationDetail(UUID userId, UUID consultationId) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        return consultationRepository
                .findByIdAndUserId(consultationId, userId)
                .orElseThrow(() -> new DashboardConsultationNotFoundException(consultationId));
    }

    @Transactional(readOnly = true)
    public PatientProfile profile(UUID userId) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        return dashboardRepository.findProfileByUserId(userId).orElseGet(() -> PatientProfile.empty(userId));
    }

    @Transactional
    public PatientProfile updateProfile(UUID userId, List<String> riskFactors, List<String> chronicConditions) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        PatientProfile profile = PatientProfile.of(
                userId,
                riskFactors == null ? List.of() : List.copyOf(riskFactors),
                chronicConditions == null ? List.of() : List.copyOf(chronicConditions),
                OffsetDateTime.now());
        PatientProfile saved = dashboardRepository.saveProfile(profile);
        log.info(
                "DashboardService: perfil actualizado userId={}, riesgos={}, crónicas={}",
                userId,
                saved.riskFactors().size(),
                saved.chronicConditions().size());
        return saved;
    }

    @Transactional
    public Reminder createReminder(UUID userId, Reminder reminder) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        if (reminder == null) {
            throw new IllegalArgumentException("reminder no puede ser null");
        }
        Reminder owned = Reminder.of(
                reminder.id(),
                userId,
                reminder.type(),
                reminder.title(),
                reminder.scheduledAt(),
                true,
                OffsetDateTime.now());
        return dashboardRepository.saveReminder(owned);
    }

    @Transactional(readOnly = true)
    public List<Reminder> reminders(UUID userId) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        return dashboardRepository.findActiveRemindersByUserId(userId);
    }

    @Transactional(readOnly = true)
    public CarePlan carePlan(UUID userId) {
        if (!properties.isEnabled()) {
            throw new DashboardDisabledException();
        }
        List<String> conditions = identifiedConditions(userId);
        if (conditions.isEmpty()) {
            return CarePlan.empty();
        }
        return careRegistry.planFor(conditions);
    }

    /**
     * Condiciones identificadas = condiciones crónicas del perfil + condiciones
     * más frecuentes del historial (top 5). Determinista y deduplicado.
     */
    private List<String> identifiedConditions(UUID userId) {
        var set = new java.util.LinkedHashSet<String>();
        PatientProfile profile =
                dashboardRepository.findProfileByUserId(userId).orElseGet(() -> PatientProfile.empty(userId));
        set.addAll(profile.chronicConditions());
        List<TriageConsultation> consultations = consultationRepository.findByUserId(userId);
        for (HealthSummary.FrequentCondition fc : topConditions(consultations, 5)) {
            set.add(fc.name());
        }
        return List.copyOf(set);
    }

    private List<HealthSummary.FrequentCondition> topConditions(List<TriageConsultation> consultations, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        for (TriageConsultation consultation : consultations) {
            for (TriageConditionResult result : consultation.results()) {
                counts.merge(result.name(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, limit))
                .map(e -> new HealthSummary.FrequentCondition(e.getKey(), e.getValue()))
                .toList();
    }
}
