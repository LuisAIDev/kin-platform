package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.AnamnesisResponse;
import com.kinplatform.kin.health.hce.dto.CreateAnamnesisRequest;
import com.kinplatform.kin.health.hce.dto.UpdateAnamnesisRequest;
import com.kinplatform.kin.health.hce.entity.Anamnesis;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.repository.AnamnesisRepository;
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
class AnamnesisServiceTest {

    @Mock
    private AnamnesisRepository anamnesisRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnamnesisService anamnesisService;

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
    void createAnamnesis_happyPath_withValidEncounter() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));
        when(anamnesisRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                    .encounterId(encounterId)
                    .onsetDatetime(Instant.now())
                    .evolutionDescription("Patient reports abdominal pain for 3 days")
                    .severitySelfReported(7)
                    .systemsReview("{\"gastrointestinal\": \"pain\", \"cardiovascular\": \"normal\"}")
                    .previousEpisodes(2)
                    .previousTreatments("Antacids")
                    .functionalImpact("Limits daily activities")
                    .build();

            AnamnesisResponse response = anamnesisService.createAnamnesis(request);

            assertThat(response).isNotNull();
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getSeveritySelfReported()).isEqualTo(7);
            assertThat(response.getSystemsReview()).isEqualTo("{\"gastrointestinal\": \"pain\", \"cardiovascular\": \"normal\"}");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createAnamnesis_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                    .encounterId(UUID.randomUUID())
                    .build();

            assertThatThrownBy(() -> anamnesisService.createAnamnesis(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createAnamnesis_throwsWhenSeveritySelfReportedGreaterThan10() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                    .encounterId(encounterId)
                    .severitySelfReported(11)
                    .build();

            assertThatThrownBy(() -> anamnesisService.createAnamnesis(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 10");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createAnamnesis_throwsWhenSeveritySelfReportedLessThan1() {
        when(encounterRepository.findById(any())).thenReturn(Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateAnamnesisRequest request = CreateAnamnesisRequest.builder()
                    .encounterId(encounterId)
                    .severitySelfReported(0)
                    .build();

            assertThatThrownBy(() -> anamnesisService.createAnamnesis(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 10");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounterId_returnsEmptyWhenNotFound() {
        when(anamnesisRepository.findByEncounterId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anamnesisService.getByEncounterId(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Anamnesis not found");
    }

    @Test
    void updateAnamnesis_throwsWhenIdNotFound() {
        when(anamnesisRepository.findById(any())).thenReturn(Optional.empty());

        UpdateAnamnesisRequest request = UpdateAnamnesisRequest.builder()
                .severitySelfReported(5)
                .build();

        assertThatThrownBy(() -> anamnesisService.updateAnamnesis(UUID.randomUUID(), request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Anamnesis not found");
    }

    @Test
    void updateAnamnesis_throwsWhenSeverityInvalid() {
        Anamnesis anamnesis = Anamnesis.builder()
                .id(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .build();

        when(anamnesisRepository.findById(any())).thenReturn(Optional.of(anamnesis));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            UpdateAnamnesisRequest request = UpdateAnamnesisRequest.builder()
                    .severitySelfReported(15)
                    .build();

            assertThatThrownBy(() -> anamnesisService.updateAnamnesis(UUID.randomUUID(), request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 10");
        } finally {
            clearSecurityContext();
        }
    }
}