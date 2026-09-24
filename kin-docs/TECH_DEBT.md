# Technical Debt Register — KIN Medical Platform

## Overview
This document tracks known technical debt items that must be addressed before production release or in upcoming sprints.

---

## TD-001: Mockito Mocks - JwtService.generateToken Signature Change

**Priority**: Medium  
**Impact**: Blocks test execution for 14 test methods  
**Effort**: ~2 hours  

### Description
The `JwtService.generateToken()` method signature was changed to include `organizationId` parameter for multi-tenant support:
```java
// Old signature (used in 14 test mocks)
generateToken(UUID userId, String email, String role, String platform)

// New signature
generateToken(UUID userId, String email, String role, String platform, UUID organizationId)
```

### Affected Tests
| Test Class | Methods Affected | Location |
|------------|------------------|----------|
| `AuthServiceImplTest` | 7 methods | `src/test/java/com/kinplatform/auth/AuthServiceImplTest.java` |
| `VerticalAccessTest` | 7 methods | `src/test/java/com/kinplatform/auth/VerticalAccessTest.java` |

### Required Fix
Update all mock stubs from:
```java
when(jwtService.generateToken(any(), anyString(), anyString(), anyString())).thenReturn("token");
```
To:
```java
when(jwtService.generateToken(any(), anyString(), anyString(), anyString(), any())).thenReturn("token");
```

### Acceptance Criteria
- All 14 test methods in `AuthServiceImplTest` and `VerticalAccessTest` pass
- No Mockito `PotentialStubbingProblem` warnings

---

## TD-002: Testcontainers / Docker Dependency for Integration Tests

**Priority**: High  
**Impact**: 32 integration tests skipped/failed due to missing Docker  
**Effort**: Infrastructure setup  

### Description
Multiple integration tests use Testcontainers with PostgreSQL 18 but Docker Desktop is not available in the CI/CD environment.

### Affected Tests
All tests extending `PostgresTestSupport` or using `@SpringBootTest` with database:
- `EnterpriseProjectJpaIntegrationTest`
- `RedisKnowledgeRepositoryTest`
- `ProjectReportJsonbJpaTest`
- `AiUsageJpaIntegrationTest`
- `ProjectQuotaFreeRegressionTest`
- `ChatStreamSecurityAsyncTest`
- `EnterpriseRuntimeCertificationTest`
- `AIAssistIntegrationTest`
- `AuditIntegrationTest`
- `JpaDashboardRepositoryIntegrationTest`
- `JpaDifferentialRepositoryIntegrationTest`
- `DocumentIntegrationTest`
- `DocumentStorageQuotaPortImplTest`
- `FollowUpIntegrationTest`
- `JpaPhysicianRepositoryIntegrationTest`
- `PhysicianPatientRelationshipIntegrationTest`
- `SchedulingIntegrationTest`
- `JpaTelemedicineRepositoryIntegrationTest`
- `JpaTriageRepositoryIntegrationTest`
- `OutboxRelayDisabledIntegrationTest`
- `OutboxRelayIntegrationTest`
- `PricingPlanFeaturesJpaTest`
- `PricingPlanV17BillingPeriodTest`
- `ProjectStructuredInfoJpaTest`
- `JpaTelemedicineRepositoryIntegrationTest`
- `JpaTriageRepositoryIntegrationTest`
- `SecurityConfigCorsTest`

### Required Fix
- Provision Docker-enabled CI/CD runners
- Or migrate to H2 in-memory database for CI tests
- Or use shared PostgreSQL test instance

---

## TD-003: SecurityConfig CORS Configuration Null Pointer

**Priority**: Low  
**Impact**: 2 test failures in `SecurityConfigCorsTest`  
**Effort**: ~30 minutes  

### Description
`CorsConfiguration.getAllowedOrigins()` returns null causing NPE in tests.

---

## TD-004: DocumentStorageQuotaPortImplTest Unnecessary Stubbing

**Priority**: Low  
**Impact**: 4 test warnings in `DocumentStorageQuotaPortImplTest`  
**Effort**: ~15 minutes  

---

## TD-005: AuthControllerTest VerifyEmail NPE

**Priority**: Medium  
**Impact**: 4 test failures in `AuthControllerTest`  
**Effort**: ~30 minutes  

