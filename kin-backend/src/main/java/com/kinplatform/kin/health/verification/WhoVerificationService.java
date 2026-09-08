package com.kinplatform.kin.health.verification;

import com.kinplatform.kin.health.verification.domain.ClinicalVerification;
import com.kinplatform.kin.health.verification.domain.DiagnosticMatch;
import com.kinplatform.kin.health.verification.domain.GlobalStatistic;
import com.kinplatform.kin.health.verification.domain.VerificationStatus;
import com.kinplatform.kin.health.verification.port.GlobalHealthStatProvider;
import com.kinplatform.kin.health.verification.port.IcdDiagnosisLookup;
import com.kinplatform.kin.health.verification.port.WhoHealthDataSourceException;
import com.kinplatform.kin.health.verification.resilience.CircuitBreaker;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fachada de verificación clínica con la OMS (ADR-041).
 *
 * <p>Antes de que la IA genere una respuesta sobre un documento clínico, este
 * servicio consulta la ICD-API para mapear los términos/hallazgos a códigos de
 * diagnóstico oficiales y (si hay indicadores configurados) el GHO para dar
 * contexto estadístico. La llamada a cada fuente externa está protegida por un
 * {@link CircuitBreaker}: si la OMS no responde, el flujo degrada con un
 * {@link ClinicalVerification#status()} {@code UNAVAILABLE} y la IA queda
 * instruida para no inventar datos. Este servicio NUNCA lanza excepciones.</p>
 *
 * <p>Unidades típicas detectadas en los hallazgos del documento (mg/dL, mmHg…)
 * se usan como términos de búsqueda junto con la pregunta del paciente, con un
 * tope de consultas por turno.</p>
 */
@Service
public class WhoVerificationService {

    private static final Logger log = LoggerFactory.getLogger(WhoVerificationService.class);

    /** Expresión para detectar líneas de hallazgo con valor + unidad en el PDF. */
    private static final Pattern FINDING_PATTERN = Pattern.compile(
            "([A-Za-zÁÉÍÓÚáéíóúñÑ][A-Za-zÁÉÍÓÚáéíóúñÑ .,/()%-]{2,60}?)\\s*\\d+(?:[.,]\\d+)?\\s*"
                    + "(mg/dL|mg/dl|g/dL|mmol/L|mmol/l|mm ?Hg|mmHg|%|U/L|u/L|UI/L|pg/mL|ng/mL|µg/mL|ug/mL|cells/µL)");

    private final WhoVerificationProperties properties;
    private final IcdDiagnosisLookup icdLookup;
    private final GlobalHealthStatProvider ghoProvider;
    private final CircuitBreaker icdBreaker;
    private final CircuitBreaker ghoBreaker;

    public WhoVerificationService(
            WhoVerificationProperties properties,
            IcdDiagnosisLookup icdLookup,
            GlobalHealthStatProvider ghoProvider) {
        this(properties, icdLookup, ghoProvider, null, null);
    }

    public WhoVerificationService(
            WhoVerificationProperties properties,
            IcdDiagnosisLookup icdLookup,
            GlobalHealthStatProvider ghoProvider,
            CircuitBreaker icdBreaker,
            CircuitBreaker ghoBreaker) {
        this.properties = properties;
        this.icdLookup = icdLookup;
        this.ghoProvider = ghoProvider;
        this.icdBreaker = icdBreaker != null
                ? icdBreaker
                : new CircuitBreaker("who-icd", properties.getFailureThreshold(),
                        properties.getOpenTimeoutMillis());
        this.ghoBreaker = ghoBreaker != null
                ? ghoBreaker
                : new CircuitBreaker("who-gho", properties.getFailureThreshold(),
                        properties.getOpenTimeoutMillis());
    }

    /**
     * Verifica el contexto de un turno de análisis contra la ICD-API (y el GHO
     * si hay indicadores configurados).
     *
     * @param documentText texto extraído del documento (puede ser vacío).
     * @param userMessage mensaje del paciente en el chat.
     */
    public ClinicalVerification verifyDocumentContext(String documentText, String userMessage) {
        if (!properties.isEnabled()) {
            return new ClinicalVerification(
                    VerificationStatus.UNVERIFIED, List.of(), List.of(),
                    "La verificación clínica con fuentes OMS está deshabilitada en esta instalación.");
        }
        WhoVerificationProperties.Icd icd = properties.getIcd();
        if (!icd.isEnabled() || isBlank(icd.getClientId()) || isBlank(icd.getClientSecret())) {
            return new ClinicalVerification(
                    VerificationStatus.UNCONFIGURED, List.of(), List.of(),
                    "La ICD-API de la OMS no está configurada (faltan WHO_ICD_CLIENT_ID/WHO_ICD_CLIENT_SECRET). "
                            + "La IA no puede verificar códigos de diagnóstico oficiales.");
        }

        List<String> terms = deriveSearchTerms(documentText, userMessage, icd.getMaxQueriesPerTurn());
        List<DiagnosticMatch> matches = new ArrayList<>();
        VerificationStatus status = VerificationStatus.UNVERIFIED;
        String message = "No se encontraron coincidencias de diagnóstico verificables para esta consulta.";
        if (terms.isEmpty()) {
            message = "No se pudo extraer ningún término clínico para verificar contra la ICD-API.";
        } else {
            boolean interrupted = false;
            for (String term : terms) {
                if (icdBreaker.isOpen()) {
                    status = VerificationStatus.UNAVAILABLE;
                    message = "La ICD-API de la OMS no está disponible temporalmente "
                            + "(demasiados errores consecutivos). La IA debe indicarlo y no inventar datos.";
                    interrupted = true;
                    break;
                }
                try {
                    List<DiagnosticMatch> found = icdLookup.search(term);
                    icdBreaker.recordSuccess();
                    matches.addAll(found);
                } catch (WhoHealthDataSourceException e) {
                    icdBreaker.recordFailure();
                    log.warn("WhoVerificationService: fallo ICD-API ({}): {}", icdBreaker.state(), e.getMessage());
                    status = VerificationStatus.UNAVAILABLE;
                    message = "No se pudo verificar contra la ICD-API de la OMS en este momento. "
                            + "La IA no debe inventar códigos de diagnóstico ni cifras oficiales.";
                    interrupted = true;
                    break;
                }
            }
            if (!interrupted && !matches.isEmpty()) {
                status = VerificationStatus.VERIFIED;
                message = "Códigos de diagnóstico verificados contra la ICD-API de la OMS.";
            }
        }

        List<GlobalStatistic> statistics = fetchGlobalStatistics();
        ClinicalVerification verification = new ClinicalVerification(status, limit(matches), statistics, message);
        log.info("WhoVerificationService: estado de verificación {} ({} coincidencias, {} estadísticas)",
                verification.status(), verification.diagnosticMatches().size(), verification.globalStatistics().size());
        return verification;
    }

    /** Sección de contexto para el prompt: solo números verificados, sin texto crudo del documento. */
    public String promptSection(ClinicalVerification verification) {
        if (verification == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## VERIFICACIÓN CLÍNICA (fuente oficial OMS)\n");
        sb.append("Estado: ").append(verification.status()).append('\n');
        if (!verification.message().isBlank()) {
            sb.append(verification.message()).append('\n');
        }
        if (!verification.diagnosticMatches().isEmpty()) {
            sb.append("Coincidencias ICD (CIE-11):\n");
            for (DiagnosticMatch match : verification.diagnosticMatches()) {
                sb.append("- [").append(match.code()).append("] ")
                        .append(match.title()).append(" (búsqueda: ").append(match.searchTerm()).append(")\n");
            }
        }
        if (!verification.globalStatistics().isEmpty()) {
            sb.append("Contexto estadístico GHO:\n");
            for (GlobalStatistic stat : verification.globalStatistics()) {
                sb.append("- ").append(stat.indicatorCode())
                        .append(stat.indicatorLabel().isBlank() ? "" : " (" + stat.indicatorLabel() + ")")
                        .append(" → ").append(stat.value())
                        .append(stat.countryCode().isBlank() ? "" : " · " + stat.countryCode())
                        .append(stat.year().isBlank() ? "" : " · " + stat.year())
                        .append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Deriva hasta {@code max} términos de búsqueda para la ICD-API a partir del
     * texto del documento (hallazgos con valor + unidad) y del mensaje del
     * paciente. Visible a nivel de paquete para tests.
     */
    static List<String> deriveSearchTerms(String documentText, String userMessage, int max) {
        Set<String> terms = new LinkedHashSet<>();
        if (documentText != null && !documentText.isBlank()) {
            Matcher matcher = FINDING_PATTERN.matcher(documentText);
            while (matcher.find() && terms.size() < max) {
                String label = cleanLabel(matcher.group(1));
                if (!label.isBlank()) {
                    terms.add(label);
                }
            }
        }
        String message = userMessage == null ? "" : userMessage.trim();
        if (terms.size() < max && !message.isBlank()) {
            String query = message.length() > 140 ? message.substring(0, 140) : message;
            terms.add(query);
        }
        return terms.stream().limit(max).toList();
    }

    private static String cleanLabel(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().replaceAll("\\s+", " ").replaceAll("^[,.;:\\-]+|[,.;:\\-]+$", "");
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }

    private List<GlobalStatistic> fetchGlobalStatistics() {
        if (!properties.getGho().isEnabled() || properties.getGho().getIndicators().isEmpty()) {
            return List.of();
        }
        List<GlobalStatistic> stats = new ArrayList<>();
        for (WhoVerificationProperties.GhoIndicator indicator : properties.getGho().getIndicators()) {
            if (indicator.getIndicatorCode() == null || indicator.getIndicatorCode().isBlank()) {
                continue;
            }
            if (ghoBreaker.isOpen()) {
                break;
            }
            try {
                List<GlobalStatistic> found = ghoProvider.indicator(indicator.getIndicatorCode(),
                        indicator.getCountryCode());
                ghoBreaker.recordSuccess();
                found.forEach(s -> stats.add(new GlobalStatistic(
                        s.indicatorCode(),
                        indicator.getLabel() == null ? s.indicatorLabel() : indicator.getLabel(),
                        s.countryCode(), s.value(), s.year())));
            } catch (WhoHealthDataSourceException e) {
                ghoBreaker.recordFailure();
                log.warn("WhoVerificationService: fallo GHO ({}): {}", ghoBreaker.state(), e.getMessage());
            }
        }
        return stats;
    }

    private List<DiagnosticMatch> limit(List<DiagnosticMatch> matches) {
        int max = properties.getIcd().getMaxResults();
        return matches.size() <= max ? matches : new ArrayList<>(matches).subList(0, max);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
