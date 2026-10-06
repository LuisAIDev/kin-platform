package com.kinplatform.kin.health.telemedicine.api;

import com.kinplatform.common.event.DomainEventBus;
import com.kinplatform.common.audit.api.AuditService;
import com.kinplatform.common.audit.domain.AuditAction;
import com.kinplatform.common.audit.domain.AuditResourceType;
import com.kinplatform.kin.health.physician.access.RelationshipAccessValidator;
import com.kinplatform.kin.health.telemedicine.config.TelemedicineProperties;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.domain.Message;
import com.kinplatform.kin.health.telemedicine.event.TelemedicineEvent;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación de telemedicina (ADR-032).
 *
 * <p>Mensajería asíncrona paciente ↔ médico y gestión de citas, con
 * <strong>aislamiento estricto</strong>: los mensajes solo son visibles para
 * emisor/receptor y las citas solo para el paciente y su médico asignado. La
 * validación de asignación se reutiliza del portal de médicos
 * ({@link PhysicianPatientRepository}, ADR-031). Publica {@link TelemedicineEvent}
 * para métricas y notificaciones.</p>
 */
@Service
public class TelemedicineService {

    private static final Logger log = LoggerFactory.getLogger(TelemedicineService.class);

    private final MessageRepository messageRepository;
    private final AppointmentRepository appointmentRepository;
    private final RelationshipAccessValidator accessValidator;
    private final TelemedicineProperties properties;
    private final DomainEventBus eventBus;
    private final AuditService auditService;

    public TelemedicineService(
            MessageRepository messageRepository,
            AppointmentRepository appointmentRepository,
            RelationshipAccessValidator accessValidator,
            TelemedicineProperties properties,
            AuditService auditService) {
        this(messageRepository, appointmentRepository, accessValidator, properties, auditService, null);
    }

    @Autowired
    public TelemedicineService(
            MessageRepository messageRepository,
            AppointmentRepository appointmentRepository,
            RelationshipAccessValidator accessValidator,
            TelemedicineProperties properties,
            AuditService auditService,
            DomainEventBus eventBus) {
        this.messageRepository = messageRepository;
        this.appointmentRepository = appointmentRepository;
        this.accessValidator = accessValidator;
        this.properties = properties;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    // ---------- Mensajería ----------

    /**
     * Envía un mensaje de {@code sender} a {@code receiver}. Se requiere que el
     * médico esté asignado al paciente (cualquier dirección). El contenido se
     * cifra en reposo por el adaptador JPA.
     */
    @Transactional
    public Message sendMessage(UUID senderId, UUID receiverId, String content) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        requireAssignedOrSelf(senderId, receiverId);
        Message message = Message.of(
                UUID.randomUUID(),
                senderId,
                receiverId,
                Message.conversationIdOf(senderId, receiverId),
                content == null ? "" : content,
                false,
                OffsetDateTime.now());
        Message saved = messageRepository.save(message);
        auditService.logAccess(senderId, AuditAction.SEND_MESSAGE, AuditResourceType.MENSAJE, saved.id(), null,
                java.util.Map.of("conversationId", saved.conversationId()));
        if (eventBus != null) {
            eventBus.publish(TelemedicineEvent.message(senderId, saved.id()));
        }
        log.info("TelemedicineService: mensaje enviado de {} a {}", senderId, receiverId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Message> conversationMessages(UUID userId, UUID otherId) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        requireAssignedOrSelf(userId, otherId);
        return messageRepository.findByConversationId(Message.conversationIdOf(userId, otherId));
    }

    /**
     * Marca como leídos los mensajes de la conversación dirigidos al usuario.
     * Devuelve la conversación actualizada.
     */
    @Transactional
    public List<Message> markConversationRead(UUID userId, UUID otherId) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        List<Message> messages = conversationMessages(userId, otherId);
        auditService.logAccess(userId, AuditAction.READ_MESSAGES, AuditResourceType.MENSAJE, null, null,
                java.util.Map.of("conversationWith", otherId));
        for (Message message : messages) {
            if (!message.read() && message.receiverId().equals(userId)) {
                messageRepository.save(Message.of(
                        message.id(),
                        message.senderId(),
                        message.receiverId(),
                        message.conversationId(),
                        message.content(),
                        true,
                        message.createdAt()));
            }
        }
        return conversationMessages(userId, otherId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        if (!properties.isEnabled()) {
            return 0;
        }
        return messageRepository.countUnreadByReceiver(userId);
    }

    /**
     * Todos los mensajes donde el usuario participa (como emisor o receptor),
     * para construir la lista de conversaciones. Requiere consulta por emisor
     * y receptor.
     */
    @Transactional(readOnly = true)
    public List<Message> allMessagesFor(UUID userId) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        if (userId == null) {
            return List.of();
        }
        var sent = messageRepository.findBySenderId(userId);
        var received = messageRepository.findByReceiverId(userId);
        var all = new java.util.ArrayList<Message>(sent);
        all.addAll(received);
        all.sort(java.util.Comparator.comparing(Message::createdAt));
        return List.copyOf(all);
    }

