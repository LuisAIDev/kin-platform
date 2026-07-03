package com.kinplatform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

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
}