### Description
`VerifyEmailOutcome` is null in controller tests causing NPE.

---

## TD-006: PricingPlanV17BillingPeriodTest ExceptionInInitializerError

**Priority**: Low  
**Impact**: 1 test error  
**Effort**: ~15 minutes  

---

## TD-007: EnterpriseProjectJpaIntegrationTest ExceptionInInitializerError

**Priority**: Low  
**Impact**: 1 test error (related to Testcontainers/Docker)  

---

## Summary
| Category | Count | Priority |
|----------|-------|----------|
| Test Mock Updates | 14 | Medium |
| Infrastructure (Docker) | 32 | High |
| Null Pointer Fixes | 10 | Medium |
| Other | 6 | Low |
| **Total** | **62** |  |

---

## Deuda técnica: 38 tests fallidos (preexistentes)

**Fecha detección**: 2026-09-23
**Impacto**: Bloquea deploy a producción (CI/CD falla)
**Prioridad**: ALTA — arreglar antes de Día 7

**Evidencia (baseline → actual)**:
```
[INFO] Tests run: 3429, Failures: 4, Errors: 48, Skipped: 40   (test-output.txt, baseline)
[INFO] Tests run: 3453, Failures: 4, Errors: 34, Skipped: 40   (test_output.txt, 2026-09-23)
```
Ambas ejecuciones: `mvn test`. El fix del compiler plugin (commit 3ccac6e) redujo los errores 48 → 34 y resolvió 6 errores de `login_*`; los 4 Failures persisten idénticos.

### Grupo 1: 4 Failures AuthServiceImplTest (mock mismatch)
- resend_conUsuarioNoVerificadoYFueraDeCooldown_deberiaEnviar
- registerPatient_deberiaCrearPacienteEnviarVerificacionYSinToken
- register_deberiaCrearUsuarioNoVerificadoYNoEntregarToken
- registerPhysician_deberiaCrearMedicoPendienteEnviarVerificacion
**Causa**: `Argument(s) are different! Wanted: emailSender.sendVerificationEmail(...)` en `AuthServiceImpl.java:397`. Mock espera `sendVerificationEmail(...)` pero el código cambió la firma o el flujo.
**Fix**: Actualizar mocks o ajustar firma del método.

### Grupo 2: 4 Errors VerifyEmailOutcome.ordinal() NPE (bug real)
**Causa**: `outcome` es null en AuthControllerTest (`Cannot invoke "VerifyEmailOutcome.ordinal()" because "outcome" is null`).
**Fix**: Añadir null check o default enum value.

### Grupo 3: 19 Errors Docker/Testcontainers
**Causa**: `Could not find a valid Docker environment` — Docker Desktop no disponible en entorno CI.
**Fix**: Configurar Docker en CI o usar perfil test con Neon/H2.

### Grupo 4: 2 Errors SecurityConfigCorsTest
**Causa**: `CorsConfiguration.getAllowedOrigins()` null.
**Fix**: Revisar test setup.

### Grupo 5: ~7 Errors DataSource/Flyway
**Causa**: `Failed to determine a suitable driver class` / Flyway en contexto de integración sin DB en test.
**Fix**: Añadir @Disabled o configurar H2 para tests unitarios.

---

## Deuda técnica Día 7 (2026-09-23)

### TD-D7-1: Roles IPS_* no implementados (security billing)
**Prioridad**: Media  
**Estado**: `SecurityConfig` protege `/api/v1/billing/**` con fallback
`hasAnyRole("PHYSICIAN","ADMIN")` porque no existe `IPS_ADMIN`, `IPS_FACTURADOR`
ni `IPS_MEDICO` en el sistema de roles.  
**Fix**: Introducir roles IPS (ADR de personas/verticales) y migrar las reglas.

### TD-D7-2: KmsXmlSigner / DianHttpClient son placeholders (prod)
**Prioridad**: Alta (bloquea DIAN real)  
**Estado**: `@Profile("prod")` con `UnsupportedOperationException`; el flujo real
usa `StubXmlSigner`/`DianStubClient` (`@Profile("!prod")`).  
**Fix**: Integrar certificado X.509 (HSM/AWS KMS) y servicio DIAN (sandbox→prod),
sin secretos en el repo.

