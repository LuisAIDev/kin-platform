package com.kinplatform.kin.health.legal;

import com.kinplatform.common.entity.DataRectificationRequest;
import com.kinplatform.common.repository.DataRectificationRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.kin.health.hce.entity.PatientIdentification;
import com.kinplatform.kin.health.hce.repository.PatientIdentificationRepository;
import com.kinplatform.user.User;
import com.kinplatform.user.UserRepository;
import com.kinplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataRectificationServiceTest {

    @Mock
    private DataRectificationRepository rectificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientIdentificationRepository patientIdentificationRepository;

    @Mock
    private AuditLogJpaRepository auditRepository;

    private DataRectificationService service;

    private User testUser;
    private UUID userId;
    private UUID requestId;
    private UUID adminId;
    private DataRectificationRequest pendingRequest;

    @BeforeEach
    void setUp() {
        service = new DataRectificationService(
            rectificationRepository, userRepository, patientIdentificationRepository, auditRepository
        );

        userId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        adminId = UUID.randomUUID();

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

        pendingRequest = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("+573001111111")
            .newValue("+573002222222")
            .reason("Cambio de número")
            .status("PENDING")
            .build();

        // Default mocks
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(patientIdentificationRepository.findByUserId(userId)).thenReturn(Optional.of(
            PatientIdentification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .documentType(PatientIdentification.DocumentType.CC)
                .documentNumber("123456789")
                .build()
        ));
    }

    @Test
    void requestRectification_happyPath_createsPending() {
        when(rectificationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(0L);
        when(rectificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(rectificationRepository.save(any(DataRectificationRequest.class))).thenAnswer(inv -> {
            DataRectificationRequest r = inv.getArgument(0);
            return DataRectificationRequest.builder()
                .id(requestId)
                .userId(r.getUserId())
                .fieldPath(r.getFieldPath())
                .oldValue(r.getOldValue())
                .newValue(r.getNewValue())
                .reason(r.getReason())
                .status(r.getStatus())
                .build();
        });

        DataRectificationRequest result = service.requestRectification(
            userId, "users.phone", "+573001111111", "+573002222222", "Cambio de número");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(requestId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getFieldPath()).isEqualTo("users.phone");
        assertThat(result.getOldValue()).isEqualTo("+573001111111");
        assertThat(result.getNewValue()).isEqualTo("+573002222222");
        assertThat(result.getReason()).isEqualTo("Cambio de número");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void requestRectification_withInvalidFieldPath_throwsException() {
        assertThatThrownBy(() -> service.requestRectification(
            userId, "invalid.field", "old", "new", "razón"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Campo no soportado");
    }

    @Test
    void requestRectification_withProtectedFieldPath_throwsException() {
        assertThatThrownBy(() -> service.requestRectification(
            userId, "encounters", "old", "new", "razón"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no puede ser rectificado");
    }

    @Test
    void requestRectification_duplicateForSameField_throwsException() {
        when(rectificationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(0L);
        when(rectificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(pendingRequest));

        assertThatThrownBy(() -> service.requestRectification(
            userId, "users.phone", "+573001111111", "+573003333333", "Otro cambio"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ya existe una solicitud pendiente");
    }

    @Test
    void requestRectification_maxActiveLimit_throwsException() {
        when(rectificationRepository.countByUserIdAndStatusIn(any(), any())).thenReturn(5L);

        assertThatThrownBy(() -> service.requestRectification(
            userId, "users.fullName", "Old", "New", "razón"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Máximo 5 solicitudes activas");
    }

    @Test
    void getRequestsByUser_returnsOrderedList() {
        Instant base = Instant.parse("2026-09-28T20:00:00Z");
        DataRectificationRequest r1 = DataRectificationRequest.builder()
            .id(UUID.randomUUID()).userId(userId).fieldPath("f1").oldValue("o").newValue("n").reason("r")
            .status("PENDING").createdAt(base.minusSeconds(60)).build();
        DataRectificationRequest r2 = DataRectificationRequest.builder()
            .id(UUID.randomUUID()).userId(userId).fieldPath("f2").oldValue("o").newValue("n").reason("r")
            .status("APPROVED").createdAt(base.minusSeconds(30)).build();
        DataRectificationRequest r3 = DataRectificationRequest.builder()
            .id(UUID.randomUUID()).userId(userId).fieldPath("f3").oldValue("o").newValue("n").reason("r")
            .status("REJECTED").createdAt(base).build();

        when(rectificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(r3, r2, r1));

        List<DataRectificationRequest> requests = service.getRequestsByUser(userId);

        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).getCreatedAt()).isEqualTo(base);
        assertThat(requests.get(1).getCreatedAt()).isEqualTo(base.minusSeconds(30));
        assertThat(requests.get(2).getCreatedAt()).isEqualTo(base.minusSeconds(60));
    }

    @Test
    void getRequestsByStatus_returnsFiltered() {
        DataRectificationRequest r1 = DataRectificationRequest.builder()
            .id(UUID.randomUUID()).userId(userId).fieldPath("f1").oldValue("o").newValue("n").reason("r")
            .status("PENDING").build();
        DataRectificationRequest r2 = DataRectificationRequest.builder()
            .id(UUID.randomUUID()).userId(userId).fieldPath("f2").oldValue("o").newValue("n").reason("r")
            .status("PENDING").build();

        when(rectificationRepository.findByStatus("PENDING")).thenReturn(List.of(r1, r2));

        List<DataRectificationRequest> requests = service.getRequestsByStatus("PENDING");

        assertThat(requests).hasSize(2);
        assertThat(requests).allMatch(r -> "PENDING".equals(r.getStatus()));
    }

    @Test
    void approveRectification_changesStatusAndAppliesChange() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId).userId(userId).fieldPath("users.phone").oldValue("old").newValue("new").reason("r")
            .status("PENDING").build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(rectificationRepository.save(any(DataRectificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataRectificationRequest result = service.approveRectification(requestId, UUID.randomUUID(), "Aprobado");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getReviewedBy()).isNotNull();
        assertThat(result.getReviewedAt()).isNotNull();
        assertThat(result.getReviewNotes()).isEqualTo("Aprobado");
    }

    @Test
    void approveRectification_notPending_throwsException() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId).userId(userId).fieldPath("f1").oldValue("o").newValue("n").reason("r")
            .status("APPROVED").build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.approveRectification(requestId, UUID.randomUUID(), "Aprobado"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Solo se pueden aprobar solicitudes en estado PENDING");
    }

    @Test
    void rejectRectification_changesStatus() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId).userId(userId).fieldPath("f1").oldValue("o").newValue("n").reason("r")
            .status("PENDING").build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(rectificationRepository.save(any(DataRectificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataRectificationRequest result = service.rejectRectification(requestId, UUID.randomUUID(), "Datos incorrectos");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getReviewNotes()).isEqualTo("Datos incorrectos");
    }

    @Test
    void executeRectification_appliesToSourceEntity() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("users.phone")
            .oldValue("old")
            .newValue("+573009999999")
            .reason("razón")
            .status("APPROVED")
            .build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(rectificationRepository.save(any(DataRectificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataRectificationRequest result = service.executeRectification(requestId);

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        assertThat(result.getExecutedAt()).isNotNull();
        verify(userRepository).save(argThat(u -> "+573009999999".equals(u.getPhone())));
    }

    @Test
    void executeRectification_alreadyExecuted_throwsException() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId).userId(userId).fieldPath("f1").oldValue("o").newValue("n").reason("r")
            .status("EXECUTED").build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.executeRectification(requestId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ya fue ejecutada");
    }

    @Test
    void executeRectification_notApproved_throwsException() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId).userId(userId).fieldPath("f1").oldValue("o").newValue("n").reason("r")
            .status("PENDING").build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.executeRectification(requestId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Solo se pueden ejecutar solicitudes aprobadas");
    }

    @Test
    void executeRectification_appliesToPatientIdentification() {
        DataRectificationRequest req = DataRectificationRequest.builder()
            .id(requestId)
            .userId(userId)
            .fieldPath("patient_identification.document_number")
            .oldValue("123456789")
            .newValue("987654321")
            .reason("Corrección cédula")
            .status("APPROVED")
            .build();
        when(rectificationRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(rectificationRepository.save(any(DataRectificationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataRectificationRequest result = service.executeRectification(requestId);

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
        verify(patientIdentificationRepository).save(argThat(pi -> "987654321".equals(pi.getDocumentNumber())));
    }
}

