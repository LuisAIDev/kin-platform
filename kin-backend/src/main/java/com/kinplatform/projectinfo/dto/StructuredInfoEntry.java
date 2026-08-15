package com.kinplatform.projectinfo.dto;

import com.kinplatform.projectinfo.StructuredInfoSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una entrada de información estructurada (sección + clave + valor + origen). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructuredInfoEntry {

    @NotBlank(message = "Section is required")
    @Size(max = 64, message = "Section must not exceed 64 characters")
    private String section;

    @NotBlank(message = "Key is required")
    @Size(max = 64, message = "Key must not exceed 64 characters")
    private String key;

    @NotBlank(message = "Value is required")
    @Size(max = 10000, message = "Value must not exceed 10000 characters")
    private String value;

    @NotNull(message = "Source type is required")
    private StructuredInfoSourceType sourceType;
}
