package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateSurgicalHistoryRequest;
import com.kinplatform.kin.health.hce.dto.SurgicalHistoryResponse;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory.AnesthesiaType;
import com.kinplatform.kin.health.hce.entity.SurgicalHistory.SurgeryType;
import com.kinplatform.kin.health.hce.repository.SurgicalHistoryRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurgicalHistoryServiceTest {

    @Mock
    private SurgicalHistoryRepository surgicalHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SurgicalHistoryService surgicalHistoryService;

    private UUID patientId;
    private UUID physicianId;
    private com.kinplatform.user.User patient;
    private com.kinplatform.user.User physician;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();

        patient = com.kinplatform.user.User.builder()
                .id(patientId)
                .email("patient@test.com")
                .fullName("Test Patient")
                .role(com.kinplatform.user.UserRole.PATIENT)
                .organizationId(UUID.randomUUID())
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
    void addSurgery_happyPath() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(surgicalHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateSurgicalHistoryRequest request = CreateSurgicalHistoryRequest.builder()
                    .patientId(patientId)
                    .surgeryDate(LocalDate.now().minusDays(10))
                    .procedureCupsCode("35.0")
                    .procedureCupsDescription("Laparoscopic appendectomy")
                    .diagnosisCie10("K35.2")
                    .diagnosisDescription("Acute appendicitis")
                    .surgeryType(SurgeryType.URGENT)
                    .anesthesiaType(AnesthesiaType.GENERAL)
                    .asaClassification(2)
                    .durationMinutes(45)
                    .estimatedBloodLossMl(50)
                    .surgeonId(physicianId)
                    .institution("Hospital Central")
                    .notes("Uneventful recovery")
                    .build();

            SurgicalHistoryResponse response = surgicalHistoryService.addSurgery(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getProcedureCupsCode()).isEqualTo("35.0");
            assertThat(response.getSurgeryType()).isEqualTo(SurgeryType.URGENT);
            assertThat(response.getAnesthesiaType()).isEqualTo(AnesthesiaType.GENERAL);
            assertThat(response.getAsaClassification()).isEqualTo(2);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addSurgery_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateSurgicalHistoryRequest request = CreateSurgicalHistoryRequest.builder()
                    .patientId(UUID.randomUUID())
                    .surgeryDate(LocalDate.now().minusDays(10))
                    .procedureCupsCode("35.0")
                    .build();

            assertThatThrownBy(() -> surgicalHistoryService.addSurgery(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Patient not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addSurgery_throwsWhenProcedureCupsCodeNull() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateSurgicalHistoryRequest request = CreateSurgicalHistoryRequest.builder()
                    .patientId(patientId)
                    .surgeryDate(LocalDate.now().minusDays(10))
                    .procedureCupsCode(null)
                    .build();

            assertThatThrownBy(() -> surgicalHistoryService.addSurgery(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Procedure CUPS code is required");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addSurgery_throwsWhenSurgeryDateFuture() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateSurgicalHistoryRequest request = CreateSurgicalHistoryRequest.builder()
                    .patientId(patientId)
                    .surgeryDate(LocalDate.now().plusDays(1))
                    .procedureCupsCode("35.0")
                    .build();

            assertThatThrownBy(() -> surgicalHistoryService.addSurgery(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be in the future");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addSurgery_throwsWhenAsaClassificationGreaterThan6() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateSurgicalHistoryRequest request = CreateSurgicalHistoryRequest.builder()
                    .patientId(patientId)
                    .surgeryDate(LocalDate.now().minusDays(10))
                    .procedureCupsCode("35.0")
                    .asaClassification(7)
                    .build();

            assertThatThrownBy(() -> surgicalHistoryService.addSurgery(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 6");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addSurgery_withAnesthesiaTypeGeneral() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(surgicalHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateSurgicalHistoryRequest request = CreateSurgicalHistoryRequest.builder()
                    .patientId(patientId)
                    .surgeryDate(LocalDate.now().minusDays(10))
                    .procedureCupsCode("35.0")
                    .anesthesiaType(AnesthesiaType.GENERAL)
                    .build();

            SurgicalHistoryResponse response = surgicalHistoryService.addSurgery(request);

            assertThat(response).isNotNull();
            assertThat(response.getAnesthesiaType()).isEqualTo(AnesthesiaType.GENERAL);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatient_returnsList() {
        SurgicalHistory surgery1 = SurgicalHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .surgeryDate(LocalDate.now().minusDays(10))
                .procedureCupsCode("35.0")
                .surgeryType(SurgeryType.URGENT)
                .build();

        SurgicalHistory surgery2 = SurgicalHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .surgeryDate(LocalDate.now().minusDays(5))
                .procedureCupsCode("35.1")
                .surgeryType(SurgeryType.ELECTIVE)
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(surgicalHistoryRepository.findByPatientIdOrderBySurgeryDateDesc(any()))
                .thenReturn(List.of(surgery1, surgery2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<SurgicalHistoryResponse> responses = surgicalHistoryService.getByPatient(patientId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(SurgicalHistoryResponse::getProcedureCupsCode)
                    .containsExactlyInAnyOrder("35.0", "35.1");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByCupsCode_returnsFilteredList() {
        SurgicalHistory surgery1 = SurgicalHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .surgeryDate(LocalDate.now().minusDays(10))
                .procedureCupsCode("35.0")
                .surgeryType(SurgeryType.URGENT)
                .build();

        SurgicalHistory surgery2 = SurgicalHistory.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .surgeryDate(LocalDate.now().minusDays(5))
                .procedureCupsCode("35.0")
                .surgeryType(SurgeryType.ELECTIVE)
                .build();

        when(surgicalHistoryRepository.findByProcedureCupsCode(any()))
                .thenReturn(List.of(surgery1, surgery2));

        List<SurgicalHistoryResponse> responses = surgicalHistoryService.getByCupsCode("35.0");

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.getProcedureCupsCode().equals("35.0"));
    }

    @Test
    void getByPatient_returnsEmptyWhenNoSurgeries() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(surgicalHistoryRepository.findByPatientIdOrderBySurgeryDateDesc(any()))
                .thenReturn(List.of());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<SurgicalHistoryResponse> responses = surgicalHistoryService.getByPatient(patientId);

            assertThat(responses).isEmpty();
        } finally {
            clearSecurityContext();
        }
    }
}