    // ---------- Citas ----------

    @Transactional
    public Appointment requestAppointment(UUID patientId, UUID physicianId, OffsetDateTime scheduledAt, String reason) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        requireAssigned(physicianId, patientId);
        Appointment appointment = Appointment.of(
                UUID.randomUUID(),
                patientId,
                physicianId,
                scheduledAt,
                reason == null ? "" : reason,
                AppointmentStatus.PENDIENTE,
                OffsetDateTime.now());
        Appointment saved = appointmentRepository.save(appointment);
        auditService.logAccess(patientId, AuditAction.REQUEST_APPOINTMENT, AuditResourceType.CITA, saved.id(),
                patientId, java.util.Map.of());
        log.info("TelemedicineService: cita solicitada paciente {} → médico {}", patientId, physicianId);
        return saved;
    }

    /**
     * El médico actualiza el estado de una cita (confirmar/rechazar/completar).
     * Solo el médico de la cita puede cambiarla.
     */
    @Transactional
    public Appointment updateAppointmentStatus(UUID physicianId, UUID appointmentId, AppointmentStatus status) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        if (status == null || status == AppointmentStatus.PENDIENTE) {
            throw new IllegalArgumentException("Estado inválido para la cita");
        }
        Appointment appointment = appointmentRepository
                .findById(appointmentId)
                .orElseThrow(() -> new TelemedicineAppointmentNotFoundException(appointmentId));
        if (!appointment.physicianId().equals(physicianId)) {
            throw new TelemedicineAppointmentNotFoundException(appointmentId);
        }
        Appointment updated = Appointment.of(
                appointment.id(),
                appointment.patientId(),
                appointment.physicianId(),
                appointment.scheduledAt(),
                appointment.reason(),
                status,
                appointment.createdAt());
        log.info("TelemedicineService: cita {} → {} por médico {}", appointmentId, status, physicianId);
        Appointment saved = appointmentRepository.save(updated);
        auditService.logAccess(physicianId, auditActionFor(status), AuditResourceType.CITA, saved.id(),
                saved.patientId(), java.util.Map.of());
        if (eventBus != null) {
            eventBus.publish(TelemedicineEvent.appointment(physicianId, saved.id()));
        }
        return saved;
    }

    private static AuditAction auditActionFor(AppointmentStatus status) {
        return switch (status) {
            case CONFIRMADA -> AuditAction.CONFIRM_APPOINTMENT;
            case CANCELADA -> AuditAction.CANCEL_APPOINTMENT;
            case COMPLETADA -> AuditAction.COMPLETE_APPOINTMENT;
            default -> AuditAction.REQUEST_APPOINTMENT;
        };
    }

    /**
     * Lista de citas del usuario: las suyas como paciente o como médico
     * (según el rol). El filtro por rol se resuelve en el controller.
     */
    @Transactional(readOnly = true)
    public List<Appointment> listAppointments(UUID userId, boolean asPhysician) {
        if (!properties.isEnabled()) {
            throw new TelemedicineDisabledException();
        }
        return asPhysician
                ? appointmentRepository.findByPhysicianId(userId)
                : appointmentRepository.findByPatientId(userId);
    }

    // ---------- Validación ----------

    private void requireAssignedOrSelf(UUID a, UUID b) {
        if (a.equals(b)) {
            throw new IllegalArgumentException("No se puede interactuar con uno mismo");
        }
        requireAssigned(a, b);
    }

    /**
     * La relación es simétrica: un mensaje/cita es válido solo si existe una
     * relación ACTIVA en cualquiera de las direcciones (Área 5 — permisos por
     * estado de relación).
     */
    private void requireAssigned(UUID a, UUID b) {
        accessValidator.requireActiveRelationshipBetween(a, b);
    }
}


