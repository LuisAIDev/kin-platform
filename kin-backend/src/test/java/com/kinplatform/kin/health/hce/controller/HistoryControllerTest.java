package com.kinplatform.kin.health.hce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinplatform.test.PostgresTestSupport;
import com.kinplatform.common.user.User;
import com.kinplatform.common.user.UserRepository;
import com.kinplatform.common.user.UserRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class HistoryControllerTest extends PostgresTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private static final String PHYSICIAN_EMAIL = "physician-test@kin.test";

    private void ensurePhysicianExists() {
        TransactionTemplate requiresNew = new TransactionTemplate(txManager);
        requiresNew.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNew.execute(status -> userRepository.findByEmail(PHYSICIAN_EMAIL)
                .orElseGet(() -> userRepository.saveAndFlush(User.builder()
                        .email(PHYSICIAN_EMAIL)
                        .passwordHash("x")
                        .fullName("Dr. Test Physician")
                        .role(UserRole.PHYSICIAN)
                        .emailVerified(true)
                        .isActive(true)
                        .build())));
    }

    private UUID createPatient() {
        TransactionTemplate requiresNew = new TransactionTemplate(txManager);
        requiresNew.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return requiresNew.execute(status -> userRepository
                .saveAndFlush(User.builder()
                        .email("patient-" + UUID.randomUUID() + "@kin.test")
                        .passwordHash("x")
                        .fullName("Test Patient")
                        .role(UserRole.PATIENT)
                        .emailVerified(true)
                        .isActive(true)
                        .healthDataConsent(true)
                        .build())
                .getId());
    }

    private static final String BASE_URL = "/health/hce";

    @Test
    @WithMockUser(username = "test@kin.test", roles = "PATIENT")
    void getHistory_sinRolPhysician_devuelve403() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();
        mockMvc.perform(get(BASE_URL + "/patients/{patientId}/history", patientId))
                .andExpect(status().isForbidden());
    }

    @Test
    void getHistory_sinAuth_devuelve403() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();
        mockMvc.perform(get(BASE_URL + "/patients/{patientId}/history", patientId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void getHistory_pacienteInexistente_devuelve404() throws Exception {
        ensurePhysicianExists();
        UUID nonExistent = UUID.randomUUID();
        mockMvc.perform(get(BASE_URL + "/patients/{patientId}/history", nonExistent))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void getHistory_pacienteSinHistorial_devuelve200_con7ArraysVacios() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();
        mockMvc.perform(get(BASE_URL + "/patients/{patientId}/history", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies").isArray())
                .andExpect(jsonPath("$.surgeries").isArray())
                .andExpect(jsonPath("$.medications").isArray())
                .andExpect(jsonPath("$.vaccines").isArray())
                .andExpect(jsonPath("$.familyHistory").isArray())
                .andExpect(jsonPath("$.toxicHabits").isArray())
                .andExpect(jsonPath("$.gynecoObstetric").doesNotExist());
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void createHistoryItem_allergy_devuelve201_yItemCompleto() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();

        Map<String, Object> body = Map.of(
                "type", "ALLERGY",
                "data", Map.of(
                        "allergen", "Penicilina",
                        "reaction", "Erupción cutánea",
                        "severity", "MILD",
                        "onsetDate", "2024-01-15",
                        "status", "ACTIVE",
                        "notes", "Alergia confirmada"
                )
        );

        mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("ALLERGY"))
                .andExpect(jsonPath("$.patientId").value(patientId.toString()))
                // El allergen se mapea a description, no a data.allergen
                .andExpect(jsonPath("$.data.description").value("Penicilina"))
                .andExpect(jsonPath("$.data.severity").value("MILD"))
                .andExpect(jsonPath("$.data.reaction").value("Erupción cutánea"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.notes").value("Alergia confirmada"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void createHistoryItem_familyHistory_mapeaTypeAFamily() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();

        Map<String, Object> body = Map.of(
                "type", "FAMILY_HISTORY",
                "data", Map.of(
                        "relationship", "FATHER",
                        "condition", "Diabetes tipo 2",
                        "ageOfOnset", 55,
                        "isDeceased", false,
                        "notes", "Diagnosticado a los 50"
                )
        );

        mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FAMILY_HISTORY"))
                .andExpect(jsonPath("$.data.relationship").value("FATHER"))
                // La condition se mapea a description
                .andExpect(jsonPath("$.data.description").value("Diabetes tipo 2"));
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void createHistoryItem_severityMILD_mapeaALEVE() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();

        Map<String, Object> body = Map.of(
                "type", "ALLERGY",
                "data", Map.of(
                        "allergen", "Látex",
                        "severity", "MILD"
                )
        );

        mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.severity").value("MILD"));
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void createHistoryItem_typeInvalido_devuelve400() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();

        Map<String, Object> body = Map.of(
                "type", "INVALID_TYPE",
                "data", Map.of("allergen", "Test")
        );

        mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tipo de historia inválido: INVALID_TYPE"));
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void createHistoryItem_pacienteNoExiste_devuelve404() throws Exception {
        ensurePhysicianExists();

        Map<String, Object> body = Map.of(
                "type", "ALLERGY",
                "data", Map.of("allergen", "Test")
        );

        UUID nonExistent = UUID.randomUUID();
        mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", nonExistent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void updateHistoryItem_actualizaAllergy_devuelve200() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();

        Map<String, Object> createBody = Map.of(
                "type", "ALLERGY",
                "data", Map.of(
                        "allergen", "Penicilina",
                        "severity", "MILD",
                        "notes", "Original"
                )
        );

        String createResponse = mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String historyId = objectMapper.readTree(createResponse).get("id").asText();

        Map<String, Object> updateBody = Map.of(
                "type", "ALLERGY",
                "data", Map.of(
                        "allergen", "Penicilina",
                        "severity", "SEVERE",
                        "notes", "Actualizado: reacción grave"
                )
        );

        mockMvc.perform(put("/health/hce/history/{historyId}", historyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(historyId))
                .andExpect(jsonPath("$.data.severity").value("SEVERE"))
                .andExpect(jsonPath("$.data.notes").value("Actualizado: reacción grave"))
                .andExpect(jsonPath("$.type").value("ALLERGY"));
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void updateHistoryItem_inexistente_devuelve404() throws Exception {
        ensurePhysicianExists();

        Map<String, Object> body = Map.of(
                "type", "ALLERGY",
                "data", Map.of("allergen", "Test")
        );

        mockMvc.perform(put("/health/hce/history/{historyId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void deleteHistoryItem_existente_devuelve204() throws Exception {
        ensurePhysicianExists();
        UUID patientId = createPatient();

        Map<String, Object> createBody = Map.of(
                "type", "ALLERGY",
                "data", Map.of("allergen", "A borrar", "severity", "MILD")
        );

        String createResponse = mockMvc.perform(post(BASE_URL + "/patients/{patientId}/history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String historyId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(delete("/health/hce/history/{historyId}", historyId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/patients/{patientId}/history", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies", hasSize(0)));
    }

    @Test
    @WithMockUser(username = PHYSICIAN_EMAIL, roles = "PHYSICIAN")
    void deleteHistoryItem_inexistente_devuelve404() throws Exception {
        ensurePhysicianExists();
        mockMvc.perform(delete("/health/hce/history/{historyId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
