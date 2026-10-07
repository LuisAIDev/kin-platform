package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.triage.config.TriageProperties;
import com.kinplatform.kin.health.triage.domain.Symptom;
import com.kinplatform.kin.health.triage.domain.TriageCatalog;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.TriageInput;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import com.kinplatform.kin.health.triage.port.TriageKnowledgeRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del módulo de triaje (ADR-028).
 *
 * <p>Orquesta la consulta del paciente: valida el feature flag, carga el
 * catálogo, ejecuta el {@link TriageEngine} (determinista), persiste la
 * consulta (auditoría e historial por usuario) y expone el catálogo de
 * síntomas al frontend. El aislamiento por usuario es responsabilidad del
 * llamador (controller resuelve el {@code userId} desde la autenticación).</p>
 */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final TriageEngine engine;
    private final TriageKnowledgeRepository knowledgeRepository;
    private final TriageConsultationRepository consultationRepository;
    private final TriageProperties properties;
    private final HealthQuotaPort healthQuotaPort;
    private final UserRepository userRepository;

    public TriageService(
            TriageEngine engine,
            TriageKnowledgeRepository knowledgeRepository,
            TriageConsultationRepository consultationRepository,
            TriageProperties properties,
            HealthQuotaPort healthQuotaPort,
            UserRepository userRepository) {
        this.engine = engine;
        this.knowledgeRepository = knowledgeRepository;
        this.consultationRepository = consultationRepository;
        this.properties = properties;
        this.healthQuotaPort = healthQuotaPort;
        this.userRepository = userRepository;
    }

    /**
     * Realiza una consulta de triaje para el paciente.
     *
     * @param userId   id del paciente autenticado (se usa para el historial)
     * @param symptoms síntomas reportados
     * @return resultado del motor de triaje
     */
    @Transactional
    public TriageResult analyze(UUID userId, List<String> symptoms) {
        if (!properties.isEnabled()) {
            throw new TriageDisabledException();
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }

        // Defensa en profundidad: check directo del flag unlimitedAccess
        User user = userRepository.findById(userId).orElse(null);
        boolean isUnlimited = user != null && Boolean.TRUE.equals(user.getUnlimitedAccess());

        if (!isUnlimited) {
            Integer limit = healthQuotaPort.getMaxTriagesPerMonth(userId);
            if (limit != null) {
                OffsetDateTime startOfMonth = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                OffsetDateTime endOfMonth = startOfMonth.plusMonths(1);
                long count = consultationRepository.countByUserIdAndCreatedAtBetween(userId, startOfMonth, endOfMonth);
                if (count >= limit) {
                    throw new QuotaExceededException(
                            "Has alcanzado el límite de triajes de tu plan.",
                            "QUOTA_EXCEEDED",
                            "/dashboard/patient/plans");
                }
            }
        }
        TriageCatalog catalog = knowledgeRepository.loadCatalog();
        TriageResult result = engine.evaluate(TriageInput.of(symptoms), catalog);
        if (!result.isEmpty()) {
            TriageConsultation consultation =
                    TriageConsultation.of(UUID.randomUUID(), userId, symptoms, result.results(), OffsetDateTime.now());
            consultationRepository.save(consultation);
            log.info(
                    "TriageService: userId={} -> {} condición(es), top={}",
                    userId,
                    result.results().size(),
                    result.results().isEmpty() ? "-" : result.results().get(0).name());
        } else {
            log.info("TriageService: userId={} -> sin condiciones candidatas", userId);
        }
        return result;
    }

    /**
     * Catálogo de síntomas disponibles (para el formulario del paciente).
     */
    @Transactional(readOnly = true)
    public List<Symptom> listSymptoms() {
        if (!properties.isEnabled()) {
            throw new TriageDisabledException();
        }
        return knowledgeRepository.loadCatalog().symptoms();
    }

    /**
     * Historial de consultas del paciente (aislamiento por {@code userId}).
     */
    @Transactional(readOnly = true)
    public List<TriageConsultation> history(UUID userId) {
        if (!properties.isEnabled()) {
            throw new TriageDisabledException();
        }
        if (userId == null) {
            return List.of();
        }
        return consultationRepository.findByUserId(userId);
    }

    /**
     * Historial de consultas del paciente EXCLUYENDO las ocultas.
     */
    @Transactional(readOnly = true)
    public List<TriageConsultation> historyExcludingHidden(UUID userId) {
        if (!properties.isEnabled()) {
            throw new TriageDisabledException();
        }
        if (userId == null) {
            return List.of();
        }
        return consultationRepository.findByUserIdExcludingHidden(userId, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    /**
     * Oculta una consulta del historial del paciente (soft delete).
     *
     * @param consultationId id de la consulta a ocultar
     * @param userId         id del paciente propietario
     */
    @Transactional
    public void hideConsultation(UUID consultationId, UUID userId) {
        if (!properties.isEnabled()) {
            throw new TriageDisabledException();
        }
        if (consultationId == null || userId == null) {
            throw new IllegalArgumentException("consultationId y userId no pueden ser null");
        }
        consultationRepository.hideConsultation(consultationId, userId);
    }
}

