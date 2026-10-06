package com.kinplatform.platform.projectinfo.dto;

import com.kinplatform.platform.projectinfo.StructuredInfoSourceType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Respuesta de una entrada de información estructurada del proyecto. */
@Data
@AllArgsConstructor
@Builder
public class StructuredInfoResponse {

    private UUID projectId;
    private String section;
    private String key;
    private String value;
    private StructuredInfoSourceType sourceType;
    private StructuredInfoSourceType originalSourceType;
    private String sourceDocument;
    private OffsetDateTime confirmedAt;
    private OffsetDateTime updatedAt;
}


