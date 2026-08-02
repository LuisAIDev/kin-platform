package com.kinplatform.ai.context.adapter;

import com.kinplatform.kin.context.AnalysisResult;
import com.kinplatform.kin.context.AnalyzedDimension;
import com.kinplatform.kin.context.ContextAnalyzerPort;
import com.kinplatform.kin.context.ProjectContext;
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
            "(?:es un|es una|es un negocio de|es un emprendimiento de|se dedica a|trata de|es una empresa de|es un proyecto de|giro del negocio es|rubro es|rubro) " +
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
