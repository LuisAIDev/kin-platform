package com.kinplatform.kin.health.hce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientHistoryRequest {

    @NotNull
    @Pattern(regexp = "ALLERGY|SURGERY|MEDICATION|VACCINE|FAMILY|TOXICOLOGICAL|GYNECO_OBSTETRIC")
    private String historyType;

    @NotBlank
    private String description;
}