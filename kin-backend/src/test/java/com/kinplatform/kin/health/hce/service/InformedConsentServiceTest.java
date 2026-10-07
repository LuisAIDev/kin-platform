package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateInformedConsentRequest;
import com.kinplatform.kin.health.hce.dto.InformedConsentResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.InformedConsent;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.InformedConsentRepository;
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
class InformedConsentServiceTest {

    @Mock
    private InformedConsentRepository informedConsentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @InjectMocks
    private InformedConsentService informedConsentService;

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
    void createConsent_happyPath_surgical() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(informedConsentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                    .patientId(patientId)
                    .procedureName("Appendectomy")
                    .procedureCupsCode("35.0")
                    .consentType(InformedConsent.ConsentType.SURGICAL)
                    .documentVersion("1.0")
                    .documentStorageKey("consent/2024/appendectomy.pdf")
                    .patientSignatureHash("sha256:patient_signature")
                    .witness1Name("Witness One")
                    .witness1Document("12345678")
                    .witness1SignatureHash("sha256:witness1")
                    .witness2Name("Witness Two")
                    .witness2Document("87654321")
                    .witness2SignatureHash("sha256:witness2")
                    .physicianSignatureHash("sha256:physician")
                    .build();

            InformedConsentResponse response = informedConsentService.createConsent(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getProcedureName()).isEqualTo("Appendectomy");
            assertThat(response.getConsentType()).isEqualTo(InformedConsent.ConsentType.SURGICAL);
            assertThat(response.getStatus()).isEqualTo(InformedConsent.Status.VALID);
            assertThat(response.getSignedAt()).isNotNull();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createConsent_happyPath_anesthesia() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));
        when(informedConsentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                    .patientId(patientId)
                    .procedureName("General Anesthesia")
                    .consentType(InformedConsent.ConsentType.ANESTHESIA)
                    .documentVersion("1.0")
                    .build();

            InformedConsentResponse response = informedConsentService.createConsent(request);

            assertThat(response).isNotNull();
            assertThat(response.getConsentType()).isEqualTo(InformedConsent.ConsentType.ANESTHESIA);
            assertThat(response.getStatus()).isEqualTo(InformedConsent.Status.VALID);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createConsent_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                    .patientId(UUID.randomUUID())
                    .procedureName("Test Procedure")
                    .consentType(InformedConsent.ConsentType.SURGICAL)
                    .documentVersion("1.0")
                    .build();

            assertThatThrownBy(() -> informedConsentService.createConsent(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Patient not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createConsent_throwsWhenConsentTypeInvalid() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                    .patientId(patientId)
                    .procedureName("Test Procedure")
                    .consentType(null)
                    .documentVersion("1.0")
                    .build();

            assertThatThrownBy(() -> informedConsentService.createConsent(request))
                    .isInstanceOf(NullPointerException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void revokeConsent_happyPath_validToRevoked() {
        UUID consentId = UUID.randomUUID();
        InformedConsent consent = InformedConsent.builder()
                .id(consentId)
                .patientId(patientId)
                .physicianId(physicianId)
                .procedureName("Appendectomy")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .status(InformedConsent.Status.VALID)
                .signedAt(java.time.Instant.now().minusSeconds(3600))
                .build();

        when(informedConsentRepository.findById(any())).thenReturn(Optional.of(consent));
        when(informedConsentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            InformedConsentResponse response = informedConsentService.revokeConsent(consentId, "Patient changed mind");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(InformedConsent.Status.REVOKED);
            assertThat(response.getRevokedAt()).isNotNull();
            assertThat(response.getRevocationReason()).isEqualTo("Patient changed mind");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void revokeConsent_throwsWhenAlreadyRevoked() {
        UUID consentId = UUID.randomUUID();
        InformedConsent consent = InformedConsent.builder()
                .id(consentId)
                .patientId(patientId)
                .physicianId(physicianId)
                .procedureName("Appendectomy")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .status(InformedConsent.Status.REVOKED)
                .build();

        when(informedConsentRepository.findById(any())).thenReturn(Optional.of(consent));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> informedConsentService.revokeConsent(consentId, "Duplicate revocation"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already revoked");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void revokeConsent_throwsWhenExpired() {
        UUID consentId = UUID.randomUUID();
        InformedConsent consent = InformedConsent.builder()
                .id(consentId)
                .patientId(patientId)
                .physicianId(physicianId)
                .procedureName("Appendectomy")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .status(InformedConsent.Status.EXPIRED)
                .build();

        when(informedConsentRepository.findById(any())).thenReturn(Optional.of(consent));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> informedConsentService.revokeConsent(consentId, "Attempt to revoke expired"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expired");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void revokeConsent_throwsWhenConsentNotFound() {
        when(informedConsentRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> informedConsentService.revokeConsent(UUID.randomUUID(), "Reason"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Informed consent not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatient_returnsList() {
        when(userRepository.findById(any())).thenReturn(Optional.of(patient));

        InformedConsent consent1 = InformedConsent.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(physicianId)
                .procedureName("Appendectomy")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .status(InformedConsent.Status.VALID)
                .signedAt(java.time.Instant.now().minusSeconds(3600))
                .build();

        InformedConsent consent2 = InformedConsent.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .physicianId(physicianId)
                .procedureName("Anesthesia")
                .consentType(InformedConsent.ConsentType.ANESTHESIA)
                .status(InformedConsent.Status.VALID)
                .signedAt(java.time.Instant.now())
                .build();

        when(informedConsentRepository.findByPatientIdOrderBySignedAtDesc(any()))
                .thenReturn(List.of(consent1, consent2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<InformedConsentResponse> responses = informedConsentService.getByPatient(patientId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(InformedConsentResponse::getProcedureName)
                    .containsExactlyInAnyOrder("Appendectomy", "Anesthesia");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatient_throwsWhenPatientNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> informedConsentService.getByPatient(UUID.randomUUID()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Patient not found");
        } finally {
            clearSecurityContext();
        }
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
    void createConsentForEncounter_happyPath_derivesPatient() {
        UUID encounterId = UUID.randomUUID();

        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(buildEncounter(encounterId)));
        when(userRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(informedConsentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                    .procedureName("Appendectomy")
                    .consentType(InformedConsent.ConsentType.SURGICAL)
                    .documentVersion("1.0")
                    .build();

            InformedConsentResponse response = informedConsentService.createConsentForEncounter(encounterId, request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(request.getPatientId()).isEqualTo(patientId);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void createConsentForEncounter_throwsWhenEncounterNotFound() {
        UUID encounterId = UUID.randomUUID();
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.empty());

        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .procedureName("Appendectomy")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .documentVersion("1.0")
                .build();

        assertThatThrownBy(() -> informedConsentService.createConsentForEncounter(encounterId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }

    @Test
    void createConsentForEncounter_throwsWhenPatientNotFound() {
        UUID encounterId = UUID.randomUUID();
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(buildEncounter(encounterId)));
        when(userRepository.findById(patientId)).thenReturn(Optional.empty());

        CreateInformedConsentRequest request = CreateInformedConsentRequest.builder()
                .procedureName("Appendectomy")
                .consentType(InformedConsent.ConsentType.SURGICAL)
                .documentVersion("1.0")
                .build();

        assertThatThrownBy(() -> informedConsentService.createConsentForEncounter(encounterId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Patient not found");
    }
}

