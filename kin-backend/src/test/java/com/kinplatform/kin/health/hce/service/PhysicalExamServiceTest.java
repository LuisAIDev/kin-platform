package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreatePhysicalExamRequest;
import com.kinplatform.kin.health.hce.dto.PhysicalExamResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.PhysicalExamRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysicalExamServiceTest {

    @Mock
    private PhysicalExamRepository physicalExamRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PhysicalExamService physicalExamService;

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
    void recordExam_happyPath_withValidEncounter() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(physicalExamRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .bpSystolic(120)
                    .bpDiastolic(80)
                    .heartRate(72)
                    .respiratoryRate(16)
                    .temperature(BigDecimal.valueOf(36.5))
                    .spo2(98)
                    .weightKg(BigDecimal.valueOf(70))
                    .heightCm(BigDecimal.valueOf(175))
                    .glasgowScore(15)
                    .painScale(3)
                    .generalAppearance("Normal")
                    .cardiovascular("Regular rhythm, no murmurs")
                    .build();

            PhysicalExamResponse response = physicalExamService.recordExam(request);

            assertThat(response).isNotNull();
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getBpSystolic()).isEqualTo(120);
            assertThat(response.getHeartRate()).isEqualTo(72);
            assertThat(response.getTemperature()).isEqualTo(BigDecimal.valueOf(36.5));
            assertThat(response.getSpo2()).isEqualTo(98);
            assertThat(response.getBmi()).isEqualTo(BigDecimal.valueOf(22.86)); // 70 / (1.75^2) = 22.86
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(UUID.randomUUID())
                    .build();

            assertThatThrownBy(() -> physicalExamService.recordExam(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_throwsWhenBpSystolicGreaterThan300() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .bpSystolic(301)
                    .build();

            assertThatThrownBy(() -> physicalExamService.recordExam(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 50 and 300");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_throwsWhenHeartRateLessThan30() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .heartRate(29)
                    .build();

            assertThatThrownBy(() -> physicalExamService.recordExam(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 30 and 250");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_throwsWhenTemperatureGreaterThan45() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .temperature(BigDecimal.valueOf(45.1))
                    .build();

            assertThatThrownBy(() -> physicalExamService.recordExam(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 30.0 and 45.0");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_throwsWhenSpo2LessThan50() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .spo2(49)
                    .build();

            assertThatThrownBy(() -> physicalExamService.recordExam(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 50 and 100");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_throwsWhenGlasgowScoreGreaterThan15() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .glasgowScore(16)
                    .build();

            assertThatThrownBy(() -> physicalExamService.recordExam(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 3 and 15");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void recordExam_calculatesBmiCorrectly() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(physicalExamRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePhysicalExamRequest request = CreatePhysicalExamRequest.builder()
                    .encounterId(encounterId)
                    .weightKg(BigDecimal.valueOf(70))
                    .heightCm(BigDecimal.valueOf(175))
                    .build();

            PhysicalExamResponse response = physicalExamService.recordExam(request);

            // BMI = 70 / (1.75 * 1.75) = 70 / 3.0625 = 22.86
            assertThat(response.getBmi()).isEqualTo(BigDecimal.valueOf(22.86));
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_returnsPhysicalExam() {
        PhysicalExam exam = PhysicalExam.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .bpSystolic(120)
                .build();

        when(physicalExamRepository.findByEncounterId(any())).thenReturn(Optional.of(exam));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            PhysicalExamResponse response = physicalExamService.getByEncounter(encounterId);

            assertThat(response).isNotNull();
            assertThat(response.getBpSystolic()).isEqualTo(120);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_throwsWhenNotFound() {
        when(physicalExamRepository.findByEncounterId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> physicalExamService.getByEncounter(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Physical exam not found");
    }

    @Test
    void getLatestByPatient_returnsLatestExam() {
        PhysicalExam exam1 = PhysicalExam.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .recordedAt(java.time.Instant.now().minusSeconds(3600))
                .bpSystolic(120)
                .build();

        PhysicalExam exam2 = PhysicalExam.builder()
                .id(UUID.randomUUID())
                .encounterId(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(physicianId)
                .recordedAt(java.time.Instant.now())
                .bpSystolic(130)
                .build();

        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(any()))
                .thenReturn(List.of(exam2, exam1));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            PhysicalExamResponse response = physicalExamService.getLatestByPatient(patientId);

            assertThat(response).isNotNull();
            assertThat(response.getBpSystolic()).isEqualTo(130);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getLatestByPatient_throwsWhenNoExams() {
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(any()))
                .thenReturn(List.of());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> physicalExamService.getLatestByPatient(patientId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("No physical exams found");
        } finally {
            clearSecurityContext();
        }
    }
}

