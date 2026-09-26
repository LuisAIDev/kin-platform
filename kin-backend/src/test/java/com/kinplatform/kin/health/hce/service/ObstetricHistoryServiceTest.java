package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateObstetricHistoryRequest;
import com.kinplatform.kin.health.hce.dto.ObstetricHistoryResponse;
import com.kinplatform.kin.health.hce.entity.ObstetricHistory;
import com.kinplatform.kin.health.hce.repository.ObstetricHistoryRepository;
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
class ObstetricHistoryServiceTest {

    @Mock
    private ObstetricHistoryRepository obstetricHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ObstetricHistoryService obstetricHistoryService;

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
    void upsertHistory_createsIfNotExists() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(obstetricHistoryRepository.findByPatientId(any())).thenReturn(Optional.empty());
        when(obstetricHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateObstetricHistoryRequest request = CreateObstetricHistoryRequest.builder()
                    .patientId(patientId)
                    .gravida(3)
                    .para(2)
                    .abortions(1)
                    .ectopicPregnancies(0)
                    .stillbirths(0)
                    .livingChildren(2)
                    .currentPregnancy(false)
                    .lmp(LocalDate.now().minusWeeks(20))
                    .estimatedEdd(LocalDate.now().plusWeeks(20))
                    .gestationalWeeks(20)
                    .prenatalControls(5)
                    .breastfeedingStatus("EXCLUSIVE")
                    .breastfeedingDurationMonths(6)
                    .build();

            ObstetricHistoryResponse response = obstetricHistoryService.upsertHistory(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getGravida()).isEqualTo(3);
            assertThat(response.getPara()).isEqualTo(2);
            assertThat(response.getAbortions()).isEqualTo(1);
            assertThat(response.getCurrentPregnancy()).isEqualTo(false);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void upsertHistory_updatesIfExists() {
        ObstetricHistory existing = ObstetricHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .gravida(2)
                .para(1)
                .abortions(1)
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(obstetricHistoryRepository.findByPatientId(any())).thenReturn(Optional.of(existing));
        when(obstetricHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateObstetricHistoryRequest request = CreateObstetricHistoryRequest.builder()
                    .patientId(patientId)
                    .gravida(3)
                    .para(2)
                    .abortions(0)
                    .build();

            ObstetricHistoryResponse response = obstetricHistoryService.upsertHistory(request);

            assertThat(response).isNotNull();
            assertThat(response.getGravida()).isEqualTo(3);
            assertThat(response.getPara()).isEqualTo(2);
            assertThat(response.getAbortions()).isEqualTo(0);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void upsertHistory_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateObstetricHistoryRequest request = CreateObstetricHistoryRequest.builder()
                    .patientId(UUID.randomUUID())
                    .gravida(3)
                    .para(2)
                    .build();

            assertThatThrownBy(() -> obstetricHistoryService.upsertHistory(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Patient not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void upsertHistory_throwsWhenGravidaLessThanSum() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateObstetricHistoryRequest request = CreateObstetricHistoryRequest.builder()
                    .patientId(patientId)
                    .gravida(2) // Less than para + abortions = 3
                    .para(2)
                    .abortions(1)
                    .build();

            assertThatThrownBy(() -> obstetricHistoryService.upsertHistory(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Gravida cannot be less than sum");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void upsertHistory_withCurrentPregnancyTrue() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(obstetricHistoryRepository.findByPatientId(any())).thenReturn(Optional.empty());
        when(obstetricHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateObstetricHistoryRequest request = CreateObstetricHistoryRequest.builder()
                    .patientId(patientId)
                    .gravida(1)
                    .para(0)
                    .currentPregnancy(true)
                    .lmp(LocalDate.now().minusWeeks(10))
                    .estimatedEdd(LocalDate.now().plusWeeks(30))
                    .gestationalWeeks(10)
                    .build();

            ObstetricHistoryResponse response = obstetricHistoryService.upsertHistory(request);

            assertThat(response).isNotNull();
            assertThat(response.getCurrentPregnancy()).isEqualTo(true);
            assertThat(response.getGestationalWeeks()).isEqualTo(10);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void upsertHistory_validatesBreastfeedingStatus() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(obstetricHistoryRepository.findByPatientId(any())).thenReturn(Optional.empty());
        when(obstetricHistoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateObstetricHistoryRequest request = CreateObstetricHistoryRequest.builder()
                    .patientId(patientId)
                    .gravida(3)
                    .para(2)
                    .breastfeedingStatus("EXCLUSIVE")
                    .breastfeedingDurationMonths(6)
                    .build();

            ObstetricHistoryResponse response = obstetricHistoryService.upsertHistory(request);

            assertThat(response).isNotNull();
            assertThat(response.getBreastfeedingStatus()).isEqualTo("EXCLUSIVE");
            assertThat(response.getBreastfeedingDurationMonths()).isEqualTo(6);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatientId_returnsRecord() {
        ObstetricHistory history = ObstetricHistory.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .gravida(3)
                .para(2)
                .abortions(1)
                .ectopicPregnancies(0)
                .stillbirths(0)
                .livingChildren(2)
                .currentPregnancy(false)
                .build();

        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(obstetricHistoryRepository.findByPatientId(any())).thenReturn(Optional.of(history));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            Optional<ObstetricHistoryResponse> response = obstetricHistoryService.getByPatientId(patientId);

            assertThat(response).isPresent();
            assertThat(response.get().getGravida()).isEqualTo(3);
            assertThat(response.get().getPara()).isEqualTo(2);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatientId_returnsEmptyWhenNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(obstetricHistoryRepository.findByPatientId(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            Optional<ObstetricHistoryResponse> response = obstetricHistoryService.getByPatientId(patientId);

            assertThat(response).isEmpty();
        } finally {
            clearSecurityContext();
        }
    }
}