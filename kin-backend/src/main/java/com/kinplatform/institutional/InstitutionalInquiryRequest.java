package com.kinplatform.institutional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record InstitutionalInquiryRequest(
        @NotBlank String ipsName,
        @NotBlank String nit,
        String city,
        @NotBlank String contactName,
        @NotBlank @Email String email,
        String phone,
        @PositiveOrZero Integer beds,
        String comments
) {
}
