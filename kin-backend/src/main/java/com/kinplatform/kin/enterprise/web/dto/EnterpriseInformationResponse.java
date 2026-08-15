package com.kinplatform.kin.enterprise.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Vista "Información y fuentes" del dashboard Enterprise: dimensiones
 * resueltas, datos suplementarios y documentos, cada uno con origen y estado
 * explícito. Un dato ausente se muestra como PENDING/NOT_AVAILABLE, nunca como 0.
 */
public record EnterpriseInformationResponse(
        List<DimensionInfo> resolvedDimensions,
        SupplementalInfo supplemental,
        List<DocumentInfo> documents,
        List<String> conflicts) {

    public record DimensionInfo(
            String dimension, String displayName, String value, String sourceType, String state, String origin) {}

    public record SupplementalInfo(
            Map<String, ValueInfo> financial,
            Map<String, ValueInfo> market,
            Map<String, ValueInfo> impact,
            Map<String, ValueInfo> risk,
            ValueInfo breakeven,
            List<String> breakevenMissing,
            boolean breakevenCalculable) {}

    public record ValueInfo(String value, String sourceType, String state, String origin) {}

    public record DocumentInfo(
            String id,
            String filename,
            String mimeType,
            long size,
            String status,
            String createdAt,
            String summary,
            List<String> relevantFacts) {}
}
