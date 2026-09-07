package com.kinplatform.kin.health.physician.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.event.PatientInvitedEvent;
import com.kinplatform.kin.health.physician.event.RelationshipAcceptedEvent;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests del servicio del ciclo de vida de la relación médico-paciente (V30):
 * invitación, aceptación, rechazo, duplicados y validaciones.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelationshipServiceTest {

    private static final UUID PHYSICIAN = UUID.randomUUID();
    private static final UUID PATIENT = UUID.randomUUID();
    private static final String PATIENT_EMAIL = "paciente@kin.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainEventBus eventBus;

    private InMemoryPhysicianRepositories repos;

    @BeforeEach
    void setUp() {
        repos = new InMemoryPhysicianRepositories();
        when(userRepository.findById(PHYSICIAN))
                .thenReturn(Optional.of(User.builder()
                        .id(PHYSICIAN)
                        .email("medico@kin.com")
                        .fullName("Dr. Test")
                        .role(UserRole.PHYSICIAN)
                        .build()));
        when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .fullName("Paciente Test")
                        .role(UserRole.PATIENT)
                        .build()));
    }

    private static PhysicianProperties properties(boolean enabled, boolean inviteEnabled) {
        var props = new PhysicianProperties();
        props.setEnabled(enabled);
        props.setInviteEnabled(inviteEnabled);
        return props;
    }

    private RelationshipService service() {
        return new RelationshipService(
                repos.patientRepository(), userRepository, properties(true, true), eventBus, null);
    }

    @Test
    void invitePatient_deberiaCrearInvitacionPendiente() {
        var invitation = service().invitePatient(PHYSICIAN, PATIENT_EMAIL, "Hola, te invito a mi cartera");

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        assertEquals(PHYSICIAN, invitation.invitedBy());
        assertEquals(PATIENT, invitation.patientId());
        assertTrue(invitation.invitedAt() != null);
        verify(eventBus).publish(any(PatientInvitedEvent.class));
    }

    @Test
    void invitePatient_pacienteNoRegistrado_deberiaLanzar404ConMensajeClaro() {
        when(userRepository.findByEmail("desconocido@kin.com")).thenReturn(Optional.empty());

        var ex = assertThrows(PatientNotRegisteredException.class, () -> service()
                .invitePatient(PHYSICIAN, "desconocido@kin.com", null));
        assertTrue(ex.getReason().contains("No existe una cuenta KIN con ese correo"));
    }

    @Test
    void invitePatient_emailConRolEmpresarialSinConsentimiento_deberiaCrearPendingConsent() {
        var targetId = UUID.randomUUID();
        when(userRepository.findByEmail("empresa@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(targetId)
                        .email("empresa@kin.com")
                        .role(UserRole.FREE)
                        .healthDataConsent(false)
                        .build()));

        var invitation = service().invitePatient(PHYSICIAN, "empresa@kin.com", null);

        assertEquals(RelationshipStatus.PENDING_CONSENT, invitation.status());
        assertEquals(targetId, invitation.patientId());
        verify(eventBus).publish(any(PatientInvitedEvent.class));
    }

    @Test
    void invitePatient_medicoInexistente_deberiaLanzar() {
        when(userRepository.findById(PHYSICIAN)).thenReturn(Optional.empty());

        assertThrows(PhysicianNotFoundException.class, () -> service().invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
    }

    @Test
    void invitePatient_relacionActivaExistente_deberiaBloquearConMensajeClaro() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));

        var ex = assertThrows(
                ActiveRelationshipException.class, () -> service().invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
        assertTrue(ex.getReason().contains("El paciente ya está vinculado a usted"));
    }

    @Test
    void invitePatient_invitacionPendienteExistente_deberiaReenviarSinBloquear() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));

        var invitation = service().invitePatient(PHYSICIAN, PATIENT_EMAIL, "Recordatorio");

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        assertTrue(invitation.invitedAt() != null);
        var captor = ArgumentCaptor.forClass(PatientInvitedEvent.class);
        verify(eventBus).publish(captor.capture());
        assertTrue(captor.getValue().resend(), "el evento de reenvío debe marcarse como resend=true");
    }

    @Test
    void invitePatient_invitacionPendingConsentExistente_deberiaReenviarSinBloquear() {
        when(userRepository.findByEmail(PATIENT_EMAIL))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email(PATIENT_EMAIL)
                        .role(UserRole.FREE)
                        .healthDataConsent(false)
                        .build()));
        repos.patientRepository()
                .assign(PhysicianPatientAssignment.pendingConsent(PHYSICIAN, PATIENT, PHYSICIAN, null));

        var invitation = service().invitePatient(PHYSICIAN, PATIENT_EMAIL, null);

        assertEquals(RelationshipStatus.PENDING_CONSENT, invitation.status());
        var captor = ArgumentCaptor.forClass(PatientInvitedEvent.class);
        verify(eventBus).publish(captor.capture());
        assertTrue(captor.getValue().resend(), "el evento de reenvío debe marcarse como resend=true");
    }

    @Test
    void invitePatient_sinRelacionExistente_deberiaEmitirEventoSinResend() {
        var invitation = service().invitePatient(PHYSICIAN, PATIENT_EMAIL, "Hola");

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        var captor = ArgumentCaptor.forClass(PatientInvitedEvent.class);
        verify(eventBus).publish(captor.capture());
        assertFalse(captor.getValue().resend(), "la invitación inicial no es un reenvío");
    }

    @Test
    void acceptInvitation_deberiaActivarRelacion() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));

        var accepted = service().acceptInvitation(PATIENT, PHYSICIAN);

        assertEquals(RelationshipStatus.ACTIVE, accepted.status());
        assertTrue(accepted.acceptedAt() != null);
        verify(eventBus).publish(any(RelationshipAcceptedEvent.class));
    }

    @Test
    void acceptInvitation_sinInvitacion_deberiaLanzar404() {
        assertThrows(InvitationNotFoundException.class, () -> service().acceptInvitation(PATIENT, PHYSICIAN));
    }

    @Test
    void acceptInvitation_yaAceptada_deberiaLanzar404() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));

        assertThrows(InvitationNotFoundException.class, () -> service().acceptInvitation(PATIENT, PHYSICIAN));
    }

    @Test
    void rejectInvitation_deberiaFinalizarConMotivo() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));

        var ended = service().rejectInvitation(PATIENT, PHYSICIAN);

        assertEquals(RelationshipStatus.ENDED, ended.status());
        assertEquals(RelationshipService.REJECTED_BY_PATIENT, ended.endedReason());
        assertTrue(ended.endedAt() != null);
    }

    @Test
    void rejectInvitation_sinInvitacion_deberiaLanzar404() {
        assertThrows(InvitationNotFoundException.class, () -> service().rejectInvitation(PATIENT, PHYSICIAN));
    }

    @Test
    void pendingInvitationsForPatient_deberiaDevolverSoloPendientes() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, UUID.randomUUID()));

        var pending = service().pendingInvitationsForPatient(PATIENT);

        assertEquals(1, pending.size());
        assertEquals(RelationshipStatus.PENDING, pending.get(0).status());
        assertEquals(PHYSICIAN, pending.get(0).physicianId());
    }

    @Test
    void pendingInvitationsForPatient_deberiaIncluirPendingConsent() {
        var otherPhysician = UUID.randomUUID();
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));
        repos.patientRepository()
                .assign(PhysicianPatientAssignment.pendingConsent(otherPhysician, PATIENT, otherPhysician, null));

        var pending = service().pendingInvitationsForPatient(PATIENT);

        assertEquals(2, pending.size());
        assertTrue(pending.stream().anyMatch(a -> a.status() == RelationshipStatus.PENDING));
        assertTrue(pending.stream().anyMatch(a -> a.status() == RelationshipStatus.PENDING_CONSENT));
    }

    @Test
    void pendingInvitationsForPatient_conSoloPendingConsent_deberiaDevolverla() {
        repos.patientRepository()
                .assign(PhysicianPatientAssignment.pendingConsent(PHYSICIAN, PATIENT, PHYSICIAN, null));

        var pending = service().pendingInvitationsForPatient(PATIENT);

        assertEquals(1, pending.size());
        assertEquals(RelationshipStatus.PENDING_CONSENT, pending.get(0).status());
    }

    @Test
    void conInviteDeshabilitado_deberiaLanzar() {
        var service = new RelationshipService(
                repos.patientRepository(), userRepository, properties(true, false), eventBus, null);

        assertThrows(PhysicianDisabledException.class, () -> service.invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
        assertThrows(PhysicianDisabledException.class, () -> service.acceptInvitation(PATIENT, PHYSICIAN));
        assertThrows(PhysicianDisabledException.class, () -> service.pendingInvitationsForPatient(PATIENT));
    }

    // ------------------------------------------------------------------
    // Capacidad de paciente desacoplada de users.role (ADR-040): cualquier
    // usuario con health_data_consent=true puede ser invitado, y los PATIENT
    // legacy se mantienen (compatibilidad hacia atrás).
    // ------------------------------------------------------------------

    @Test
    void invitePatient_freeConConsentimiento_deberiaInvitar() {
        var targetId = UUID.randomUUID();
        when(userRepository.findByEmail("free-paciente@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(targetId)
                        .email("free-paciente@kin.com")
                        .role(UserRole.FREE)
                        .healthDataConsent(true)
                        .build()));

        var invitation = service().invitePatient(PHYSICIAN, "free-paciente@kin.com", "Hola");

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        assertEquals(targetId, invitation.patientId());
        verify(eventBus).publish(any(PatientInvitedEvent.class));
    }

    @Test
    void invitePatient_freeSinConsentimiento_deberiaCrearPendingConsent() {
        var targetId = UUID.randomUUID();
        when(userRepository.findByEmail("free-sin-consent@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(targetId)
                        .email("free-sin-consent@kin.com")
                        .role(UserRole.FREE)
                        .healthDataConsent(false)
                        .build()));

        var invitation = service().invitePatient(PHYSICIAN, "free-sin-consent@kin.com", null);

        assertEquals(RelationshipStatus.PENDING_CONSENT, invitation.status());
        assertEquals(targetId, invitation.patientId());
        verify(eventBus).publish(any(PatientInvitedEvent.class));
    }

    @Test
    void invitePatient_premiumConConsentimiento_deberiaInvitar() {
        var targetId = UUID.randomUUID();
        when(userRepository.findByEmail("premium-paciente@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(targetId)
                        .email("premium-paciente@kin.com")
                        .role(UserRole.PREMIUM)
                        .healthDataConsent(true)
                        .build()));

        var invitation = service().invitePatient(PHYSICIAN, "premium-paciente@kin.com", null);

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        assertEquals(targetId, invitation.patientId());
    }

    @Test
    void invitePatient_physicianConConsentimiento_deberiaInvitar() {
        var targetId = UUID.randomUUID();
        when(userRepository.findByEmail("medico-paciente@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(targetId)
                        .email("medico-paciente@kin.com")
                        .role(UserRole.PHYSICIAN)
                        .healthDataConsent(true)
                        .build()));

        var invitation = service().invitePatient(PHYSICIAN, "medico-paciente@kin.com", null);

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        assertEquals(targetId, invitation.patientId());
    }

    @Test
    void invitePatient_patientLegacySinConsentimiento_deberiaInvitar() {
        var targetId = UUID.randomUUID();
        when(userRepository.findByEmail("patient-legacy@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(targetId)
                        .email("patient-legacy@kin.com")
                        .role(UserRole.PATIENT)
                        .healthDataConsent(false)
                        .build()));

        var invitation = service().invitePatient(PHYSICIAN, "patient-legacy@kin.com", null);

        assertEquals(RelationshipStatus.PENDING, invitation.status());
        assertEquals(targetId, invitation.patientId());
    }

    // ------------------------------------------------------------------
    // Flujo "consentimiento en un clic": el paciente acepta el consentimiento
    // de salud y la relación PENDING_CONSENT pasa a ACTIVE automáticamente.
    // ------------------------------------------------------------------

    @Test
    void acceptWithConsent_deberiaConcederConsentimientoYActivarRelacion() {
        var patient = User.builder()
                .id(PATIENT)
                .email("paciente@kin.com")
                .fullName("Paciente Test")
                .role(UserRole.FREE)
                .healthDataConsent(false)
                .build();
        when(userRepository.findById(PATIENT)).thenReturn(Optional.of(patient));
        repos.patientRepository()
                .assign(PhysicianPatientAssignment.pendingConsent(PHYSICIAN, PATIENT, PHYSICIAN, null));

        var accepted = service().acceptWithConsent(PATIENT, PHYSICIAN);

        assertEquals(RelationshipStatus.ACTIVE, accepted.status());
        assertTrue(accepted.acceptedAt() != null);
        assertTrue(patient.getHealthDataConsent());
        verify(userRepository).save(patient);
        verify(eventBus).publish(any(RelationshipAcceptedEvent.class));
    }

    @Test
    void acceptWithConsent_cuandoYaActiva_deberiaSerIdempotente() {
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email("paciente@kin.com")
                        .role(UserRole.FREE)
                        .healthDataConsent(false)
                        .build()));
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));

        var result = service().acceptWithConsent(PATIENT, PHYSICIAN);

        assertEquals(RelationshipStatus.ACTIVE, result.status());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void acceptWithConsent_sinInvitacion_deberiaLanzar404() {
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder()
                        .id(PATIENT)
                        .email("paciente@kin.com")
                        .role(UserRole.FREE)
                        .healthDataConsent(false)
                        .build()));

        assertThrows(InvitationNotFoundException.class, () -> service().acceptWithConsent(PATIENT, PHYSICIAN));
    }

    // ------------------------------------------------------------------
    // Cuota de invitaciones: el bypass configurable (allowUnlimitedInvites)
    // elimina el QUOTA_EXCEEDED para pruebas sin abrir la puerta en default.
    // ------------------------------------------------------------------

    private HealthQuotaPort quota(boolean eligiblePlan, Integer maxPatients) {
        return new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return null;
            }

            @Override
            public Integer getMaxPatients(UUID physicianId) {
                return maxPatients;
            }

            @Override
            public Integer getTrialDays(UUID userId) {
                return null;
            }

            @Override
            public boolean hasEligibleSubscription(
                    UUID userId, ProductVertical vertical, SubscriptionStatus... statuses) {
                return eligiblePlan;
            }
        };
    }

    private RelationshipService serviceWithQuota(
            HealthQuotaPort healthQuotaPort, boolean allowUnlimitedInvites, boolean enforceInviteQuota) {
        var props = new PhysicianProperties();
        props.setEnabled(true);
        props.setInviteEnabled(true);
        props.setInvitationEmailEnabled(true);
        props.setAllowUnlimitedInvites(allowUnlimitedInvites);
        props.setEnforceInviteQuota(enforceInviteQuota);
        return new RelationshipService(
                repos.patientRepository(), userRepository, props, eventBus, null, healthQuotaPort);
    }

    @Test
    void invitePatient_sinPlanConCuotaObligatoria_deberiaLanzarQuotaExceeded() {
        assertThrows(QuotaExceededException.class, () -> serviceWithQuota(quota(false, null), false, true)
                .invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
    }

    @Test
    void invitePatient_cuotaDesactivadaPorDefault_deberiaPermitirSinPlan() {
        // Fase piloto: enforceInviteQuota=false (default) => no exige plan.
        var invitation =
                serviceWithQuota(quota(false, null), false, false).invitePatient(PHYSICIAN, PATIENT_EMAIL, null);

        assertEquals(RelationshipStatus.PENDING, invitation.status());
    }

    @Test
    void invitePatient_conBypassSinPlan_deberiaPermitirLaInvitacion() {
        var invitation = serviceWithQuota(quota(false, null), true, true).invitePatient(PHYSICIAN, PATIENT_EMAIL, null);

        assertEquals(RelationshipStatus.PENDING, invitation.status());
    }

    @Test
    void invitePatient_conBypassALimiteAlcanzado_deberiaPermitirLaInvitacion() {
        // Alcanzó el límite del plan (0 cupos) pero el bypass está activo.
        var invitation = serviceWithQuota(quota(true, 0), true, true).invitePatient(PHYSICIAN, PATIENT_EMAIL, null);

        assertEquals(RelationshipStatus.PENDING, invitation.status());
    }
}
