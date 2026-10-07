package com.kinplatform.kin.medical.catalog;

import com.kinplatform.kin.medical.catalog.api.CatalogController;
import com.kinplatform.kin.medical.catalog.loader.CatalogLoaderService;
import com.kinplatform.common.config.SecurityConfig;
import com.kinplatform.common.security.JwtAuthenticationFilter;
import com.kinplatform.common.security.RateLimitingFilter;
import com.kinplatform.common.security.SubscriptionAccessFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CatalogController.class)
@Import(SecurityConfig.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogSearchService searchService;

    @MockBean
    private CatalogLoaderService loaderService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @BeforeEach
    void setUp() throws Exception {
        when(searchService.searchCups(anyString(), anyInt())).thenReturn(
                List.of(new CupsSuggestion("890201", "CONSULTA MEDICINA GENERAL", "CONSULTA")));
        passThrough(jwtAuthenticationFilter);
        passThrough(rateLimitingFilter);
        passThrough(subscriptionAccessFilter);
    }

    private void passThrough(jakarta.servlet.Filter filter) throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2, FilterChain.class);
            chain.doFilter(invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "PHYSICIAN")
    void searchCups_asPhysician_returns200() throws Exception {
        mockMvc.perform(get("/catalogs/cups/search").param("q", "consulta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cupsCode").value("890201"));
    }

    @Test
    @WithMockUser(roles = "IPS_FACTURADOR")
    void searchCups_asFacturador_returns200() throws Exception {
        mockMvc.perform(get("/catalogs/cups/search").param("q", "consulta"))
                .andExpect(status().isOk());
    }

    @Test
    void searchCups_asAnonymous_isForbidden() throws Exception {
        // Nota: la app devuelve 403 para anonimo (no 401); no hay entry point 401.
        mockMvc.perform(get("/catalogs/cups/search").param("q", "consulta"))
                .andExpect(status().isForbidden());
    }
}


