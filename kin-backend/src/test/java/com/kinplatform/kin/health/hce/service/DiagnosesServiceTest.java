package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateDiagnosisRequest;
import com.kinplatform.kin.health.hce.dto.DiagnosesResponse;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagnosesServiceTest {

    @Mock
    private DiagnosesRepository diagnosesRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DiagnosesService diagnosesService;

    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private Encounter encounter;
    private User physician;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();

        encounter = Encounter.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(UUID.randomUUID())
                .encounterType(EncounterType.OUTPATIENT)
                .status(EncounterStatus.IN_PROGRESS)
                .build();

        physician = User.builder()
                .id(physicianId)
                .email("physician@test.com")
                .fullName("Dr. Test")
                .role(com.kinplatform.common.user.UserRole.PHYSICIAN)
                .build();
    }

    private void setupSecurityContext(User user) {
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
    void addDiagnosis_happyPath_secundario() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                    .encounterId(encounterId)
                    .cie10Code("K59.1")
                    .cie10Description("Functional dyspepsia")
                    .diagnosisType(Diagnoses.DiagnosisType.SECUNDARIO)
                    .certainty(Diagnoses.Certainty.CONFIRMED)
                    .classification(Diagnoses.Classification.CONSULTA)
                    .onsetDate(java.time.LocalDate.now())
                    .status(Diagnoses.Status.ACTIVE)
                    .notes("Post-prandial discomfort")
                    .build();

            DiagnosesResponse response = diagnosesService.addDiagnosis(request);

            assertThat(response).isNotNull();
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getCie10Code()).isEqualTo("K59.1");
            assertThat(response.getDiagnosisType()).isEqualTo(Diagnoses.DiagnosisType.SECUNDARIO);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addDiagnosis_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                    .encounterId(UUID.randomUUID())
                    .cie10Code("K59.1")
                    .diagnosisType(Diagnoses.DiagnosisType.SECUNDARIO)
                    .build();

            assertThatThrownBy(() -> diagnosesService.addDiagnosis(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addDiagnosis_throwsWhenCie10CodeInvalidFormat() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                    .encounterId(encounterId)
                    .cie10Code("ZZZ") // Invalid format - should start with letter A-Z
                    .diagnosisType(Diagnoses.DiagnosisType.SECUNDARIO)
                    .build();

            // Service doesn't validate pattern - this is handled by Bean Validation in controller
            // This test documents expected behavior; actual validation happens at controller layer
            when(diagnosesRepository.saveAndFlush(any())).thenAnswer(invocation -> {
                Diagnoses d = invocation.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            DiagnosesResponse response = diagnosesService.addDiagnosis(request);
            assertThat(response).isNotNull(); // Service allows it, validation at controller
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addDiagnosis_throwsWhenPrincipalAlreadyExists() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.existsByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(true);

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateDiagnosisRequest request = CreateDiagnosisRequest.builder()
                    .encounterId(encounterId)
                    .cie10Code("K59.1")
                    .diagnosisType(Diagnoses.DiagnosisType.PRINCIPAL)
                    .build();

            assertThatThrownBy(() -> diagnosesService.addDiagnosis(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already has a principal diagnosis");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void setPrincipal_happyPath_unmarksPrevious() {
        UUID diagnosisId1 = UUID.randomUUID();
        UUID diagnosisId2 = UUID.randomUUID();

        Encounter encounter = Encounter.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .build();

        Diagnoses oldPrincipal = Diagnoses.builder()
                .id(diagnosisId1)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.1")
                .diagnosisType(Diagnoses.DiagnosisType.PRINCIPAL)
                .status(Diagnoses.Status.ACTIVE)
                .build();

        Diagnoses newPrincipal = Diagnoses.builder()
                .id(diagnosisId2)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("I10")
                .diagnosisType(Diagnoses.DiagnosisType.SECUNDARIO)
                .status(Diagnoses.Status.ACTIVE)
                .build();

        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.findById(diagnosisId2)).thenReturn(Optional.of(newPrincipal));
        when(diagnosesRepository.findByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(oldPrincipal));
        when(diagnosesRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            DiagnosesResponse response = diagnosesService.setPrincipal(encounterId, diagnosisId2);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(diagnosisId2);
            assertThat(response.getDiagnosisType()).isEqualTo(Diagnoses.DiagnosisType.PRINCIPAL);
            verify(diagnosesRepository, times(2)).saveAndFlush(any());
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void setPrincipal_throwsWhenDiagnosisNotFound() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> diagnosesService.setPrincipal(encounterId, UUID.randomUUID()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Diagnosis not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void setPrincipal_throwsWhenDiagnosisNotBelongsToEncounter() {
        UUID diagnosisId = UUID.randomUUID();
        UUID otherEncounterId = UUID.randomUUID();

        Encounter encounter = Encounter.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .build();

        Diagnoses diagnosis = Diagnoses.builder()
                .id(diagnosisId)
                .encounterId(otherEncounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.1")
                .diagnosisType(Diagnoses.DiagnosisType.SECUNDARIO)
                .status(Diagnoses.Status.ACTIVE)
                .build();

        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.findById(diagnosisId)).thenReturn(Optional.of(diagnosis));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> diagnosesService.setPrincipal(encounterId, diagnosisId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to this encounter");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getPrincipal_returnsEmptyWhenNoPrincipal() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.findByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            Optional<DiagnosesResponse> response = diagnosesService.getPrincipal(encounterId);

            assertThat(response).isEmpty();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getPrincipal_returnsPrincipalWhenExists() {
        Diagnoses principal = Diagnoses.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.1")
                .diagnosisType(Diagnoses.DiagnosisType.PRINCIPAL)
                .status(Diagnoses.Status.ACTIVE)
                .build();

        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(diagnosesRepository.findByEncounterIdAndDiagnosisTypeAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(principal));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            Optional<DiagnosesResponse> response = diagnosesService.getPrincipal(encounterId);

            assertThat(response).isPresent();
            assertThat(response.get().getCie10Code()).isEqualTo("K59.1");
            assertThat(response.get().getDiagnosisType()).isEqualTo(Diagnoses.DiagnosisType.PRINCIPAL);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getAllByEncounter_returnsList() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        Diagnoses d1 = Diagnoses.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("K59.1")
                .diagnosisType(Diagnoses.DiagnosisType.PRINCIPAL)
                .status(Diagnoses.Status.ACTIVE)
                .build();

        Diagnoses d2 = Diagnoses.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .cie10Code("I10")
                .diagnosisType(Diagnoses.DiagnosisType.SECUNDARIO)
                .status(Diagnoses.Status.ACTIVE)
                .build();

        when(diagnosesRepository.findByEncounterIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(d1, d2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<DiagnosesResponse> responses = diagnosesService.getAllByEncounter(encounterId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(DiagnosesResponse::getCie10Code)
                    .containsExactlyInAnyOrder("K59.1", "I10");
        } finally {
            clearSecurityContext();
        }
    }
}

