package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.dto.UpdateEncounterRequest;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EncounterService encounterService;

    private UUID patientId;
    private UUID physicianId;
    private UUID organizationId;
    private com.kinplatform.user.User patient;
    private com.kinplatform.user.User physician;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        patient = com.kinplatform.user.User.builder()
                .id(patientId)
                .email("patient@test.com")
                .fullName("Test Patient")
                .role(com.kinplatform.user.UserRole.PATIENT)
                .organizationId(organizationId)
                .build();

        physician = com.kinplatform.user.User.builder()
                .id(physicianId)
                .email("physician@test.com")
                .fullName("Dr. Test")
                .role(com.kinplatform.user.UserRole.PHYSICIAN)
                .build();
    }

    @Test
    void createEncounter_happyPath() {
        var patient = com.kinplatform.user.User.builder()
                .id(patientId)
                .email("patient@test.com")
                .fullName("Test Patient")
                .role(com.kinplatform.user.UserRole.PATIENT)
                .organizationId(organizationId)
                .build();

        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(patient));
        when(encounterRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock the static AuthenticatedUsers.require() to return the physician
        try (MockedStatic<com.kinplatform.common.security.AuthenticatedUsers> mockedStatic = mockStatic(com.kinplatform.common.security.AuthenticatedUsers.class)) {
            mockedStatic.when(() -> com.kinplatform.common.security.AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            CreateEncounterRequest request = CreateEncounterRequest.builder()
                    .patientId(patientId)
                    .encounterType("OUTPATIENT")
                    .chiefComplaint("Dolor abdominal")
                    .build();

            EncounterResponse response = encounterService.createEncounter(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getEncounterType()).isEqualTo("OUTPATIENT");
            assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        }
    }

    @Test
    void createEncounter_throwsWhenOrganizationIdNull() {
        var patientNoOrg = com.kinplatform.user.User.builder()
                .id(patientId)
                .organizationId(null)
                .build();
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(patientNoOrg));

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(patientId)
                .encounterType("OUTPATIENT")
                .build();

        assertThatThrownBy(() -> encounterService.createEncounter(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("organization_id");
    }

    @Test
    void createEncounter_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(java.util.Optional.empty());

        CreateEncounterRequest request = CreateEncounterRequest.builder()
                .patientId(UUID.randomUUID())
                .encounterType("OUTPATIENT")
                .build();

        assertThatThrownBy(() -> encounterService.createEncounter(request))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }

    @Test
    void closeEncounter_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> encounterService.closeEncounter(UUID.randomUUID()))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }
}