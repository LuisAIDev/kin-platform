package com.kinplatform.user;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de usuarios (vertical Salud): listado y verificación de
 * identidad de médicos auto-registrados.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PendingPhysicianResponse> pendingPhysicians() {
        return userRepository
                .findByRoleAndPhysicianVerificationStatus(UserRole.PHYSICIAN, PhysicianVerificationStatus.PENDING)
                .stream()
                .map(u -> PendingPhysicianResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .licenseNumber(u.getLicenseNumber())
                        .specialty(u.getSpecialty())
                        .country(u.getCountry())
                        .phone(u.getPhone())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void setVerificationStatus(UUID userId, PhysicianVerificationStatus status) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (user.getRole() != UserRole.PHYSICIAN) {
            throw new IllegalArgumentException("El usuario no es un médico");
        }
        user.setPhysicianVerificationStatus(status);
        userRepository.save(user);
    }
}
