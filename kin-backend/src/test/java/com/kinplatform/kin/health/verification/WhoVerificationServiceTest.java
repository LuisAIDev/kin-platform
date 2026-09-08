package com.kinplatform.kin.health.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kinplatform.kin.health.verification.domain.ClinicalVerification;
import com.kinplatform.kin.health.verification.domain.DiagnosticMatch;
import com.kinplatform.kin.health.verification.domain.GlobalStatistic;
import com.kinplatform.kin.health.verification.domain.VerificationStatus;
import com.kinplatform.kin.health.verification.port.WhoHealthDataSourceException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Tests de la fachada de verificación clínica OMS (ADR-041) con adaptadores
 * falsos (sin red). Cubre el degradado por falta de credenciales, por error de
 * la API y el flujo verificado.
 */
class WhoVerificationServiceTest {

    private static final String DOC_TEXT =
            "Examen de laboratorio\nGlucosa en ayunas 110 mg/dL\nColesterol total 190 mg/dL\n";

    private WhoVerificationProperties properties() {
        WhoVerificationProperties props = new WhoVerificationProperties();
        props.setEnabled(true);
        props.getIcd().setEnabled(true);
        props.getIcd().setClientId("cid");
        props.getIcd().setClientSecret("secret");
        props.getIcd().setMaxQueriesPerTurn(1);
        return props;
    }

    @Test
    void sinCredenciales_devuelveUnconfigured() {
        WhoVerificationProperties props = properties();
        props.getIcd().setClientId("");
        props.getIcd().setClientSecret("");
        WhoVerificationService service = new WhoVerificationService(
                props, term -> List.of(), (code, country) -> List.of());

        ClinicalVerification result = service.verifyDocumentContext(DOC_TEXT, "¿Qué significa?");

        assertEquals(VerificationStatus.UNCONFIGURED, result.status());
        assertTrue(result.diagnosticMatches().isEmpty());
        assertTrue(result.message().contains("WHO_ICD_CLIENT_ID"));
    }

    @Test
    void conMasterSwitchApagado_devuelveUnverified() {
        WhoVerificationProperties props = properties();
        props.setEnabled(false);
        WhoVerificationService service = new WhoVerificationService(
                props, term -> List.of(), (code, country) -> List.of());

        ClinicalVerification result = service.verifyDocumentContext(DOC_TEXT, "¿Qué significa?");

        assertEquals(VerificationStatus.UNVERIFIED, result.status());
    }

    @Test
    void conCoincidencias_devuelveVerified() {
        WhoVerificationService service = new WhoVerificationService(
                properties(),
                term -> List.of(new DiagnosticMatch("5B75", "Diabetes mellitus", term, "2024-01")),
                (code, country) -> List.of());

        ClinicalVerification result = service.verifyDocumentContext(DOC_TEXT, "tengo glucosa alta");

        assertEquals(VerificationStatus.VERIFIED, result.status());
        assertEquals(1, result.diagnosticMatches().size());
        assertEquals("5B75", result.diagnosticMatches().get(0).code());
        assertTrue(result.message().contains("verificados"));
    }

    @Test
    void cuandoLaApiFalla_devuelveUnavailableYNoRompe() {
        WhoVerificationProperties props = properties();
        props.setFailureThreshold(3);
        WhoVerificationService service = new WhoVerificationService(
                props,
                term -> {
                    throw new WhoHealthDataSourceException("timeout", null);
                },
                (code, country) -> List.of());

        ClinicalVerification result = service.verifyDocumentContext(DOC_TEXT, "¿Qué es?");

        assertEquals(VerificationStatus.UNAVAILABLE, result.status());
        assertTrue(result.diagnosticMatches().isEmpty());
    }

    @Test
    void conBreakerAbierto_noConsultaLaApi() {
        WhoVerificationProperties props = properties();
        props.setFailureThreshold(1);
        props.setOpenTimeoutMillis(30_000);
        AtomicInteger calls = new AtomicInteger();
        WhoVerificationService service = new WhoVerificationService(
                props,
                term -> {
                    calls.incrementAndGet();
                    throw new WhoHealthDataSourceException("boom", null);
                },
                (code, country) -> List.of());
        service.verifyDocumentContext(DOC_TEXT, "una");
        service.verifyDocumentContext(DOC_TEXT, "dos");
        assertEquals(VerificationStatus.UNAVAILABLE,
                service.verifyDocumentContext(DOC_TEXT, "tres").status());
        assertEquals(1, calls.get(), "con el breaker abierto no se debe llamar a la API");
    }

    @Test
    void promptSection_incluyeSoloDatosVerificados() {
        WhoVerificationService service = new WhoVerificationService(
                properties(),
                term -> List.of(new DiagnosticMatch("5B75", "Diabetes", term, "2024-01")),
                (code, country) -> List.of(new GlobalStatistic("NCD", "diabetes", "COL", "10.5", "2021")));
        ClinicalVerification result = service.verifyDocumentContext(DOC_TEXT, "azúcar");

        String section = service.promptSection(result);

        assertTrue(section.contains("5B75"));
        assertTrue(section.contains("VERIFICACIÓN CLÍNICA"));
    }

    @Test
    void deriveSearchTerms_extraeHallazgosDelDocumento() {
        List<String> terms = WhoVerificationService.deriveSearchTerms(DOC_TEXT, "", 2);
        assertTrue(terms.stream().anyMatch(t -> t.toLowerCase().contains("glucosa")));
    }
}
