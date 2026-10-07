package com.kinplatform.kin.health.triage.share.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kinplatform.kin.health.common.exception.QuotaExceededException;
import com.kinplatform.kin.health.subscription.port.HealthQuotaPort;
import com.kinplatform.kin.health.triage.InMemoryTriageConsultationRepository;
import com.kinplatform.kin.health.triage.domain.Severity;
import com.kinplatform.kin.health.triage.domain.TriageConditionResult;
import com.kinplatform.kin.health.triage.domain.TriageConsultation;
import com.kinplatform.kin.health.triage.domain.Urgency;
import com.kinplatform.kin.health.triage.share.InMemoryTriageShareLinkRepository;
import com.kinplatform.common.pricing.ProductVertical;
import com.kinplatform.common.pricing.SubscriptionStatus;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TriageShareServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID TRIAGE = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // No stub global: cada test configura userRepository.findById solo cuando
        // el flujo lo necesita (patientName en resolvePublicContent).
    }

    private static HealthQuotaPort quota(boolean eligible) {
        return new HealthQuotaPort() {
            @Override
            public Integer getMaxTriagesPerMonth(UUID userId) {
                return null;
            }

            @Override
            public Integer getMaxPatients(UUID physicianId) {
                return null;
            }

            @Override
            public Integer getTrialDays(UUID userId) {
                return null;
            }

            @Override
            public Integer getTriagesUsed(UUID userId) {
                return 0;
            }

            @Override
            public boolean hasEligibleSubscription(
                    UUID userId, ProductVertical vertical, SubscriptionStatus... statuses) {
                return eligible;
            }
        };
    }

    private static InMemoryTriageConsultationRepository consultations() {
        var repo = new InMemoryTriageConsultationRepository();
        repo.save(TriageConsultation.of(
                TRIAGE,
                PATIENT,
                List.of("fiebre"),
                List.of(new TriageConditionResult(
                        UUID.randomUUID(),
                        "Gripe",
                        "Infección viral",
                        0.8,
                        Severity.MODERADO,
                        Urgency.MEDIA,
                        "Consulta médica.",
                        List.of("fiebre"))),
                OffsetDateTime.now()));
        return repo;
    }

    private TriageShareService service(boolean eligible) {
        return new TriageShareService(
                new InMemoryTriageShareLinkRepository(),
                consultations(),
                quota(eligible),
                userRepository);
    }

    @Test
    void createShare_pacienteConPersonalPlus_generaTokenYExpiraEn24h() {
        TriageShareService service = service(true);
        var link = service.createShare(PATIENT, TRIAGE);

        assertNotNull(link.token());
        assertEquals(PATIENT, link.patientId());
        assertEquals(TRIAGE, link.triageId());
        assertNull(link.revokedAt());
        assertTrue(link.expiresAt().isAfter(OffsetDateTime.now()));
        assertTrue(link.expiresAt().isBefore(OffsetDateTime.now().plusHours(25)));
    }

    @Test
    void createShare_pacienteSinPersonalPlus_lanzaQuotaExceeded() {
        TriageShareService service = service(false);
        assertThrows(QuotaExceededException.class, () -> service.createShare(PATIENT, TRIAGE));
    }

    @Test
    void createShare_reutilizaEnlaceVigenteDelMismoTriaje() {
        TriageShareService service = service(true);
        var first = service.createShare(PATIENT, TRIAGE);
        var second = service.createShare(PATIENT, TRIAGE);

        assertEquals(first.id(), second.id());
        assertEquals(first.token(), second.token());
    }

    @Test
    void createShare_triajeDeOtroPaciente_lanzaIllegalArgument() {
        TriageShareService service = service(true);
        UUID other = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.createShare(other, TRIAGE));
    }

    @Test
    void revokeShare_marcaRevocado() {
        TriageShareService service = service(true);
        var link = service.createShare(PATIENT, TRIAGE);
        service.revokeShare(PATIENT, TRIAGE);

        var after = service.resolvePublicContent(link.token());
        assertNull(after);
    }

    @Test
    void resolvePublicContent_tokenValido_devuelveContenidoMinimo() {
        when(userRepository.findById(PATIENT))
                .thenReturn(Optional.of(User.builder().id(PATIENT).fullName("Ana Pérez").role(UserRole.PATIENT).build()));
        TriageShareService service = service(true);
        var link = service.createShare(PATIENT, TRIAGE);

        var content = service.resolvePublicContent(link.token());
        assertNotNull(content);
        assertEquals("Ana Pérez", content.patientName());
        assertEquals(List.of("fiebre"), content.symptoms());
        assertEquals(1, content.conditions().size());
        assertEquals("Gripe", content.conditions().get(0).name());
    }

    @Test
    void resolvePublicContent_tokenInexistente_devuelveNull() {
        TriageShareService service = service(true);
        assertNull(service.resolvePublicContent("no-existe"));
    }
}

