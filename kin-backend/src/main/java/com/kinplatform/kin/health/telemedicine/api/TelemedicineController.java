package com.kinplatform.kin.health.telemedicine.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.physician.api.RelationshipService;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.domain.Message;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de telemedicina (ADR-032).
 *
 * <ul>
 *   <li>{@code GET /api/v1/health/telemedicine/conversations} — conversaciones
 *       (mensajes no leídos + lista de interlocutores).</li>
 *   <li>{@code GET /api/v1/health/telemedicine/messages?with=...} — mensajes de
 *       una conversación (marca leídos).</li>
 *   <li>{@code POST /api/v1/health/telemedicine/messages} — enviar mensaje.</li>
 *   <li>{@code GET /api/v1/health/telemedicine/unread} — contador de no leídos.</li>
 *   <li>{@code POST /api/v1/health/telemedicine/appointments} — solicitar cita.</li>
 *   <li>{@code PUT /api/v1/health/telemedicine/appointments/{id}/status} —
 *       el médico actualiza el estado.</li>
 *   <li>{@code GET /api/v1/health/telemedicine/appointments} — lista de citas
 *       (rol determina el filtro).</li>
 * </ul>
 *
 * <p>Protegido por JWT. El {@code userId} se resuelve siempre desde la
 * autenticación; los mensajes y citas se aíslan por usuario y por asignación
 * médico-paciente.</p>
 */
@RestController
@RequestMapping({
    "/health/telemedicine",
    "/medical/telemedicine"
})
public class TelemedicineController {

    private static final Logger log = LoggerFactory.getLogger(TelemedicineController.class);

    private final TelemedicineService telemedicineService;
    private final UserRepository userRepository;
    private final RelationshipService relationshipService;
    private final MessageRepository messageRepository;

    public TelemedicineController(TelemedicineService telemedicineService, UserRepository userRepository,
            RelationshipService relationshipService, MessageRepository messageRepository) {
        this.telemedicineService = telemedicineService;
        this.userRepository = userRepository;
        this.relationshipService = relationshipService;
        this.messageRepository = messageRepository;
    }

    // ---------- Mensajería ----------

