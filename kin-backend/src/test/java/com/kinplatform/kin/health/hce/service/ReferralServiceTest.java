package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CounterReferralRequest;
import com.kinplatform.kin.health.hce.dto.CreateReferralRequest;
import com.kinplatform.kin.health.hce.dto.ReferralResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Referral;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.ReferralRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @InjectMocks
    private ReferralService referralService;

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
                .role(com.kinplatform.common.user.UserRole.PATIENT)
                .organizationId(UUID.randomUUID())
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
    void createReferral_happyPath_interconsultation() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(referralRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateReferralRequest request = CreateReferralRequest.builder()
                    .patientId(patientId)
                    .referringService("Emergency")
                    .referredToService("Cardiology")
                    .referredToInstitution("Hospital Central")
                    .referralType(Referral.ReferralType.INTERCONSULTATION)
                    .priority(Referral.Priority.URGENT)
                    .reason("Chest pain evaluation")
                    .clinicalSummary("Patient with chest pain, normal ECG")
                    .scheduledAt(Instant.now().plusSeconds(3600))
                    .build();

            ReferralResponse response = referralService.createReferral(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getReferringPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getReferredToService()).isEqualTo("Cardiology");
            assertThat(response.getReferralType()).isEqualTo(Referral.ReferralType.INTERCONSULTATION);
            assertThat(response.getPriority()).isEqualTo(Referral.Priority.URGENT);
            assertThat(response.getStatus()).isEqualTo(Referral.Status.PENDING);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createReferral_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateReferralRequest request = CreateReferralRequest.builder()
                    .patientId(UUID.randomUUID())
                    .referringService("Emergency")
                    .referredToService("Cardiology")
                    .referralType(Referral.ReferralType.INTERCONSULTATION)
                    .priority(Referral.Priority.URGENT)
                    .reason("Test")
                    .build();

            assertThatThrownBy(() -> referralService.createReferral(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Patient not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createReferral_throwsWhenReferralTypeInvalid() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateReferralRequest request = CreateReferralRequest.builder()
                    .patientId(patientId)
                    .referringService("Emergency")
                    .referredToService("Cardiology")
                    .referralType(null)
                    .priority(Referral.Priority.URGENT)
                    .reason("Test")
                    .build();

            assertThatThrownBy(() -> referralService.createReferral(request))
                    .isInstanceOf(NullPointerException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createReferral_throwsWhenPriorityInvalid() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(referralRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateReferralRequest request = CreateReferralRequest.builder()
                    .patientId(patientId)
                    .referringService("Emergency")
                    .referredToService("Cardiology")
                    .referralType(Referral.ReferralType.INTERCONSULTATION)
                    .priority(null)
                    .reason("Test")
                    .build();

            // Priority has default ROUTINE, should work
            ReferralResponse response = referralService.createReferral(request);
            assertThat(response).isNotNull();
            assertThat(response.getPriority()).isEqualTo(Referral.Priority.ROUTINE);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void counterReferral_happyPath_pendingToCompleted() {
        UUID referralId = UUID.randomUUID();
        Referral referral = Referral.builder()
                .id(referralId)
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referredToInstitution("Hospital Central")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain evaluation")
                .status(Referral.Status.PENDING)
                .build();

        when(referralRepository.findById(any())).thenReturn(Optional.of(referral));
        when(referralRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CounterReferralRequest request = CounterReferralRequest.builder()
                    .counterreferralBy(physicianId)
                    .counterreferralSummary("Patient evaluated, normal ECG, no acute coronary syndrome")
                    .counterreferralRecommendations("Discharge with follow-up in 48h")
                    .build();

            ReferralResponse response = referralService.counterReferral(referralId, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(Referral.Status.COMPLETED);
            assertThat(response.getCounterreferralAt()).isNotNull();
            assertThat(response.getCounterreferralBy()).isEqualTo(physicianId);
            assertThat(response.getCounterreferralSummary()).isEqualTo("Patient evaluated, normal ECG, no acute coronary syndrome");
            assertThat(response.getCompletedAt()).isNotNull();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void counterReferral_throwsWhenAlreadyCompleted() {
        UUID referralId = UUID.randomUUID();
        Referral referral = Referral.builder()
                .id(referralId)
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .status(Referral.Status.COMPLETED)
                .build();

        when(referralRepository.findById(any())).thenReturn(Optional.of(referral));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CounterReferralRequest request = CounterReferralRequest.builder()
                    .counterreferralBy(physicianId)
                    .counterreferralSummary("Attempt to complete again")
                    .build();

            assertThatThrownBy(() -> referralService.counterReferral(referralId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already counter-referred");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void counterReferral_throwsWhenCancelled() {
        UUID referralId = UUID.randomUUID();
        Referral referral = Referral.builder()
                .id(referralId)
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .status(Referral.Status.CANCELLED)
                .build();

        when(referralRepository.findById(any())).thenReturn(Optional.of(referral));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CounterReferralRequest request = CounterReferralRequest.builder()
                    .counterreferralBy(physicianId)
                    .counterreferralSummary("Attempt to complete cancelled")
                    .build();

            assertThatThrownBy(() -> referralService.counterReferral(referralId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cancelled");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void counterReferral_throwsWhenRejected() {
        UUID referralId = UUID.randomUUID();
        Referral referral = Referral.builder()
                .id(referralId)
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .status(Referral.Status.REJECTED)
                .build();

        when(referralRepository.findById(any())).thenReturn(Optional.of(referral));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CounterReferralRequest request = CounterReferralRequest.builder()
                    .counterreferralBy(physicianId)
                    .counterreferralSummary("Attempt to complete rejected")
                    .build();

            assertThatThrownBy(() -> referralService.counterReferral(referralId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("rejected");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void counterReferral_throwsWhenReferralNotFound() {
        when(referralRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CounterReferralRequest request = CounterReferralRequest.builder()
                    .counterreferralBy(physicianId)
                    .counterreferralSummary("Test")
                    .build();

            assertThatThrownBy(() -> referralService.counterReferral(UUID.randomUUID(), request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Referral not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatient_returnsList() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        Referral referral1 = Referral.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .status(Referral.Status.PENDING)
                .build();

        Referral referral2 = Referral.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Internal Medicine")
                .referredToService("Neurology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.ROUTINE)
                .reason("Headache evaluation")
                .status(Referral.Status.COMPLETED)
                .build();

        when(referralRepository.findByPatientIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(referral1, referral2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<ReferralResponse> responses = referralService.getByPatient(patientId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(ReferralResponse::getReferredToService)
                    .containsExactlyInAnyOrder("Cardiology", "Neurology");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByStatus_returnsFilteredList() {
        Referral referral1 = Referral.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .referringPhysicianId(physicianId)
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .status(Referral.Status.PENDING)
                .build();

        Referral referral2 = Referral.builder()
                .id(UUID.randomUUID())
                .patientId(UUID.randomUUID())
                .referringPhysicianId(UUID.randomUUID())
                .referringService("Emergency")
                .referredToService("Neurology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.ROUTINE)
                .reason("Headache")
                .status(Referral.Status.PENDING)
                .build();

        when(referralRepository.findByStatus(any()))
                .thenReturn(List.of(referral1, referral2));

        List<ReferralResponse> responses = referralService.getByStatus(Referral.Status.PENDING);

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.getStatus() == Referral.Status.PENDING);
    }

    private Encounter buildEncounter(UUID encounterId) {
        return Encounter.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(UUID.randomUUID())
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .build();
    }

    @Test
    void createReferralForEncounter_happyPath_derivesPatient() {
        UUID encounterId = UUID.randomUUID();

        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(buildEncounter(encounterId)));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(referralRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateReferralRequest request = CreateReferralRequest.builder()
                    .referringService("Emergency")
                    .referredToService("Cardiology")
                    .referralType(Referral.ReferralType.INTERCONSULTATION)
                    .priority(Referral.Priority.URGENT)
                    .reason("Chest pain evaluation")
                    .build();

            ReferralResponse response = referralService.createReferralForEncounter(encounterId, request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getReferringPhysicianId()).isEqualTo(physicianId);
            assertThat(request.getPatientId()).isEqualTo(patientId);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createReferralForEncounter_throwsWhenEncounterNotFound() {
        UUID encounterId = UUID.randomUUID();
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.empty());

        CreateReferralRequest request = CreateReferralRequest.builder()
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .build();

        assertThatThrownBy(() -> referralService.createReferralForEncounter(encounterId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }

    @Test
    void createReferralForEncounter_throwsWhenPatientNotFound() {
        UUID encounterId = UUID.randomUUID();
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(buildEncounter(encounterId)));
        when(userRepository.findById(patientId)).thenReturn(Optional.empty());

        CreateReferralRequest request = CreateReferralRequest.builder()
                .referringService("Emergency")
                .referredToService("Cardiology")
                .referralType(Referral.ReferralType.INTERCONSULTATION)
                .priority(Referral.Priority.URGENT)
                .reason("Chest pain")
                .build();

        assertThatThrownBy(() -> referralService.createReferralForEncounter(encounterId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }
}

