package com.kinplatform.institutional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record OrganizationMemberRequest(
        @NotBlank @Email String email,
        @NotBlank String role,
        UUID branchId
) {
}
