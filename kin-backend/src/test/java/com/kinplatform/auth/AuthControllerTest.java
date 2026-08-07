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

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void register_deberiaResponder201ConToken() throws Exception {
        when(authService.register(any()))
                .thenReturn(AuthResponse.builder()
                        .token("t")
                        .email("a@kin.com")
                        .fullName("A")
                        .role("FREE")
                        .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@kin.com\",\"password\":\"KINpass123!a\",\"fullName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("t"))
                .andExpect(jsonPath("$.role").value("FREE"));
    }

    @Test
    void login_deberiaResponder200ConToken() throws Exception {
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
                .andExpect(jsonPath("$.token").value("t"));
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
}
