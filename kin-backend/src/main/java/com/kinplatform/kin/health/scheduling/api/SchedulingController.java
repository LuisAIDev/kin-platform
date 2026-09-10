package com.kinplatform.kin.health.scheduling.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.scheduling.domain.PhysicianAvailability;
import com.kinplatform.kin.health.telemedicine.domain.Appointment;
import com.kinplatform.kin.health.telemedicine.domain.Appointment.AppointmentStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de la agenda y disponibilidad de médicos (ADR-034).
 *
 * <p>Médico: configura disponibilidad, ve/elimina horarios, consulta slots de un
 * paciente, confirma y completa citas. Paciente: ve slots de su médico, solicita,
 * cancela o reprograma citas. Ambos: citas próximas e historial. Toda operación
 * sobre un paciente exige relación {@code ACTIVE} (Área 5).</p>
 */
@RestController
@RequestMapping({
    "/health/scheduling",
    "/medical/scheduling"
})
public class SchedulingController {

    private static final Logger log = LoggerFactory.getLogger(SchedulingController.class);

    private final SchedulingService schedulingService;
    private final UserRepository userRepository;

    public SchedulingController(SchedulingService schedulingService, UserRepository userRepository) {
        this.schedulingService = schedulingService;
        this.userRepository = userRepository;
    }

    // ---------- Médico: disponibilidad ----------

