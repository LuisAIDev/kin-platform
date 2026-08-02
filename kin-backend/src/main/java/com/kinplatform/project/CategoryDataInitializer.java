package com.kinplatform.project;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Siembra el catálogo de categorías cuando la tabla está vacía (dev con
 * {@code ddl-auto: update}, donde Flyway está deshabilitado). En producción el
 * seed lo aporta la migración Flyway {@code V6}; el initializer es idempotente
 * (no inserta si ya hay datos). Una nueva categoría se agrega como dato, sin
 * tocar código.
 */
@Component
public class CategoryDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public CategoryDataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        categoryRepository.saveAll(List.of(
            category("TECNOLOGIA", "Tecnología e Innovación", 1, "#6366f1"),
            category("EMPRESARIAL", "Empresarial", 2, "#0ea5e9"),
            category("AGROINDUSTRIA", "Agroindustria", 3, "#84cc16"),
            category("SALUD", "Salud", 4, "#ef4444"),
            category("EDUCACION", "Educación", 5, "#f59e0b"),
            category("IMPACTO_SOCIAL", "Impacto Social", 6, "#f43f5e"),
            category("MEDIO_AMBIENTE", "Medio Ambiente", 7, "#22c55e"),
            category("INDUSTRIA", "Industria", 8, "#64748b"),
            category("GOBIERNO", "Gobierno", 9, "#8b5cf6"),
            category("FINTECH", "Fintech", 10, "#06b6d4"),
            category("COMERCIO", "Comercio", 11, "#f97316"),
            category("TURISMO", "Turismo", 12, "#14b8a6"),
            category("GASTRONOMIA", "Gastronomía", 13, "#e11d48"),
            category("LOGISTICA", "Logística", 14, "#78716c"),
            category("CREATIVIDAD", "Creatividad", 15, "#a855f7"),
            category("MARKETING_DIGITAL", "Marketing Digital", 16, "#ec4899"),
            category("INVESTIGACION", "Investigación", 17, "#3b82f6")
        ));
    }

    private Category category(String code, String name, int displayOrder, String color) {
        return Category.builder()
            .code(code)
            .name(name)
            .displayOrder(displayOrder)
            .color(color)
            .active(true)
            .build();
    }
}
