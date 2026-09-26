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

### TD-INTEGRATION-REPO-HCE: EncounterServiceIntegrationTest esqueleto @Disabled con Testcontainers pendiente por Docker en runner
- **Prioridad**: Media.
- **Problema**: `EncounterServiceIntegrationTest` no puede ejecutarse con Testcontainers (PostgreSQL 18) porque el entorno de desarrollo/CI actual no tiene Docker daemon disponible. El intento de usar H2 con `@DataJpaTest` falla porque las entidades HCE (14 tablas) usan tipos PostgreSQL-específicos (`TEXT[]`, `interval second(18,9)`, `uuid` con funciones nativas, CHECK constraints con arrays) que H2 no soporta nativamente.
- **Estado actual**: Esqueleto creado en `EncounterServiceIntegrationTest.java` con `@Disabled("Docker required - TD-INTEGRATION-REPO-HCE")` y 3 tests vacíos (flujoCompleto, constraintFK, closeEncounter). Tests unitarios con mocks (`EncounterServiceTest` 8/8 PASS, `PatientIdentificationServiceTest` 4/4 PASS) cubren la lógica de negocio. Validación de schema con `HceSchemaValidationTest` (requiere Testcontainers PG18).
- **Fix objetivo**: Cuando el runner de CI/CD tenga Docker/Testcontainers disponible, implementar los 3 tests en el esqueleto y quitar `@Disabled`. Migrar a `@SpringBootTest` + Testcontainers PostgreSQL 18 real (igual que `ApplicationContextTest` y `HceSchemaValidationTest`).
- **Owner**: Backend/DevOps. **Estimado**: 0.5 día (configurar Docker en CI + test).
- **Referencia**: Bloque 1 HCE completado, Día 7.

---

*Last Updated: 2026-09-25*  
*Owner: Backend Team*  
*Next Review: Sprint Planning*

---