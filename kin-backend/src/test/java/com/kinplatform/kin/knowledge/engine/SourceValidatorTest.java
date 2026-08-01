package com.kinplatform.kin.knowledge.engine;

import com.kinplatform.kin.knowledge.KnowledgeCandidate;
import com.kinplatform.kin.knowledge.SourceTrust;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceValidatorTest {

    private SourceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SourceValidator(
            Set.of("example.com"),
            Duration.ofDays(365),
            Set.of("application/json", "text/plain"));
    }

    private KnowledgeCandidate candidate(String url, OffsetDateTime publishedAt,
                                         String contentType, String content, Map<String, String> meta) {
        return new KnowledgeCandidate(content, "src-1", "Fuente", url, publishedAt,
            contentType, meta == null ? Map.of() : meta);
    }

    private KnowledgeCandidate validCandidate() {
        return candidate("https://example.com/report", OffsetDateTime.now().minusDays(30),
            "application/json", "Mercado retail en Colombia con crecimiento anual del 12%.", null);
    }

    @Test
    void validate_deberiaAceptarCandidatoValido() {
        var validation = validator.validate(validCandidate());

        assertTrue(validation.accepted());
        assertTrue(validation.reasons().isEmpty());
        assertEquals(SourceTrust.UNVERIFIED, validation.trust());
    }

    @Test
    void validate_deberiaDerivarConfianzaOficial() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido",
            Map.of(SourceValidator.META_SOURCE_TYPE, "official")));

        assertTrue(validation.accepted());
        assertEquals(SourceTrust.OFFICIAL_PUBLIC, validation.trust());
    }

    @Test
    void validate_deberiaDerivarConfianzaSecundaria() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "text/plain", "Contenido válido",
            Map.of(SourceValidator.META_SOURCE_TYPE, "secondary")));

        assertTrue(validation.accepted());
        assertEquals(SourceTrust.SECONDARY, validation.trust());
    }

    @Test
    void validate_deberiaDerivarNoVerificada_sinTipoDeFuente() {
        assertEquals(SourceTrust.UNVERIFIED, validator.validate(validCandidate()).trust());
    }

    @Test
    void validate_deberiaRechazarHttp() {
        var validation = validator.validate(candidate("http://example.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Protocolo HTTPS obligatorio"));
    }

    @Test
    void validate_deberiaRechazarDominioNoPermitido() {
        var validation = validator.validate(candidate("https://otro.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Dominio no permitido"));
    }

    @Test
    void validate_deberiaAceptarDominioPermitido_conMayusculas() {
        var validation = validator.validate(candidate("HTTPS://EXAMPLE.COM/report",
            OffsetDateTime.now(), "application/json", "Contenido válido", null));

        assertTrue(validation.accepted());
    }

    @Test
    void validate_deberiaRechazarUrlInvalida() {
        var validation = validator.validate(candidate("no es una url",
            OffsetDateTime.now(), "application/json", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("URL inválida"));
    }

    @Test
    void constructor_deberiaUsarDefaults_cuandoArgumentosNulos() {
        var lax = new SourceValidator(null, null, null);

        var validation = lax.validate(validCandidate());

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Dominio no permitido"));
    }

    @Test
    void validate_deberiaDerivarNoVerificada_paraTipoNoReconocido() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido",
            Map.of(SourceValidator.META_SOURCE_TYPE, "blog")));

        assertTrue(validation.accepted());
        assertEquals(SourceTrust.UNVERIFIED, validation.trust());
    }

    @Test
    void validate_deberiaRechazarEstadoHttpNo2xx() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido",
            Map.of(SourceValidator.META_HTTP_STATUS, "404")));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Estado HTTP no es 2xx"));
    }

    @Test
    void validate_deberiaAceptarEstadoHttp2xx() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido",
            Map.of(SourceValidator.META_HTTP_STATUS, "200")));

        assertTrue(validation.accepted());
    }

    @Test
    void validate_deberiaRechazarEstadoHttpInvalido() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json", "Contenido válido",
            Map.of(SourceValidator.META_HTTP_STATUS, "abc")));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Estado HTTP inválido"));
    }

    @Test
    void validate_deberiaAceptarSinEstadoHttp() {
        assertTrue(validator.validate(validCandidate()).accepted());
    }

    @Test
    void validate_deberiaRechazarTipoDeContenidoNoPermitido() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "text/html", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Tipo de contenido no permitido"));
    }

    @Test
    void validate_deberiaRechazarTipoDeContenidoAusente() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), " ", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Formato inválido: tipo de contenido ausente"));
    }

    @Test
    void validate_deberiaAceptarTipoDeContenidoConParametros() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json; charset=utf-8", "Contenido válido", null));

        assertTrue(validation.accepted());
    }

    @Test
    void validate_deberiaRechazarContenidoVacio() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now(), "application/json", "   ", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Formato inválido: contenido vacío"));
    }

    @Test
    void validate_deberiaRechazarSinFechaDePublicacion() {
        var validation = validator.validate(candidate("https://example.com/report",
            null, "application/json", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Publicado sin fecha (frescura no verificable)"));
    }

    @Test
    void validate_deberiaRechazarFueraDeLaVentanaDeFrescura() {
        var validation = validator.validate(candidate("https://example.com/report",
            OffsetDateTime.now().minusDays(400), "application/json", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Fuera de la ventana de frescura"));
    }

    @Test
    void validate_deberiaAceptarDentroDeLaVentanaDeFrescura() {
        assertTrue(validator.validate(validCandidate()).accepted());
    }

    @Test
    void validate_deberiaRechazarCandidatoNulo() {
        var validation = validator.validate(null);

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Candidato nulo"));
    }

    @Test
    void validate_deberiaAcumularTodosLosMotivos() {
        var validation = validator.validate(candidate("http://otro.com/report",
            OffsetDateTime.now(), "text/html", "Contenido válido", null));

        assertFalse(validation.accepted());
        assertTrue(validation.reasons().contains("Protocolo HTTPS obligatorio"));
        assertTrue(validation.reasons().contains("Dominio no permitido"));
        assertTrue(validation.reasons().contains("Tipo de contenido no permitido"));
    }

    @Test
    void validateAll_deberiaRechazarDuplicados() {
        var first = validCandidate();
        var duplicate = candidate("https://example.com/report", OffsetDateTime.now(),
            "application/json", "Otro contenido", null);

        var validations = validator.validateAll(List.of(first, duplicate));

        assertTrue(validations.get(0).accepted());
        assertFalse(validations.get(1).accepted());
        assertTrue(validations.get(1).reasons().contains("Fuente duplicada (sourceId, url)"));
    }

    @Test
    void validateAll_deberiaIgnorarDuplicadosEntreFuentes() {
        var v1 = validCandidate();
        var v2 = candidate("https://example.com/report", OffsetDateTime.now(),
            "application/json", "Otro contenido", null);

        var validations = validator.validateAll(List.of(v1, v2));

        assertEquals(2, validations.size());
        assertFalse(validations.get(1).accepted());
    }

    @Test
    void validateAll_deberiaSerReentrante() {
        var solo = validCandidate();
        assertEquals(1, validator.validateAll(List.of(solo)).size());
        assertEquals(1, validator.validateAll(List.of(solo)).size());
        assertTrue(validator.validateAll(List.of(solo)).get(0).accepted());
    }

    @Test
    void validateAll_deberiaTratarCandidatoNuloComoRechazado() {
        var validations = validator.validateAll(Arrays.asList(validCandidate(), null));

        assertEquals(2, validations.size());
        assertTrue(validations.get(0).accepted());
        assertFalse(validations.get(1).accepted());
    }

    @Test
    void validateAll_deberiaRetornarVacio_cuandoListaNula() {
        assertTrue(validator.validateAll(null).isEmpty());
    }
}
