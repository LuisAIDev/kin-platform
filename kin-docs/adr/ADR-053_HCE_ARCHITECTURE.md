# ADR-053: Arquitectura del Módulo HCE (Historia Clínica Electrónica)

## Estado
Aceptado

## Contexto
El módulo HCE implementa la Historia Clínica Electrónica según Resolución 839/1995 (Colombia). Requiere:
- 14 entidades clínicas (Encounter, PatientIdentification, Anamnesis, PatientHistory, PhysicalExam, Diagnoses, TreatmentPlan, MedicalOrder, InformedConsent, Referral, DischargeSummary, ClinicalAttachment, ObstetricHistory, SurgicalHistory)
- Cumplimiento de estándares CIE-10, CUPS, RIPS, MIPRES
- Trazabilidad completa con auditoría
- Validaciones de reglas clínicas (ej. única diagnóstico PRINCIPAL, gravida >= suma outcomes)

## Decisión
Arquitectura en capas:
1. **Entidades JPA** (14 tablas) con índices optimizados, enums, JSONB, columnas GENERATED (length_of_stay, bmi)
2. **Repositories** (14) extendiendo JpaRepository con queries personalizadas
3. **Servicios** (14) con lógica de negocio, validaciones, herencia de patientId/physicianId, ownership
4. **DTOs** con Bean Validation (CIE-10 regex, rangos vitales, enums)
5. **Tests unitarios** (Mockito, ≥8 por servicio) + **Tests integración @Disabled** (Testcontainers PG18)
6. **Validación esquema**: `HceSchemaValidationTest` con `ddl-auto=validate` + Testcontainers PG18

## Reglas de negocio clave
- `Encounter`: status IN_PROGRESS → COMPLETED requiere diagnóstico PRINCIPAL + plan de manejo
- `Diagnoses`: único PRINCIPAL activo por encounter (constraint + service)
- `MedicalOrder`: hereda encounterId/patientId/physicianId del TreatmentPlan
- `InformedConsent`: status VALID → REVOKED/EXPIRED, auditoría de firma
- `ObstetricHistory`: gravida >= para + abortions + ectopic + stillbirths
- `PatientIdentification`: unique (patient_id, document_type, document_number)

## Migraciones
- V75: 14 tablas HCE + triggers updated_at + vista hce_complete_view
- V76: 20 columnas SMALLINT → INTEGER (V75 usó SMALLINT, entidades usan Integer)

## Testing
- Unitarios: 124 tests (Mockito) - 14 servicios × ≥8 tests
- Integración: 13 esqueletos @Disabled (Testcontainers PG18) - requieren Docker
- Schema validation: `HceSchemaValidationTest` (Testcontainers PG18 + `ddl-auto=validate`)
- Contexto: `ApplicationContextTest` (Testcontainers PG18)

## Consecuencias
- ✅ Cobertura ≥80% módulo HCE
- ✅ Validación esquema en CI (cuando Docker disponible)
- ⚠️ 13 tests integración @Disabled (requieren Docker en runner CI)
- ⚠️ V76 corrige V75 (SMALLINT → INTEGER)

## Referencias
- V75: `V75__complete_hce_res_839_1995.sql`
- V76: `V76__align_hce_integer_columns.sql`
- TD-INTEGRATION-REPO-HCE (13 integration tests pendientes Docker)