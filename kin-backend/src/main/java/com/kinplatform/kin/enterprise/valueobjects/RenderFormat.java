package com.kinplatform.kin.enterprise.valueobjects;

/**
 * Formato de salida de los documentos del proyecto empresarial (value object).
 *
 * <p>Catálogo de formatos que el puerto {@code DocumentRenderer} puede
 * producir. PDF es el formato objetivo del Milestone 3; DOCX y PPTX quedan
 * preparados para versiones posteriores (múltiples renderizadores).</p>
 */
public enum RenderFormat {
    PDF,
    DOCX,
    PPTX
}
