package com.kinplatform.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository);
    }

    @Test
    void getActive_deberiaDevolverSoloActivasOrdenadasPorDisplayOrder() {
        var salud = category("SALUD", "Salud", 4);
        var tecnologia = category("TECNOLOGIA", "Tecnología e Innovación", 1);
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAsc())
            .thenReturn(List.of(tecnologia, salud));

        var result = service.getActive();

        assertEquals(2, result.size());
        assertEquals("TECNOLOGIA", result.get(0).code());
        assertEquals("SALUD", result.get(1).code());
        assertTrue(result.get(0).active());
    }

    @Test
    void getActive_noDebeIncluirCategoriasInactivas() {
        when(categoryRepository.findByActiveTrueOrderByDisplayOrderAsc())
            .thenReturn(List.of(category("COMERCIO", "Comercio", 11)));

        var result = service.getActive();

        assertEquals(1, result.size());
        assertTrue(result.get(0).active());
    }

    @Test
    void requireByCode_deberiaDevolverLaCategoria() {
        when(categoryRepository.findByCode("SALUD"))
            .thenReturn(Optional.of(category("SALUD", "Salud", 4)));

        assertEquals("SALUD", service.requireByCode("SALUD").getCode());
    }

    @Test
    void requireByCode_desconocido_deberiaLanzar() {
        when(categoryRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.requireByCode("XYZ"));
    }

    @Test
    void findByCode_blankONulo_deberiaDevolverVacio() {
        assertTrue(service.findByCode("  ").isEmpty());
        assertTrue(service.findByCode(null).isEmpty());
    }

    private Category category(String code, String name, int order) {
        return Category.builder().code(code).name(name).displayOrder(order).active(true).build();
    }
}
