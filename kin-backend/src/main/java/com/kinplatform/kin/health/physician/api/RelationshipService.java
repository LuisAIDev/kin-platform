package com.kinplatform.kin.health.physician.api;

import com.kinplatform.kin.event.DomainEvent;
import com.kinplatform.kin.event.DomainEventBus;
import com.kinplatform.kin.eventbus.port.OutboxEventPublisher;
import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.physician.config.PhysicianProperties;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import com.kinplatform.kin.health.physician.domain.RelationshipStatus;
import com.kinplatform.kin.health.physician.event.PatientInvitedEvent;
import com.kinplatform.kin.health.physician.event.RelationshipAcceptedEvent;
import com.kinplatform.kin.health.physician.port.PhysicianPatientRepository;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
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
    private final HealthQuotaPort healthQuotaPort;

    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties) {
        this(patientRepository, userRepository, properties, null, null, null);
    }

    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties,
            DomainEventBus eventBus) {
        this(patientRepository, userRepository, properties, eventBus, null, null);
    }

    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher) {
        this(patientRepository, userRepository, properties, eventBus, outboxEventPublisher, null);
    }

    @Autowired
    public RelationshipService(
            PhysicianPatientRepository patientRepository,
            UserRepository userRepository,
            PhysicianProperties properties,
            DomainEventBus eventBus,
            OutboxEventPublisher outboxEventPublisher,
            HealthQuotaPort healthQuotaPort) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.eventBus = eventBus;
        this.outboxEventPublisher = outboxEventPublisher;
        this.healthQuotaPort = healthQuotaPort;
    }

    /**
     * Invita a un usuario existente en KIN a vincularse con el médico. No crea
     * cuentas nuevas y NUNCA bloquea por falta de consentimiento: si el usuario
     * ya tiene capacidad de paciente ({@code PatientAccess.isPatient}) la
     * invitación queda {@code PENDING}; si no la tiene (falta aceptar el
     * consentimiento de salud), la invitación queda {@code PENDING_CONSENT} y
     * el correo la guía para aceptar consentimiento y vincularse en un solo
     * clic. No se permiten duplicados (ACTIVE/PENDING/PENDING_CONSENT).
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
        User patient = userRepository.findByEmail(normalized).orElse(null);
        if (patient == null) {
            // 404: no existe cuenta KIN con ese correo (el flujo NO crea usuarios).
            throw new PatientNotRegisteredException();
        }
        if (duplicateRelationship(physicianId, patient.getId())) {
            throw new DuplicateRelationshipException();
        }

        requirePhysicianCanInvite(physicianId);

        boolean requiresConsent = !com.kinplatform.common.security.PatientAccess.isPatient(patient);
        PhysicianPatientAssignment invitation = requiresConsent
                ? PhysicianPatientAssignment.pendingConsent(
                        physicianId, patient.getId(), physicianId, OffsetDateTime.now())
                : PhysicianPatientAssignment.invitation(
                        physicianId, patient.getId(), physicianId, OffsetDateTime.now());
        patientRepository.assign(invitation);
        publish(new PatientInvitedEvent(
                patient.getId(), physicianId, physician.getFullName(), message == null ? "" : message));
        log.info(
                "RelationshipService: médico {} invitó al usuario {} (estado {}, requiereConsentimiento={})",
                physicianId,
                patient.getId(),
                invitation.status(),
                requiresConsent);
        return invitation;
    }

    /**
     * El paciente acepta el consentimiento de datos de salud y, con un único
     * clic, se vincula al médico que lo invitó (transición
     * {@code PENDING_CONSENT} → {@code ACTIVE}). Idempotente: si la relación ya
     * está {@code ACTIVE}, solo garantiza el consentimiento y responde OK.
     */
    @Transactional
    public PhysicianPatientAssignment acceptWithConsent(UUID patientId, UUID physicianId) {
        requireInviteEnabled();
        requireNotNull(patientId, physicianId);

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Paciente no encontrado"));
        if (!Boolean.TRUE.equals(patient.getHealthDataConsent())) {
            patient.setHealthDataConsent(true);
            userRepository.save(patient);
            log.info("RelationshipService: paciente {} aceptó el consentimiento de datos de salud", patientId);
        }

        PhysicianPatientAssignment current = patientRepository
                .findByPhysicianIdAndPatientId(physicianId, patientId)
                .orElseThrow(() -> new InvitationNotFoundException(physicianId, patientId));

        if (current.isActive()) {
            return current;
        }
        if (!current.isAwaitingAcceptance()) {
            throw new InvitationNotFoundException(physicianId, patientId);
        }

        PhysicianPatientAssignment accepted = current.accepted(OffsetDateTime.now());
        patientRepository.assign(accepted);
        publish(new RelationshipAcceptedEvent(patientId, physicianId));
        log.info("RelationshipService: paciente {} aceptó consentimiento y se vinculó con médico {}", patientId, physicianId);
        return accepted;
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

    /**
     * Devuelve los usuarios con relación {@code ACTIVE} con el usuario indicado:
     * si es PHYSICIAN, sus pacientes ACTIVE; si es PATIENT, sus médicos ACTIVE.
     * Se usa para iniciar conversaciones y reservar citas sin depender de mensajes previos.
     */
    @Transactional(readOnly = true)
    public List<ContactResponse> activeContactsFor(UUID userId, UserRole role) {
        if (userId == null) {
            return List.of();
        }
        List<UUID> ids = role == UserRole.PHYSICIAN
                ? patientRepository.findPatientIdsByPhysician(userId)
                : patientRepository.findPhysicianIdsByPatient(userId);
        return ids.stream()
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(u -> new ContactResponse(u.getId(), u.getFullName(), u.getRole()))
                .toList();
    }

    public record ContactResponse(UUID id, String name, UserRole role) {}

    private PhysicianPatientAssignment pending(UUID patientId, UUID physicianId) {
        return patientRepository
                .findByPhysicianIdAndPatientId(physicianId, patientId)
                .filter(PhysicianPatientAssignment::isAwaitingAcceptance)
                .orElseThrow(() -> new InvitationNotFoundException(physicianId, patientId));
    }

    /** ¿Ya existe una relación activa o una invitación pendiente (PENDING o PENDING_CONSENT)? */
    private boolean duplicateRelationship(UUID physicianId, UUID patientId) {
        return patientRepository.existsByPhysicianIdAndPatientIdAndStatus(
                        physicianId, patientId, RelationshipStatus.ACTIVE)
                || patientRepository.existsByPhysicianIdAndPatientIdAndStatus(
                        physicianId, patientId, RelationshipStatus.PENDING)
                || patientRepository.existsByPhysicianIdAndPatientIdAndStatus(
                        physicianId, patientId, RelationshipStatus.PENDING_CONSENT);
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

    /**
     * Valida que el médico puede incorporar pacientes según su plan:
     * <ul>
     *   <li>Debe tener una suscripción {@code ACTIVE} o {@code TRIAL} vigente
     *       del producto SALUD_PROFESIONAL (un médico sin plan o con trial
     *       expirado no puede invitar pacientes).</li>
     *   <li>Si el plan de pago tiene un límite de pacientes (maxPatients, p. ej.
     *       100 en Profesional), no puede excederlo. Solo cuentan las
     *       relaciones {@code ACTIVE} (la invitación PENDING aún no ocupa cupo).</li>
     * </ul>
     */
    private void requirePhysicianCanInvite(UUID physicianId) {
        if (healthQuotaPort == null) {
            // Constructores de test sin HealthQuotaPort: no bloquean.
            return;
        }
        boolean hasPlan = healthQuotaPort.hasEligibleSubscription(
                physicianId,
                ProductVertical.SALUD_PROFESIONAL,
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.TRIAL);
        if (!hasPlan) {
            throw new QuotaExceededException(
                    "Tu plan no permite invitar pacientes. Contrata el plan Profesional ($35/mes) o activa tu prueba de 30 días.",
                    "QUOTA_EXCEEDED",
                    "/dashboard/physician/plans");
        }
        Integer maxPatients = healthQuotaPort.getMaxPatients(physicianId);
        if (maxPatients != null) {
            long activePatients = patientRepository.findPatientIdsByPhysician(physicianId).size();
            if (activePatients >= maxPatients) {
                throw new QuotaExceededException(
                        "Has alcanzado el límite de " + maxPatients + " pacientes de tu plan.",
                        "QUOTA_EXCEEDED",
                        "/dashboard/physician/plans");
            }
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