    @GetMapping("/contacts")
    public ResponseEntity<List<RelationshipService.ContactResponse>> contacts(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        return ResponseEntity.ok(relationshipService.activeContactsFor(user.getId(), user.getRole()));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            Authentication authentication, @Valid @RequestBody SendMessageRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== TELEMEDICINE MESSAGE === from={}, to={}", userId, request.receiverId());
        return ResponseEntity.ok(MessageResponse.from(
                telemedicineService.sendMessage(userId, request.receiverId(), request.content()), userId));
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MessageResponse>> conversationMessages(
            Authentication authentication, @RequestParam UUID with) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(telemedicineService.markConversationRead(userId, with).stream()
                .map(m -> MessageResponse.from(m, userId))
                .toList());
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> conversations(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        var messages = telemedicineService.allMessagesFor(userId);
        var conversations = ConversationResponse.fromMessages(messages, userId, userRepository);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/unread")
    public ResponseEntity<UnreadResponse> unread(Authentication authentication) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(new UnreadResponse(telemedicineService.unreadCount(userId)));
    }

    // ---------- Citas ----------

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> requestAppointment(
            Authentication authentication, @Valid @RequestBody AppointmentRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        log.info("=== TELEMEDICINE APPOINTMENT === patient={}, physician={}", userId, request.physicianId());
        return ResponseEntity.ok(AppointmentResponse.from(telemedicineService.requestAppointment(
                userId, request.physicianId(), request.scheduledAt(), request.reason())));
    }

    @PutMapping("/appointments/{appointmentId}/status")
    public ResponseEntity<AppointmentResponse> updateAppointmentStatus(
            Authentication authentication,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody StatusRequest request) {
        UUID physicianId =
                AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(AppointmentResponse.from(
                telemedicineService.updateAppointmentStatus(physicianId, appointmentId, request.status())));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<AppointmentResponse>> appointments(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        boolean asPhysician = user.getRole() == UserRole.PHYSICIAN;
        return ResponseEntity.ok(telemedicineService.listAppointments(user.getId(), asPhysician).stream()
                .map(AppointmentResponse::from)
                .toList());
    }

    // ---------- DTOs ----------

    public record SendMessageRequest(
            @NotNull(message = "receiverId es obligatorio") UUID receiverId,
            @NotBlank(message = "El contenido es obligatorio")
                    @Size(max = 4000, message = "El contenido no debe superar 4000 caracteres")
                    String content) {}

    public record AppointmentRequest(
            @NotNull(message = "physicianId es obligatorio") UUID physicianId,
            @NotNull(message = "La fecha programada es obligatoria") OffsetDateTime scheduledAt,
            @NotBlank(message = "El motivo es obligatorio")
                    @Size(max = 500, message = "El motivo no debe superar 500 caracteres")
                    String reason) {}

    public record StatusRequest(@NotNull(message = "El estado es obligatorio") AppointmentStatus status) {}

    public record UnreadResponse(long unread) {}

    public record ConversationResponse(
            UUID otherId, String otherName, String lastMessage, OffsetDateTime lastMessageAt, long unread) {

        static List<ConversationResponse> fromMessages(
                List<com.kinplatform.kin.health.telemedicine.domain.Message> messages,
                UUID userId,
                UserRepository userRepository) {
            var byConversation =
                    new java.util.LinkedHashMap<UUID, com.kinplatform.kin.health.telemedicine.domain.Message>();
            long unreadTotal = messages.stream()
                    .filter(m -> !m.read() && m.receiverId().equals(userId))
                    .count();
            for (var m : messages) {
                byConversation.putIfAbsent(m.conversationId(), m);
            }
            var out = new java.util.ArrayList<ConversationResponse>();
            for (var entry : byConversation.entrySet()) {
                var message = entry.getValue();
                UUID otherId = message.senderId().equals(userId) ? message.receiverId() : message.senderId();
                String otherName = userRepository
                        .findById(otherId)
                        .map(com.kinplatform.user.User::getFullName)
                        .orElse("Usuario");
                out.add(new ConversationResponse(
                        otherId, otherName, message.content(), message.createdAt(), unreadTotal));
            }
            out.sort(java.util.Comparator.comparing(ConversationResponse::lastMessageAt)
                    .reversed());
            return List.copyOf(out);
        }
    }

    // ---------- WhatsApp Notification ----------

    /**
     * Genera un enlace de WhatsApp para avisar al médico de un mensaje pendiente.
     * Requiere que el médico tenga activado el opt-in (whatsapp_notifications_enabled)
     * y tenga un número de teléfono configurado. No incluye datos clínicos en el mensaje.
     */
    @PostMapping("/patient/messages/{messageId}/whatsapp-link")
    public ResponseEntity<WhatsAppLinkResponse> generateWhatsAppLink(
            @PathVariable UUID messageId,
            Authentication authentication) {

        User patient = AuthenticatedUsers.require(userRepository, authentication);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Mensaje no encontrado"));

        // Validar que el paciente es el emisor
        if (!message.senderId().equals(patient.getId())) {
            throw new IllegalStateException("No puedes generar avisos de mensajes que no enviaste");
        }

        User physician = userRepository.findById(message.receiverId())
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado"));

        // Validar opt-in del médico
        if (!Boolean.TRUE.equals(physician.getWhatsappNotificationsEnabled())) {
            throw new IllegalStateException("El médico no tiene activados los avisos por WhatsApp");
        }
        if (physician.getPhone() == null || physician.getPhone().isBlank()) {
            throw new IllegalStateException("El médico no tiene un número de WhatsApp configurado");
        }

        // Validar que el médico no haya respondido ya
        boolean alreadyReplied = messageRepository.findByConversationId(message.conversationId()).stream()
                .anyMatch(m -> m.senderId().equals(physician.getId()) && m.createdAt().isAfter(message.createdAt()));
        if (alreadyReplied) {
            throw new IllegalStateException("El médico ya respondió a este mensaje");
        }

        // Generar el mensaje genérico (SIN datos clínicos)
        String physicianFirstName = physician.getFullName() != null
                ? physician.getFullName().split(" ")[0]
                : "Doctor";
        String patientName = patient.getFullName() != null ? patient.getFullName() : "un paciente";
        String chatUrl = "https://www.kin-platform-medical.com/dashboard/physician/messages";

        String genericMessage = String.format(
                "Hola %s, soy %s. Te escribí por KIN Medical y me gustaría que revises mi mensaje cuando puedas. " +
                "Por seguridad no incluyo el contenido aquí. Podrás leerlo completo en: %s",
                physicianFirstName, patientName, chatUrl);

        // Limpiar el teléfono (solo dígitos para wa.me)
        String phoneDigits = physician.getPhone().replaceAll("[^0-9]", "");

        String whatsappUrl = "https://wa.me/" + phoneDigits + "?text=" +
                URLEncoder.encode(genericMessage, StandardCharsets.UTF_8);

        log.info("TelemedicineController: enlace WhatsApp generado para mensaje {} de paciente {} a médico {}",
                messageId, patient.getId(), physician.getId());

        return ResponseEntity.ok(new WhatsAppLinkResponse(whatsappUrl));
    }

    public record WhatsAppLinkResponse(String whatsappUrl) {}
}
