package com.kinplatform.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kinplatform.auth.dto.AuthResponse;
import com.kinplatform.auth.dto.UserDTO;
import com.kinplatform.auth.password.PasswordResetService;
import com.kinplatform.auth.verification.VerifyEmailOutcome;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, passwordResetService))
                .build();
    }

    @Test
    void register_deberiaResponder201SinTokenYNoVerificado() throws Exception {
        when(authService.register(any()))
                .thenReturn(AuthResponse.builder()
                        .email("a@kin.com")
                        .fullName("A")
                        .role("FREE")
                        .emailVerified(false)
                        .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\",\"password\":\"KINpass123!a\",\"fullName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FREE"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void register_noDeberiaEstablecerCookieDeSesion() throws Exception {
        when(authService.register(any()))
                .thenReturn(AuthResponse.builder()
                        .email("a@kin.com")
                        .fullName("A")
                        .role("FREE")
                        .emailVerified(false)
                        .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\",\"password\":\"KINpass123!a\",\"fullName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void login_deberiaResponder200ConToken() throws Exception {
        when(authService.login(any()))
                .thenReturn(AuthResponse.builder()
                        .token("t")
                        .email("a@kin.com")
                        .fullName("A")
                        .role("FREE")
                        .emailVerified(true)
                        .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("t"));
    }

    @Test
    void verifyEmail_conTokenValido_deberiaResponder200YNoEstablecerCookie() throws Exception {
        when(authService.verifyEmail("valid-token")).thenReturn(VerifyEmailOutcome.SUCCESS);

        mockMvc.perform(get("/auth/verify-email").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.message").value("Correo verificado correctamente. Ya puedes iniciar sesión."));
    }

    @Test
    void verifyEmail_conTokenExpirado_deberiaResponder400ConCodigo() throws Exception {
        when(authService.verifyEmail("expired")).thenReturn(VerifyEmailOutcome.EXPIRED);

        mockMvc.perform(get("/auth/verify-email").param("token", "expired"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPIRED"));
    }

    @Test
    void verifyEmail_conTokenInvalido_deberiaResponder400() throws Exception {
        when(authService.verifyEmail("bad")).thenReturn(VerifyEmailOutcome.INVALID);

        mockMvc.perform(get("/auth/verify-email").param("token", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID"));
    }

    @Test
    void verifyEmail_conTokenYaUsado_deberiaResponder400() throws Exception {
        when(authService.verifyEmail("used")).thenReturn(VerifyEmailOutcome.ALREADY_USED);

        mockMvc.perform(get("/auth/verify-email").param("token", "used"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALREADY_USED"));
    }

    @Test
    void resendVerification_deberiaResponder200Generico() throws Exception {
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\"}"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Si existe una cuenta asociada a este correo y necesita verificación, recibirás un nuevo mensaje."));
    }

    @Test
    void me_conBearerValido_deberiaResponder200() throws Exception {
        when(authService.getCurrentUser("token"))
                .thenReturn(UserDTO.builder()
                        .id(UUID.randomUUID())
                        .email("a@kin.com")
                        .fullName("A")
                        .role("FREE")
                        .build());

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@kin.com"));
    }

    @Test
    void me_sinHeader_deberiaResponder401() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_conHeaderSinBearer_deberiaResponder401() throws Exception {
        mockMvc.perform(get("/auth/me").header("Authorization", "Basic abc")).andExpect(status().isUnauthorized());
    }

    @Test
    void login_deberiaEstablecerCookieHttpOnly() throws Exception {
        when(authService.login(any()))
                .thenReturn(AuthResponse.builder()
                        .token("t")
                        .email("a@kin.com")
                        .fullName("A")
                        .role("FREE")
                        .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                "Set-Cookie",
                                org.hamcrest.Matchers.allOf(
                                        org.hamcrest.Matchers.containsString("kin_token_v2=t"),
                                        org.hamcrest.Matchers.containsString("HttpOnly"))));
    }

    @Test
    void logout_conBearer_deberiaResponder200() throws Exception {
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer t"))
                .andExpect(status().isOk());

        verify(authService).logout("t");
    }

    @Test
    void logout_sinToken_noDeberiaFallar() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isOk());

        verify(authService).logout(null);
    }

    @Test
    void logout_soloConCookie_deberiaBlacklistearCookie() throws Exception {
        mockMvc.perform(post("/auth/logout").cookie(new jakarta.servlet.http.Cookie("kin_token_v2", "ct")))
                .andExpect(status().isOk());

        verify(authService).logout("ct");
    }

    @Test
    void forgotPassword_correoValido_deberiaResponder200MensajeGenerico() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(passwordResetService).requestReset("a@kin.com");
    }

    @Test
    void forgotPassword_sinCorreo_deberiaResponder400() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_conExito_deberiaResponder200() throws Exception {
        when(passwordResetService.resetPassword("tok", "NuevaPass1!")).thenReturn(true);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"newPassword\":\"NuevaPass1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_tokenInvalido_deberiaResponder400() throws Exception {
        when(passwordResetService.resetPassword("tok", "NuevaPass1!")).thenReturn(false);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"newPassword\":\"NuevaPass1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void resetPassword_contrasenaCorta_deberiaResponder400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPatient_deberiaResponder201ConRolPaciente() throws Exception {
        when(authService.registerPatient(any()))
                .thenReturn(AuthResponse.builder()
                        .email("p@kin.com")
                        .fullName("Ana Paciente")
                        .role("PATIENT")
                        .emailVerified(false)
                        .build());

        mockMvc.perform(post("/auth/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"p@kin.com\",\"password\":\"KINpass123!a\","
                                + "\"fullName\":\"Ana Paciente\",\"dateOfBirth\":\"1990-05-15\","
                                + "\"sex\":\"FEMENINO\",\"healthDataConsent\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void registerPhysician_deberiaResponder201ConVerificacionPendiente() throws Exception {
        when(authService.registerPhysician(any()))
                .thenReturn(AuthResponse.builder()
                        .email("m@kin.com")
                        .fullName("Dr. García")
                        .role("PHYSICIAN")
                        .emailVerified(false)
                        .verificationStatus("PENDING")
                        .build());

        mockMvc.perform(post("/auth/register/physician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"m@kin.com\",\"password\":\"KINpass123!a\","
                                + "\"fullName\":\"Dr. García\",\"licenseNumber\":\"CEDULA-12345\","
                                + "\"specialty\":\"Medicina Interna\",\"country\":\"España\","
                                + "\"healthDataConsent\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PHYSICIAN"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));
    }

    @Test
    void registerPhysician_sinConsentimiento_deberiaResponder400() throws Exception {
        mockMvc.perform(post("/auth/register/physician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"m@kin.com\",\"password\":\"KINpass123!a\","
                                + "\"fullName\":\"Dr. García\",\"licenseNumber\":\"CEDULA-12345\","
                                + "\"specialty\":\"Medicina Interna\",\"country\":\"España\","
                                + "\"healthDataConsent\":false}"))
                .andExpect(status().isBadRequest());
    }
}
