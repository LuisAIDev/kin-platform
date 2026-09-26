package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateDischargeSummaryRequest;
import com.kinplatform.kin.health.hce.dto.DischargeSummaryResponse;
import com.kinplatform.kin.health.hce.entity.DischargeSummary;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.repository.DischargeSummaryRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
class DischargeSummaryServiceTest {

    @Mock
    private DischargeSummaryRepository dischargeSummaryRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DischargeSummaryService dischargeSummaryService;

    private UUID encounterId;
    private UUID admissionId;
    private UUID patientId;
    private UUID physicianId;
    private Encounter encounter;
    private User physician;

    @BeforeEach
    void setUp() {
        encounterId = UUID.randomUUID();
        admissionId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();

        encounter = Encounter.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(UUID.randomUUID())
                .encounterType(EncounterType.INPATIENT)
                .status(EncounterStatus.IN_PROGRESS)
                .build();

        physician = User.builder()
                .id(physicianId)
                .email("physician@test.com")
                .fullName("Dr. Test")
                .role(com.kinplatform.user.UserRole.PHYSICIAN)
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
    void createDischargeSummary_happyPath() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(dischargeSummaryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            Instant admissionDate = Instant.now().minusSeconds(86400); // 1 day ago
            Instant dischargeDate = Instant.now();

            CreateDischargeSummaryRequest request = CreateDischargeSummaryRequest.builder()
                    .encounterId(encounterId)
                    .admissionId(admissionId)
                    .admissionDate(admissionDate)
                    .dischargeDate(dischargeDate)
                    .dischargeDiagnosisCie10("K35.2")
                    .admissionDiagnosisCie10("K35.2")
                    .clinicalSummary("Patient admitted with acute appendicitis, underwent laparoscopic appendectomy. Recovery uneventful.")
                    .dischargeCondition(com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition.IMPROVED)
                    .dischargeDisposition("Home with follow-up")
                    .generalRecommendations("Wound care, avoid heavy lifting for 2 weeks")
                    .build();

            DischargeSummaryResponse response = dischargeSummaryService.createDischargeSummary(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getAttendingPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getDischargeDiagnosisCie10()).isEqualTo("K35.2");
            assertThat(response.getDischargeCondition()).isEqualTo(com.kinplatform.kin.health.hce.entity.DischargeSummary.DischargeCondition.IMPROVED);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createDischargeSummary_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateDischargeSummaryRequest request = CreateDischargeSummaryRequest.builder()
                    .encounterId(UUID.randomUUID())
                    .admissionId(admissionId)
                    .admissionDate(Instant.now().minusSeconds(86400))
                    .dischargeDate(Instant.now())
                    .dischargeDiagnosisCie10("K35.2")
                    .clinicalSummary("Test")
                    .build();

            assertThatThrownBy(() -> dischargeSummaryService.createDischargeSummary(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createDischargeSummary_throwsWhenDischargeDateBeforeAdmissionDate() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            Instant dischargeDate = Instant.now();
            Instant admissionDate = Instant.now().plusSeconds(86400); // Discharge before admission

            CreateDischargeSummaryRequest request = CreateDischargeSummaryRequest.builder()
                    .encounterId(encounterId)
                    .admissionId(admissionId)
                    .admissionDate(admissionDate)
                    .dischargeDate(dischargeDate)
                    .dischargeDiagnosisCie10("K35.2")
                    .clinicalSummary("Test")
                    .build();

            assertThatThrownBy(() -> dischargeSummaryService.createDischargeSummary(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Discharge date cannot be before admission date");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createDischargeSummary_throwsWhenDischargeDiagnosisCie10Null() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateDischargeSummaryRequest request = CreateDischargeSummaryRequest.builder()
                    .encounterId(encounterId)
                    .admissionId(admissionId)
                    .admissionDate(Instant.now().minusSeconds(86400))
                    .dischargeDate(Instant.now())
                    .dischargeDiagnosisCie10(null)
                    .clinicalSummary("Test")
                    .build();

            assertThatThrownBy(() -> dischargeSummaryService.createDischargeSummary(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Discharge diagnosis CIE-10 is required");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void signDischargeSummary_happyPath() {
        DischargeSummary summary = DischargeSummary.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attendingPhysicianId(physicianId)
                .admissionDate(Instant.now().minusSeconds(86400))
                .dischargeDate(Instant.now())
                .dischargeDiagnosisCie10("K35.2")
                .clinicalSummary("Test")
                .signedAt(null)
                .physicianSignatureHash(null)
                .build();

        when(dischargeSummaryRepository.findById(any())).thenReturn(java.util.Optional.of(summary));
        when(dischargeSummaryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            DischargeSummaryResponse response = dischargeSummaryService.signDischargeSummary(summary.getId(), physicianId);

            assertThat(response).isNotNull();
            assertThat(response.getSignedAt()).isNotNull();
            assertThat(response.getPhysicianSignatureHash()).isNotNull();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void signDischargeSummary_throwsWhenPhysicianIdMismatch() {
        DischargeSummary summary = DischargeSummary.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attendingPhysicianId(UUID.randomUUID()) // Different physician
                .build();

        when(dischargeSummaryRepository.findById(any())).thenReturn(java.util.Optional.of(summary));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> dischargeSummaryService.signDischargeSummary(summary.getId(), physicianId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void signDischargeSummary_throwsWhenAlreadySigned() {
        DischargeSummary summary = DischargeSummary.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .attendingPhysicianId(physicianId)
                .signedAt(java.time.Instant.now())
                .physicianSignatureHash("already_signed")
                .build();

        when(dischargeSummaryRepository.findById(any())).thenReturn(java.util.Optional.of(summary));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> dischargeSummaryService.signDischargeSummary(summary.getId(), physicianId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already signed");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_returnsList() {
        // Note: getByEncounter uses findByAdmissionId which expects an admissionId
        // This test just verifies the method exists and can be called
        when(dischargeSummaryRepository.findByAdmissionId(any())).thenReturn(java.util.Optional.empty());

        var response = dischargeSummaryService.getByEncounter(UUID.randomUUID());

        assertThat(response).isEmpty();
    }
}