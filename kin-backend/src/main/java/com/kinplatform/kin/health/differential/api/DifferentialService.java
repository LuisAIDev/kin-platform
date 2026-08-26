package com.kinplatform.kin.health.differential.api;

import com.kinplatform.kin.health.differential.config.DifferentialProperties;
import com.kinplatform.kin.health.differential.domain.DifferentialInput;
import com.kinplatform.kin.health.differential.domain.DifferentialResult;
import com.kinplatform.kin.health.differential.domain.PatientContext;
import com.kinplatform.kin.health.differential.engine.DifferentialEngine;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.TriageInput;
import com.kinplatform.kin.health.triage.domain.TriageResult;
import com.kinplatform.kin.health.triage.engine.TriageEngine;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del diagnóstico diferencial (ADR-029).
 *
 * <p>Dos entradas: una consulta de triaje existente del paciente autenticado
 * ({@code GET /health/differential?consultationId=...}) o síntomas directos
 * ({@code POST /health/differential}). En ambos casos ejecuta el
 * {@link DifferentialEngine} de forma determinista sobre el
 * {@link TriageResult}. El {@code userId} se resuelve desde la autenticación
 * (aislamiento por paciente).</p>
 */
@Service
public class DifferentialService {

    private static final Logger log = LoggerFactory.getLogger(DifferentialService.class);

    private final DifferentialEngine differentialEngine;
    private final TriageEngine triageEngine;
    private final TriageConsultationRepository consultationRepository;
    private final DifferentialProperties properties;

    public DifferentialService(
            DifferentialEngine differentialEngine,
            TriageEngine triageEngine,
            TriageConsultationRepository consultationRepository,
            DifferentialProperties properties) {
        this.differentialEngine = differentialEngine;
        this.triageEngine = triageEngine;
        this.consultationRepository = consultationRepository;
        this.properties = properties;
    }

    /**
     * Diagnóstico diferencial a partir de una consulta de triaje persistida.
     */
    @Transactional(readOnly = true)
    public DifferentialResult fromConsultation(UUID userId, UUID consultationId, Set<String> riskFactors) {
        if (!properties.isEnabled()) {
            throw new DifferentialDisabledException();
        }
        TriageConsultation consultation = consultationRepository
                .findByIdAndUserId(consultationId, userId)
                .orElseThrow(() -> new DifferentialNotFoundException(consultationId));
        return evaluate(consultation.symptoms(), consultation.results(), riskFactors);
    }

    /**
     * Diagnóstico diferencial a partir de síntomas directos (ejecuta triaje
     * primero, como si hubiera pasado por el pipeline).
     */
    @Transactional(readOnly = true)
    public DifferentialResult fromSymptoms(List<String> symptoms, Set<String> riskFactors) {
        if (!properties.isEnabled()) {
            throw new DifferentialDisabledException();
        }
        TriageResult triage = triageEngine.evaluate(TriageInput.of(symptoms));
        return evaluate(symptoms, triage.results(), riskFactors);
    }

    private DifferentialResult evaluate(
            List<String> symptoms, List<TriageConditionResult> triageConditions, Set<String> riskFactors) {
        DifferentialInput input = DifferentialInput.of(symptoms, triageConditions, PatientContext.of(riskFactors));
        DifferentialResult result = differentialEngine.evaluate(input);
        log.info(
                "DifferentialService: {} condiciones priorizadas (top={})",
                result.items().size(),
                result.items().isEmpty() ? "-" : result.items().get(0).name());
        return result;
    }
}
