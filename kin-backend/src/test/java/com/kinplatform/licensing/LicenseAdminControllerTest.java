package com.kinplatform.licensing;

import com.kinplatform.test.PostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LicenseAdminControllerTest extends PostgresTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void generateLicense_devuelve200() throws Exception {
        String body = """
                {
                  "licenseId": "LIC-2026-9999",
                  "ipsName": "IPS Demo Test",
                  "nit": "900999888-7",
                  "serverHash": "0000000000000000000000000000000000000000000000000000000000000000",
                  "maxPhysicians": 5,
                  "modules": ["HCE", "TRIAJE"]
                }
                """;

        mockMvc.perform(post("/admin/licensing/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void generateLicense_payloadInvalido_devuelve400() throws Exception {
        String body = """
                {
                  "licenseId": "INVALID",
                  "ipsName": "",
                  "nit": "900999888-7",
                  "serverHash": "short",
                  "maxPhysicians": 5,
                  "modules": ["HCE"]
                }
                """;

        mockMvc.perform(post("/admin/licensing/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void status_sinLicencia_devuelveActivoFalse() throws Exception {
        mockMvc.perform(get("/admin/licensing/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void generateLicense_sinRolAdmin_devuelve403() throws Exception {
        String body = """
                {
                  "licenseId": "LIC-2026-9999",
                  "ipsName": "IPS Demo Test",
                  "nit": "900999888-7",
                  "serverHash": "0000000000000000000000000000000000000000000000000000000000000000",
                  "maxPhysicians": 5,
                  "modules": ["HCE"]
                }
                """;

        mockMvc.perform(post("/admin/licensing/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}