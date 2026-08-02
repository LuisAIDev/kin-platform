package com.kinplatform.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio del catálogo de categorías (SaaS-ready). Expone solo categorías
 * activas ordenadas por {@code displayOrder} y resuelve una categoría por
 * {@code code} para el alta/edición de proyectos.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActive() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
            .stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Category> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return categoryRepository.findByCode(code.trim());
    }

    @Transactional(readOnly = true)
    public Category requireByCode(String code) {
        return findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + code));
    }
}
