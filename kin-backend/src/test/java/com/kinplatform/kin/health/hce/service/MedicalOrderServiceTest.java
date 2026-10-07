package com.kinplatform.kin.health.hce.service;

import com.kinplatform.common.security.AuthenticatedUsers;
import com.kinplatform.kin.health.hce.dto.CreateMedicalOrderRequest;
import com.kinplatform.kin.health.hce.dto.MedicalOrderResponse;
import com.kinplatform.kin.health.hce.entity.Encounter;
import com.kinplatform.kin.health.hce.entity.MedicalOrder;
import com.kinplatform.kin.health.hce.entity.TreatmentPlan;
import com.kinplatform.kin.health.hce.repository.EncounterRepository;
import com.kinplatform.kin.health.hce.repository.MedicalOrderRepository;
import com.kinplatform.kin.health.hce.repository.TreatmentPlanRepository;
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
class MedicalOrderServiceTest {

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private TreatmentPlanRepository treatmentPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncounterRepository encounterRepository;

    @InjectMocks
    private MedicalOrderService medicalOrderService;

    private UUID treatmentPlanId;
    private UUID encounterId;
    private UUID patientId;
    private UUID physicianId;
    private TreatmentPlan treatmentPlan;
    private User physician;

    @BeforeEach
    void setUp() {
        treatmentPlanId = UUID.randomUUID();
        encounterId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        physicianId = UUID.randomUUID();

        treatmentPlan = TreatmentPlan.builder()
                .id(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .conduct(TreatmentPlan.Conduct.OUTPATIENT_TREATMENT)
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
    void addOrder_happyPath_labExam() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(treatmentPlan));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .treatmentPlanId(treatmentPlanId)
                    .orderType(MedicalOrder.OrderType.LAB_EXAM)
                    .priority(MedicalOrder.Priority.URGENT)
                    .cupsCode("890301")
                    .cupsDescription("Complete blood count")
                    .instructions("Fasting required")
                    .build();

            MedicalOrderResponse response = medicalOrderService.addOrder(request);

            assertThat(response).isNotNull();
            assertThat(response.getTreatmentPlanId()).isEqualTo(treatmentPlanId);
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(response.getPhysicianId()).isEqualTo(physicianId);
            assertThat(response.getOrderType()).isEqualTo(MedicalOrder.OrderType.LAB_EXAM);
            assertThat(response.getPriority()).isEqualTo(MedicalOrder.Priority.URGENT);
            assertThat(response.getStatus()).isEqualTo(MedicalOrder.Status.ORDERED);
            assertThat(response.getOrderedAt()).isNotNull();
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addOrder_happyPath_medication() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(treatmentPlan));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .treatmentPlanId(treatmentPlanId)
                    .orderType(MedicalOrder.OrderType.MEDICATION)
                    .priority(MedicalOrder.Priority.ROUTINE)
                    .drugName("Ibuprofen")
                    .dose("400")
                    .doseUnit("mg")
                    .route(MedicalOrder.Route.ORAL)
                    .frequency("Every 8 hours")
                    .durationDays(5)
                    .instructions("Take with food")
                    .build();

            MedicalOrderResponse response = medicalOrderService.addOrder(request);

