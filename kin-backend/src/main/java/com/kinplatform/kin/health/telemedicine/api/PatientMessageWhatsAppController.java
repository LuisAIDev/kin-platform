package com.kinplatform.kin.health.telemedicine.api;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.kin.health.telemedicine.domain.Message;
import com.kinplatform.kin.health.telemedicine.port.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping({"/patient/messages", "/medical/patient/messages"})
@Slf4j
public class PatientMessageWhatsAppController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public PatientMessageWhatsAppController(
            UserRepository userRepository,
            MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/{messageId}/whatsapp-link")
    public ResponseEntity<WhatsAppLinkResponse> generateWhatsAppLink(
            @PathVariable UUID messageId,
            Authentication auth) {

        User patient = AuthenticatedUsers.require(userRepository, auth);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

        // Validar que el paciente es el emisor
        if (!message.senderId().equals(patient.getId())) {
            throw new RuntimeException("No puedes generar avisos de mensajes que no enviaste");
        }

        User physician = userRepository.findById(message.receiverId())
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        // Validar opt-in del médico
        if (!Boolean.TRUE.equals(physician.getWhatsappNotificationsEnabled())) {
            throw new IllegalStateException("El médico no tiene activados los avisos por WhatsApp");
        }
        if (physician.getPhone() == null || physician.getPhone().isBlank()) {
            throw new IllegalStateException("El médico no tiene un número de WhatsApp configurado");
        }

        // Mensaje genérico (SIN datos clínicos)
        String genericMessage = String.format(
            "Hola%s, soy %s. Te escribí por KIN Medical y me gustaría que revises mi mensaje cuando puedas. " +
            "Por seguridad no incluyo el contenido aquí. Podrás leerlo completo en: %s",
            physician.getFullName() != null ? " " + physician.getFullName().split(" ")[0] : "",
            patient.getFullName() != null ? patient.getFullName() : "un paciente",
            "https://www.kin-platform-medical.com/dashboard/physician/messages"
        );

        String phoneDigits = physician.getPhone().replaceAll("[^0-9]", "");
        String whatsappUrl = "https://wa.me/" + phoneDigits + "?text=" +
                URLEncoder.encode(genericMessage, StandardCharsets.UTF_8);

        log.info("WhatsApp link generated for message {} by patient {}", messageId, patient.getId());

        return ResponseEntity.ok(new WhatsAppLinkResponse(whatsappUrl));
    }

    public record WhatsAppLinkResponse(String whatsappUrl) {}
}
