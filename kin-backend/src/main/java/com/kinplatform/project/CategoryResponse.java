package com.kinplatform.project;

/**
 * Respuesta pública de una categoría del catálogo (SaaS-ready).
 */
public record CategoryResponse(
    java.util.UUID id,
    String code,
    String name,
    String description,
    int displayOrder,
    String icon,
    String color,
    boolean active
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getCode(),
            category.getName(),
            category.getDescription(),
            category.getDisplayOrder(),
            category.getIcon(),
            category.getColor(),
            category.isActive()
        );
    }
}
