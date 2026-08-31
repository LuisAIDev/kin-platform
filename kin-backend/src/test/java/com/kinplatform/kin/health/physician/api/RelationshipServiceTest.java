package com.kinplatform.kin.health.physician.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.health.physician.InMemoryPhysicianRepositories;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.event.PatientInvitedEvent;
import com.kinplatform.kin.health.physician.event.RelationshipAcceptedEvent;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void invitePatient_pacienteNoRegistrado_deberiaLanzar404() {
        when(userRepository.findByEmail("desconocido@kin.com")).thenReturn(Optional.empty());

        assertThrows(
                PatientNotRegisteredException.class,
                () -> service().invitePatient(PHYSICIAN, "desconocido@kin.com", null));
    }

    @Test
    void invitePatient_emailConRolEmpresarial_deberiaLanzar404() {
        when(userRepository.findByEmail("empresa@kin.com"))
                .thenReturn(Optional.of(User.builder()
                        .id(UUID.randomUUID())
                        .email("empresa@kin.com")
                        .role(UserRole.FREE)
                        .build()));

        assertThrows(PatientNotRegisteredException.class, () -> service().invitePatient(PHYSICIAN, "empresa@kin.com", null));
    }

    @Test
    void invitePatient_medicoInexistente_deberiaLanzar() {
        when(userRepository.findById(PHYSICIAN)).thenReturn(Optional.empty());

        assertThrows(PhysicianNotFoundException.class, () -> service().invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
    }

    @Test
    void invitePatient_relacionActivaExistente_deberiaLanzarConflicto() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));

        assertThrows(DuplicateRelationshipException.class, () -> service().invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
    }

    @Test
    void invitePatient_invitacionPendienteExistente_deberiaLanzarConflicto() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.pendingAssignment(PHYSICIAN, PATIENT));

        assertThrows(DuplicateRelationshipException.class, () -> service().invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
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
        assertThrows(
                InvitationNotFoundException.class,
                () -> service().acceptInvitation(PATIENT, PHYSICIAN));
    }

    @Test
    void acceptInvitation_yaAceptada_deberiaLanzar404() {
        repos.patientRepository().assign(InMemoryPhysicianRepositories.assignment(PHYSICIAN, PATIENT));

        assertThrows(
                InvitationNotFoundException.class,
                () -> service().acceptInvitation(PATIENT, PHYSICIAN));
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
        assertThrows(
                InvitationNotFoundException.class,
                () -> service().rejectInvitation(PATIENT, PHYSICIAN));
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
    void conInviteDeshabilitado_deberiaLanzar() {
        var service = new RelationshipService(
                repos.patientRepository(), userRepository, properties(true, false), eventBus, null);

        assertThrows(
                PhysicianDisabledException.class,
                () -> service.invitePatient(PHYSICIAN, PATIENT_EMAIL, null));
        assertThrows(
                PhysicianDisabledException.class,
                () -> service.acceptInvitation(PATIENT, PHYSICIAN));
        assertThrows(
                PhysicianDisabledException.class,
                () -> service.pendingInvitationsForPatient(PATIENT));
    }
}
