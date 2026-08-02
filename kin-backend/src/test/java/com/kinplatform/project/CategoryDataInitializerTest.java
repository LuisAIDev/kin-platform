package com.kinplatform.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDataInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void run_deberiaSembrarLas17Categorias_cuandoLaTablaEstaVacia() {
        when(categoryRepository.count()).thenReturn(0L);

        new CategoryDataInitializer(categoryRepository).run(null);

        @SuppressWarnings("rawtypes")
        var captor = ArgumentCaptor.forClass(Iterable.class);
        verify(categoryRepository).saveAll(captor.capture());
        assertEquals(17, ((List<?>) captor.getValue()).size());
    }

    @Test
    void run_noDebeSembrar_cuandoYaExistenCategorias() {
        when(categoryRepository.count()).thenReturn(5L);

        new CategoryDataInitializer(categoryRepository).run(null);

        verify(categoryRepository, never()).saveAll(any());
    }
}
