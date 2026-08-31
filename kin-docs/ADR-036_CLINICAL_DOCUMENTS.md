# ADR-036: Documentos Clínicos Compartidos — KIN Health

## Estado
**Aceptado** — 2026-08-30

## Contexto

KIN Health necesita un módulo de **documentos clínicos compartidos** (Área 11) que permita al médico subir archivos (resultados de laboratorio, informes, recetas escaneadas, etc.) y compartirlos con sus pacientes asignados. El paciente debe poder ver y descargar sus propios documentos.

Principios aplicados (intactos):
- **Java decide. El LLM únicamente comunica.** La gestión de documentos es puramente funcional, sin intervención del LLM.
- **Aditividad**: nuevo bounded context `kin.health.documents` y tabla nueva (V34), sin tocar contratos congelados.
- **Seguridad por relación**: toda operación sobre un documento exige relación `ACTIVE` entre médico y paciente (Área 5).
- **Auditoría**: toda operación (subida, descarga, eliminación) queda registrada (Área 12).
- **Outbox**: los eventos de dominio se publican vía `OutboxEventPublisher` para entrega asíncrona.
- **Almacenamiento configurable**: archivos en sistema de archivos local (futuro: S3/Azure Blob).

## Decisión

Crear un bounded context `com.kinplatform.kin.health.documents` (Clean Architecture + DDD):

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `kin.health.documents.domain` | `ClinicalDocument` (entidad inmutable), `DocumentStatus` enum |
| Puertos | `kin.health.documents.port` | `ClinicalDocumentRepository` (save, findById, findActiveByPatientId, findActiveByPhysicianId, findActiveByPatientIdAndPhysicianId, countActiveByPatientId) |
| Adaptadores | `kin.health.documents.adapter` | `ClinicalDocumentEntity` (JPA), `ClinicalDocumentJpaRepository` (Spring Data), `JpaClinicalDocumentRepository` (adaptador) |
| Infraestructura | `kin.health.documents.infrastructure` | `DocumentStorage` (almacenamiento local configurable) |
| Configuración | `kin.health.documents.config` | `DocumentProperties` (`kin.health.documents.*`), `DocumentConfig` |
| Eventos | `kin.health.documents.event` | `DocumentUploadedEvent`, `DocumentDeletedEvent` |
| Servicio | `kin.health.documents.api` | `DocumentService` (upload, list, download, delete), controladores REST, excepciones |

### Modelo de dominio

```
ClinicalDocument (UUID id, fileName, fileSize, mimeType, storageKey,
    uploadedBy, patientId, physicianId, description, DocumentStatus status,
    uploadedAt, createdAt)

DocumentStatus: ACTIVE | ARCHIVED | DELETED
```

Reglas de negocio:
- Solo el médico que subió el documento o un ADMIN puede eliminarlo.
- El paciente solo ve/ Descarga sus propios documentos (los que pertenecen a su `patientId`).
- El médico ve los documentos de sus pacientes con relación `ACTIVE`.
- La eliminación es soft delete (`status → DELETED`); el archivo se borra del almacenamiento.
- El almacenamiento guarda archivos en una carpeta configurable (`kin.health.documents.storage-path`) usando la `storageKey` (UUID + nombre sanitizado).

### API REST

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| `POST` | `/health/documents/upload` | PHYSICIAN, ADMIN | Sube un archivo multipart para un paciente |
| `GET` | `/health/documents/patients/{patientId}` | PHYSICIAN, ADMIN | Lista documentos de un paciente (vía médico) |
| `GET` | `/health/documents/my` | PATIENT | Lista los propios documentos |
| `GET` | `/health/documents/{id}/download` | PHYSICIAN, PATIENT, ADMIN | Descarga un documento |
| `DELETE` | `/health/documents/{id}` | PHYSICIAN, ADMIN | Elimina (soft delete) un documento |

### Integración con otros módulos

- **Área 5 (RelationshipAccessValidator)**: toda operación verifica `requireActiveRelationship(physicianId, patientId)`.
- **Área 12 (AuditService)**: se audita `UPLOAD_DOCUMENT`, `DOWNLOAD_DOCUMENT`, `DELETE_DOCUMENT` con `AuditResourceType.DOCUMENTO`.
- **Notificaciones (NotificationCountsService)**: el badge de documentos activos del paciente usa `documentService.activeDocumentCountForPatient(patientId)`.
- **Outbox (OutboxEventPublisher)**: `DocumentUploadedEvent` y `DocumentDeletedEvent` se publican transaccionalmente.

### Configuración

```yaml
kin:
  health:
    documents:
      enabled: ${KIN_HEALTH_DOCUMENTS_ENABLED:true}
      storage-path: ${KIN_HEALTH_DOCUMENTS_STORAGE_PATH:./storage/documents}
      max-file-size: ${KIN_HEALTH_DOCUMENTS_MAX_FILE_SIZE:10485760}
```

### Migración Flyway V34

Tabla `clinical_documents` con columnas: `id`, `file_name`, `file_size`, `mime_type`, `storage_key`, `uploaded_by`, `patient_id`, `physician_id`, `description`, `status`, `uploaded_at`, `created_at`.

Índices: `idx_cd_patient_status` (patient_id, status), `idx_cd_physician` (physician_id, status).

## Alternativas consideradas

| Opción | Decisión |
|--------|----------|
| **S3/Azure Blob desde el inicio** | ❌ Complejidad innecesaria para el MVP; se usa filesystem local con interfaz intercambiable |
| **AOP para auditoría** | ❌ Inyección explícita de `AuditService` en `DocumentService` (consistente con otros módulos) |
| **Hardcodear rutas de acceso** | ❌ `DoctorOnly`/`PatientOnly`/`Shared`; se usa `uploadedBy` + `physicianId` para control dinámico |
| **Eliminación física** | ❌ Soft delete (`DELETED`) para preservar auditoría; archivo borrado del storage |
| **Documentos vinculados a sesiones** | ❌ Tabla `document_sessions` para adjuntar documentos a una sesión (no requerido en V1) |

## Consecuencias

**Positivas**:
- Compartir documentos clínicos entre médico y paciente de forma segura y trazada.
- Control granular por relación ACTIVE.
- Auditoría completa de todas las operaciones.
- Badges de notificaciones actualizados (contador de documentos).
- Almacenamiento configurable y extensible.

**Negativas**:
- Nuevo BC (+1 tabla V34, +2 eventos de dominio, +3 tests).
- El almacenamiento local no escala horizontal sin una capa de objetos (S3).
- La sanitización del nombre de archivo es básica (mejorar en futuras iteraciones).

## Referencias

- Migración `V34__create_clinical_documents.sql`
- `kin-docs/ADR-035_AUDIT.md` (auditoría)
- `kin-docs/ADR-031_PHYSICIAN_PORTAL.md` (permisos por relación)
- `kin-docs/ADR-026_TRANSACTIONAL_OUTBOX.md` (entrega asíncrona de eventos)
