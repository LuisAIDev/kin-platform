package com.kinplatform.common.ai.context.adapter;

import com.kinplatform.common.context.AnalysisResult;
import com.kinplatform.common.context.AnalyzedDimension;
import com.kinplatform.common.context.ContextAnalyzerPort;
import com.kinplatform.common.context.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HeuristicContextAnalyzerAdapter implements ContextAnalyzerPort {

    private static final Logger log = LoggerFactory.getLogger(HeuristicContextAnalyzerAdapter.class);

    private static final Pattern PATTERN_PROJECT_NAME = Pattern.compile(
            "(?:mi proyecto se llama|el proyecto se llama|el nombre del proyecto es|se llama) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-]{2,60}?)(?=[\\.\\,\\;\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_CITY = Pattern.compile(
            "(?:soy de|vivo en|estoy en|estamos en|queda en|basado en|ubicad[oa] en|en la ciudad de|en el pueblo de) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ\\s\\-\\.]{2,50}?)(?=[\\.\\,\\;\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SECTOR = Pattern.compile(
            "(?:se dedica a|trata de|es una empresa de|es un proyecto de|giro del negocio es|rubro es|rubro|sector (?:es)?) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\/]{2,60}?)(?=[\\.\\,\\;\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_PROBLEM = Pattern.compile(
            "(?:el problema (?:es|que resuelve|principal (?:es|que resuelve))|el dolor (?:es|principal es)|la necesidad (?:es|principal es)|lo que resolvemos es|resuelve el problema de|resuelvo) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;]{5,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SOLUTION = Pattern.compile(
            "(?:mi soluci[óo]n (?:es|propuesta es)|la soluci[óo]n (?:es|que propongo es)|ofrezco|mi propuesta (?:es|de valor es)|consiste en|lo que ofrezco es|creamos|desarrollamos) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;]{5,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_CUSTOMER = Pattern.compile(
            "(?:clientes (?:son|ser[íi]an|objetivo son|a los que apunta)|apunta a|est[áa] dirigido a|target (?:es|son)|p[uú]blico objetivo (?:es|son)|est[áa] pensado para|para qui[ée]n es|beneficiarios (?:son|ser[íi]an)) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;]{5,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_VALUE_PROPOSITION = Pattern.compile(
            "(?:mi propuesta de valor (?:es)?|nuestra propuesta de valor (?:es)?|la propuesta de valor (?:es)?|propuesta de valor (?:es)?|el diferenciador (?:es)?|lo que nos distingue (?:es)?|nos distingue por|valor [uú]nico (?:es)?) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;]{4,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_REVENUE_MODEL = Pattern.compile(
            "(?:modelo de ingresos (?:es)?|modelo de negocio (?:es)?|fuentes de ingresos (?:son|es)?|c[óo]mo facturamos|nuestros ingresos (?:vienen|provienen) de|facturamos|ticket promedio (?:es|de)|retainers) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9%$][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9%$\\s\\-\\,\\;\\/\\.]{2,200}?)(?=[\\!\\?]|\\.\\s|\\.$|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_COMPETITION = Pattern.compile(
            "(?:la competencia (?:es|son)?|nuestra competencia (?:es|son)?|competencia (?:es|son)?|competimos (?:con|contra)|nuestros (?:competidores|rivales) (?:son)?|(?:el )?competidor principal (?:es)?|rivales (?:son)?) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;\\/\\.]{2,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_RISKS = Pattern.compile(
            "(?:los riesgos (?:son|principales son)?|el riesgo (?:principal )?(?:es)?|riesgos (?:son)?|riesgo (?:es)?|la mitigaci[óo]n (?:es)?|mitigaci[óo]n (?:es)?|dependencia (?:de|es)) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;\\/\\.]{4,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_RESOURCES = Pattern.compile(
            "(?:los recursos (?:son|necesarios son)?|recursos (?:son|necesarios)?|necesitamos|requerimos|el capital (?:es|necesario es|requerido es)?|capital (?:es)?|la inversi[óo]n (?:es|necesaria es)?|inversi[óo]n (?:es)?|presupuesto (?:es|de)?|la caja (?:es)?|pr[ée]stamo (?:de|por)?) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\$][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\.\\$]{2,200}?)(?=[\\!\\?]|\\.\\s|\\.$|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_MVP = Pattern.compile(
            "(?:mi mvp (?:es)?|el mvp (?:es)?|mvp (?:es|consiste en)?|producto m[íi]nimo viable (?:es)?|validaci[óo]n temprana (?:es)?|el piloto (?:es|consiste en)?|piloto (?:es)?|producto de entrada (?:es)?) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;\\/\\.]{2,200}?)(?=[\\!\\?]|\\.\\s|\\.$|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SCALABILITY = Pattern.compile(
            "(?:la escalabilidad(?: (?:es|depende de|consiste en|se basa en|implica))?|escalabilidad(?: (?:es|depende de|consiste en|se basa en|implica))?|podemos escalar|escalar|c[óo]mo escalamos|los playbooks (?:son)?|playbooks (?:son)?|crecer sin|para escalar) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;\\/\\.]{2,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_OBJECTIVES = Pattern.compile(
            "(?:los objetivos (?:son|del proyecto son)?|objetivos (?:son)?|el objetivo (?:es|principal es)?|objetivo (?:es)?|las metas (?:son)?|metas (?:son)?|nuestra meta (?:es)?|a (?:3|6|12) meses|en (?:3|6|12) meses) " +
            "[:;]?\\s*([A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9][A-ZÁÉÍÓÚÜÑa-záéíóúüñ0-9\\s\\-\\,\\;\\/\\.]{2,200}?)(?=[\\.\\!\\?]|$)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public AnalysisResult analyze(String userMessage, ProjectContext currentContext) {
        if (userMessage == null || userMessage.isBlank()) {
            return AnalysisResult.empty();
        }

        var extracted = new EnumMap<AnalyzedDimension, String>(AnalyzedDimension.class);

        extract(extracted, AnalyzedDimension.PROJECT_NAME, PATTERN_PROJECT_NAME, userMessage);
        extract(extracted, AnalyzedDimension.CITY, PATTERN_CITY, userMessage);
        extract(extracted, AnalyzedDimension.SECTOR, PATTERN_SECTOR, userMessage);
        extract(extracted, AnalyzedDimension.PROBLEM, PATTERN_PROBLEM, userMessage);
        extract(extracted, AnalyzedDimension.SOLUTION, PATTERN_SOLUTION, userMessage);
        extract(extracted, AnalyzedDimension.TARGET_CUSTOMER, PATTERN_CUSTOMER, userMessage);
        extract(extracted, AnalyzedDimension.VALUE_PROPOSITION, PATTERN_VALUE_PROPOSITION, userMessage);
        extract(extracted, AnalyzedDimension.REVENUE_MODEL, PATTERN_REVENUE_MODEL, userMessage);
        extract(extracted, AnalyzedDimension.COMPETITION, PATTERN_COMPETITION, userMessage);
        extract(extracted, AnalyzedDimension.RISKS, PATTERN_RISKS, userMessage);
        extract(extracted, AnalyzedDimension.RESOURCES, PATTERN_RESOURCES, userMessage);
        extract(extracted, AnalyzedDimension.MVP, PATTERN_MVP, userMessage);
        extract(extracted, AnalyzedDimension.SCALABILITY, PATTERN_SCALABILITY, userMessage);
        extract(extracted, AnalyzedDimension.OBJECTIVES, PATTERN_OBJECTIVES, userMessage);

        var result = new AnalysisResult(extracted);
        if (!result.isEmpty()) {
            log.info("ContextAnalyzer extracted {} dimensions from message: {}", extracted.size(), extracted.keySet());
        }
        return result;
    }

    private void extract(Map<AnalyzedDimension, String> target, AnalyzedDimension dim,
                         Pattern pattern, String message) {
        var matcher = pattern.matcher(message);
        if (matcher.find()) {
            var value = matcher.group(1).strip();
            if (!value.isBlank()) {
                target.put(dim, value);
            }
        }
    }
}


