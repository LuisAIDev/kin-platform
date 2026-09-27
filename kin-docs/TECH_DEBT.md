### TD-CI-1: Agregar `ApplicationContextTest` al pipeline de CI/CD de GitHub Actions
- **Prioridad**: Alta.
- **Contexto**: el fallo de arranque `c2a645a` (bean `WebClient` faltante) pasó el CI porque solo corrían tests unitarios con mocks. `ApplicationContextTest` (`@SpringBootTest @ActiveProfiles("test")` + Testcontainers) detecta beans faltantes y aplica Flyway V1..V75 completo.
- **Fix**: en `.github/workflows/*`, asegurar Docker/Testcontainers y ejecutar al menos `./mvnw.cmd test -Dtest=ApplicationContextTest`. Requisito: `DOCKER_HOST` configurado en el runner.
- **Owner**: DevOps/Backend. **Estimado**: 0.5 día.

### TD-CI-2: Bloquear merge si `ApplicationContextTest` falla
- **Prioridad**: Alta.
- **Fix**: configurar branch protection / required status check en GitHub de modo que un PR no pueda mergearse si `ApplicationContextTest` no pasa. Complementa TD-CI-1.
- **Owner**: DevOps. **Estimado**: 0.25 día.

### TD-CI-3: Agregar `HceSchemaValidationTest` al pipeline de CI/CD de GitHub Actions
- **Prioridad**: Alta.
- **Contexto**: el fallo de arranque `c2a645a` (bean `WebClient` faltante) pasó el CI porque solo corrían tests unitarios con mocks. `HceSchemaValidationTest` (`@SpringBootTest @ActiveProfiles("test")` + Testcontainers) detecta beans faltantes y aplica Flyway V1..V76 completo.
- **Fix**: en `.github/workflows/*`, asegurar Docker/Testcontainers y ejecutar al menos `./mvnw.cmd test -Dtest=HceSchemaValidationTest`. Requisito: `DOCKER_HOST` configurado en el runner.
- **Owner**: DevOps/Backend. **Estimado**: 0.5 día.

### TD-CI-4: Bloquear merge si `HceSchemaValidationTest` falla
- **Prioridad**: Alta.
- **Fix**: configurar branch protection / required status check en GitHub de modo que un PR no pueda mergearse si `HceSchemaValidationTest` no pasa. Complementa TD-CI-3.
- **Owner**: DevOps. **Estimado**: 0.25 día.

---

## Deuda técnica resuelta (2026-09-25) - V76 HCE SMALLINT/INTEGER mismatch
- ✅ **Mismatch SMALLINT/INTEGER en 20 columnas HCE (V76)**:
  - **Problema**: V75 declaró 20 columnas como `SMALLINT` (ej. `severity_self_reported`, `bp_systolic`, `stratum`, etc.) pero las entidades JPA usan `Integer`. En producción con `ddl-auto=validate` fallaba: `wrong column type encountered ... found [int2 (SMALLINT)], but expecting [integer (Types#INTEGER)]`.
  - **Fix V76**: `V76__align_hce_integer_columns.sql` — 20 `ALTER COLUMN TYPE INTEGER` (widening lossless, mantiene CHECK constraints, sin pérdida de datos). Verificado en Docker local (PostgreSQL 16) y en **Neon prod**: `flyway_schema_history version=76, success=t` (installed_on 2026-09-25 05:57:46), 14 tablas presentes.
  - **Test preventivo**: `HceSchemaValidationTest` (`@SpringBootTest` + Testcontainers PG18 + `spring.jpa.hibernate.ddl-auto=validate`) — detecta mismatches schema/entidad ANTES de llegar a prod.
  - **Nota**: los tests unitarios con mocks NO detectan esto (usaban `Integer` en mocks). `ApplicationContextTest` con `ddl-auto=validate` SÍ lo detecta.

---

### TD-INTEGRATION-REPO-HCE: 13 HCE Integration Tests @Disabled pendientes Docker en runner
- **Prioridad**: Media.
- **Problema**: Los 13 tests de integración HCE (`*IntegrationTest`) usan `@SpringBootTest` + Testcontainers PostgreSQL 18, pero el entorno de desarrollo/CI actual no tiene Docker daemon disponible. H2 no soporta los tipos PostgreSQL-específicos de las 14 entidades HCE (`TEXT[]`, `interval second(18,9)`, `uuid` con funciones nativas, CHECK constraints con arrays).
- **Estado actual**: 13 esqueletos `@Disabled("Docker required - TD-INTEGRATION-REPO-HCE")` con tests vacíos (4-7 por servicio). Tests unitarios con mocks (124 tests PASS) cubren la lógica de negocio. Validación de schema con `HceSchemaValidationTest` (requiere Testcontainers PG18).
- **Lista de esqueletos @Disabled**:
  1. `EncounterServiceIntegrationTest` (3 tests) — Día 7
  2. `PatientHistoryServiceIntegrationTest` (5 tests) — Día 8
  3. `AnamnesisServiceIntegrationTest` (4 tests) — Día 11
  4. `ReferralServiceIntegrationTest` (6 tests) — Día 11
  5. `PhysicalExamServiceIntegrationTest` (4 tests) — Día 9
  6. `DiagnosesServiceIntegrationTest` (3 tests) — Día 9
  7. `TreatmentPlanServiceIntegrationTest` (5 tests) — Día 10
  8. `MedicalOrderServiceIntegrationTest` (5 tests) — Día 10
  9. `InformedConsentServiceIntegrationTest` (5 tests) — Día 11
  10. `ReferralServiceIntegrationTest` (6 tests) — Día 11
  10. `DischargeSummaryServiceIntegrationTest` (7 tests) — Día 12
  11. `ClinicalAttachmentServiceIntegrationTest` (6 tests) — Día 12
  11. `ObstetricHistoryServiceIntegrationTest` (4 tests) — Día 13
  12. `SurgicalHistoryServiceIntegrationTest` (7 tests) — Día 13
