package com.kinplatform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String email;
    private String fullName;
    private String role;
    private Boolean emailVerified;

    /** Estado de verificación de identidad (médicos): PENDING/APPROVED/REJECTED o null. */
    private String verificationStatus;
}