### TD-D7-3: 38 tests preexistentes fallidos (sin cambios en Día 7)
**Prioridad**: ALTA  
**Estado**: suite completa `Tests run: 3524, Failures: 4, Errors: 34, Skipped: 40`.
34 errores > 20 ⇒ no se intentó fix en Día 7 (regla de priorización).  
**Fix**: Día 7+ / sprint de estabilización (ver grupos 1-5 arriba).

### TD-D7-4: aging_bucket / days_overdue no son columnas generadas
**Prioridad**: Baja  
**Causa**: PostgreSQL exige expresiones `IMMUTABLE` en `GENERATED`; `CURRENT_DATE`
es `STABLE`.  
**Estado**: calculadas por `AgingCalculator` y persistidas; requieren el job
nocturno (`@Scheduled`, `@Profile("!test")`) para refrescar.  
**Fix**: Si se requiere frescura en tiempo real, recalcular en lectura o mover a
columna materializada + trigger.

---

## Bug Crítico Resuelto (2026-09-23)

### Doble prefijo /api/v1 en billing controllers
- **Detectado**: Día 9 por smoke test / auditoría.
- **Causa**: `server.servlet.context-path=/api/v1` + `@RequestMapping("/api/v1/billing/...")` en los 6 controllers de billing. URL real: `/api/v1/api/v1/billing/...`.
- **Impacto**: 100% de KIN Billing inoperable en producción (404 en todas las llamadas); el smoke test sin token lo enmascaraba (403 por `anyRequest().authenticated()`), y `SecurityConfig` con matchers `/api/v1/billing/**` no coincidía con el servlet-path real.
- **Fix**: 6 controllers (`Contract`, `Authorization`, `FevRips`, `Glosa`, `Cartera`, `Dashboard`) sin prefijo `/api/v1`; `SecurityConfig` actualizado a `/billing/**`; 3 tests actualizados.
- **Prevención**: `BillingEndpointPathTest` (valida que el mapping no lleva `/api/v1` y que el path duplicado da 404).
- **Nota**: pendiente validar en producción con token (smoke test E2E).

## Fixed (2026-09-23): Dependencia circular ContractService ↔ TariffImportService
- **Síntoma**: Render fallaba al arrancar: `Requested bean is currently in creation: Is there an unresolvable circular reference?` (`contractController`).
- **Causa raíz**: `ContractService` inyectaba `TariffImportService` y este inyectaba `ContractService`.
- **Fix**: `TariffImportService` ahora depende de `EpsContractRepository` (valida `findByIdAndOrganizationId(contractId, TenantContext.get())`), rompiendo el ciclo. No se usó `spring.main.allow-circular-references=true`.
- **Nota**: la delegación `ContractService.importTariffs` se mantiene (ya no hay ciclo); no se requirió mover la orquestación al controller.

## Deuda técnica — Formulario Beta IPS (2026-09-23)

### TD-BETA-1: Email de prueba no verificable
**Prioridad**: Baja  
**Estado**: las pruebas usaron `test@example.com`; la auto-respuesta llegó al
Product Owner, pero no se pudo verificar visualmente el correo del formulario.  
**Fix**: usar un email real en pruebas manuales.

### TD-BETA-2: Rate limiting del endpoint público
**Prioridad**: Media (resuelto)  
**Estado**: añadido límite `5/min por IP` para `/institutional/inquiries` y test
(`RateLimitingFilterTest.institutionalInquiries_*`). Documentado en
`kin-docs/TESTING_IPS.md`.  
**Fix**: considerar prueba de carga >100 req en prod para confirmar 429.

### TD-BETA-3: Dashboard de leads (opcional)
**Prioridad**: Baja  
**Estado**: existe `GET /institutional/inquiries` (ADMIN) pero sin UI.  
**Fix**: panel admin para listar/clasificar leads (`PENDING/CONTACTED/QUALIFIED/REJECTED`).

