package com.kinplatform.kin.health.hce.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class HistoryJsonHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HistoryJsonHelper() { }

    public static String toJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("Error serializando JSON: " + e.getMessage(), e);
        }
    }

    public static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            throw new IllegalStateException("Error deserializando JSON: " + e.getMessage(), e);
        }
    }
}