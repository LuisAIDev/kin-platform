package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreatePatientHistoryRequest;
import com.kinplatform.kin.health.hce.dto.PatientHistoryResponse;
import com.kinplatform.kin.health.hce.entity.PatientHistory;
import com.kinplatform.kin.health.hce.entity.PatientHistory.HistoryType;
import com.kinplatform.kin.health.hce.entity.PatientHistory.Status;
import com.kinplatform.kin.health.hce.repository.PatientHistoryRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientHistoryServiceTest {

    @Mock
    private PatientHistoryRepository patientHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PatientHistoryService patientHistoryService;

    private UUID patientId;
    private UUID physicianId;
    private User patient;
    private User physician;

    @BeforeEach
    void setUp() {
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();

        patient = User.builder()
                .id(patientId)
                .email("patient@test.com")
                .fullName("Test Patient")
                .role(com.kinplatform.user.UserRole.PATIENT)
                .organizationId(UUID.randomUUID())
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
    void addHistory_happyPath_allergy() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(patientHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePatientHistoryRequest request = CreatePatientHistoryRequest.builder()
                    .patientId(patientId)
                    .historyType(HistoryType.ALLERGY)
                    .description("Penicillin allergy - rash and swelling")
                    .onsetDate(LocalDate.of(2020, 1, 15))
                    .status(Status.ACTIVE)
                    .severity(PatientHistory.Severity.MODERADO)
                    .notes("Avoid penicillin class antibiotics")
                    .details("{\"reaction\": \"rash and swelling\", \"confirmed\": true}")
                    .build();

            PatientHistoryResponse response = patientHistoryService.addHistory(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getHistoryType()).isEqualTo(HistoryType.ALLERGY);
            assertThat(response.getDescription()).isEqualTo("Penicillin allergy - rash and swelling");
            assertThat(response.getStatus()).isEqualTo(Status.ACTIVE);
            assertThat(response.getSeverity()).isEqualTo(PatientHistory.Severity.MODERADO);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addHistory_happyPath_surgery() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(patientHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePatientHistoryRequest request = CreatePatientHistoryRequest.builder()
                    .patientId(patientId)
                    .historyType(HistoryType.SURGERY)
                    .description("Appendectomy - laparoscopic")
                    .onsetDate(LocalDate.of(2018, 6, 10))
                    .resolutionDate(LocalDate.of(2018, 6, 12))
                    .status(Status.RESOLVED)
                    .severity(PatientHistory.Severity.LEVE)
                    .notes("No complications")
                    .details("{\"procedure\": \"laparoscopic appendectomy\", \"surgeon\": \"Dr. Smith\"}")
                    .build();

            PatientHistoryResponse response = patientHistoryService.addHistory(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getHistoryType()).isEqualTo(HistoryType.SURGERY);
            assertThat(response.getDescription()).isEqualTo("Appendectomy - laparoscopic");
            assertThat(response.getStatus()).isEqualTo(Status.RESOLVED);
            assertThat(response.getSeverity()).isEqualTo(PatientHistory.Severity.LEVE);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addHistory_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePatientHistoryRequest request = CreatePatientHistoryRequest.builder()
                    .patientId(UUID.randomUUID())
                    .historyType(HistoryType.ALLERGY)
                    .description("Test")
                    .build();

            assertThatThrownBy(() -> patientHistoryService.addHistory(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Patient not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addHistory_throwsWhenHistoryTypeInvalid() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreatePatientHistoryRequest request = CreatePatientHistoryRequest.builder()
                    .patientId(patientId)
                    .historyType(null)
                    .description("Test")
                    .build();

            assertThatThrownBy(() -> patientHistoryService.addHistory(request))
                    .isInstanceOf(NullPointerException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatientAndType_returnsFilteredList() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        PatientHistory allergy1 = PatientHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .historyType(HistoryType.ALLERGY)
                .description("Penicillin allergy")
                .status(Status.ACTIVE)
                .build();

        PatientHistory allergy2 = PatientHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .historyType(HistoryType.ALLERGY)
                .description("Latex allergy")
                .status(Status.ACTIVE)
                .build();

        when(patientHistoryRepository.findByPatientIdAndHistoryType(any(), any()))
                .thenReturn(List.of(allergy1, allergy2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<PatientHistoryResponse> responses = patientHistoryService.getByPatientAndType(patientId, HistoryType.ALLERGY);

            assertThat(responses).hasSize(2);
            assertThat(responses).allMatch(r -> r.getHistoryType() == HistoryType.ALLERGY);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getAllByPatient_returnsAllEntries() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        PatientHistory allergy = PatientHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .historyType(HistoryType.ALLERGY)
                .description("Penicillin allergy")
                .status(Status.ACTIVE)
                .build();

        PatientHistory surgery = PatientHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .historyType(HistoryType.SURGERY)
                .description("Appendectomy")
                .status(Status.RESOLVED)
                .build();

        when(patientHistoryRepository.findByPatientIdOrderByRecordedAtDesc(any()))
                .thenReturn(List.of(allergy, surgery));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<PatientHistoryResponse> responses = patientHistoryService.getAllByPatient(patientId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(PatientHistoryResponse::getHistoryType)
                    .containsExactlyInAnyOrder(HistoryType.ALLERGY, HistoryType.SURGERY);
        } finally {
            clearSecurityContext();
        }
    }
}