## Deuda técnica resuelta (2026-09-23) - Hermetización de tests
- Suite de tests ahora **hermética**: no depende del `.env` del desarrollador.
- `springdotenv.enabled=false` para tests (Surefire `systemPropertyVariables`).
- `jwt.secret` de test añadido en `application-test.yml` (Base64 >= 32 bytes).
- `AgingCalculator` sin `@Profile("!test")` (permitía crear CarteraService/Controller).
- `ContentCipher` con clave de test segura (`kin-test-telemedicine-crypto-secret-0123456789`).
- Verificado: `PhysicianPatientRelationshipIntegrationTest` 8/8 sin `.env`.

## Deuda técnica pendiente (2026-09-23) - salud de tests de integración
- **Aislamiento de datos / FK**: `JpaDashboard/Physician/Telemedicine/Triage/Differential`,
  `SchedulingIntegrationTest` insertan hijos sin padre o dependen de catálogos
  truncados por `cleanDatabaseBeforeClass`. Requiere estrategia de seeding.
- **Conexiones PostgreSQL**: `FATAL: sorry, too many clients already` (muchos
  contextos/Hikari pools). Ajustar `spring.datasource.hikari.maximum-pool-size`
  en test o compartir contexto.
- **DataSource sin URL** (2 clases): `OutboxRelayDisabledIntegrationTest`,
  otros `@SpringBootTest` sin Testcontainers ni datasource.
- **OutboxRelayIntegrationTest**: 3 fallos (concurrencia/reintentos) por analizar.
- **DocumentStorageQuotaPortImplTest**: `UnnecessaryStubbing` + `No active pricing
  plan found for vertical SALUD_PERSONAL` (seed/dispatch).
- **IA/red**: `llm down`, `429 Too Many Requests`, `stream closed` → requieren
  mock de dependencias externas.
- **SchedulingIntegrationTest**: `expected 201 but was 409` (conflicto de reserva;
  aislamiento/flake).

## Deuda técnica de tests (2026-09-23) - Sprint 2

### Prioridad ALTA
- **Grupo A**: Aislamiento de datos en tests (`JpaDashboard`, `JpaPhysician`,
  `JpaTelemedicine`, `JpaTriage`, `JpaDifferential`, `Scheduling`).
  - Causa: insertan hijos sin padre / catálogos truncados por `cleanDatabaseBeforeClass`.
  - Solución: definir política de seeding (fixtures por clase).
  - Impacto: 6 failures + ~11 errors.

### Prioridad MEDIA
- **Grupo D**: `OutboxRelayIntegrationTest` (3) - concurrencia/reintentos.
- **Grupo E**: `DocumentStorageQuotaPortImplTest` (5) - `UnnecessaryStubbing` + pricing seed.

### Prioridad BAJA
- **Grupo F**: IA/red (`llm down`, `429 Too Many Requests`, `stream closed`) - mockear IA externa (WireMock). No bloquea: llamadas externas reales.

### Resuelto (Sprint 2 pre-work, 2026-09-23)
- Hikari `maximum-pool-size: 5` en test → elimina `too many clients already`.
- `OutboxRelayDisabledIntegrationTest`: datasource `jdbc:tc` + `@Transactional`.

### Criterio de éxito Sprint 2
- Todos los tests verdes · suite 100% hermética · CI/CD limpio.

## Deuda técnica (2026-09-23) - Fase 2.1

### Resuelto
- ✅ Roles IPS_* (V62-V64): IPS_ADMIN, IPS_MEDICO, IPS_ENFERMERA, IPS_FACTURADOR, IPS_AUDITOR.
- ✅ Multi-sede (`institutional_branches`, V63).
- ✅ Equipos médicos (`organization_members`, V64).
- ✅ Billing para IPS_* (`SecurityConfig` + `ContractController`).

### Pendiente (Fase 2.2)
- **TD-IPS-1**: Invitación de miembros por email (hoy es por `userId`).
- **TD-IPS-2**: Asignación automática de rol IPS al aceptar la invitación.
- **TD-IPS-3**: Pipeline de catálogos CUPS/INVIMA.
- **TD-IPS-4**: `/billing/rips` sin controller REST (generación RIPS no expuesta).
- **TD-IPS-5**: No existe `/institutional/admin/**` ni `/institutional/facturacion/**` con endpoints aún (reglas listas, rutas por implementar).

---

*Last Updated: 2026-09-23*  
*Owner: Backend Team*  
*Next Review: Sprint Planning*