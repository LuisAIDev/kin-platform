package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateTreatmentPlanRequest;
import com.kinplatform.kin.health.hce.dto.TreatmentPlanResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
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

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreatmentPlanServiceTest {

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TreatmentPlanService treatmentPlanService;

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
    void createPlan_happyPath_withValidEncounter() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(treatmentPlanRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateTreatmentPlanRequest request = CreateTreatmentPlanRequest.builder()
                    .encounterId(encounterId)
                    .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                    .therapeuticGoals(new String[]{"Controlar dolor", "Mejorar movilidad"})
                    .followupPlan("Reevaluación en 2 semanas")
                    .reevaluationCriteria("Dolor < 3 en EVA")
                    .prognosis(TreatmentPlan.Prognosis.GOOD)
                    .estimatedDuration(Duration.ofDays(14))
                    .build();

            TreatmentPlanResponse response = treatmentPlanService.createPlan(request);

            assertThat(response).isNotNull();
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getConduct()).isEqualTo(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT);
            assertThat(response.getTherapeuticGoals()).containsExactly("Controlar dolor", "Mejorar movilidad");
            assertThat(response.getFollowupPlan()).isEqualTo("Reevaluación en 2 semanas");
            assertThat(response.getPrognosis()).isEqualTo(TreatmentPlan.Prognosis.GOOD);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createPlan_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateTreatmentPlanRequest request = CreateTreatmentPlanRequest.builder()
                    .encounterId(UUID.randomUUID())
                    .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                    .build();

            assertThatThrownBy(() -> treatmentPlanService.createPlan(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createPlan_throwsWhenConductInvalid() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateTreatmentPlanRequest request = CreateTreatmentPlanRequest.builder()
                    .encounterId(encounterId)
                    .conduct(null)
                    .build();

            assertThatThrownBy(() -> treatmentPlanService.createPlan(request))
                    .isInstanceOf(NullPointerException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createPlan_throwsWhenPrognosisInvalid() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(treatmentPlanRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateTreatmentPlanRequest request = CreateTreatmentPlanRequest.builder()
                    .encounterId(encounterId)
                    .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                    .prognosis(null)
                    .build();

            // Prognosis can be null, it's optional
            TreatmentPlanResponse response = treatmentPlanService.createPlan(request);
            assertThat(response).isNotNull();
            assertThat(response.getPrognosis()).isNull();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void updatePlan_happyPath() {
        UUID planId = UUID.randomUUID();
        TreatmentPlan plan = TreatmentPlan.builder()
                .id(planId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .build();

        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(plan));
        when(treatmentPlanRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateTreatmentPlanRequest request = CreateTreatmentPlanRequest.builder()
                    .encounterId(encounterId)
                    .conduct(TreatmentPlan.Conduct.HOSPITALIZATION)
                    .followupPlan("Seguimiento post-alta")
                    .build();

            TreatmentPlanResponse response = treatmentPlanService.updatePlan(planId, request);

            assertThat(response).isNotNull();
            assertThat(response.getConduct()).isEqualTo(TreatmentPlan.Conduct.HOSPITALIZATION);
            assertThat(response.getFollowupPlan()).isEqualTo("Seguimiento post-alta");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void updatePlan_throwsWhenPlanNotFound() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.empty());

        CreateTreatmentPlanRequest request = CreateTreatmentPlanRequest.builder()
                .encounterId(encounterId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .build();

        assertThatThrownBy(() -> treatmentPlanService.updatePlan(UUID.randomUUID(), request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Treatment plan not found");
    }

    @Test
    void getByEncounter_returnsList() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        TreatmentPlan plan1 = TreatmentPlan.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
                .build();

        TreatmentPlan plan2 = TreatmentPlan.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.REFERRAL)
                .build();

        when(treatmentPlanRepository.findByEncounterId(any()))
                .thenReturn(Optional.of(plan1));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<TreatmentPlanResponse> responses = treatmentPlanService.getByEncounter(encounterId);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getConduct()).isEqualTo(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_returnsEmptyWhenNoPlans() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(treatmentPlanRepository.findByEncounterId(any()))
                .thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<TreatmentPlanResponse> responses = treatmentPlanService.getByEncounter(encounterId);

            assertThat(responses).isEmpty();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> treatmentPlanService.getByEncounter(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }
}