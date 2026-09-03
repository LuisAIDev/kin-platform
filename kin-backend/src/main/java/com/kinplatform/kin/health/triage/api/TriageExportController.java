package com.kinplatform.kin.health.triage.api;

import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.triage.application.TriageExportAssembler;
import com.kinplatform.kin.health.triage.application.TriageExportDocument;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health/triage")
public class TriageExportController {

    private static final Logger log = LoggerFactory.getLogger(TriageExportController.class);

    private final TriageConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final TriageExportAssembler exportAssembler;
    private final HealthQuotaPort healthQuotaPort;

    public TriageExportController(
            TriageConsultationRepository consultationRepository,
            UserRepository userRepository,
            TriageExportAssembler exportAssembler,
            HealthQuotaPort healthQuotaPort) {
        this.consultationRepository = consultationRepository;
        this.userRepository = userRepository;
        this.exportAssembler = exportAssembler;
        this.healthQuotaPort = healthQuotaPort;
    }

    @GetMapping("/{triageId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable @NotNull UUID triageId,
            Principal principal) {

        UUID userId = extractUserId(principal);
        TriageConsultation consultation = loadTriageConsultation(triageId, userId);

        // La exportación PDF es una funcionalidad del plan de pago Personal+
        // (pdf_export = FALSE en el plan gratuito). Un paciente en plan FREE se
        // bloquea aunque todavía tenga triajes gratuitos disponibles este mes.
        boolean eligible = healthQuotaPort.hasEligibleSubscription(
                userId, ProductVertical.SALUD_PERSONAL,
                SubscriptionStatus.ACTIVE);
        if (!eligible) {
            throw new QuotaExceededException(
                    "La exportación PDF del informe de triaje requiere el plan Personal+ ($9/mes).",
                    "QUOTA_EXCEEDED",
                    "/dashboard/patient/plans");
        }

        TriageExportDocument doc = buildExportDocument(triageId, userId, consultation);

        byte[] pdfBytes = exportAssembler.toPdf(doc);

        String filename = "informe-triaje-" + triageId + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private UUID extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Principal nulo");
        }
        String name = principal.getName();
        var user = userRepository.findByEmail(name).orElseThrow(
                () -> new IllegalArgumentException("Usuario no encontrado: " + name)
        );
        return user.getId();
    }

    private TriageConsultation loadTriageConsultation(UUID triageId, UUID userId) {
        return consultationRepository.findByIdAndUserId(triageId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Triaje no encontrado o acceso denegado: " + triageId));
    }

    private TriageExportDocument buildExportDocument(
            UUID triageId,
            UUID userId,
            TriageConsultation consultation) {

        String symptoms = formatSymptoms(consultation.symptoms());
        String results = formatResults(consultation.results());
        String recommendation = formatRecommendation(consultation.results());

        return TriageExportDocument.of(
                triageId,
                userId,
                getPatientName(userId),
                consultation.createdAt(),
                symptoms,
                results,
                recommendation
        );
    }

    private String getPatientName(UUID userId) {
        var user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getFullName() : "Paciente";
    }

    private String formatSymptoms(List<String> symptoms) {
        if (symptoms == null || symptoms.isEmpty()) {
            return "No reportó síntomas";
        }
        return String.join(", ", symptoms);
    }

    private String formatResults(List<TriageConditionResult> results) {
        if (results == null || results.isEmpty()) {
            return "No se identificaron condiciones candidatas";
        }
        var sb = new StringBuilder();
        for (var r : results) {
            sb.append(" - ")
                    .append(r.name())
                    .append(": probabilidad ")
                    .append(String.format("%.0f", r.probability() * 100))
                    .append("%, ")
                    .append("urgencia ")
                    .append(r.urgency().name())
                    .append("\n");
        }
        return sb.toString();
    }

    private String formatRecommendation(List<TriageConditionResult> results) {
        if (results == null || results.isEmpty()) {
            return "No hay recomendación disponible";
        }
        // Usar la condición de mayor probabilidad como base de la recomendación
        var top = results.get(0);
        return top.description() != null && !top.description().isEmpty()
                ? top.description()
                : "Consulta de seguimiento según resultado del triaje";
    }
}