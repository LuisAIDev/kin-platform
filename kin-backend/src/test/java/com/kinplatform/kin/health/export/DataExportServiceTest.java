package com.kinplatform.kin.health.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kinplatform.common.entity.DataExportRequest;
import com.kinplatform.common.repository.DataExportRequestRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.kin.health.common.entity.UserConsent;
import com.kinplatform.kin.health.common.repository.UserConsentRepository;
import com.kinplatform.kin.health.documents.adapter.ClinicalDocumentJpaRepository;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.entity.Diagnoses;
import com.kinplatform.kin.health.hce.repository.DiagnosesRepository;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.repository.MedicalOrderRepository;
import com.kinplatform.kin.health.hce.entity.PhysicalExam;
import com.kinplatform.kin.health.hce.repository.PhysicalExamRepository;
import com.kinplatform.kin.health.documents.adapter.ClinicalDocumentJpaRepository;
import com.kinplatform.kin.health.telemedicine.adapter.MessageJpaRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataExportServiceTest {

    @Mock
    private DataExportRequestRepository exportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConsentRepository consentRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private DiagnosesRepository diagnosesRepository;

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private PhysicalExamRepository physicalExamRepository;

    @Mock
    private ClinicalDocumentJpaRepository documentRepository;

    @Mock
    private MessageJpaRepository messageRepository;

    @Mock
    private AuditLogJpaRepository auditRepository;

    private ObjectMapper objectMapper;
    private DataExportService service;

    @TempDir
    Path tempDir;

    private User testUser;
    private UUID userId;
    private UUID requestId;
    private DataExportRequest pendingRequest;
    private DataExportRequest completedRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

        service = new DataExportService(
            exportRepository, userRepository, consentRepository,
            encounterRepository, diagnosesRepository, treatmentPlanRepository,
            medicalOrderRepository, physicalExamRepository, documentRepository,
            messageRepository, auditRepository
        );
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(service, "EXPORT_DIR", tempDir.toString() + "/");

        userId = UUID.randomUUID();
        requestId = UUID.randomUUID();

        testUser = User.builder()
            .id(userId)
            .email("test@example.com")
            .fullName("Test User")
            .passwordHash("hash")
            .role(UserRole.PATIENT)
            .healthDataConsent(true)
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .sex("M")
            .phone("+573001234567")
            .build();

        // Default mocks for user lookup (used by async processExport)
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());

        pendingRequest = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("PENDING")
            .requestedAt(Instant.now())
            .downloadCount(0)
            .build();

        completedRequest = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .filePath(tempDir.resolve(requestId + ".zip").toString())
            .fileSizeBytes(1024L)
            .downloadCount(0)
            .build();
    }

    @Test
    void requestExport_happyPath_createsPendingRequest() {
        when(exportRepository.countActiveByUser(userId)).thenReturn(0L);
        when(exportRepository.save(any(DataExportRequest.class))).thenAnswer(inv -> {
            DataExportRequest r = inv.getArgument(0);
            return DataExportRequest.builder()
                .id(requestId)
                .userId(r.getUserId())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .downloadCount(r.getDownloadCount())
                .build();
        });
        // Mock for async processExport call
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        DataExportRequest request = service.requestExport(userId);

        assertThat(request).isNotNull();
        assertThat(request.getId()).isEqualTo(requestId);
        assertThat(request.getUserId()).isEqualTo(userId);
        assertThat(request.getStatus()).isEqualTo("PENDING");
        assertThat(request.getRequestedAt()).isNotNull();
        verify(exportRepository).countActiveByUser(userId);
        // save is called 3 times: once in requestExport, twice in processExport (PROCESSING + COMPLETED)
        verify(exportRepository, times(3)).save(any(DataExportRequest.class));
    }

    @Test
    void requestExport_duplicateActive_throwsException() {
        when(exportRepository.countActiveByUser(userId)).thenReturn(1L);

        assertThatThrownBy(() -> service.requestExport(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ya tienes una exportación en proceso");
        verify(exportRepository).countActiveByUser(userId);
        verify(exportRepository, never()).save(any());
    }

    @Test
    void getExportStatus_validOwner_returnsRequest() {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(completedRequest));

        DataExportRequest result = service.getExportStatus(requestId, userId);

        assertThat(result.getId()).isEqualTo(requestId);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void getExportStatus_wrongUser_throwsSecurityException() {
        UUID otherUserId = UUID.randomUUID();
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(completedRequest));

        assertThatThrownBy(() -> service.getExportStatus(requestId, otherUserId))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("No autorizado");
    }

    @Test
    void getExportStatus_notFound_throwsException() {
        when(exportRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExportStatus(requestId, userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Export no encontrado");
    }

    @Test
    void getExportsByUser_returnsOrderedList() {
        Instant base = Instant.parse("2026-09-28T20:00:00Z");
        DataExportRequest req1 = DataExportRequest.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(base.minusSeconds(60))
            .downloadCount(0)
            .build();
        DataExportRequest req2 = DataExportRequest.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(base.minusSeconds(30))
            .downloadCount(0)
            .build();
        DataExportRequest req3 = DataExportRequest.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(base)
            .downloadCount(0)
            .build();
        // Repository should return in DESC order (newest first): req3, req2, req1
        List<DataExportRequest> requests = List.of(req3, req2, req1);
        when(exportRepository.findByUserIdOrderByRequestedAtDesc(userId)).thenReturn(requests);

        List<DataExportRequest> exports = service.getExportsByUser(userId);

        assertThat(exports).hasSize(3);
        // Should be in DESC order (newest first)
        assertThat(exports.get(0).getRequestedAt()).isEqualTo(base);
        assertThat(exports.get(1).getRequestedAt()).isEqualTo(base.minusSeconds(30));
        assertThat(exports.get(2).getRequestedAt()).isEqualTo(base.minusSeconds(60));
    }

    @Test
    void downloadExport_completed_returnsBytes() throws IOException {
        Path zipPath = tempDir.resolve(requestId + ".zip");
        Files.write(zipPath, "test zip content".getBytes());
        DataExportRequest req = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .filePath(zipPath.toString())
            .fileSizeBytes(1024L)
            .downloadCount(0)
            .build();

        when(exportRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(exportRepository.save(any(DataExportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] bytes = service.downloadExport(requestId, userId);

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes)).isEqualTo("test zip content");
        verify(exportRepository).save(argThat(r -> r.getDownloadCount() == 1));
    }

    @Test
    void downloadExport_pending_throwsException() {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> service.downloadExport(requestId, userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Export no completado");
    }

    @Test
    void downloadExport_expired_throwsException() {
        DataExportRequest expiredRequest = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .expiresAt(Instant.now().minusSeconds(1))
            .filePath("/tmp/test.zip")
            .fileSizeBytes(1024L)
            .downloadCount(0)
            .build();
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(expiredRequest));

        assertThatThrownBy(() -> service.downloadExport(requestId, userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Export expirado");
    }

    @Test
    void downloadExport_notFound_throwsException() {
        when(exportRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadExport(requestId, userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Export no encontrado");
    }

    @Test
    void downloadExport_incrementsDownloadCount() throws IOException {
        Path zipPath = tempDir.resolve(requestId + ".zip");
        Files.write(zipPath, "test zip content".getBytes());
        DataExportRequest req = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .filePath(zipPath.toString())
            .fileSizeBytes(1024L)
            .downloadCount(0)
            .build();

        when(exportRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(exportRepository.save(any(DataExportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.downloadExport(requestId, userId);
        service.downloadExport(requestId, userId);
        service.downloadExport(requestId, userId);

        verify(exportRepository, times(3)).save(argThat(r -> r.getDownloadCount() >= 1 && r.getDownloadCount() <= 3));
    }

    @Test
    void expireOldExports_deletesOldFilesAndMarksExpired() throws IOException {
        Path zipPath = tempDir.resolve(requestId + ".zip");
        Files.write(zipPath, "test zip content".getBytes());
        DataExportRequest expiredRequest = DataExportRequest.builder()
            .id(requestId)
            .userId(userId)
            .status("COMPLETED")
            .requestedAt(Instant.now().minusSeconds(10))
            .completedAt(Instant.now())
            .expiresAt(Instant.now().minusSeconds(1))
            .filePath(zipPath.toString())
            .fileSizeBytes(1024L)
            .downloadCount(0)
            .build();

        when(exportRepository.findExpiredCompleted(any(Instant.class))).thenReturn(List.of(expiredRequest));
        when(exportRepository.save(any(DataExportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.expireOldExports();

        assertThat(Files.exists(zipPath)).isFalse();
        verify(exportRepository).save(argThat(r -> "EXPIRED".equals(r.getStatus())));
    }

    @Test
    void generateZip_includesReadmeAndJsonFiles() throws IOException {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        assertThat(Files.exists(zipPath)).isTrue();

        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            assertThat(zipFile.getEntry("README.txt")).isNotNull();
            assertThat(zipFile.getEntry("profile.json")).isNotNull();
            assertThat(zipFile.getEntry("consents.json")).isNotNull();
            assertThat(zipFile.getEntry("encounters.json")).isNotNull();
            assertThat(zipFile.getEntry("diagnoses.json")).isNotNull();
            assertThat(zipFile.getEntry("treatment_plans.json")).isNotNull();
            assertThat(zipFile.getEntry("medical_orders.json")).isNotNull();
            assertThat(zipFile.getEntry("physical_exams.json")).isNotNull();
            assertThat(zipFile.getEntry("messages.json")).isNotNull();
            assertThat(zipFile.getEntry("audit_log.json")).isNotNull();
            assertThat(zipFile.getEntry("metadata.json")).isNotNull();
        }
    }

    @Test
    void generateZip_profileJsonContainsUserData() throws IOException {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            String profileJson = readZipEntry(zipFile, "profile.json");
            assertThat(profileJson).contains("test@example.com");
            assertThat(profileJson).contains("Test User");
            assertThat(profileJson).contains("PATIENT");
        }
    }

    @Test
    void generateZip_consentsJsonContainsConsentHistory() throws IOException {
        UserConsent consent = UserConsent.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .consentType(UserConsent.ConsentType.HEALTH_DATA)
            .version("1.0")
            .accepted(true)
            .acceptedAt(Instant.now())
            .ipAddress("127.0.0.1")
            .userAgent("test-agent")
            .build();
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of(consent));
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            String consentsJson = readZipEntry(zipFile, "consents.json");
            assertThat(consentsJson).contains("HEALTH_DATA");
            assertThat(consentsJson).contains("1.0");
            assertThat(consentsJson).contains("ACCEPTED");
        }
    }

    @Test
    void generateZip_metadataJsonContainsExportInfo() throws IOException {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            String metadataJson = readZipEntry(zipFile, "metadata.json");
            assertThat(metadataJson).contains("exportVersion");
            assertThat(metadataJson).contains("generatedAt");
            assertThat(metadataJson).contains("KIN HEALTH SAS");
            assertThat(metadataJson).contains("Ley 1581");
        }
    }

    @Test
    void generateZip_zipSlipProtection() throws IOException {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            assertThat(zipFile.getEntry("profile.json")).isNotNull();
            assertThat(zipFile.getEntry("../etc/passwd")).isNull();
        }
    }

    @Test
    void generateZip_userNotFound_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        when(userRepository.findById(otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processExport(otherUserId))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void generateZip_containsAll12ExpectedFiles() throws IOException {
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            // Verify all 12 expected entries
            assertThat(zipFile.getEntry("README.txt")).isNotNull();
            assertThat(zipFile.getEntry("profile.json")).isNotNull();
            assertThat(zipFile.getEntry("consents.json")).isNotNull();
            assertThat(zipFile.getEntry("encounters.json")).isNotNull();
            assertThat(zipFile.getEntry("diagnoses.json")).isNotNull();
            assertThat(zipFile.getEntry("treatment_plans.json")).isNotNull();
            assertThat(zipFile.getEntry("medical_orders.json")).isNotNull();
            assertThat(zipFile.getEntry("physical_exams.json")).isNotNull();
            assertThat(zipFile.getEntry("messages.json")).isNotNull();
            assertThat(zipFile.getEntry("audit_log.json")).isNotNull();
            assertThat(zipFile.getEntry("metadata.json")).isNotNull();
            // documents/ is a directory, not a file entry
        }
    }

    @Test
    void generateZip_encountersJsonHasCorrectStructure() throws IOException {
        Encounter encounter = Encounter.builder()
            .id(UUID.randomUUID())
            .patientId(userId)
            .physicianId(UUID.randomUUID())
            .organizationId(UUID.randomUUID())
            .encounterType(Encounter.EncounterType.OUTPATIENT)
            .status(Encounter.EncounterStatus.COMPLETED)
            .chiefComplaint("Test complaint")
            .startedAt(Instant.now().minusSeconds(3600))
            .closedAt(Instant.now())
            .createdAt(Instant.now().minusSeconds(7200))
            .updatedAt(Instant.now())
            .build();
        when(exportRepository.findById(requestId)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(consentRepository.findByUserId(userId)).thenReturn(List.of());
        when(encounterRepository.findByPatientIdOrderByStartedAtDesc(any(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(encounter)));
        when(diagnosesRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(treatmentPlanRepository.findByPatientIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(userId)).thenReturn(List.of());
        when(physicalExamRepository.findByPatientIdOrderByRecordedAtDesc(userId)).thenReturn(List.of());
        when(documentRepository.findVisibleByPatientId(userId)).thenReturn(List.of());
        when(messageRepository.findBySenderIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(messageRepository.findByReceiverIdOrderByCreatedAtAsc(userId)).thenReturn(List.of());
        when(auditRepository.findByUserIdOrderByTimestampDesc(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());

        service.processExport(requestId);

        Path zipPath = tempDir.resolve(userId.toString()).resolve(requestId + ".zip");
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(zipPath.toFile())) {
            String encountersJson = readZipEntry(zipFile, "encounters.json");
            assertThat(encountersJson).contains("encounters");
            assertThat(encountersJson).contains("Test complaint");
            assertThat(encountersJson).contains("OUTPATIENT");
            assertThat(encountersJson).contains("COMPLETED");
        }
    }

    private String readZipEntry(java.util.zip.ZipFile zipFile, String entryName) throws IOException {
        try (java.io.InputStream is = zipFile.getInputStream(zipFile.getEntry(entryName))) {
            return new String(is.readAllBytes(), "UTF-8");
        }
    }
}

