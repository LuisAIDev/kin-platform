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

*Last Updated: 2026-09-25*  
*Owner: Backend Team*  
*Next Review: Sprint Planning*

---