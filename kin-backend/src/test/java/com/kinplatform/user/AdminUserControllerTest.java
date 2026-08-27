package com.kinplatform.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.common.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(adminUserService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pendingPhysicians_deberiaDevolverLista() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.pendingPhysicians())
                .thenReturn(List.of(PendingPhysicianResponse.builder()
                        .id(id)
                        .email("medico@kin.com")
                        .fullName("Dr. García")
                        .licenseNumber("CEDULA-12345")
                        .specialty("Medicina Interna")
                        .country("España")
                        .createdAt(OffsetDateTime.now())
                        .build()));

        mockMvc.perform(get("/admin/users/physicians/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("medico@kin.com"))
                .andExpect(jsonPath("$[0].licenseNumber").value("CEDULA-12345"))
                .andExpect(jsonPath("$[0].specialty").value("Medicina Interna"));
    }

    @Test
    void approvePhysician_deberiaLlamarAlServicio() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/admin/users/physicians/{userId}/approve", id)).andExpect(status().isOk());

        verify(adminUserService).setVerificationStatus(id, PhysicianVerificationStatus.APPROVED);
    }

    @Test
    void rejectPhysician_deberiaLlamarAlServicio() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/admin/users/physicians/{userId}/reject", id)).andExpect(status().isOk());

        verify(adminUserService).setVerificationStatus(id, PhysicianVerificationStatus.REJECTED);
    }

    @Test
    void approvePhysician_conUsuarioNoMedico_deberiaDevolver400() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new IllegalArgumentException("El usuario no es un médico"))
                .when(adminUserService)
                .setVerificationStatus(eq(id), any(PhysicianVerificationStatus.class));

        mockMvc.perform(post("/admin/users/physicians/{userId}/approve", id)).andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_deberiaLlamarAlServicio() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/admin/users/{userId}/verify", id)).andExpect(status().isOk());

        verify(adminUserService).verifyEmail(id);
    }

    @Test
    void resetPasswordLink_deberiaDevolverUrl() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminUserService.generateResetLink(id)).thenReturn("https://kin-platform.com/reset-password?token=abc");

        mockMvc.perform(post("/admin/users/{userId}/reset-password-link", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetUrl").value("https://kin-platform.com/reset-password?token=abc"));

        verify(adminUserService).generateResetLink(id);
    }
}
