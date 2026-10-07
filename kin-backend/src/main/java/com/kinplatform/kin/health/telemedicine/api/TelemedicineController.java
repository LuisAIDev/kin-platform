package com.kinplatform.kin.health.telemedicine.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.physician.api.RelationshipService;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.kin.health.telemedicine.domain.Message;
import com.kinplatform.kin.health.telemedicine.port.AppointmentRepository;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private final AppointmentRepository appointmentRepository;

    public TelemedicineController(TelemedicineService telemedicineService, UserRepository userRepository,
            RelationshipService relationshipService, MessageRepository messageRepository,
            AppointmentRepository appointmentRepository) {
        this.telemedicineService = telemedicineService;
        this.userRepository = userRepository;
        this.relationshipService = relationshipService;
        this.messageRepository = messageRepository;
        this.appointmentRepository = appointmentRepository;
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

    // ---------- Videollamada Jitsi Meet (ADR-042) ----------

    @PostMapping("/appointments/{appointmentId}/video-room")
    public ResponseEntity<VideoRoomResponse> getOrCreateVideoRoom(
            @PathVariable UUID appointmentId,
            Authentication auth) {
        UUID userId = AuthenticatedUsers.require(userRepository, auth).getId();
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        // Validar participante (1 llamada)
        if (!appointment.involves(userId)) {
            throw new RuntimeException("No eres participante de esta cita");
        }

        // Validar estado (PENDIENTE o CONFIRMADA)
        if (!appointment.isOpen()) {
            throw new IllegalStateException(
                    "La videollamada solo está disponible para citas pendientes o confirmadas"
            );
        }

        // Validar ventana de tiempo (30 min antes a 2h después)
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime scheduled = appointment.scheduledAt();
        OffsetDateTime windowStart = scheduled.minusMinutes(30);
        OffsetDateTime windowEnd = scheduled.plusHours(2);

        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException(
                    "La videollamada solo está disponible desde 30 minutos antes " +
                    "hasta 2 horas después de la cita agendada"
            );
        }

        // Obtener o crear el room ID (idempotente)
        String roomId = appointment.videoRoomId();
        if (roomId == null || roomId.isBlank()) {
            String randomPart = UUID.randomUUID().toString().replace("-", "");
            roomId = "kin-medical-" + randomPart;
            OffsetDateTime createdAt = OffsetDateTime.now();
            appointment = appointment.withVideoRoom(roomId, createdAt);
            appointmentRepository.save(appointment);
            log.info("Video room creado para cita {} por usuario {}", appointmentId, userId);
        }

        String videoUrl = "https://meet.jit.si/" + roomId;
        return ResponseEntity.ok(new VideoRoomResponse(videoUrl, roomId));
    }

    public record VideoRoomResponse(String videoUrl, String roomId) {}

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
                        .map(com.kinplatform.common.user.User::getFullName)
                        .orElse("Usuario");
                out.add(new ConversationResponse(
                        otherId, otherName, message.content(), message.createdAt(), unreadTotal));
            }
            out.sort(java.util.Comparator.comparing(ConversationResponse::lastMessageAt)
                    .reversed());
            return List.copyOf(out);
        }
    }
}


