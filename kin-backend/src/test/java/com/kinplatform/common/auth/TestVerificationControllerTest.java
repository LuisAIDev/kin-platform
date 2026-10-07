package com.kinplatform.common.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TestVerificationControllerTest {

    private TestVerificationStore store;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        store = new TestVerificationStore();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestVerificationController(store)).build();
    }

    @Test
    void conEnlaceAlmacenado_deberiaResponder200ConLink() throws Exception {
        store.put("a@kin.test", "https://kin.test/verify-email?token=abc");

        mockMvc.perform(get("/auth/test/verification-link").param("email", "a@kin.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").value("https://kin.test/verify-email?token=abc"));
    }

    @Test
    void emailSinEnlace_deberiaResponder404() throws Exception {
        mockMvc.perform(get("/auth/test/verification-link").param("email", "nadie@kin.test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void storeVacio_deberiaResponder404() throws Exception {
        mockMvc.perform(get("/auth/test/verification-link").param("email", "a@kin.test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reemplazo_deberiaDevolverElUltimoEnlace() throws Exception {
        store.put("a@kin.test", "https://kin.test/verify-email?token=old");
        store.put("a@kin.test", "https://kin.test/verify-email?token=new");

        mockMvc.perform(get("/auth/test/verification-link").param("email", "a@kin.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.link").value("https://kin.test/verify-email?token=new"));
    }

    @Test
    void removeYClear_deberianDejarElStoreSinEnlace() throws Exception {
        store.put("a@kin.test", "https://kin.test/verify-email?token=abc");
        store.remove("a@kin.test");
        store.put("b@kin.test", "https://kin.test/verify-email?token=def");
        store.clear();

        org.junit.jupiter.api.Assertions.assertTrue(store.isEmpty());
    }

    @Test
    void concurrenciaBasica_deberiaCapturarTodasLasEntradas() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 100; i++) {
            String email = "user-" + i + "@kin.test";
            pool.submit(() -> store.put(email, "https://kin.test/verify-email?token=" + email));
        }
        pool.shutdown();
        org.junit.jupiter.api.Assertions.assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        org.junit.jupiter.api.Assertions.assertEquals(100, store.size());
    }
}

