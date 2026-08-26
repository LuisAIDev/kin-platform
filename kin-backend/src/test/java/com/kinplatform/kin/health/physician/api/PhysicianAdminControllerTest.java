package com.kinplatform.kin.health.physician.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.kin.health.physician.domain.PhysicianPatientAssignment;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test del endpoint administrativo de asignación médico-paciente (ADR-031).
 */
@ExtendWith(MockitoExtension.class)
class PhysicianAdminControllerTest {

    @Mock
    private PhysicianService physicianService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PhysicianAdminController(physicianService))
                .build();
    }

    @Test
    void assign_deberiaAsignarPacienteAMedico() throws Exception {
        UUID physicianId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        when(physicianService.assignPatient(physicianId, patientId))
                .thenReturn(PhysicianPatientAssignment.of(physicianId, patientId, OffsetDateTime.now()));

        mockMvc.perform(post("/admin/health/physician/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PhysicianAdminController.AssignRequest(physicianId, patientId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicianId").value(physicianId.toString()))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }
}
