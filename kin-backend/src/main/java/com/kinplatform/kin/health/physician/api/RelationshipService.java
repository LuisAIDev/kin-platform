package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.event.PatientInvitedEvent;
import com.kinplatform.kin.health.physician.event.RelationshipAcceptedEvent;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del ciclo de vida de la relación médico-paciente.
 *
 * <p>Implementa el flujo de invitación/aceptación/rechazo (V30): el médico
 * invita a un paciente registrado (por email), el paciente acepta o rechaza y
 * la relación pasa por estados ({@link RelationshipStatus}). Solo las
 * relaciones {@code ACTIVE} habilitan mensajería y citas. Los eventos de
 * dominio se publican de forma transaccional vía
 * {@link OutboxEventPublisher} (fallback al bus en memoria) para futuras
 * notificaciones y auditoría. La asignación por ADMIN/piloto sigue creando
 * relaciones {@code ACTIVE} directas sin pasar por la invitación.</p>
 */
@Service
public class RelationshipService {

    private static final Logger log = LoggerFactory.getLogger(RelationshipService.class);

    public static final String REJECTED_BY_PATIENT = "REJECTED_BY_PATIENT";

    private final PhysicianPatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PhysicianProperties properties;
    private final DomainEventBus eventBus;
    private final OutboxEventPublisher outboxEventPublisher;

    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties) {
        this(patientRepository, userRepository, properties, null, null);
    }

    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties,
            DomainEventBus eventBus) {
        this(patientRepository, userRepository, properties, eventBus, null);
    }

    @Autowired
    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    /**
     * Invita a un paciente (existente en KIN, rol PATIENT) a vincularse.
     * La relación queda {@code PENDING}; no se permiten duplicados
     * (ACTIVE o PENDING ya existentes).
     */
    @Transactional
    public PhysicianPatientAssignment invitePatient(UUID physicianId, String patientEmail, String message) {
        requireInviteEnabled();
        if (physicianId == null) {
            throw new IllegalArgumentException("physicianId es obligatorio");
        }
        String normalized = normalizeEmail(patientEmail);
        User physician = userRepository
                .findById(physicianId)
                .filter(u -> u.getRole() == UserRole.PHYSICIAN)
                .orElseThrow(() -> new PhysicianNotFoundException(physicianId));
        User patient = userRepository
                .findByEmail(normalized)
                .filter(u -> u.getRole() == UserRole.PATIENT)
                .orElseThrow(() -> new PatientNotRegisteredException(patientEmail));

        if (patientRepository.existsByPhysicianIdAndPatientIdAndStatus(
                        physicianId, patient.getId(), RelationshipStatus.ACTIVE)
                || patientRepository.existsByPhysicianIdAndPatientIdAndStatus(
                        physicianId, patient.getId(), RelationshipStatus.PENDING)) {
            throw new DuplicateRelationshipException(physicianId, patient.getId());
        }

        PhysicianPatientAssignment invitation =
                PhysicianPatientAssignment.invitation(physicianId, patient.getId(), physicianId, OffsetDateTime.now());
        patientRepository.assign(invitation);
        publish(new PatientInvitedEvent(
                patient.getId(), physicianId, physician.getFullName(), message == null ? "" : message));
        log.info("RelationshipService: médico {} invitó al paciente {}", physicianId, patient.getId());
        return invitation;
    }

    /**
     * El paciente acepta la invitación pendiente del médico: la relación
     * pasa a {@code ACTIVE} y se registra {@code acceptedAt}.
     */
    @Transactional
    public PhysicianPatientAssignment acceptInvitation(UUID patientId, UUID physicianId) {
        requireInviteEnabled();
        requireNotNull(patientId, physicianId);
        PhysicianPatientAssignment pending = pending(patientId, physicianId);
        PhysicianPatientAssignment accepted = pending.accepted(OffsetDateTime.now());
        patientRepository.assign(accepted);
        publish(new RelationshipAcceptedEvent(patientId, physicianId));
        log.info("RelationshipService: paciente {} aceptó la invitación del médico {}", patientId, physicianId);
        return accepted;
    }

    /**
     * El paciente rechaza la invitación pendiente: la relación pasa a
     * {@code ENDED} con motivo {@code REJECTED_BY_PATIENT}.
     */
    @Transactional
    public PhysicianPatientAssignment rejectInvitation(UUID patientId, UUID physicianId) {
        requireInviteEnabled();
        requireNotNull(patientId, physicianId);
        PhysicianPatientAssignment pending = pending(patientId, physicianId);
        PhysicianPatientAssignment ended = pending.ended(OffsetDateTime.now(), REJECTED_BY_PATIENT);
        patientRepository.assign(ended);
        log.info("RelationshipService: paciente {} rechazó la invitación del médico {}", patientId, physicianId);
        return ended;
    }

    /** Invitaciones pendientes dirigidas al paciente. */
    @Transactional(readOnly = true)
    public List<PhysicianPatientAssignment> pendingInvitationsForPatient(UUID patientId) {
        requireInviteEnabled();
        if (patientId == null) {
            return List.of();
        }
        return patientRepository.findByPatientIdAndStatus(patientId, RelationshipStatus.PENDING);
    }

    private PhysicianPatientAssignment pending(UUID patientId, UUID physicianId) {
        return patientRepository
                .findPendingInvitation(physicianId, patientId)
                .orElseThrow(() -> new InvitationNotFoundException(physicianId, patientId));
    }

    private void requireNotNull(UUID patientId, UUID physicianId) {
        if (patientId == null) {
            throw new IllegalArgumentException("patientId es obligatorio");
        }
        if (physicianId == null) {
            throw new IllegalArgumentException("physicianId es obligatorio");
        }
    }

    private void requireInviteEnabled() {
        if (!properties.isEnabled() || !properties.isInviteEnabled()) {
            throw new PhysicianDisabledException();
        }
    }

    private void publish(DomainEvent event) {
        if (outboxEventPublisher != null) {
            outboxEventPublisher.publish(event);
        } else if (eventBus != null) {
            eventBus.publish(event);
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("patientEmail es obligatorio");
        }
        return email.trim().toLowerCase();
    }
}
