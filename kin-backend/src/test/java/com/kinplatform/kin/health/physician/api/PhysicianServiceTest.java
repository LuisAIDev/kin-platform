package com.kinplatform.kin.health.physician.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.health.dashboard.InMemoryDashboardRepository;
import com.kinplatform.kin.health.audit.api.AuditService;
import com.kinplatform.kin.health.audit.config.AuditProperties;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.physician.access.RelationshipNotActiveException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.ClinicalAlert;
import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhysicianServiceTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email("p@kin.com")
                        .fullName("Paciente Test")
                        .role(UserRole.PATIENT)
                        .build()));
    }

    private static PhysicianProperties properties(boolean enabled) {
        var props = new PhysicianProperties();
        props.setEnabled(enabled);
        return props;
    }

    private static HealthQuotaPort healthQuotaPort() {
        return new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return null;
            }

            @Override
            public Integer getMaxPatients(UUID physicianId) {
                return null;
            }

            @Override
            public Integer getTrialDays(UUID userId) {
                return null;
            }
        };
    }

    private static InMemoryTriageConsultationRepository consultations() {
        var repo = new InMemoryTriageConsultationRepository();
        repo.save(TriageConsultation.of(
                UUID.randomUUID(),
                PATIENT,
                List.of("fiebre"),
                List.of(new TriageConditionResult(
                        UUID.randomUUID(),
                        "Gripe",
                        "D",
                        0.8,
                        Severity.MODERADO,
                        Urgency.MEDIA,
                        "R",
                        List.of("fiebre"))),
                OffsetDateTime.now()));
        return repo;
    }

    private static AuditService auditService() {
        var props = new AuditProperties();
        props.setEnabled(false);
        return new AuditService(null, null, null, props);
    }

    private PhysicianService service(boolean enabled, InMemoryPhysicianRepositories repos) {
        return new PhysicianService(
                repos.patientRepository(),
                repos.alertRepository(),
                consultations(),
                new InMemoryDashboardRepository(),
                userRepository,
                properties(enabled),
                new RelationshipAccessValidator(repos.patientRepository()),
                auditService(),
                healthQuotaPort());
    }

    @Test
    void listPatients_deberiaDevolverSoloLosAsignados() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);

        var page = service.listPatients(PHYSICIAN, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Paciente Test", page.getContent().get(0).patientName());
        assertEquals(1, page.getContent().get(0).totalTriages());
    }

    @Test
    void patientSummary_deberiaRechazarPacienteNoAsignado() {
        var service = service(true, new InMemoryPhysicianRepositories());

        assertThrows(RelationshipNotActiveException.class, () -> service.patientSummary(PHYSICIAN, PATIENT));
        assertThrows(RelationshipNotActiveException.class, () -> service.patientHistory(PHYSICIAN, PATIENT));
    }

    @Test
    void patientSummary_conRelacionPendiente_deberiaRechazar() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);

        assertThrows(RelationshipNotActiveException.class, () -> service.patientSummary(PHYSICIAN, PATIENT));
        assertThrows(RelationshipNotActiveException.class, () -> service.patientHistory(PHYSICIAN, PATIENT));
    }

    @Test
    void patientSummary_conRelacionFinalizada_deberiaRechazar() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(
                InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT)
                        .ended(OffsetDateTime.now(), "REJECTED_BY_PATIENT"));
        var service = service(true, repos);

        assertThrows(RelationshipNotActiveException.class, () -> service.patientSummary(PHYSICIAN, PATIENT));
    }

    @Test
    void patientHistory_deberiaDevolverHistorialDelPacienteAsignado() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);

        var history = service.patientHistory(PHYSICIAN, PATIENT);

        assertEquals(1, history.size());
        assertTrue(history.get(0).results().stream().anyMatch(r -> r.name().equals("Gripe")));
    }

    @Test
    void createHighUrgencyAlerts_deberiaCrearParaMedicosAsignados() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);

        service.createHighUrgencyAlerts(PATIENT, List.of("dolor de pecho"), List.of("Angina de pecho"));

        var alerts = service.activeAlerts(PHYSICIAN);
        assertEquals(1, alerts.size());
        assertEquals(ClinicalAlert.AlertSeverity.ALTA, alerts.get(0).severity());
        assertTrue(alerts.get(0).message().contains("Angina de pecho"));
    }

    @Test
    void createHighUrgencyAlerts_sinMedicos_deberiaNoCrear() {
        var service = service(true, new InMemoryPhysicianRepositories());

        service.createHighUrgencyAlerts(PATIENT, List.of("dolor"), List.of("x"));

        assertTrue(service.activeAlerts(PHYSICIAN).isEmpty());
    }

    @Test
    void acknowledgeAlert_deberiaMarcarComoAtendida() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);
        service.createHighUrgencyAlerts(PATIENT, List.of("dolor"), List.of("Angina de pecho"));
        var alert = service.activeAlerts(PHYSICIAN).get(0);

        var acknowledged = service.acknowledgeAlert(PHYSICIAN, alert.id());

        assertEquals(ClinicalAlert.AlertStatus.ACKNOWLEDGED, acknowledged.status());
        assertTrue(service.activeAlerts(PHYSICIAN).isEmpty());
    }

    @Test
    void acknowledgeAlert_deOtroMedico_deberiaLanzar() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);
        service.createHighUrgencyAlerts(PATIENT, List.of("dolor"), List.of("Angina de pecho"));
        var alert = service.activeAlerts(PHYSICIAN).get(0);

        assertThrows(
                PhysicianAlertNotFoundException.class, () -> service.acknowledgeAlert(UUID.randomUUID(), alert.id()));
    }

    @Test
    void acknowledgeAlert_conRelacionFinalizada_deberiaRechazar() {
        var repos = new InMemoryPhysicianRepositories();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));
        var service = service(true, repos);
        service.createHighUrgencyAlerts(PATIENT, List.of("dolor"), List.of("Angina de pecho"));
        var alert = service.activeAlerts(PHYSICIAN).get(0);

        // La relacion se termina despues de crear la alerta: el medico ya no puede gestionarla.
        repos.patientRepository().assign(
                InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT)
                        .ended(OffsetDateTime.now(), "ENDED_BY_ADMIN"));

        assertThrows(RelationshipNotActiveException.class, () -> service.acknowledgeAlert(PHYSICIAN, alert.id()));
    }

    @Test
    void assignPatient_deberiaCrearAsignacion() {
        var repos = new InMemoryPhysicianRepositories();
        var service = service(true, repos);

        service.assignPatient(PHYSICIAN, PATIENT);

        assertTrue(service.listPatients(PHYSICIAN, PageRequest.of(0, 10)).getTotalElements() == 1);
    }

    @Test
    void conModuloDeshabilitado_deberiaLanzar() {
        var service = service(false, new InMemoryPhysicianRepositories());

        assertThrows(PhysicianDisabledException.class, () -> service.listPatients(PHYSICIAN, PageRequest.of(0, 10)));
        assertThrows(PhysicianDisabledException.class, () -> service.activeAlerts(PHYSICIAN));
        assertThrows(PhysicianDisabledException.class, () -> service.patientSummary(PHYSICIAN, PATIENT));
    }
}