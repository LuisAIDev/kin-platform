package com.kinplatform.kin.health.documents.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Documento clínico compartido entre médico y paciente (ADR-036).
 *
 * <p>Entidad de dominio inmutable: referencia un archivo almacenado
 * ({@code storageKey}), quién lo subió ({@code uploadedBy}), a qué paciente
 * pertenece y el médico asociado. Visibilidad controlada por rol: el paciente
 * ve sus documentos; el médico ve los de sus pacientes activos.</p>
 */
public record ClinicalDocument(
        UUID id,
        String fileName,
        long fileSize,
        String mimeType,
        String storageKey,
        UUID uploadedBy,
        UUID patientId,
        UUID physicianId,
        String description,
        DocumentStatus status,
        OffsetDateTime uploadedAt,
        OffsetDateTime createdAt,
        String extractedText) {

    public ClinicalDocument {
        if (id == null || uploadedBy == null || patientId == null) {
            throw new IllegalArgumentException("id/uploadedBy/patientId no pueden ser null");
        }
        fileName = fileName == null ? "" : fileName;
        storageKey = storageKey == null ? "" : storageKey;
        mimeType = mimeType == null ? "" : mimeType;
        description = description == null ? "" : description;
        status = status == null ? DocumentStatus.ACTIVE : status;
        uploadedAt = uploadedAt == null ? OffsetDateTime.now() : uploadedAt;
        createdAt = createdAt == null ? OffsetDateTime.now() : createdAt;
        extractedText = extractedText == null ? "" : extractedText;
    }

    public static ClinicalDocument of(
            UUID id,
            String fileName,
            long fileSize,
            String mimeType,
            String storageKey,
            UUID uploadedBy,
            UUID patientId,
            UUID physicianId,
            String description,
            DocumentStatus status,
            OffsetDateTime uploadedAt,
            OffsetDateTime createdAt) {
        return new ClinicalDocument(
                id, fileName, fileSize, mimeType, storageKey, uploadedBy, patientId, physicianId,
                description, status, uploadedAt, createdAt, "");
    }

    public static ClinicalDocument of(
            UUID id,
            String fileName,
            long fileSize,
            String mimeType,
            String storageKey,
            UUID uploadedBy,
            UUID patientId,
            UUID physicianId,
            String description,
            DocumentStatus status,
            OffsetDateTime uploadedAt,
            OffsetDateTime createdAt,
            String extractedText) {
        return new ClinicalDocument(
                id, fileName, fileSize, mimeType, storageKey, uploadedBy, patientId, physicianId,
                description, status, uploadedAt, createdAt, extractedText);
    }

    public boolean isActive() {
        return status == DocumentStatus.ACTIVE;
    }

    /** {@code true} si existe texto extraído utilizable para el análisis con IA. */
    public boolean hasExtractedText() {
        return extractedText != null && !extractedText.isBlank();
    }

    /** Copia con el texto extraído actualizado (cache de extracción perezosa). */
    public ClinicalDocument withExtractedText(String text) {
        return new ClinicalDocument(
                id, fileName, fileSize, mimeType, storageKey, uploadedBy, patientId, physicianId,
                description, status, uploadedAt, createdAt, text == null ? "" : text);
    }

    /** Soft delete: marca el documento como eliminado. */
    public ClinicalDocument deleted() {
        return new ClinicalDocument(
                id, fileName, fileSize, mimeType, storageKey, uploadedBy, patientId, physicianId,
                description, DocumentStatus.DELETED, uploadedAt, createdAt, extractedText);
    }
}
