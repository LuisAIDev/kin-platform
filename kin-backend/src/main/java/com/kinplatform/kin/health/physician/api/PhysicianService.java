package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.health.dashboard.domain.PatientProfile;
import com.kinplatform.kin.health.dashboard.port.DashboardRepository;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.domain.AuditAction;
import com.kinplatform.kin.health.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.physician.domain.PatientSummary;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del portal de médicos (ADR-031).
 *
 * <p>Ofrece al médico (rol PHYSICIAN) la lista de sus pacientes asignados, el
 * historial y el resumen clínico de un paciente, y las alertas activas.
 * <strong>Aislamiento estricto</strong>: un médico solo ve pacientes asignados
 * a él; cualquier acceso a un paciente no asignado devuelve 404. Las alertas
 * se generan de forma determinista cuando un triaje alcanza urgencia ALTA
 * (escuchando {@code TriagePerformedEvent}).</p>
 */
@Service
public class PhysicianService {

    private static final Logger log = LoggerFactory.getLogger(PhysicianService.class);

    private final PhysicianPatientRepository patientRepository;
    private final ClinicalAlertRepository alertRepository;
    private final TriageConsultationRepository consultationRepository;
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private final PhysicianProperties properties;
    private final RelationshipAccessValidator accessValidator;
    private final AuditService auditService;
    private final HealthQuotaPort healthQuotaPort;

    public PhysicianService(
            PhysicianPatientRepository patientRepository,
            ClinicalAlertRepository alertRepository,
            TriageConsultationRepository consultationRepository,
            DashboardRepository dashboardRepository,
            UserRepository userRepository,
            PhysicianProperties properties,
            RelationshipAccessValidator accessValidator,
            AuditService auditService,
            HealthQuotaPort healthQuotaPort) {
        this.patientRepository = patientRepository;
        this.alertRepository = alertRepository;
        this.consultationRepository = consultationRepository;
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.accessValidator = accessValidator;
        this.auditService = auditService;
        this.healthQuotaPort = healthQuotaPort;
    }

    @Transactional(readOnly = true)
    public Page<PatientSummary> listPatients(UUID physicianId, Pageable pageable) {
        return listPatients(physicianId, RelationshipStatus.ACTIVE, pageable);
    }