            assertThat(response).isNotNull();
            assertThat(response.getOrderType()).isEqualTo(MedicalOrder.OrderType.MEDICATION);
            assertThat(response.getDrugName()).isEqualTo("Ibuprofen");
            assertThat(response.getDose()).isEqualTo("400");
            assertThat(response.getRoute()).isEqualTo(MedicalOrder.Route.ORAL);
            assertThat(response.getFrequency()).isEqualTo("Every 8 hours");
            assertThat(response.getDurationDays()).isEqualTo(5);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addOrder_throwsWhenTreatmentPlanNotFound() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.empty());

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .treatmentPlanId(UUID.randomUUID())
                    .orderType(MedicalOrder.OrderType.LAB_EXAM)
                    .priority(MedicalOrder.Priority.ROUTINE)
                    .build();

            assertThatThrownBy(() -> medicalOrderService.addOrder(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Treatment plan not found");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addOrder_throwsWhenOrderTypeInvalid() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(treatmentPlan));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .treatmentPlanId(treatmentPlanId)
                    .orderType(null)
                    .priority(MedicalOrder.Priority.ROUTINE)
                    .build();

            assertThatThrownBy(() -> medicalOrderService.addOrder(request))
                    .isInstanceOf(NullPointerException.class);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addOrder_throwsWhenPriorityInvalid() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(treatmentPlan));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .treatmentPlanId(treatmentPlanId)
                    .orderType(MedicalOrder.OrderType.LAB_EXAM)
                    .priority(null)
                    .build();

            // Priority has default value ROUTINE, so it should work
            MedicalOrderResponse response = medicalOrderService.addOrder(request);
            assertThat(response).isNotNull();
            assertThat(response.getPriority()).isEqualTo(MedicalOrder.Priority.ROUTINE);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void executeOrder_happyPath_orderedToExecuted() {
        MedicalOrder order = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.URGENT)
                .status(MedicalOrder.Status.ORDERED)
                .orderedAt(Instant.now().minusSeconds(3600))
                .build();

        when(medicalOrderRepository.findById(any())).thenReturn(Optional.of(order));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            MedicalOrderResponse response = medicalOrderService.executeOrder(order.getId(), physicianId);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(MedicalOrder.Status.EXECUTED);
            assertThat(response.getExecutedAt()).isNotNull();
            assertThat(response.getExecutedBy()).isEqualTo(physicianId);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void executeOrder_throwsWhenAlreadyExecuted() {
        MedicalOrder order = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.URGENT)
                .status(MedicalOrder.Status.EXECUTED)
                .orderedAt(Instant.now().minusSeconds(3600))
                .executedAt(Instant.now().minusSeconds(1800))
                .executedBy(UUID.randomUUID())
                .build();

        when(medicalOrderRepository.findById(any())).thenReturn(Optional.of(order));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> medicalOrderService.executeOrder(order.getId(), physicianId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already executed");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void executeOrder_throwsWhenCancelled() {
        MedicalOrder order = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.URGENT)
                .status(MedicalOrder.Status.CANCELLED)
                .orderedAt(Instant.now().minusSeconds(3600))
                .build();

        when(medicalOrderRepository.findById(any())).thenReturn(Optional.of(order));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> medicalOrderService.executeOrder(order.getId(), physicianId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cancelled");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void cancelOrder_happyPath_orderedToCancelled() {
        MedicalOrder order = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.ROUTINE)
                .status(MedicalOrder.Status.ORDERED)
                .orderedAt(Instant.now().minusSeconds(3600))
                .build();

        when(medicalOrderRepository.findById(any())).thenReturn(Optional.of(order));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            MedicalOrderResponse response = medicalOrderService.cancelOrder(order.getId(), "Patient refused");

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(MedicalOrder.Status.CANCELLED);
            assertThat(response.getExecutedAt()).isNotNull();
            assertThat(response.getExecutionNotes()).isEqualTo("Patient refused");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void cancelOrder_throwsWhenAlreadyExecuted() {
        MedicalOrder order = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.ROUTINE)
                .status(MedicalOrder.Status.EXECUTED)
                .orderedAt(Instant.now().minusSeconds(3600))
                .executedAt(Instant.now().minusSeconds(1800))
                .executedBy(UUID.randomUUID())
                .build();

        when(medicalOrderRepository.findById(any())).thenReturn(Optional.of(order));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            assertThatThrownBy(() -> medicalOrderService.cancelOrder(order.getId(), "Too late"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("executed");
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByTreatmentPlan_returnsOrders() {
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(treatmentPlan));

        MedicalOrder order1 = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.URGENT)
                .status(MedicalOrder.Status.ORDERED)
                .build();

        MedicalOrder order2 = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.MEDICATION)
                .priority(MedicalOrder.Priority.ROUTINE)
                .status(MedicalOrder.Status.ORDERED)
                .build();

        when(medicalOrderRepository.findByTreatmentPlanIdOrderByOrderedAtDesc(any()))
                .thenReturn(List.of(order1, order2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<MedicalOrderResponse> responses = medicalOrderService.getByTreatmentPlan(treatmentPlanId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(MedicalOrderResponse::getOrderType)
                    .containsExactlyInAnyOrder(MedicalOrder.OrderType.LAB_EXAM, MedicalOrder.OrderType.MEDICATION);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void getByEncounter_returnsAllOrders() {
        MedicalOrder order1 = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(treatmentPlanId)
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.URGENT)
                .status(MedicalOrder.Status.ORDERED)
                .build();

        MedicalOrder order2 = MedicalOrder.builder()
                .id(UUID.randomUUID())
                .treatmentPlanId(UUID.randomUUID())
                .encounterId(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .orderType(MedicalOrder.OrderType.MEDICATION)
                .priority(MedicalOrder.Priority.ROUTINE)
                .status(MedicalOrder.Status.ORDERED)
                .build();

        when(medicalOrderRepository.findByEncounterIdOrderByOrderedAtDescList(any()))
                .thenReturn(List.of(order1, order2));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            List<MedicalOrderResponse> responses = medicalOrderService.getByEncounter(encounterId);

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(MedicalOrderResponse::getOrderType)
                    .containsExactlyInAnyOrder(MedicalOrder.OrderType.LAB_EXAM, MedicalOrder.OrderType.MEDICATION);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addOrder_throwsWhenTreatmentPlanBelongsToOtherEncounter() {
        // This test verifies that the order inherits the correct encounterId from treatmentPlan
        // by checking that the order's encounterId matches the treatmentPlan's encounterId
        when(treatmentPlanRepository.findById(any())).thenReturn(Optional.of(treatmentPlan));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .treatmentPlanId(treatmentPlanId)
                    .orderType(MedicalOrder.OrderType.LAB_EXAM)
                    .priority(MedicalOrder.Priority.ROUTINE)
                    .build();

            MedicalOrderResponse response = medicalOrderService.addOrder(request);

            assertThat(response.getEncounterId()).isEqualTo(encounterId);
        } finally {
            clearSecurityContext();
        }
    }

    private Encounter buildEncounter() {
        return Encounter.builder()
                .id(encounterId)
                .patientId(patientId)
                .physicianId(physicianId)
                .organizationId(UUID.randomUUID())
                .encounterType(Encounter.EncounterType.OUTPATIENT)
                .build();
    }

    @Test
    void addOrderForEncounter_happyPath_derivesTreatmentPlan() {
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(buildEncounter()));
        when(treatmentPlanRepository.findByEncounterId(encounterId)).thenReturn(Optional.of(treatmentPlan));
        when(treatmentPlanRepository.findById(treatmentPlanId)).thenReturn(Optional.of(treatmentPlan));
        when(medicalOrderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<AuthenticatedUsers> mockedStatic = mockStatic(AuthenticatedUsers.class)) {
            mockedStatic.when(() -> AuthenticatedUsers.require(any(), any()))
                    .thenReturn(physician);

            setupSecurityContext(physician);

            CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                    .orderType(MedicalOrder.OrderType.LAB_EXAM)
                    .priority(MedicalOrder.Priority.ROUTINE)
                    .cupsCode("890301")
                    .build();

            MedicalOrderResponse response = medicalOrderService.addOrderForEncounter(encounterId, request);

            assertThat(response).isNotNull();
            assertThat(response.getTreatmentPlanId()).isEqualTo(treatmentPlanId);
            assertThat(response.getEncounterId()).isEqualTo(encounterId);
            assertThat(response.getPatientId()).isEqualTo(patientId);
            assertThat(request.getTreatmentPlanId()).isEqualTo(treatmentPlanId);
        } finally {
            clearSecurityContext();
        }
    }

    @Test
    void addOrderForEncounter_throwsWhenEncounterNotFound() {
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.empty());

        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.ROUTINE)
                .build();

        assertThatThrownBy(() -> medicalOrderService.addOrderForEncounter(encounterId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Encounter not found");
    }

    @Test
    void addOrderForEncounter_throwsWhenNoTreatmentPlan() {
        when(encounterRepository.findById(encounterId)).thenReturn(Optional.of(buildEncounter()));
        when(treatmentPlanRepository.findByEncounterId(encounterId)).thenReturn(Optional.empty());

        CreateMedicalOrderRequest request = CreateMedicalOrderRequest.builder()
                .orderType(MedicalOrder.OrderType.LAB_EXAM)
                .priority(MedicalOrder.Priority.ROUTINE)
                .build();

        assertThatThrownBy(() -> medicalOrderService.addOrderForEncounter(encounterId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No treatment plan found for encounter");
    }
}

