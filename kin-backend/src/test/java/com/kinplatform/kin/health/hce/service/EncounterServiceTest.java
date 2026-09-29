package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.common.security.TenantContext;
import com.kinplatform.kin.health.hce.dto.CreateEncounterRequest;
import com.kinplatform.kin.health.hce.dto.EncounterResponse;
import com.kinplatform.kin.health.hce.dto.UpdateEncounterRequest;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct;
import com.kinplatform.kin.health.hce.mapper.EncounterMapper;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Mock
    private com.kinplatform.kin.health.hce.repository.DiagnosesRepository diagnosesRepository;

    @Mock
    private com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository treatmentPlanRepository;

    @Spy
    private EncounterMapper encounterMapper = new EncounterMapper();

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

    private void setupSecurityContext(com.kinplatform.user.User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PHYSICIAN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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

            setupSecurityContext(physician);

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
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createEncounter_patientWithoutOrg_usesPhysicianContextOrg() {
        UUID physicianOrg = UUID.randomUUID();
        var patientNoOrg = com.kinplatform.user.User.builder()
                .id(patientId)
                .email("patient@test.com")
                .fullName("Test Patient")
                .role(com.kinplatform.user.UserRole.PATIENT)
                .organizationId(null)
                .build();
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(patientNoOrg));
        when(encounterRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any())).thenReturn(physician);
            setupSecurityContext(physician);
            TenantContext.set(physicianOrg);
            try {
                CreateEncounterRequest request = CreateEncounterRequest.builder()
                        .patientId(patientId)
                        .encounterType("OUTPATIENT")
                        .chiefComplaint("Dolor abdominal")
                        .build();

                EncounterResponse response = encounterService.createEncounter(request);

                assertThat(response).isNotNull();
                ArgumentCaptor<Encounter> captor = ArgumentCaptor.forClass(Encounter.class);
                verify(encounterRepository).saveAndFlush(captor.capture());
                assertThat(captor.getValue().getOrganizationId()).isEqualTo(physicianOrg);
            } finally {
                TenantContext.clear();
                clearSecurityContext();
            }
        }
    }

    @Test
    void createEncounter_patientWithOrg_usesPatientOrg() {
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(patient));
        when(encounterRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any())).thenReturn(physician);
            setupSecurityContext(physician);
            try {
                CreateEncounterRequest request = CreateEncounterRequest.builder()
                        .patientId(patientId)
                        .encounterType("OUTPATIENT")
                        .chiefComplaint("Dolor abdominal")
                        .build();

                encounterService.createEncounter(request);

                ArgumentCaptor<Encounter> captor = ArgumentCaptor.forClass(Encounter.class);
                verify(encounterRepository).saveAndFlush(captor.capture());
                assertThat(captor.getValue().getOrganizationId()).isEqualTo(organizationId);
            } finally {
                clearSecurityContext();
            }
        }
    }

    @Test
    void createEncounter_patientIsPhysician_throwsException() {
        var self = com.kinplatform.user.User.builder()
                .id(physicianId)
                .email("physician@test.com")
                .fullName("Dr. Test")
                .role(com.kinplatform.user.UserRole.PHYSICIAN)
                .build();
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(self));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any())).thenReturn(physician);
            setupSecurityContext(physician);
            try {
                CreateEncounterRequest request = CreateEncounterRequest.builder()
                        .patientId(physicianId)
                        .encounterType("OUTPATIENT")
                        .chiefComplaint("Dolor abdominal")
                        .build();

                assertThatThrownBy(() -> encounterService.createEncounter(request))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("mismo que el médico");
            } finally {
                clearSecurityContext();
            }
        }
    }

    @Test
    void createEncounter_physicianNotResolved_throwsException() {
        when(userRepository.findById(any())).thenReturn(java.util.Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenThrow(new IllegalArgumentException("Authenticated user not found"));
            setupSecurityContext(physician);
            try {
                CreateEncounterRequest request = CreateEncounterRequest.builder()
                        .patientId(patientId)
                        .encounterType("OUTPATIENT")
                        .chiefComplaint("Dolor abdominal")
                        .build();

                assertThatThrownBy(() -> encounterService.createEncounter(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Authenticated user not found");
            } finally {
                clearSecurityContext();
            }
        }
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

        // Mock the static AuthenticatedUsers.require() to return the physician
        try (MockedStatic<com.kinplatform.common.security.AuthenticatedUsers> mockedStatic = mockStatic(com.kinplatform.common.security.AuthenticatedUsers.class)) {
            mockedStatic.when(() -> com.kinplatform.common.security.AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> encounterService.closeEncounter(UUID.randomUUID()))
                    .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void closeEncounter_happyPath_withPrincipalDiagnosisAndTreatmentPlan() {
        var encounter = com.kinplatform.kin.health.hce.entity.Encounter.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(com.kinplatform.kin.health.hce.entity.Encounter.EncounterType.OUTPATIENT)
                .status(com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS)
                .chiefComplaint("Dolor abdominal")
                .build();

        var principalDiagnosis = com.kinplatform.kin.health.hce.entity.Diagnoses.builder()
                .id(UUID.randomUUID())
                .encounterId(encounter.getId())
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.1")
                .diagnosisType(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty.CONFIRMED)
                .status(com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE)
                .build();

        var treatmentPlan = com.kinplatform.kin.health.hce.entity.TreatmentPlan.builder()
                .id(UUID.randomUUID())
                .encounterId(encounter.getId())
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(com.kinplatform.kin.health.hce.entity.TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .build();

        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(diagnosesRepository.existsByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(true);
        when(treatmentPlanRepository.findByEncounterId(any())).thenReturn(java.util.Optional.of(treatmentPlan));
        when(encounterRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock the static AuthenticatedUsers.require() to return the physician
        try (MockedStatic<com.kinplatform.common.security.AuthenticatedUsers> mockedStatic = mockStatic(com.kinplatform.common.security.AuthenticatedUsers.class)) {
            mockedStatic.when(() -> com.kinplatform.common.security.AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            var response = encounterService.closeEncounter(encounter.getId());

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getClosedAt()).isNotNull();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void closeEncounter_throwsWhenMissingPrincipalDiagnosis() {
        var encounter = com.kinplatform.kin.health.hce.entity.Encounter.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(com.kinplatform.kin.health.hce.entity.Encounter.EncounterType.OUTPATIENT)
                .status(com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS)
                .build();

        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(diagnosesRepository.existsByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(false);

        // Mock the static AuthenticatedUsers.require() to return the physician
        try (MockedStatic<com.kinplatform.common.security.AuthenticatedUsers> mockedStatic = mockStatic(com.kinplatform.common.security.AuthenticatedUsers.class)) {
            mockedStatic.when(() -> com.kinplatform.common.security.AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> encounterService.closeEncounter(encounter.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("principal diagnosis");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void closeEncounter_throwsWhenMissingTreatmentPlan() {
        var encounter = com.kinplatform.kin.health.hce.entity.Encounter.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(organizationId)
                .encounterType(com.kinplatform.kin.health.hce.entity.Encounter.EncounterType.OUTPATIENT)
                .status(com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS)
                .build();

        var principalDiagnosis = com.kinplatform.kin.health.hce.entity.Diagnoses.builder()
                .id(UUID.randomUUID())
                .encounterId(encounter.getId())
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.1")
                .diagnosisType(com.kinplatform.kin.health.hce.entity.Diagnoses.DiagnosisType.PRINCIPAL)
                .certainty(com.kinplatform.kin.health.hce.entity.Diagnoses.Certainty.CONFIRMED)
                .status(com.kinplatform.kin.health.hce.entity.Diagnoses.Status.ACTIVE)
                .build();

        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(diagnosesRepository.existsByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(true);
        when(treatmentPlanRepository.findByEncounterId(any())).thenReturn(java.util.Optional.empty());

        // Mock the static AuthenticatedUsers.require() to return the physician
        try (MockedStatic<com.kinplatform.common.security.AuthenticatedUsers> mockedStatic = mockStatic(com.kinplatform.common.security.AuthenticatedUsers.class)) {
            mockedStatic.when(() -> com.kinplatform.common.security.AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> encounterService.closeEncounter(encounter.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("treatment plan");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void updateEncounter_throwsWhenPhysicianIdMismatch() {
        var encounter = com.kinplatform.kin.health.hce.entity.Encounter.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(UUID.randomUUID()) // Different physician
                .organizationId(organizationId)
                .encounterType(com.kinplatform.kin.health.hce.entity.Encounter.EncounterType.OUTPATIENT)
                .status(com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus.IN_PROGRESS)
                .build();

        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));

        var otherPhysician = com.kinplatform.user.User.builder()
                .id(UUID.randomUUID()) // Different physician
                .email("other@test.com")
                .build();

        // Mock the static AuthenticatedUsers.require to return a different physician
        try (MockedStatic<com.kinplatform.common.security.AuthenticatedUsers> mockedStatic = mockStatic(com.kinplatform.common.security.AuthenticatedUsers.class)) {
            mockedStatic.when(() -> com.kinplatform.common.security.AuthenticatedUsers.require(any(), any()))
                    .thenReturn(otherPhysician);

            setupSecurityContext(otherPhysician);

            UpdateEncounterRequest request = UpdateEncounterRequest.builder()
                    .chiefComplaint("Updated complaint")
                    .build();

            assertThatThrownBy(() -> encounterService.updateEncounter(UUID.randomUUID(), request))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            clearSecurityContext();
        }
    }
}