package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateClinicalAttachmentRequest;
import com.kinplatform.kin.health.hce.dto.ClinicalAttachmentResponse;
import com.kinplatform.kin.health.hce.entity.ClinicalAttachment;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterType;
import com.kinplatform.kin.health.hce.repository.ClinicalAttachmentRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalAttachmentServiceTest {

    @Mock
    private ClinicalAttachmentRepository clinicalAttachmentRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClinicalAttachmentService clinicalAttachmentService;

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
    void uploadAttachment_happyPath_labResult() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(clinicalAttachmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateClinicalAttachmentRequest request = CreateClinicalAttachmentRequest.builder()
                    .encounterId(encounterId)
                    .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                    .loincCode("718-7")
                    .loincDisplay("Hemoglobin")
                    .resultValue(java.math.BigDecimal.valueOf(14.5))
                    .resultUnit("g/dL")
                    .referenceRangeLow(java.math.BigDecimal.valueOf(13.5))
                    .referenceRangeHigh(java.math.BigDecimal.valueOf(17.5))
                    .referenceRangeText("13.5-17.5 g/dL")
                    .abnormalFlag(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag.NORMAL)
                    .performedAt(Instant.now().minusSeconds(3600))
                    .build();

            ClinicalAttachmentResponse response = clinicalAttachmentService.uploadAttachment(request);

            assertThat(response).isNotNull();
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getAttachmentType()).isEqualTo(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT);
            assertThat(response.getLoincCode()).isEqualTo("718-7");
            assertThat(response.getResultValue()).isEqualByComparingTo("14.5");
            assertThat(response.getAbnormalFlag()).isEqualTo(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AbnormalFlag.NORMAL);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void uploadAttachment_happyPath_imaging() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(clinicalAttachmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateClinicalAttachmentRequest request = CreateClinicalAttachmentRequest.builder()
                    .encounterId(encounterId)
                    .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING)
                    .dicomStudyUid("1.2.840.113619.2.55.3.2831164357.123.1234")
                    .dicomSeriesUid("1.2.840.113619.2.55.3.2831164357.123.1234.1")
                    .dicomModality(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.DicomModality.CT)
                    .performedAt(Instant.now().minusSeconds(7200))
                    .build();

            ClinicalAttachmentResponse response = clinicalAttachmentService.uploadAttachment(request);

            assertThat(response).isNotNull();
            assertThat(response.getAttachmentType()).isEqualTo(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING);
            assertThat(response.getDicomStudyUid()).isEqualTo("1.2.840.113619.2.55.3.2831164357.123.1234");
            assertThat(response.getDicomModality()).isEqualTo(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.DicomModality.CT);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void uploadAttachment_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateClinicalAttachmentRequest request = CreateClinicalAttachmentRequest.builder()
                    .encounterId(UUID.randomUUID())
                    .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                    .performedAt(Instant.now().minusSeconds(3600))
                    .build();

            assertThatThrownBy(() -> clinicalAttachmentService.uploadAttachment(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Encounter not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void uploadAttachment_throwsWhenAttachmentTypeInvalid() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateClinicalAttachmentRequest request = CreateClinicalAttachmentRequest.builder()
                    .encounterId(encounterId)
                    .attachmentType(null)
                    .performedAt(Instant.now().minusSeconds(3600))
                    .build();

            assertThatThrownBy(() -> clinicalAttachmentService.uploadAttachment(request))
                    .isInstanceOf(NullPointerException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void uploadAttachment_throwsWhenPerformedAtFuture() {
        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateClinicalAttachmentRequest request = CreateClinicalAttachmentRequest.builder()
                    .encounterId(encounterId)
                    .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                    .performedAt(Instant.now().plusSeconds(3600)) // Future date
                    .build();

            assertThatThrownBy(() -> clinicalAttachmentService.uploadAttachment(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Performed at cannot be in the future");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_returnsList() {
        ClinicalAttachment attachment1 = ClinicalAttachment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                .performedAt(Instant.now().minusSeconds(3600))
                .build();

        ClinicalAttachment attachment2 = ClinicalAttachment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING)
                .performedAt(Instant.now().minusSeconds(7200))
                .build();

        when(encounterRepository.findById(any())).thenReturn(java.util.Optional.of(encounter));
        when(clinicalAttachmentRepository.findByEncounterIdOrderByPerformedAtDesc(any()))
                .thenReturn(List.of(attachment1, attachment2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<ClinicalAttachmentResponse> responses = clinicalAttachmentService.getByEncounter(encounterId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(ClinicalAttachmentResponse::getAttachmentType)
                    .containsExactlyInAnyOrder(
                            com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT,
                            com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByPatientAndType_returnsFilteredList() {
        ClinicalAttachment attachment1 = ClinicalAttachment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                .performedAt(Instant.now().minusSeconds(3600))
                .build();

        ClinicalAttachment attachment2 = ClinicalAttachment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                .performedAt(Instant.now().minusSeconds(7200))
                .build();

        when(clinicalAttachmentRepository.findByPatientIdAndTypeOrderByPerformedAtDesc(any(), any()))
                .thenReturn(List.of(attachment1, attachment2));

        List<ClinicalAttachmentResponse> responses = clinicalAttachmentService.getByPatientAndType(patientId, com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT);

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(r -> r.getAttachmentType() == com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT);
    }

    @Test
    void getByPatientAndType_returnsEmptyWhenNoType() {
        when(clinicalAttachmentRepository.findByPatientIdAndTypeOrderByPerformedAtDesc(any(), any()))
                .thenReturn(List.of());

        List<ClinicalAttachmentResponse> responses = clinicalAttachmentService.getByPatientAndType(patientId, com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.PATHOLOGY);

        assertThat(responses).isEmpty();
    }

    @Test
    void getByPatient_returnsAllAttachments() {
        ClinicalAttachment attachment1 = ClinicalAttachment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT)
                .performedAt(Instant.now().minusSeconds(3600))
                .build();

        ClinicalAttachment attachment2 = ClinicalAttachment.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .encounterId(encounterId)
                .attachmentType(com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING)
                .performedAt(Instant.now().minusSeconds(7200))
                .build();

        when(clinicalAttachmentRepository.findByPatientIdOrderByPerformedAtDesc(any()))
                .thenReturn(List.of(attachment1, attachment2));

        List<ClinicalAttachmentResponse> responses = clinicalAttachmentService.getByPatient(patientId);

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ClinicalAttachmentResponse::getAttachmentType)
                .containsExactlyInAnyOrder(
                        com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.LAB_RESULT,
                        com.kinplatform.kin.health.hce.entity.ClinicalAttachment.AttachmentType.IMAGING);
    }
}

