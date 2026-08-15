package com.kinplatform.projectinfo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Solicitud de guardado/actualización de información estructurada del proyecto. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructuredInfoRequest {

    @NotEmpty(message = "At least one information entry is required")
    @Size(max = 100, message = "Too many entries")
    private List<@Valid StructuredInfoEntry> entries;
}
