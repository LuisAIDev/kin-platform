package com.kinplatform.kin.health.physician.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de preferencias del médico (notificaciones WhatsApp, etc.).
 *
 * <p>Protegido por JWT con rol {@code PHYSICIAN}. Permite al médico
 * configurar si quiere recibir avisos por WhatsApp cuando un paciente
 * le envía un mensaje.</p>
 */
@RestController
@RequestMapping({
    "/api/v1/physician/preferences",
    "/health/physician/preferences"
})
@RequiredArgsConstructor
public class PhysicianPreferencesController {

    private static final Logger log = LoggerFactory.getLogger(PhysicianPreferencesController.class);

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<PhysicianPreferencesResponse> getPreferences(Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (!com.kinplatform.common.security.PhysicianAccess.isPhysician(user)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(new PhysicianPreferencesResponse(
                Boolean.TRUE.equals(user.getWhatsappNotificationsEnabled()),
                user.getPhone()
        ));
    }

    @PutMapping
    public ResponseEntity<PhysicianPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdatePhysicianPreferencesRequest request,
            Authentication authentication) {
        User user = AuthenticatedUsers.require(userRepository, authentication);
        if (!com.kinplatform.common.security.PhysicianAccess.isPhysician(user)) {
            return ResponseEntity.status(403).build();
        }

        // Validar teléfono si se activa WhatsApp
        if (Boolean.TRUE.equals(request.whatsappNotificationsEnabled())) {
            if (request.phone() == null || request.phone().isBlank()) {
                throw new IllegalArgumentException("Debes proporcionar un número de teléfono para activar WhatsApp");
            }
            // Validación básica de formato internacional
            String cleaned = request.phone().replaceAll("[^0-9+]", "");
            if (!cleaned.startsWith("+") || cleaned.length() < 10 || cleaned.length() > 16) {
                throw new IllegalArgumentException("Formato de teléfono inválido. Usa formato internacional (ej. +573186197995)");
            }
            user.setPhone(cleaned);
        }

        user.setWhatsappNotificationsEnabled(request.whatsappNotificationsEnabled());
        userRepository.save(user);

        log.info("PhysicianPreferencesController: preferencias actualizadas para médico {} - whatsappEnabled={}", 
                user.getId(), request.whatsappNotificationsEnabled());

        return ResponseEntity.ok(new PhysicianPreferencesResponse(
                user.getWhatsappNotificationsEnabled(),
                user.getPhone()
        ));
    }

    public record PhysicianPreferencesResponse(
            boolean whatsappNotificationsEnabled,
            String phone
    ) {}

    public record UpdatePhysicianPreferencesRequest(
            boolean whatsappNotificationsEnabled,
            @Pattern(regexp = "^\\+?[0-9\\s-]{8,}$", message = "Formato de teléfono inválido")
            @NotBlank(message = "El teléfono es obligatorio si se activa WhatsApp")
            String phone
    ) {}
}