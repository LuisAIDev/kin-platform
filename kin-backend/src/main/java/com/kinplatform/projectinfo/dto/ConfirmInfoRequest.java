package com.kinplatform.projectinfo.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Solicitud de confirmación de un dato importado como USER_INPUT (opcional). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmInfoRequest {

    @Size(max = 255, message = "Source document must not exceed 255 characters")
    private String sourceDocument;
}
