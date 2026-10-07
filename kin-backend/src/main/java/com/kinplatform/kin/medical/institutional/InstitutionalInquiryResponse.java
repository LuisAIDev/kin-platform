package com.kinplatform.kin.medical.institutional;

import java.util.UUID;

public record InstitutionalInquiryResponse(
        UUID id,
        String status,
        String message
) {
}

