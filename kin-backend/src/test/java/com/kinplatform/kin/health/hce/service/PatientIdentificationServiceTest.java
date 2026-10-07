package com.kinplatform.kin.health.hce.service;

import com.kinplatform.kin.health.hce.dto.CreatePatientIdentificationRequest;
import com.kinplatform.kin.health.hce.dto.PatientIdentificationResponse;
import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.DocumentType;
import com.kinplatform.kin.health.hce.entity.PatientIdentification.Regimen;
import com.kinplatform.kin.health.hce.mapper.PatientIdentificationMapper;
import com.kinplatform.kin.health.hce.repository.PatientIdentificationRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientIdentificationServiceTest {

    @Mock
    private PatientIdentificationRepository repository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private PatientIdentificationMapper mapper = new PatientIdentificationMapper();

    @InjectMocks
    private PatientIdentificationService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void upsertIdentification_createsWhenNotExists() {
        when(userRepository.existsById(any())).thenReturn(true);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("12345678")
                .rhFactor("A_POS")
                .epsCode("EPS001")
                .regimen("CONTRIBUTIVO")
                .build();

        PatientIdentificationResponse response = service.upsertIdentification(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentNumber()).isEqualTo("12345678");
    }

    @Test
    void upsertIdentification_updatesWhenExists() {
        when(userRepository.existsById(any())).thenReturn(true);
        when(repository.existsByDocumentTypeAndDocumentNumber(any(), any())).thenReturn(true);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(userId)
                .documentType("CC")
                .documentNumber("87654321")
                .rhFactor("A_POS")
                .epsCode("EPS001")
                .regimen("CONTRIBUTIVO")
                .build();

        when(repository.findByUserId(userId)).thenReturn(java.util.Optional.of(
                PatientIdentification.builder()
                        .documentNumber("11111111")
                        .build()
        ));

        PatientIdentificationResponse response = service.upsertIdentification(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentNumber()).isEqualTo("87654321");
    }

    @Test
    void upsertIdentification_throwsWhenUserNotFound() {
        when(userRepository.existsById(any())).thenReturn(false);

        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("12345678")
                .build();

        assertThatThrownBy(() -> service.upsertIdentification(UUID.randomUUID(), request))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void upsertIdentification_throwsWhenDocumentExistsForOtherUser() {
        when(userRepository.existsById(any())).thenReturn(true);
        when(repository.existsByDocumentTypeAndDocumentNumber(any(), any())).thenReturn(true);
        // El usuario actual NO tiene registro previo (Optional.empty)
        when(repository.findByUserId(any())).thenReturn(java.util.Optional.empty());

        CreatePatientIdentificationRequest request = CreatePatientIdentificationRequest.builder()
                .userId(UUID.randomUUID())
                .documentType("CC")
                .documentNumber("11111111")
                .build();

        assertThatThrownBy(() -> service.upsertIdentification(UUID.randomUUID(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered for another user");
    }

    @Test
    void getByUserIdOptional_whenExists_returnsOptionalWithValue() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(
                PatientIdentification.builder()
                        .documentType(DocumentType.CC)
                        .documentNumber("12345678")
                        .build()));

        Optional<PatientIdentificationResponse> result = service.getByUserIdOptional(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getDocumentNumber()).isEqualTo("12345678");
    }

    @Test
    void getByUserIdOptional_whenNotExists_returnsOptionalEmpty() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<PatientIdentificationResponse> result = service.getByUserIdOptional(userId);

        assertThat(result).isEmpty();
    }
}
