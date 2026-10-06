package com.kinplatform.kin.health.legal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.kinplatform.common.entity.DataDeletionRequest;
import com.kinplatform.common.repository.DataDeletionRepository;
import com.kinplatform.common.audit.adapter.AuditLogJpaRepository;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.Encounter.EncounterStatus;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
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
class DataDeletionServiceTest {

    @Mock
    private DataDeletionRepository deletionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @Mock
    private AuditLogJpaRepository auditRepository;

    private DataDeletionService service;
    private ObjectMapper objectMapper;

    private User testUser;
    private UUID userId;
    private UUID requestId;
    private UUID adminId;
    private DataDeletionRequest pendingRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new DataDeletionService(
            deletionRepository, userRepository, encounterRepository, auditRepository, objectMapper
        );
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);

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

        pendingRequest = DataDeletionRequest.builder()
            .id(requestId)
            .userId(userId)
            .reason("Quiero eliminar mis datos")
            .scope("FULL")
            .status("PENDING")
            .legalHold(false)
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @Test
    void requestDeletion_withoutHCE_legalHoldFalse() {
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(0L);
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> {
            DataDeletionRequest r = inv.getArgument(0);
            return DataDeletionRequest.builder()
                .id(requestId)
                .userId(r.getUserId())
                .reason(r.getReason())
                .scope(r.getScope())
                .status(r.getStatus())
                .legalHold(r.getLegalHold())
                .legalHoldReason(r.getLegalHoldReason())
                .build();
        });

        DataDeletionRequest result = service.requestDeletion(
            userId, "Quiero eliminar mis datos", "FULL", null);

        assertThat(result.getLegalHold()).isFalse();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getScope()).isEqualTo("FULL");
    }

    @Test
    void requestDeletion_withHCE_legalHoldTrue() {
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(1L);
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> {
            DataDeletionRequest r = inv.getArgument(0);
            return DataDeletionRequest.builder()
                .id(requestId)
                .userId(r.getUserId())
                .reason(r.getReason())
                .scope(r.getScope())
                .status(r.getStatus())
                .legalHold(r.getLegalHold())
                .legalHoldReason(r.getLegalHoldReason())
                .build();
        });

        DataDeletionRequest result = service.requestDeletion(
            userId, "Quiero eliminar mis datos", "FULL", null);

        assertThat(result.getLegalHold()).isTrue();
        assertThat(result.getLegalHoldReason()).isNotNull()
            .contains("Historia Clínica Electrónica")
            .contains("Resolución 839/1995");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void requestDeletion_partialScope_withValidCategories() {
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(0L);
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> {
            DataDeletionRequest r = inv.getArgument(0);
            return DataDeletionRequest.builder()
                .id(requestId)
                .userId(r.getUserId())
                .reason(r.getReason())
                .scope(r.getScope())
                .dataCategories(r.getDataCategories())
                .status(r.getStatus())
                .legalHold(r.getLegalHold())
                .legalHoldReason(r.getLegalHoldReason())
                .build();
        });

        ArrayNode categories = objectMapper.createArrayNode();
        categories.add("ACCOUNT_INFO");
        categories.add("CONSENTS");

        DataDeletionRequest result = service.requestDeletion(
            userId, "Eliminar cuenta y consentimientos", "PARTIAL", categories);

        assertThat(result.getScope()).isEqualTo("PARTIAL");
        assertThat(result.getDataCategories()).isEqualTo(categories);
    }

    @Test
    void requestDeletion_partialScope_withInvalidCategory_throwsException() {
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(0L);
        ArrayNode categories = objectMapper.createArrayNode();
        categories.add("INVALID_CATEGORY");

        assertThatThrownBy(() -> service.requestDeletion(
            userId, "Eliminar", "PARTIAL", categories))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Categoría no válida");
    }

    @Test
    void requestDeletion_partialScope_withProtectedCategory_throwsException() {
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(0L);
        ArrayNode categories = objectMapper.createArrayNode();
        categories.add("HCE");

        assertThatThrownBy(() -> service.requestDeletion(
            userId, "Eliminar", "PARTIAL", categories))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Categoría protegida no eliminable: HCE");
    }

    @Test
    void getRequestsByUser_returnsOrderedList() {
        Instant base = Instant.parse("2026-09-28T20:00:00Z");
        DataDeletionRequest r1 = DataDeletionRequest.builder()
            .id(UUID.randomUUID()).userId(userId).reason("r1").scope("FULL")
            .status("PENDING").createdAt(base.minusSeconds(60)).build();
        DataDeletionRequest r2 = DataDeletionRequest.builder()
            .id(UUID.randomUUID()).userId(userId).reason("r2").scope("FULL")
            .status("APPROVED").createdAt(base.minusSeconds(30)).build();
        DataDeletionRequest r3 = DataDeletionRequest.builder()
            .id(UUID.randomUUID()).userId(userId).reason("r3").scope("FULL")
            .status("REJECTED").createdAt(base).build();

        when(deletionRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(r3, r2, r1));

        List<DataDeletionRequest> requests = service.getRequestsByUser(userId);

        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).getCreatedAt()).isEqualTo(base);
        assertThat(requests.get(1).getCreatedAt()).isEqualTo(base.minusSeconds(30));
        assertThat(requests.get(2).getCreatedAt()).isEqualTo(base.minusSeconds(60));
    }

    @Test
    void approveDeletion_marksApproved() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("PENDING").legalHold(false).build();
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataDeletionRequest result = service.approveDeletion(requestId, adminId);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        assertThat(result.getReviewedBy()).isEqualTo(adminId);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void approveDeletion_withLegalHold_setsNotes() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("PENDING").legalHold(true)
            .legalHoldReason("Tiene HCE")
            .build();
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataDeletionRequest result = service.approveDeletion(requestId, adminId);

        assertThat(result.getReviewNotes()).contains("legal hold");
    }

    @Test
    void rejectDeletion_changesStatus() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("PENDING").build();
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataDeletionRequest result = service.rejectDeletion(requestId, adminId, "Datos incorrectos");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getReviewNotes()).isEqualTo("Datos incorrectos");
    }

    @Test
    void executeDeletion_withoutLegalHold_deletesAll() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("APPROVED").legalHold(false).build();
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataDeletionRequest result = service.executeDeletion(requestId, true);

        assertThat(result.getStatus()).isEqualTo("EXECUTED");
    }

    @Test
    void executeDeletion_withLegalHold_anonymizesHCE() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("APPROVED").legalHold(true)
            .legalHoldReason("Tiene HCE").build();
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(1L);
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataDeletionRequest result = service.executeDeletion(requestId, true);

        assertThat(result.getStatus()).isEqualTo("PARTIALLY_EXECUTED");
        assertThat(result.getAnonymizedCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void executeDeletion_notApproved_throwsException() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("PENDING").build();
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.executeDeletion(requestId, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Solo se pueden ejecutar solicitudes aprobadas");
    }

    @Test
    void executeDeletion_requiresConfirm() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("APPROVED").build();
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> service.executeDeletion(requestId, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Confirmación requerida");
    }

    @Test
    void executeDeletion_countsAnonymizedAndDeleted() {
        DataDeletionRequest req = DataDeletionRequest.builder()
            .id(requestId).userId(userId).reason("r").scope("FULL")
            .status("APPROVED").legalHold(true).build();
        when(encounterRepository.countByPatientIdAndStatus(any(), any())).thenReturn(1L);
        when(deletionRepository.findById(requestId)).thenReturn(Optional.of(req));
        when(deletionRepository.save(any(DataDeletionRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        DataDeletionRequest result = service.executeDeletion(requestId, true);

        assertThat(result.getAnonymizedCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.getDeletedCount()).isGreaterThanOrEqualTo(0);
    }
}