- **Fix objetivo**: Cuando el runner de CI/CD tenga Docker/Testcontainers disponible, implementar los tests en los esqueletos y quitar `@Disabled`. Migrar a `@SpringBootTest` + Testcontainers PostgreSQL 18 real (igual que `ApplicationContextTest` y `HceSchemaValidationTest`).
- **Owner**: Backend/DevOps. **Estimado**: 1 día (configurar Docker en CI + 13 tests).
- **Referencia**: Bloque 2a HCE completado (Días 7-14).

---

### TD-MAPPERS-1: Mappers Entity → Response DTO pendientes
- **Prioridad**: Alta.
- **Problema**: Los 15 Response DTOs (EncounterResponse, PatientIdentificationResponse, AnamnesisResponse, PatientHistoryResponse, PhysicalExamResponse, DiagnosisResponse, TreatmentPlanResponse, MedicalOrderResponse, InformedConsentResponse, ReferralResponse, DischargeSummaryResponse, ClinicalAttachmentResponse, ObstetricHistoryResponse, SurgicalHistoryResponse, HceSummaryResponse) son records sin mappers ni tests.
- **Contexto**: Creados en Bloque 3 (Día 17). Se usarán en los Controllers REST (Bloque 4).
- **Fix**: Implementar mapper por entidad (clase @Component con métodos estáticos o MapStruct si se añade la dependencia) que convierta Entity → Response DTO. Agregar test de mapeo por mapper (≥1 test por mapper verificando todos los campos).
- **Owner**: Backend. **Estimado**: 0.5 día (15 mappers + tests).
- **Referencia**: Bloque 4 Controllers (Día 18-22).

---

## Deuda técnica pre-existente del suite de tests (catalogada 2026-09-27)

> **Evidencia de pre-existencia**: baseline `ef28df7` = `Tests run: 4120, Failures: 6, Errors: 24, Skipped: 108`.
> Actual `8d0fafe` = `Tests run: 4118, Failures: 6, Errors: 20, Skipped: 108`.
> Los 4 errores de diferencia son `EncounterServiceTest` (2) + `PatientIdentificationServiceTest` (2), regresiones de Día 18 (`b1c7b90`) ya corregidas. Los **26 rojos restantes (6 Failures + 20 Errors) son idénticos en baseline y actual**: NO fueron introducidos por el trabajo de Día 20.

### TD-INTEGRATION-A (CRÍTICO): 8 tests JPA integration fallan por FK
- **Prioridad**: Media.
- **Tests**:
  - `JpaDashboardRepositoryIntegrationTest` (1 error) — `fk_reminders_user`
  - `JpaPhysicianRepositoryIntegrationTest` (3 errors) — `fk_ca_physician`, `fk_ppa_physician`
  - `JpaTelemedicineRepositoryIntegrationTest` (4 errors) — `fk_appointments_patient`, `fk_messages_sender`
- **Causa**: las fixtures insertan filas hijas (reminders, clinical_alerts, physician_patient_assignments, appointments, messages) sin sembrar primero el `users` padre.
- **Fix**: `@BeforeEach` para sembrar `users` (patient/physician) antes de insertar hijos.
- **Owner**: Backend. **Estimado**: 0.5 día.

### TD-INTEGRATION-D: 4 tests Outbox fallan
- **Prioridad**: Media.
- **Tests**: `OutboxRelayIntegrationTest` (3 failures + 1 error).
- **Causa**: `PENDING != PUBLISHED` (relay asíncrono/timing), `domainEventBus.publish` no invocado, y NPE al buscar métrica micrometer (`Search.counter()` null).
- **Fix**: sincronizar el relay (await); registrar/verificar el `Counter` correcto.
- **Owner**: Backend. **Estimado**: 0.5 día.

### TD-INTEGRATION-E: 5 tests DocumentStorageQuota fallan
- **Prioridad**: Media.
- **Tests**: `DocumentStorageQuotaPortImplTest` (5 errors).
- **Causa**: `UnnecessaryStubbingException` en `setUp` + `No active pricing plan found for vertical SALUD_PERSONAL` (plan no seedeado).
- **Fix**: limpiar stubs no usados (o `lenient`); seedear el plan SALUD_PERSONAL en el contexto de test.
- **Owner**: Backend. **Estimado**: 0.5 día.

### TD-INTEGRATION-TRIAGE: 8 tests triage/differential fallan
- **Prioridad**: Media.
- **Tests**: `JpaTriageRepositoryIntegrationTest` (1 failure + 6 errors), `JpaDifferentialRepositoryIntegrationTest` (1 failure).
- **Causa**: catálogo/relaciones no cargados (`NoSuchElement`, `ArrayIndexOutOfBounds`), FK `fk_scr_symptom`, y aserciones de catálogo (100 condiciones / 200 relaciones) que no se cumplen.
- **Fix**: seedear catálogo triage/differential antes de los tests.
- **Owner**: Backend. **Estimado**: 1 día.

### TD-INTEGRATION-SCHEDULING: 1 test falla
- **Prioridad**: Baja.
- **Tests**: `SchedulingIntegrationTest` (1 failure).
- **Causa**: el flujo completo espera 201 pero recibe 409 (conflicto de slot/traslape entre tests).
- **Fix**: revisar aislamiento de datos de disponibilidad entre tests.
- **Owner**: Backend. **Estimado**: 0.25 día.

---

*Last Updated: 2026-09-27*  
*Owner: Backend Team*  
*Next Review: Sprint Planning*

---