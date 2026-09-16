package com.kinplatform.kin.health.triage.share.application;

import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.port.TriageConsultationRepository;
import com.kinplatform.kin.health.triage.share.api.SharedTriageContent;
import com.kinplatform.kin.health.triage.share.domain.TriageShareLink;
import com.kinplatform.kin.health.triage.share.port.TriageShareLinkRepository;
import com.kinplatform.pricing.ProductVertical;
import com.kinplatform.pricing.SubscriptionStatus;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del enlace temporal de compartición de triaje
 * (KIN Salud Personal+).
 *
 * <p>Genera/reutiliza un token UUID reutilizable durante 24h para que un
 * médico externo (sin cuenta) consulte el contenido de un triaje concreto,
 * permite revocarlo y expone el contenido público validando vigencia.</p>
 */
@Service
public class TriageShareService {

    private static final long DEFAULT_TTL_HOURS = 24;

    private final TriageShareLinkRepository shareRepository;
    private final TriageConsultationRepository consultationRepository;
    private final HealthQuotaPort healthQuotaPort;
    private final UserRepository userRepository;

    @Value("${medical.frontend.base-url:https://www.kin-platform-medical.com}")
    private String frontendBaseUrl;

    public TriageShareService(
            TriageShareLinkRepository shareRepository,
            TriageConsultationRepository consultationRepository,
            HealthQuotaPort healthQuotaPort,
            UserRepository userRepository) {
        this.shareRepository = shareRepository;
        this.consultationRepository = consultationRepository;
        this.healthQuotaPort = healthQuotaPort;
        this.userRepository = userRepository;
    }

    /**
     * Crea (o reutiliza) el enlace de un triaje del paciente autenticado.
     *
     * @throws IllegalArgumentException si el triaje no pertenece al paciente (404 en controller)
     * @throws QuotaExceededException   si el paciente no tiene plan Personal+ (feature de pago)
     */
    @Transactional
    public TriageShareLink createShare(UUID userId, UUID triageId) {
        TriageConsultation consultation = loadOwnedConsultation(triageId, userId);
        requirePersonalPlus(userId);

        OffsetDateTime now = OffsetDateTime.now();
        // Reutilizar enlace vigente del triaje si existe y no ha expirado.
        TriageShareLink existing = shareRepository.findActiveByTriageId(triageId).orElse(null);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            return existing;
        }

        TriageShareLink link = TriageShareLink.of(
                UUID.randomUUID(),
                consultation.id(),
                userId,
                UUID.randomUUID().toString(),
                now.plusHours(DEFAULT_TTL_HOURS),
                now,
                null,
                userId);
        return shareRepository.save(link);
    }

    /**
     * Revoca el enlace vigente de un triaje del paciente (si existe). No-op
     * si no hay enlace activo.
     */
    @Transactional
    public void revokeShare(UUID userId, UUID triageId) {
        loadOwnedConsultation(triageId, userId);
        TriageShareLink existing = shareRepository.findActiveByTriageId(triageId).orElse(null);
        if (existing != null) {
            TriageShareLink revoked = TriageShareLink.of(
                    existing.id(),
                    existing.triageId(),
                    existing.patientId(),
                    existing.token(),
                    existing.expiresAt(),
                    existing.createdAt(),
                    OffsetDateTime.now(),
                    existing.createdBy());
            shareRepository.save(revoked);
        }
    }

    /**
     * Resuelve el contenido público de un token. Devuelve {@code null} si el
     * token no existe, expiró o fue revocado (el controller responde 404
     * genérico para no confirmar existencia).
     */
    @Transactional(readOnly = true)
    public SharedTriageContent resolvePublicContent(String token) {
        TriageShareLink link = shareRepository.findByToken(token).orElse(null);
        if (link == null || !link.isActive(OffsetDateTime.now())) {
            return null;
        }
        TriageConsultation consultation = consultationRepository
                .findByIdAndUserId(link.triageId(), link.patientId())
                .orElse(null);
        if (consultation == null) {
            return null;
        }
        return SharedTriageContent.from(consultation, patientName(link.patientId()));
    }

    private TriageConsultation loadOwnedConsultation(UUID triageId, UUID userId) {
        return consultationRepository
                .findByIdAndUserId(triageId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Triaje no encontrado o acceso denegado: " + triageId));
    }

    private void requirePersonalPlus(UUID userId) {
        boolean eligible = healthQuotaPort.hasEligibleSubscription(
                userId, ProductVertical.SALUD_PERSONAL, SubscriptionStatus.ACTIVE);
        if (!eligible) {
            throw new QuotaExceededException(
                    "Compartir un informe de triaje requiere el plan Personal+ ($9/mes).",
                    "QUOTA_EXCEEDED",
                    "/dashboard/patient/plans");
        }
    }

    private String patientName(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getFullName() : "Paciente";
    }
}