    @PostMapping("/availability")
    public ResponseEntity<AvailabilityResponse> setAvailability(
            Authentication authentication, @Valid @RequestBody AvailabilityRequest request) {
        User user = requirePhysician(authentication);
        PhysicianAvailability availability = schedulingService.setAvailability(
                user.getId(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.slotDurationMinutes(),
                request.active() == null || request.active());
        log.info("=== SCHEDULING AVAILABILITY === physician={}, day={}", user.getId(), request.dayOfWeek());
        return ResponseEntity.ok(AvailabilityResponse.from(availability));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(Authentication authentication) {
        User user = requirePhysician(authentication);
        List<AvailabilityResponse> list =
                schedulingService.getAvailability(user.getId()).stream()
                        .map(AvailabilityResponse::from)
                        .toList();
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/availability/{availabilityId}")
    public ResponseEntity<Void> removeAvailability(
            Authentication authentication, @PathVariable UUID availabilityId) {
        User user = requirePhysician(authentication);
        schedulingService.removeAvailability(user.getId(), availabilityId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Slots ----------

    @GetMapping("/patients/{patientId}/slots")
    public ResponseEntity<List<OffsetDateTime>> slotsForPatient(
            Authentication authentication,
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = requirePhysician(authentication);
        return ResponseEntity.ok(schedulingService.getAvailableSlots(user.getId(), date));
    }

    @GetMapping("/physicians/{physicianId}/slots")
    public ResponseEntity<List<OffsetDateTime>> slotsForPhysician(
            Authentication authentication,
            @PathVariable UUID physicianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(schedulingService.getAvailableSlotsForPatient(patientId, physicianId, date));
    }

    // ---------- Citas ----------

    @PostMapping("/appointments/request")
    public ResponseEntity<SchedulingAppointmentResponse> requestAppointment(
            Authentication authentication, @Valid @RequestBody RequestAppointmentRequest request) {
        UUID patientId = AuthenticatedUsers.require(userRepository, authentication).getId();
        Appointment appointment = schedulingService.requestAppointment(
                patientId, request.physicianId(), request.scheduledAt(), request.durationMinutes(), request.reason());
        log.info("=== SCHEDULING REQUEST === patient={}, physician={}", patientId, request.physicianId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SchedulingAppointmentResponse.from(appointment));
    }

    @PutMapping("/appointments/{appointmentId}/confirm")
    public ResponseEntity<SchedulingAppointmentResponse> confirmAppointment(
            Authentication authentication, @PathVariable UUID appointmentId) {
        User user = requirePhysician(authentication);
        return ResponseEntity.ok(SchedulingAppointmentResponse.from(
                schedulingService.confirmAppointment(appointmentId, user.getId())));
    }

    @PutMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<SchedulingAppointmentResponse> cancelAppointment(
            Authentication authentication,
            @PathVariable UUID appointmentId,
            @RequestBody(required = false) CancelAppointmentRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(SchedulingAppointmentResponse.from(
                schedulingService.cancelAppointment(appointmentId, userId, reason)));
    }

    @PutMapping("/appointments/{appointmentId}/reschedule")
    public ResponseEntity<SchedulingAppointmentResponse> rescheduleAppointment(
            Authentication authentication,
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        UUID userId = AuthenticatedUsers.require(userRepository, authentication).getId();
        return ResponseEntity.ok(SchedulingAppointmentResponse.from(schedulingService.rescheduleAppointment(
                appointmentId, userId, request.newScheduledAt(), request.reason())));
    }

    @PutMapping("/appointments/{appointmentId}/complete")
    public ResponseEntity<SchedulingAppointmentResponse> completeAppointment(
            Authentication authentication, @PathVariable UUID appointmentId) {
        User user = requirePhysician(authentication);
        return ResponseEntity.ok(SchedulingAppointmentResponse.from(
                schedulingService.completeAppointment(appointmentId, user.getId())));
    }

    // ---------- Ambos ----------

    @GetMapping("/appointments/upcoming")
    public ResponseEntity<List<SchedulingAppointmentResponse>> upcomingAppointments(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        boolean asPhysician = user.getRole() == UserRole.PHYSICIAN;
        List<SchedulingAppointmentResponse> list = schedulingService.upcomingAppointments(user.getId(), asPhysician)
                .stream()
                .map(SchedulingAppointmentResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/appointments/history")
    public ResponseEntity<List<SchedulingAppointmentResponse>> appointmentHistory(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        boolean asPhysician = user.getRole() == UserRole.PHYSICIAN;
        List<SchedulingAppointmentResponse> list = schedulingService.appointmentHistory(user.getId(), asPhysician)
                .stream()
                .map(SchedulingAppointmentResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    // ---------- Helpers ----------

    private User requirePhysician(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (user.getRole() != UserRole.PHYSICIAN && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Se requiere rol de médico para esta operación");
        }
        return user;
    }

    public record AvailabilityRequest(
            @NotNull(message = "dayOfWeek es obligatorio") DayOfWeek dayOfWeek,
            @NotNull(message = "startTime es obligatorio") LocalTime startTime,
            @NotNull(message = "endTime es obligatorio") LocalTime endTime,
            @NotNull(message = "slotDurationMinutes es obligatorio") Integer slotDurationMinutes,
            Boolean active) {}

    public record RequestAppointmentRequest(
            @NotNull(message = "physicianId es obligatorio") UUID physicianId,
            @NotNull(message = "scheduledAt es obligatorio") OffsetDateTime scheduledAt,
            Integer durationMinutes,
            String reason) {}

    public record CancelAppointmentRequest(String reason) {}

    public record RescheduleAppointmentRequest(
            @NotNull(message = "newScheduledAt es obligatorio") OffsetDateTime newScheduledAt,
            String reason) {}

    public record AvailabilityResponse(
            UUID id,
            UUID physicianId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            boolean active) {
        static AvailabilityResponse from(PhysicianAvailability a) {
            return new AvailabilityResponse(
                    a.id(), a.physicianId(), a.dayOfWeek(), a.startTime(), a.endTime(),
                    a.slotDurationMinutes(), a.isActive());
        }
    }

    public record SchedulingAppointmentResponse(
            UUID id,
            UUID patientId,
            UUID physicianId,
            OffsetDateTime scheduledAt,
            int durationMinutes,
            String reason,
            AppointmentStatus status,
            OffsetDateTime createdAt,
            UUID rescheduledFrom,
            String cancellationReason,
            UUID availabilitySlotId) {
        static SchedulingAppointmentResponse from(Appointment a) {
            return new SchedulingAppointmentResponse(
                    a.id(),
                    a.patientId(),
                    a.physicianId(),
                    a.scheduledAt(),
                    a.durationMinutes(),
                    a.reason(),
                    a.status(),
                    a.createdAt(),
                    a.rescheduledFrom(),
                    a.cancellationReason(),
                    a.availabilitySlotId());
        }
    }
}
