package com.kinplatform.kin.health.notifications;

import com.kinplatform.kin.health.documents.api.DocumentService;
import com.kinplatform.kin.health.followup.api.FollowUpService;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.port.ClinicalAlertRepository;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.kin.health.telemedicine.config.TelemedicineProperties;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agrega los contadores de notificaciones del usuario autenticado según su rol.
 *
 * <p>Paciente: invitaciones pendientes, mensajes no leídos, próximas citas y
 * tareas de seguimiento pendientes. Médico: mensajes no leídos, citas pendientes
 * de confirmación, alertas de alta urgencia y tareas de seguimiento vencidas.
 * Respeta los feature flags de los módulos (si el módulo está deshabilitado, el
 * contador correspondiente es 0).</p>
 */
@Service
public class NotificationCountsService {

    private final PhysicianPatientRepository physicianPatientRepository;
    private final MessageRepository messageRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicalAlertRepository clinicalAlertRepository;
    private final UserRepository userRepository;
    private final PhysicianProperties physicianProperties;
    private final TelemedicineProperties telemedicineProperties;
    private final FollowUpService followUpService;
    private final DocumentService documentService;

    public NotificationCountsService(
            PhysicianPatientRepository physicianPatientRepository,
            MessageRepository messageRepository,
            AppointmentRepository appointmentRepository,
            ClinicalAlertRepository clinicalAlertRepository,
            UserRepository userRepository,
            PhysicianProperties physicianProperties,
            TelemedicineProperties telemedicineProperties,
            FollowUpService followUpService,
            DocumentService documentService) {
        this.physicianPatientRepository = physicianPatientRepository;
        this.messageRepository = messageRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicalAlertRepository = clinicalAlertRepository;
        this.userRepository = userRepository;
        this.physicianProperties = physicianProperties;
        this.telemedicineProperties = telemedicineProperties;
        this.followUpService = followUpService;
        this.documentService = documentService;
    }

    @Transactional(readOnly = true)
    public NotificationCounts countsFor(UUID userId) {
        if (userId == null) {
            return NotificationCounts.empty();
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return NotificationCounts.empty();
        }
        return user.getRole() == UserRole.PHYSICIAN ? physicianCounts(userId) : patientCounts(userId);
    }

    private NotificationCounts patientCounts(UUID patientId) {
        int invitations = physicianProperties.isEnabled()
                ? physicianPatientRepository
                        .findByPatientIdAndStatus(patientId, RelationshipStatus.PENDING)
                        .size()
                : 0;
        long unread = telemedicineProperties.isEnabled() ? messageRepository.countUnreadByReceiver(patientId) : 0;
        long upcoming = telemedicineProperties.isEnabled()
                ? appointmentRepository.countUpcomingByPatient(patientId, OffsetDateTime.now())
                : 0;
        long pendingTasks = followUpService.pendingTaskCountForPatient(patientId);
        long documents = documentService.activeDocumentCountForPatient(patientId);
        return new NotificationCounts(invitations, unread, 0, upcoming, 0, pendingTasks, 0, documents);
    }

    private NotificationCounts physicianCounts(UUID physicianId) {
        long unread = telemedicineProperties.isEnabled() ? messageRepository.countUnreadByReceiver(physicianId) : 0;
        long pending = telemedicineProperties.isEnabled()
                ? appointmentRepository.countPendingByPhysician(physicianId)
                : 0;
        long alerts = physicianProperties.isEnabled()
                ? clinicalAlertRepository.countActiveHighUrgencyByPhysician(physicianId)
                : 0;
        long overdueTasks = followUpService.listOverdueTasks(physicianId).size();
        return new NotificationCounts(0, unread, pending, 0, alerts, 0, overdueTasks, 0);
    }
}

