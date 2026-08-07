package com.kinplatform.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @Test
    void getActive_deberiaDevolverCategorias() throws Exception {
        when(categoryService.getActive())
                .thenReturn(List.of(new CategoryResponse(
                        java.util.UUID.randomUUID(), "SALUD", "Salud", "Salud", 1, "icon", "#123456", true)));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(categoryService))
                .build();

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SALUD"))
                .andExpect(jsonPath("$[0].name").value("Salud"));
    }

    @Test
    void getActive_invocacionDirecta() {
        when(categoryService.getActive()).thenReturn(List.of());

        ResponseEntity<List<CategoryResponse>> response = new CategoryController(categoryService).getActive();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().size());
    }
}
