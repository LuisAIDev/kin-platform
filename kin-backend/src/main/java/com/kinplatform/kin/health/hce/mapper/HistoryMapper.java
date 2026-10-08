package com.kinplatform.kin.health.hce.mapper;

import com.kinplatform.kin.health.hce.dto.CreatePatientHistoryRequest;
import com.kinplatform.kin.health.hce.dto.PatientHistoryResponse;
import com.kinplatform.kin.health.hce.dto.response.HistoryAggregateResponse;
import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Severity;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
import com.kinplatform.kin.health.hce.util.HistoryJsonHelper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class HistoryMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public CreatePatientHistoryRequest toCreateRequest(
            String type, Map<String, Object> data, UUID patientId, UUID recordedBy) {
        HistoryType historyType = mapTypeFrontendToEntity(type);
        String description = extractDescription(historyType, data);
        LocalDate onsetDate = extractOnsetDate(historyType, data);
        LocalDate resolutionDate = extractResolutionDate(historyType, data);
        Status status = extractStatus(historyType, data);
        Severity severity = extractSeverity(historyType, data);
        String notes = extractNotes(data);
        String details = buildDetailsJson(historyType, data);

        return CreatePatientHistoryRequest.builder()
                .patientId(patientId)
                .historyType(historyType)
                .description(description)
                .onsetDate(onsetDate)
                .resolutionDate(resolutionDate)
                .status(status)
                .severity(severity)
                .notes(notes)
                .recordedBy(recordedBy)
                .details(details)
                .build();
    }

    public PatientHistory toEntity(
            String type, Map<String, Object> data, UUID patientId, UUID recordedBy, UUID existingId) {
        CreatePatientHistoryRequest req = toCreateRequest(type, data, patientId, recordedBy);
        return PatientHistory.builder()
                .id(existingId)
                .patientId(req.getPatientId())
                .historyType(req.getHistoryType())
                .description(req.getDescription())
                .onsetDate(req.getOnsetDate())
                .resolutionDate(req.getResolutionDate())
                .status(req.getStatus() != null ? req.getStatus() : Status.ACTIVE)
                .severity(req.getSeverity())
                .notes(req.getNotes())
                .recordedBy(req.getRecordedBy())
                .details(req.getDetails() != null ? req.getDetails() : "{}")
                .build();
    }

    public HistoryAggregateResponse toAggregateResponse(List<PatientHistoryResponse> responses) {
        return HistoryAggregateResponse.fromResponses(responses);
    }

    public Map<String, Object> toFrontendItem(PatientHistoryResponse r) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", r.getId().toString());
        map.put("type", mapTypeEntityToFrontend(r.getHistoryType()));
        map.put("patientId", r.getPatientId().toString());
        map.put("data", buildItemData(r));
        if (r.getCreatedAt() != null) map.put("createdAt", r.getCreatedAt().toString());
        if (r.getUpdatedAt() != null) map.put("updatedAt", r.getUpdatedAt().toString());
        return map;
    }

    private Map<String, Object> buildItemData(PatientHistoryResponse r) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("description", r.getDescription());
        if (r.getOnsetDate() != null) data.put("onsetDate", r.getOnsetDate().format(DATE_FORMATTER));
        if (r.getResolutionDate() != null)
            data.put("resolutionDate", r.getResolutionDate().format(DATE_FORMATTER));
        if (r.getStatus() != null) data.put("status", r.getStatus().name());
        if (r.getSeverity() != null) data.put("severity", mapSeverityEntityToFrontend(r.getSeverity()));
        if (r.getNotes() != null) data.put("notes", r.getNotes());

        Map<String, Object> details = parseDetails(r.getDetails());
        details.forEach(data::putIfAbsent);
        return data;
    }

    private HistoryType mapTypeFrontendToEntity(String frontendType) {
        return "FAMILY_HISTORY".equals(frontendType) ? HistoryType.FAMILY : HistoryType.valueOf(frontendType);
    }

    private String mapTypeEntityToFrontend(HistoryType entityType) {
        return entityType == HistoryType.FAMILY ? "FAMILY_HISTORY" : entityType.name();
    }

    private String extractDescription(HistoryType type, Map<String, Object> data) {
        return switch (type) {
            case ALLERGY -> (String) data.get("allergen");
            case SURGERY -> (String) data.get("procedure");
            case MEDICATION -> (String) data.get("name");
            case VACCINE -> (String) data.get("name");
            case FAMILY -> (String) data.get("condition");
            case TOXICOLOGICAL -> (String) data.get("substance");
            case GYNECO_OBSTETRIC -> "Gineco-obstétricos";
        };
    }

    private LocalDate extractOnsetDate(HistoryType type, Map<String, Object> data) {
        String dateStr =
                switch (type) {
                    case ALLERGY, SURGERY, VACCINE -> (String) data.getOrDefault("date", data.get("onsetDate"));
                    case MEDICATION -> (String) data.get("startDate");
                    case FAMILY -> null;
                    case TOXICOLOGICAL -> (String) data.get("startDate");
                    case GYNECO_OBSTETRIC -> (String) data.get("lastMenstrualPeriod");
                };
        return dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : null;
    }

    private LocalDate extractResolutionDate(HistoryType type, Map<String, Object> data) {
        String dateStr =
                switch (type) {
                    case MEDICATION, TOXICOLOGICAL -> (String) data.get("endDate");
                    default -> null;
                };
        return dateStr != null ? LocalDate.parse(dateStr, DATE_FORMATTER) : null;
    }

    private Status extractStatus(HistoryType type, Map<String, Object> data) {
        String statusStr = (String) data.get("status");
        if (statusStr == null) return null;
        try {
            return Status.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return "UNKNOWN".equalsIgnoreCase(statusStr) ? Status.ACTIVE : null;
        }
    }

    private Severity extractSeverity(HistoryType type, Map<String, Object> data) {
        String severityStr = (String) data.get("severity");
        if (severityStr == null) return null;
        return switch (severityStr) {
            case "MILD" -> Severity.LEVE;
            case "MODERATE" -> Severity.MODERADO;
            case "SEVERE" -> Severity.GRAVE;
            case "ANAPHYLAXIS" -> Severity.CRITICO;
            default -> null;
        };
    }

    private String extractNotes(Map<String, Object> data) {
        return (String) data.get("notes");
    }

    private String buildDetailsJson(HistoryType type, Map<String, Object> data) {
        Map<String, Object> details = new java.util.LinkedHashMap<>();
        switch (type) {
            case ALLERGY -> {
                putIfPresent(details, "reaction", data.get("reaction"));
                putIfPresent(details, "severity", data.get("severity"));
                putIfPresent(details, "onsetDate", data.get("onsetDate"));
                putIfPresent(details, "status", data.get("status"));
                putIfPresent(details, "notes", data.get("notes"));
            }
            case SURGERY -> {
                putIfPresent(details, "date", data.get("date"));
                putIfPresent(details, "hospital", data.get("hospital"));
                putIfPresent(details, "complications", data.get("complications"));
                putIfPresent(details, "notes", data.get("notes"));
            }
            case MEDICATION -> {
                putIfPresent(details, "dosage", data.get("dosage"));
                putIfPresent(details, "frequency", data.get("frequency"));
                putIfPresent(details, "route", data.get("route"));
                putIfPresent(details, "startDate", data.get("startDate"));
                putIfPresent(details, "endDate", data.get("endDate"));
                putIfPresent(details, "isActive", data.get("isActive"));
                putIfPresent(details, "indication", data.get("indication"));
                putIfPresent(details, "notes", data.get("notes"));
            }
            case VACCINE -> {
                putIfPresent(details, "date", data.get("date"));
                putIfPresent(details, "dose", data.get("dose"));
                putIfPresent(details, "batch", data.get("batch"));
                putIfPresent(details, "nextDoseDate", data.get("nextDoseDate"));
                putIfPresent(details, "notes", data.get("notes"));
            }
            case FAMILY -> {
                putIfPresent(details, "relationship", data.get("relationship"));
                putIfPresent(details, "ageOfOnset", data.get("ageOfOnset"));
                putIfPresent(details, "isDeceased", data.get("isDeceased"));
                putIfPresent(details, "ageAtDeath", data.get("ageAtDeath"));
                putIfPresent(details, "notes", data.get("notes"));
            }
            case TOXICOLOGICAL -> {
                putIfPresent(details, "frequency", data.get("frequency"));
                putIfPresent(details, "quantity", data.get("quantity"));
                putIfPresent(details, "startDate", data.get("startDate"));
                putIfPresent(details, "endDate", data.get("endDate"));
                putIfPresent(details, "isActive", data.get("isActive"));
                putIfPresent(details, "notes", data.get("notes"));
            }
            case GYNECO_OBSTETRIC -> {
                putIfPresent(details, "gravida", data.get("gravida"));
                putIfPresent(details, "para", data.get("para"));
                putIfPresent(details, "abortions", data.get("abortions"));
                putIfPresent(details, "cesareans", data.get("cesareans"));
                putIfPresent(details, "livingChildren", data.get("livingChildren"));
                putIfPresent(details, "lastMenstrualPeriod", data.get("lastMenstrualPeriod"));
                putIfPresent(details, "menarcheAge", data.get("menarcheAge"));
                putIfPresent(details, "menopauseAge", data.get("menopauseAge"));
                putIfPresent(details, "contraceptiveMethod", data.get("contraceptiveMethod"));
                putIfPresent(details, "notes", data.get("notes"));
            }
        }
        return HistoryJsonHelper.toJson(details);
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }

    private Map<String, Object> parseDetails(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return Map.of();
        }
        return HistoryJsonHelper.fromJson(json);
    }

    private String mapSeverityEntityToFrontend(Severity severity) {
        if (severity == null) return "MILD";
        return switch (severity) {
            case LEVE -> "MILD";
            case MODERADO -> "MODERATE";
            case GRAVE -> "SEVERE";
            case CRITICO -> "ANAPHYLAXIS";
        };
    }
}