    /**
     * Lista de pacientes del médico con filtro por estado de relación.
     * {@code ACTIVE} devuelve la cartera actual (resumen clínico); {@code PENDING}
     * devuelve las invitaciones pendientes (solo identidad, sin datos clínicos);
     * {@code null} devuelve ambas.
     */
    @Transactional(readOnly = true)
    public Page<PatientSummary> listPatients(UUID physicianId, RelationshipStatus status, Pageable pageable) {
        if (!properties.isEnabled()) {
            throw new PhysicianDisabledException();
        }
        List<PatientSummary> summaries = new java.util.ArrayList<>();
        if (status == null) {
            List<UUID> patientIds = patientRepository.findPatientIdsByPhysician(physicianId);
            patientIds.stream().sorted().forEach(id -> summaries.add(summarizePatient(physicianId, id)));
            patientRepository
                    .findByPhysicianIdAndStatus(physicianId, RelationshipStatus.PENDING)
                    .forEach(a -> summaries.add(PatientSummary.pending(a.patientId(), patientName(a.patientId()))));
        } else {
            patientRepository
                    .findByPhysicianIdAndStatus(physicianId, status)
                    .forEach(a -> {
                        if (status == RelationshipStatus.ACTIVE) {
                            summaries.add(summarizePatient(physicianId, a.patientId()));
                        } else {
                            summaries.add(PatientSummary.pending(a.patientId(), patientName(a.patientId())));
                        }
                    });
        }
        summaries.sort(java.util.Comparator.comparing(PatientSummary::patientName, String.CASE_INSENSITIVE_ORDER));
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), summaries.size());
        List<PatientSummary> page = start > summaries.size() ? List.of() : summaries.subList(start, end);
        return new PageImpl<>(page, pageable, summaries.size());
    }

    @Transactional
    public PatientSummary patientSummary(UUID physicianId, UUID patientId) {
        if (!properties.isEnabled()) {
            throw new PhysicianDisabledException();
        }
        requireAssigned(physicianId, patientId);
        auditService.logAccess(physicianId, AuditAction.VIEW_SUMMARY, AuditResourceType.PACIENTE, patientId, patientId,
                java.util.Map.of());
        return summarizePatient(physicianId, patientId);
    }

    @Transactional
    public List<TriageConsultation> patientHistory(UUID physicianId, UUID patientId) {
        if (!properties.isEnabled()) {
            throw new PhysicianDisabledException();
        }
        requireAssigned(physicianId, patientId);
        auditService.logAccess(physicianId, AuditAction.VIEW_HISTORY, AuditResourceType.PACIENTE, patientId, patientId,
                java.util.Map.of());
        return consultationRepository.findByUserId(patientId);
    }

    @Transactional(readOnly = true)
    public List<ClinicalAlert> activeAlerts(UUID physicianId) {
        if (!properties.isEnabled()) {
            throw new PhysicianDisabledException();
        }
        return alertRepository.findActiveByPhysician(physicianId);
    }

    @Transactional
    public ClinicalAlert acknowledgeAlert(UUID physicianId, UUID alertId) {
        if (!properties.isEnabled()) {
            throw new PhysicianDisabledException();
        }
        ClinicalAlert alert = alertRepository
                .findByIdAndPhysician(alertId, physicianId)
                .orElseThrow(() -> new PhysicianAlertNotFoundException(alertId));
        // Solo una relación ACTIVE permite gestionar alertas del paciente.
        accessValidator.requireActiveRelationship(physicianId, alert.patientId());
        auditService.logAccess(physicianId, AuditAction.ACKNOWLEDGE_ALERT, AuditResourceType.ALERTA, alert.id(),
                alert.patientId(), java.util.Map.of());
        if (alert.isActive()) {
            ClinicalAlert acknowledged = ClinicalAlert.of(
                    alert.id(),
                    alert.patientId(),
                    alert.physicianId(),
                    alert.type(),
                    alert.severity(),
                    alert.message(),
                    ClinicalAlert.AlertStatus.ACKNOWLEDGED,
                    alert.createdAt(),
                    OffsetDateTime.now());
            log.info("PhysicianService: alerta {} atendida por médico {}", alertId, physicianId);
            return alertRepository.save(acknowledged);
        }
        return alert;
    }

    @Transactional
    public PhysicianPatientAssignment assignPatient(UUID physicianId, UUID patientId) {
        if (!properties.isEnabled()) {
            throw new PhysicianDisabledException();
        }
        Integer limit = healthQuotaPort.getMaxPatients(physicianId);
        if (limit != null) {
            long currentPatients = patientRepository.findPatientIdsByPhysician(physicianId).size();
            if (currentPatients >= limit) {
                throw new QuotaExceededException("Has alcanzado el límite de pacientes de tu plan.");
            }
        }
        PhysicianPatientAssignment assignment =
                PhysicianPatientAssignment.of(physicianId, patientId, OffsetDateTime.now());
        return patientRepository.assign(assignment);
    }

    /**
     * Genera alertas deterministas para los médicos asignados a un paciente
     * cuando un triaje alcanza urgencia ALTA (llamado por el listener de
     * eventos, ADR-031).
     */
    @Transactional
    public void createHighUrgencyAlerts(UUID patientId, List<String> symptoms, List<String> conditions) {
        if (!properties.isEnabled()) {
            return;
        }
        List<UUID> physicians = patientRepository.findPhysicianIdsByPatient(patientId);
        if (physicians.isEmpty()) {
            log.debug("PhysicianService: sin médicos asignados al paciente {}", patientId);
            return;
        }
        String message = "Triaje de alta urgencia: " + String.join(", ", conditions.isEmpty() ? symptoms : conditions);
        for (UUID physicianId : physicians) {
            ClinicalAlert alert = ClinicalAlert.of(
                    UUID.randomUUID(),
                    patientId,
                    physicianId,
                    ClinicalAlert.AlertType.HIGH_URGENCY_TRIAGE,
                    ClinicalAlert.AlertSeverity.ALTA,
                    message,
                    ClinicalAlert.AlertStatus.PENDING,
                    OffsetDateTime.now(),
                    null);
            alertRepository.save(alert);
            log.info("PhysicianService: alerta ALTA creada para paciente {} → médico {}", patientId, physicianId);
        }
    }

    private void requireAssigned(UUID physicianId, UUID patientId) {
        // Solo una relación ACTIVE habilita el acceso clínico (Área 5).
        accessValidator.requireActiveRelationship(physicianId, patientId);
    }

    private PatientSummary summarizePatient(UUID physicianId, UUID patientId) {
        List<TriageConsultation> consultations = consultationRepository.findByUserId(patientId);
        Optional<PatientProfile> profileOpt = dashboardRepository.findProfileByUserId(patientId);
        List<String> activeConditions = topConditions(consultations, 5);
        List<String> riskFactors = profileOpt.map(PatientProfile::riskFactors).orElse(List.of());
        List<String> chronicConditions =
                profileOpt.map(PatientProfile::chronicConditions).orElse(List.of());
        OffsetDateTime lastTriageAt = consultations.stream()
                .map(TriageConsultation::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        int activeAlerts = alertRepository.findActiveByPhysician(physicianId).stream()
                .filter(a -> a.patientId().equals(patientId))
                .map(a -> 1)
                .reduce(0, Integer::sum);
        return PatientSummary.active(
                patientId,
                patientName(patientId),
                activeConditions,
                riskFactors,
                chronicConditions,
                consultations.size(),
                lastTriageAt,
                activeAlerts);
    }

    private List<String> topConditions(List<TriageConsultation> consultations, int limit) {
        Map<String, Integer> counts = new HashMap<>();
        for (TriageConsultation consultation : consultations) {
            for (TriageConditionResult result : consultation.results()) {
                counts.merge(result.name(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(1, limit))
                .map(Map.Entry::getKey)
                .toList();
    }

    private String patientName(UUID patientId) {
        return userRepository.findById(patientId).map(User::getFullName).orElse("Paciente");
    }
}
