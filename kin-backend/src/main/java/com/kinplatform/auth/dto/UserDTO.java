package com.kinplatform.auth.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
    private Integer credits;
    private Boolean emailVerified;

    /** Estado de verificación de identidad (médicos): PENDING/APPROVED/REJECTED o null. */
    private String verificationStatus;
}
