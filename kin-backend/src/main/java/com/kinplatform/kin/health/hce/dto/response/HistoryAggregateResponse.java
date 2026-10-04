package com.kinplatform.kin.health.hce.dto.response;

import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.dto.PatientHistoryResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryAggregateResponse {

    private List<Map<String, Object>> allergies;
    private List<Map<String, Object>> surgeries;
    private List<Map<String, Object>> medications;
    private List<Map<String, Object>> vaccines;
    private List<Map<String, Object>> familyHistory;
    private List<Map<String, Object>> toxicHabits;
    private Map<String, Object> gynecoObstetric;

    public static HistoryAggregateResponse fromResponses(List<PatientHistoryResponse> responses) {
        return new HistoryAggregateResponse(
                toList(responses, PatientHistory.HistoryType.ALLERGY),
                toList(responses, PatientHistory.HistoryType.SURGERY),
                toList(responses, PatientHistory.HistoryType.MEDICATION),
                toList(responses, PatientHistory.HistoryType.VACCINE),
                toList(responses, PatientHistory.HistoryType.FAMILY),
                toList(responses, PatientHistory.HistoryType.TOXICOLOGICAL),
                toSingle(responses, PatientHistory.HistoryType.GYNECO_OBSTETRIC)
        );
    }

    private static List<Map<String, Object>> toList(
            List<PatientHistoryResponse> responses,
            PatientHistory.HistoryType type) {
        return responses.stream()
                .filter(r -> r.getHistoryType() == type)
                .map(HistoryAggregateResponse::toItemMap)
                .collect(Collectors.toList());
    }

    private static Map<String, Object> toSingle(
            List<PatientHistoryResponse> responses,
            PatientHistory.HistoryType type) {
        return responses.stream()
                .filter(r -> r.getHistoryType() == type)
                .findFirst()
                .map(HistoryAggregateResponse::toItemMap)
                .orElse(null);
    }

    private static Map<String, Object> toItemMap(PatientHistoryResponse r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId().toString());
        map.put("type", mapTypeToFrontend(r.getHistoryType()));
        map.put("patientId", r.getPatientId().toString());
        map.put("data", buildDataMap(r));
        if (r.getCreatedAt() != null) map.put("createdAt", r.getCreatedAt().toString());
        if (r.getUpdatedAt() != null) map.put("updatedAt", r.getUpdatedAt().toString());
        return map;
    }

    private static Map<String, Object> buildDataMap(PatientHistoryResponse r) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("description", r.getDescription());
        if (r.getOnsetDate() != null) data.put("onsetDate", r.getOnsetDate().toString());
        if (r.getResolutionDate() != null) data.put("resolutionDate", r.getResolutionDate().toString());
        if (r.getStatus() != null) data.put("status", r.getStatus().name());
        if (r.getSeverity() != null) data.put("severity", mapSeverityToFrontend(r.getSeverity()));
        if (r.getNotes() != null) data.put("notes", r.getNotes());

        if (r.getDetails() != null && !r.getDetails().isEmpty() && !r.getDetails().equals("{}")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> details = mapper.readValue(r.getDetails(), Map.class);
                details.forEach(data::putIfAbsent);
            } catch (Exception ignored) {
            }
        }
        return data;
    }

    private static String mapTypeToFrontend(PatientHistory.HistoryType type) {
        return type == PatientHistory.HistoryType.FAMILY ? "FAMILY_HISTORY" : type.name();
    }

    private static String mapSeverityToFrontend(PatientHistory.Severity severity) {
        return switch (severity) {
            case LEVE -> "MILD";
            case MODERADO -> "MODERATE";
            case GRAVE -> "SEVERE";
            case CRITICO -> "ANAPHYLAXIS";
        };
    }
}