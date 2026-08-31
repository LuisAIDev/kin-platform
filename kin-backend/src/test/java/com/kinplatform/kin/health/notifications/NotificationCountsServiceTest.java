package com.kinplatform.kin.health.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.health.documents.api.DocumentService;
import com.kinplatform.kin.health.followup.api.FollowUpService;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.kin.health.telemedicine.config.TelemedicineProperties;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
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

/**
 * Tests del agregador de contadores de notificaciones (por rol: paciente vs.
 * médico, con respeto a los feature flags de los módulos).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationCountsServiceTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock
    private PhysicianPatientRepository physicianPatientRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ClinicalAlertRepository clinicalAlertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowUpService followUpService;

    @Mock
    private DocumentService documentService;

    private PhysicianProperties physicianProperties;
    private TelemedicineProperties telemedicineProperties;
    private NotificationCountsService service;

    @BeforeEach
    void setUp() {
        physicianProperties = new PhysicianProperties();
        telemedicineProperties = new TelemedicineProperties();
        service = new NotificationCountsService(
                physicianPatientRepository,
                messageRepository,
                appointmentRepository,
                clinicalAlertRepository,
                userRepository,
                physicianProperties,
                telemedicineProperties,
                followUpService,
                documentService);
    }

    private void userWithRole(UserRole role) {
        when(userRepository.findById(USER))
                .thenReturn(Optional.of(User.builder()
                        .id(USER)
                        .email("u@kin.com")
                        .role(role)
                        .build()));
    }

    @Test
    void paciente_deberiaSumarInvitacionesMensajesNoLeidosCitasYtareas() {
        userWithRole(UserRole.PATIENT);
        when(physicianPatientRepository.findByPatientIdAndStatus(USER, RelationshipStatus.PENDING))
                .thenReturn(List.of(
                        PhysicianPatientAssignment.invitation(UUID.randomUUID(), USER, UUID.randomUUID(), OffsetDateTime.now()),
                        PhysicianPatientAssignment.invitation(UUID.randomUUID(), USER, UUID.randomUUID(), OffsetDateTime.now()),
                        PhysicianPatientAssignment.invitation(UUID.randomUUID(), USER, UUID.randomUUID(), OffsetDateTime.now())));
        when(messageRepository.countUnreadByReceiver(USER)).thenReturn(2L);
        when(appointmentRepository.countUpcomingByPatient(org.mockito.ArgumentMatchers.eq(USER), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1L);
        when(followUpService.pendingTaskCountForPatient(USER)).thenReturn(4L);
        when(documentService.activeDocumentCountForPatient(USER)).thenReturn(2L);

        NotificationCounts counts = service.countsFor(USER);

        assertEquals(3, counts.invitations());
        assertEquals(2, counts.unreadMessages());
        assertEquals(1, counts.upcomingAppointments());
        assertEquals(4, counts.pendingTasks());
        assertEquals(2, counts.documents());
        assertEquals(0, counts.pendingAppointments());
        assertEquals(0, counts.highUrgencyAlerts());
    }

    @Test
    void medico_deberiaSumarMensajesNoLeidosCitasPendientesAlertasYTareasVencidas() {
        userWithRole(UserRole.PHYSICIAN);
        when(messageRepository.countUnreadByReceiver(USER)).thenReturn(5L);
        when(appointmentRepository.countPendingByPhysician(USER)).thenReturn(2L);
        when(clinicalAlertRepository.countActiveHighUrgencyByPhysician(USER)).thenReturn(1L);
        when(followUpService.listOverdueTasks(USER)).thenReturn(java.util.List.of(
                com.kinplatform.kin.health.followup.domain.FollowUpTask.pending(
                        UUID.randomUUID(), UUID.randomUUID(), "Tarea vencida", OffsetDateTime.now().minusDays(1))));

        NotificationCounts counts = service.countsFor(USER);

        assertEquals(0, counts.invitations());
        assertEquals(5, counts.unreadMessages());
        assertEquals(2, counts.pendingAppointments());
        assertEquals(1, counts.overdueTasks());
        assertEquals(0, counts.upcomingAppointments());
        assertEquals(1, counts.highUrgencyAlerts());
    }

    @Test
    void conTelemedicinaDeshabilitada_deberiaCeroEnMensajesYCitas() {
        telemedicineProperties.setEnabled(false);
        userWithRole(UserRole.PATIENT);
        when(physicianPatientRepository.findByPatientIdAndStatus(USER, RelationshipStatus.PENDING))
                .thenReturn(List.of());

        NotificationCounts counts = service.countsFor(USER);

        assertEquals(0, counts.unreadMessages());
        assertEquals(0, counts.upcomingAppointments());
        verify(messageRepository, never()).countUnreadByReceiver(USER);
        verify(appointmentRepository, never()).countUpcomingByPatient(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void conPortalDeshabilitado_deberiaCeroEnInvitacionesYAlertas() {
        physicianProperties.setEnabled(false);
        userWithRole(UserRole.PHYSICIAN);

        NotificationCounts counts = service.countsFor(USER);

        assertEquals(0, counts.invitations());
        assertEquals(0, counts.highUrgencyAlerts());
        verify(physicianPatientRepository, never()).findByPatientIdAndStatus(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(clinicalAlertRepository, never()).countActiveHighUrgencyByPhysician(USER);
    }

    @Test
    void usuarioInexistente_deberiaDevolverVacio() {
        when(userRepository.findById(USER)).thenReturn(Optional.empty());

        NotificationCounts counts = service.countsFor(USER);

        assertEquals(NotificationCounts.empty(), counts);
        assertEquals(0, counts.total());
    }

    @Test
    void total_deberiaSumarTodosLosContadores() {
        NotificationCounts counts = new NotificationCounts(3, 2, 1, 2, 0, 4, 1, 5);
        assertEquals(18, counts.total());
    }
}
