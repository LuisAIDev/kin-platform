# KIN_NEON_MIGRATION_MASTER_PLAN_V2.md

> **KIN — Plan Maestro de Migración Arquitectónica H2 → PostgreSQL Neon (V2)**
> Nivel: Enterprise · Documento de arquitectura, estrategia y ejecución.
> **Modo: solo diseño y revisión. No se modificó ningún archivo del repositorio, no se ejecutó Maven/Flyway/Docker/Render, no se hicieron commits ni ramas.**
> Fecha: 2026-08-06 · Versión anterior: `KIN_NEON_MIGRATION_MASTER_PLAN.md` (V1). Esta V2 corrige y amplía V1 sin descartar información útil.

---

## 0. Resumen ejecutivo

KIN migra su persistencia de **H2 → PostgreSQL 18 sobre Neon** de forma definitiva, adoptando a la vez un stack objetivo moderno y mantenible a medio plazo:

| Componente | Hoy | **Objetivo V2** | Cambio |
|---|---|---|---|
| Spring Boot | 3.2.5 | **3.5.x** | Upgrade aprovechando la migración |
| Java | 17 | **17** | Sin cambio (soporte completo en Boot 3.5 y Flyway ≤ 12) |
| Hibernate | 6.4.4.Final | **6.6.x (gestionado por Boot 3.5)** | Upgrade automático |
| PostgreSQL | 16 (Docker/Render) | **18 (Neon)** | Objetivo de plataforma |
| Flyway | 9.22.3 | **11.x** + `flyway-database-postgresql` | Upgrade obligatorio para PG 18 |
| JDBC PostgreSQL | 42.6.2 | **42.7.x (gestionado por Boot 3.5)** | Upgrade automático |
| HikariCP | 5.0.1 | **5.x (tuned Neon serverless)** | Tuning de parámetros |
| Motor de persistencia | H2 (dev) + PG (prod) | **Solo PG/Neon** | H2 eliminado definitivamente |

**Decisiones arquitectónicas clave (justificadas en §13):**
- **AD-1:** PostgreSQL 18 es el objetivo final (no quedarse en 16 por conveniencia de Flyway).
- **AD-2:** Flyway 11.x + módulo `flyway-database-postgresql` (soporte oficial PG 18, requiere Java 17+).
- **AD-3:** Spring Boot 3.5.x se incorpora en la misma migración (ventana de trabajo aprovechada, coste marginal bajo).
- **AD-4:** `ddl-auto=none` en todos los perfiles; Flyway = única fuente de verdad del esquema.
- **AD-5:** Neon organizado en ramas: `main` (prod), `dev` (desarrollo), efímeras (CI/E2E).
- **AD-6:** `JDBC_DATABASE_URL` (pooled + SSL) como único punto de conexión; se elimina `DATABASE_URL` no consumida (deuda T8).
- **AD-7:** Backups `pg_dump` antes y después de cada etapa (estrategia §7).
- **AD-8:** Tests sobre **Testcontainers PostgreSQL 18** (nunca H2).

---

## FASE A — Compatibilidad completa del stack objetivo

### A.1 Matriz de compatibilidad (target V2)

| Componente | Versión | PG 16 | PG 17 | **PG 18** | Neon | Java 17 |
|---|---|---|---|---|---|---|
| Spring Boot | 3.5.x | ✅ | ✅ | ✅ | ✅ | ✅ |
| Java | 17 | ✅ | ✅ | ✅ | — | ✅ |
| Hibernate | 6.6.x (Boot 3.5) | ✅ | ✅ | ✅ (warning de versión) | ✅ | ✅ |
| PostgreSQL JDBC | 42.7.x (Boot 3.5) | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Flyway** | **11.x** | ✅ | ✅ | ✅ **(verificado)** | ✅ | ✅ |
| springdoc | 2.5.0 → 2.8.x (recomendado) | ✅ | ✅ | ✅ | ✅ | ✅ |
| HikariCP | 5.x | ✅ | ✅ | ✅ | ⚠️ requiere tuning | ✅ |

### A.2 Verificaciones por componente

- **Spring Boot 3.5.x**: requiere Java ≥ 17 → Java 17 queda soportado. Gestión de dependencias: Hibernate 6.6.x, PostgreSQL JDBC 42.7.x, Flyway **10.x**. Soporta PostgreSQL 16/17/18 y Neon (protocolo wire de PostgreSQL).
- **Hibernate 6.6.x**: `PostgreSQLDialect` verifica versión mínima (8.2) y opera en 17/18 con *warning* informativo (no bloqueante). Tipos JSONB/SQLTypes.JSON ya soportados.
- **Flyway 11.x**: requiere **Java 17+** (hasta v12 inclusive; **v13 exigirá Java 21** — ver A.3). Soporte **verificado de PostgreSQL 18**. Requiere el módulo separado **`flyway-database-postgresql`** (obligatorio desde Flyway 10).
- **PostgreSQL JDBC 42.7.x**: retrocompatible con 17/18. SSL por defecto en Neon (`sslmode=require`).
- **Neon**: wire-compatible; soporta ramas en PG 16/17/18 según configuración del proyecto. Exige SSL y admite pooled endpoint (`-pooler`).

### A.3 Riesgos conocidos del stack

| Riesgo | Impacto | Prob. | Mitigación |
|---|---|---|---|
| **Flyway 13 exige Java 21** | Bloquea si se sube más allá de 12.x | Baja (se fija 11.x) | **Fijar Flyway 11.x** (Java 17). Documentar que 13.x requiere Java 21 (futuro). |
| API Flyway 11 vs auto-config de Spring Boot 3.5 | Fallo de arranque si cambia la API | Media | Validar con smoke test en la Etapa 4; API núcleo (`FluentConfiguration`, `Flyway.configure()`) es estable 10→11. |
| Override `flyway.version` sobre Spring Boot 3.5 | Inconsistencia con BOM | Baja | Es el mecanismo oficial de override de propiedades del parent POM; validar en CI. |
| Hibernate 6.6 *warning* en PG 18 | Solo log de advertencia | Baja | Ignorar (documentado); no bloquea. |
| JSONB `features` con texto no-JSON | Excepción en insert/update | Baja | Flujo actual garantiza JSON válido (ObjectMapper); validación en capa de servicio. |

### A.4 Estrategia de versiones (regla)

- **Fijar**: Spring Boot `3.5.x` (último patch 3.5, p. ej. 3.5.9+), Flyway `11.x` (no 13), Java 17, PostgreSQL 18.
- **No** adoptar Spring Boot 4.x ni Java 21 en este ciclo (se dejan documentados en "Arquitectura futura" §12).

---

## FASE B — Flyway 11.x: rediseño de la estrategia

### B.1 Decisión

**Flyway 9.22.3 queda descartado.** Se adopta **Flyway 11.x** como objetivo porque:
- Soporte **oficial y verificado de PostgreSQL 18** (versión más reciente verificada por Flyway).
- Requiere **Java 17** (compatible con el stack actual; Java 21 solo a partir de v13).
- Corrección de comportamiento y API estable respecto a 10.x.

### B.2 Compatibilidad con Spring Boot

- Spring Boot 3.5.x gestiona Flyway **10.x** (no 11.x). Para usar 11.x se **sobrescribe la propiedad gestionada** `flyway.version` en el `pom.xml`:
  ```xml
  <properties>
      <flyway.version>11.x.y</flyway.version>
  </properties>
  ```
- Se añade el módulo **`org.flywaydb:flyway-database-postgresql`** (obligatorio desde Flyway 10 para soporte PostgreSQL; en 9.22.3 iba embebido en `flyway-core`).
- `FlywayAutoConfiguration` de Spring Boot 3.5 usa la API núcleo (`FluentConfiguration`, `FlywayMigrationStrategy`, `MigrationInfo`), estable entre 10 y 11 → el override es viable. **Validación obligatoria** con un arranque de prueba (Etapa 4).

### B.3 Estrategia de migración de Flyway

1. Aplicar el override de versión + dependencia en `pom.xml` (Etapa 4).
2. Ejecutar `flyway migrate` contra la rama `dev` de Neon **antes** de arrancar el backend.
3. Verificar `flyway_schema_history` = V1..V11 y checksums.
4. Regla inmutable: **nunca editar una migración aplicada**; nuevas migraciones = **V12+**, idempotentes, SQL PostgreSQL.
5. En `application-prod.properties`/`application-dev.yml`: `spring.flyway.enabled=true`, `baseline-on-migrate=true`, `locations=classpath:db/migration`.

### B.4 Riesgos de Flyway 11

| Riesgo | Mitigación |
|---|---|
| API incompatible con la auto-config de Boot 3.5 | Smoke test en Etapa 4; si falla, `flyway-core` 11 + `flyway-database-postgresql` con strategy custom (`FlywayMigrationStrategy` bean). |
| Checksum de migraciones V1..V11 ya aplicadas en otras ramas | No re-aplicar sobre bases con historia: usar ramas Neon limpias (flyway clean + migrate) o baseline controlado. |
| Módulo PG olvidado | El arranque fallará con error claro "No database found to handle" → checklist de la Etapa 4. |

---

## FASE C — PostgreSQL 18: replanteamiento de la estrategia

### C.1 Por qué PostgreSQL 18 (y no quedarse en 16)

| Ventaja | Detalle |
|---|---|
| **Ciclo de vida largo** | PG 18 es la versión reciente con soporte activo y actualizaciones de seguridad (debería mantenerse hasta ~2030). |
| **Performance de I/O y vacuum** | Mejoras de rendimiento en el procesamiento de índice y vacuum/analyze; reduce coste operativo en Neon (cómputo por uso). |
| **Características SQL modernas** | `MERGE` mejorado, mejores planes de ejecución, más opciones de acceso (`SET ACCESS METHOD`), consolidación de `jsonb`/estadísticas. |
| **Alineación Neon** | Neon soporta ramas en 16/17/18; estar en la versión soportada más nueva evita migraciones futuras. |
| **Soporte Flyway** | Verificado desde Flyway 11.x (nuestra versión objetivo). |

### C.2 Riesgos

| Riesgo | Impacto | Prob. | Mitigación |
|---|---|---|---|
| Hibernate 6.6 *warning* de versión | Cosmético | Alta | Documentado; no bloqueante. |
| Funcionalidades nuevas con comportamiento distinto en `ANALYZE`/costos | Planes de ejecución distintos | Media | `EXPLAIN ANALYZE` en las consultas calientes durante la Etapa 4; revisar índices (sección "Optimización PostgreSQL"). |
| Otras herramientas/scripts asumen PG 16 | Incompatibilidad menor | Baja | Revisar `init.sql`/compose (solo referencia histórica) y drivers. |
| Flyway 9 no soporta PG 18 | Bloqueo | — (se elimina) | Flyway 11.x adoptado (Fase B). |

### C.3 Compatibilidad

- Spring Boot 3.5.x ✅ · Hibernate 6.6 ✅ · JDBC 42.7.x ✅ · Flyway 11.x ✅ · Neon ✅ (rama configurable a 18).

### C.4 Estrategia de actualización

1. Crear el proyecto/rama Neon en **PostgreSQL 18** desde el inicio (las ramas se pueden crear con la versión objetivo).
2. Confirmar con `SHOW server_version;` y `SELECT version();` antes de la Etapa 3.
3. Si el proyecto Neon existente está en 16/17: crear una rama nueva en 18 y validar; **no** migrar en caliente una rama a otra versión.
4. Documentar que "PG 18" es un estándar de plataforma de aquí en adelante (nunca se vuelve a 16).

---

## FASE D — Perfiles Spring (diseño definitivo)

> Propuesta de estructura. **No se escriben los archivos todavía.**

```
kin-backend/src/main/resources/
├── application.yml               # COMÚN (sin H2, sin datasource fijo)
├── application-dev.yml           # NUEVO: Neon rama dev (PG 18, Flyway ON)
├── application-test.yml          # MODIFICADO: Testcontainers PG 18 (Flyway clean+migrate)
├── application-prod.properties   # MODIFICADO (o renombrado a .yml): Neon rama main (PG 18)
├── application-render.properties # MANTENIDO (include prod) + env vars → Neon
├── application-enterprise.properties # MANTENIDO (include prod)
└── db/migration/V1..V11          # SIN CAMBIOS de contenido; nuevas migraciones V12+
```

### D.1 `application.yml` (común)
- `server.*`, `spring.application.name`, `jackson`, `ai/deepseek/openai`, `stripe`, `jwt`, `springdotenv`, `management` (actuator/probes/grupos), `logging`.
- **Se elimina:** datasource H2, `spring.h2.console`, `spring.jpa.database=H2`, dialecto H2, `ddl-auto: update`, `flyway.enabled: false`.
- **Común JPA:** `open-in-view=false`, `default_batch_fetch_size=50`, `format_sql`, `spring.flyway.locations=classpath:db/migration`, `baseline-on-migrate=true`.
- **No incluye** datasource por defecto: lo aporta cada perfil (evita el "modo silencioso H2" de Spring Boot).

### D.2 `application-dev.yml` (nuevo)
- Datasource: `JDBC_DATABASE_URL` o `DB_HOST/DB_PORT/DB_NAME` → rama **dev** de Neon, PG 18, `?sslmode=require`, driver `org.postgresql.Driver`.
- Hikari tuned serverless (§6.4).
- `spring.jpa.hibernate.ddl-auto=none`, `PostgreSQLDialect`, `show-sql=true`.
- `spring.flyway.enabled=true`.
- `springdotenv.directory=../`.

### D.3 `application-test.yml` (modificado)
- **Testcontainers** `postgres:18` en `src/test`, `spring.datasource.url` dinámica (del contenedor).
- `ddl-auto=none` + `spring.flyway.enabled=true` y estrategia **clean + migrate** al inicio de la suite (o por clase con `@Testcontainers`).
- `app.rate-limit.enabled=false` (se mantiene).
- **Se elimina:** `jdbc:h2:mem:testdb`, `ddl-auto: create-drop`.
- Nota: los 902 tests actuales deberán validarse contra PG (tipos JSONB, UUID, secuencias).

### D.4 `application-prod.properties` (modificado/renombrado a `.yml`)
- Igual que hoy (datasource PG, `ddl-auto=none`, Flyway ON, driver PG, Hikari, JWT, CORS, rate-limit trust-proxy, cookies Secure/SameSite=None), con `JDBC_DATABASE_URL` → rama **main** de Neon (PG 18).
- `spring.jpa.properties.hibernate.dialect=PostgreSQLDialect`.
- Si se renombra a `.yml`, eliminar el `.properties` en la misma etapa.

### D.5 `render` / `enterprise`
- Se mantienen (`spring.profiles.include=prod`); solo cambian variables de entorno (Fase G/I).

---

## FASE E — Neon: organización (ampliada)

### E.1 Ramas

```
Proyecto Neon: kin-neon (PG 18)
├── Rama MAIN        → PRODUCCIÓN (protegida, solo deploys, backups automáticos)
├── Rama DEV         → DESARROLLO (equipo local; efímera/regenerable)
└── Rama EFÍMERA     → CI / E2E / PR (creada on-demand, destruida al terminar)
```

- Branches = snapshots instantáneos del storage. Dev nunca escribe en main. Resincronización de dev desde main mediante botón/CLI de Neon.
- **Time-to-branch ≈ segundos** → entornos efímeros baratos (ver §12).

### E.2 Credenciales
- Roles por rama: `neondb_owner` (rama dev) y rol mínimo `kin_app` (rama main, solo DML sobre `public`), con passwords independientes.
- Dev y prod con credenciales distintas; **nunca** versionadas (`.env` gitignored, secrets de Render).

### E.3 SSL y pooler
- SSL obligatorio: `?sslmode=require`.
- **Pooled endpoint** (PgBouncer, sufijo `-pooler`) para Spring Boot: evita desbordar conexiones del cómputo serverless.
- Cadena JDBC objetivo:
  `jdbc:postgresql://<endpoint-pooler>.<region>.aws.neon.tech/<db>?sslmode=require`

### E.4 Operación
- `SHOW server_version;` antes de la Etapa 3.
- Autosuspend: dev corto (ahorro), prod "siempre encendido" si el SLA lo exige.
- Primera conexión tras suspensión ~1 s → reflejar en timeouts.

---

## FASE F — Migración de datos (H2 → Neon) — estrategia profesional

### F.1 Premisa
- `data/kindb.mv.db` es desechable (gitignored). **Recomendación: no migrar datos locales**; la rama dev de Neon nace vacía y Flyway + seeds (`DataInitializer`, `CategoryDataInitializer`) la pueblan.
- Si se decide conservar datos, el procedimiento controlado es:

### F.2 Procedimiento (si aplica)
1. **Backup** (`pg_dump` no aplica al origen; es backup de archivo): copiar `kin-backend/data/kindb.mv.db` con fecha.
2. **Export H2**: `SCRIPT TO 'kindb_export.sql'` o CSV por tabla (`CSVWRITE`).
3. **Transformación de tipos** H2→PG:
   - `VARCHAR/CLOB→TEXT`, `TIMESTAMP→TIMESTAMPTZ`, `BOOLEAN→BOOLEAN`, `UUID→UUID`.
   - `features` (JSON en texto) → `features::jsonb` (V8 ya lo exige; enviar JSON válido).
   - Secuencias: tras cargar `webhook_events.id`, `SELECT setval('webhook_events_id_seq', (SELECT COALESCE(MAX(id),1) FROM webhook_events));`
4. **Orden de carga por FK:** `users → categories → pricing_plans → projects → chat_messages → viability_scores → project_context → interview_state → enterprise_project → enterprise_document → user_subscriptions → webhook_events`.
5. **Validación:** conteos por tabla (origen vs destino) y `flyway_schema_history` (V1..V11, sin datos incrustados en migraciones).

> **No se ejecuta.** Es la Etapa 4 del Roadmap, solo si se conservan datos.

---

## FASE G — Render (cambios previstos)

| Elemento | Hoy | Objetivo |
|---|---|---|
| `databases: kin-db` | Postgres gestionado | **Se elimina** (la base pasa a Neon) o queda deshabilitado |
| `DATABASE_URL` (fromDatabase) | `postgresql://` no consumida (T8) | **Se elimina** |
| `DB_HOST/DB_PORT/DB_NAME` | kin-db | **Se apuntan a Neon** (rama main) |
| `DATABASE_USER/PASSWORD` | kin_admin | **Credenciales de la rama main de Neon** |
| `JDBC_DATABASE_URL` | no existe | **Se añade** (pooled + `sslmode=require`) |
| `SPRING_PROFILES_ACTIVE` | `render` | Se mantiene |
| Healthcheck | `/actuator/health` | Sin cambios (readiness incluye `db`) |

- **Ejecución segura:** eliminar el bloque de base + `fromDatabase` de forma atómica y definir `JDBC_DATABASE_URL` en el mismo cambio; validar en preview antes de producción.
- Rollback: re-deploy con blueprint anterior mientras `kin-db` exista.

---

## FASE H — Docker (cambios previstos)

| Elemento | Cambio |
|---|---|
| `docker-compose.yml` (raíz / kin-database) | El servicio `postgres-db` puede **mantenerse como fallback offline en PG 18** (imagen `postgres:18`) o eliminarse. Si se elimina: quitar `depends_on`, red y `SPRING_DATASOURCE_*`. |
| `kin-backend/Dockerfile` | **Sin cambios de base**; el upgrade de Flyway entra vía `pom.xml` (Maven lo resuelve en el build multi-stage). |
| `kin-frontend/Dockerfile` | Sin cambios. |
| Healthcheck backend | Sin cambios (`wget http://localhost:8080/api/v1/actuator/health`). |
| Variables | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` → `JDBC_DATABASE_URL` (o se eliminan). |

> Docker **no es obligatorio** para el objetivo; queda como herramienta de respaldo y desarrollo local si se desea PG local.

---

## FASE I — Variables de entorno

| Estado | Variable | Nota |
|---|---|---|
| **Nueva** | `JDBC_DATABASE_URL` | Único punto de conexión (pooled + SSL) |
| **Nueva** | `DB_HOST`, `DB_PORT`, `DB_NAME` | Alternativa a JDBC_DATABASE_URL |
| **Nueva** | `DATABASE_USER`, `DATABASE_PASSWORD` | Credenciales de la rama Neon |
| **Nueva (ops)** | `NEON_PROJECT_ID`, `NEON_BRANCH`, `NEON_API_KEY` | Solo scripts/CI (no de la app) |
| **Desaparece** | `POSTGRES_PASSWORD` | Solo Docker local (opcional si se conserva el fallback) |
| **Desaparece** | `DATABASE_URL` (Render) | No consumida (T8) |
| **Desaparece** | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (compose local) | Reemplazadas por `JDBC_DATABASE_URL` |
| **Se mantiene** | `JWT_SECRET`, `DEEPSEEK_API_KEY`, `OPENAI_API_KEY`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `SPRING_PROFILES_ACTIVE`, `PORT`, `ALLOWED_ORIGINS`, `KIN_REDIS_ENABLED`, `SPRINGDOTENV_DIRECTORY` | Sin cambios |
| **Regla** | — | Nunca versionar credenciales Neon ni `JDBC_DATABASE_URL` con password |

---

## FASE J — Skill `DATABASE_ARCHITECT` (v2 ampliada)

> Contenido de referencia de la Skill propuesta. **No se crea el archivo todavía.**

```markdown
# Skill: DATABASE_ARCHITECT

## Identidad
Arquitecto de base de datos de KIN. Única base permitida: PostgreSQL sobre Neon.

## Reglas obligatorias (no negociables)
1. **NUNCA usar H2.** Prohibido crear/escribir perfiles, scripts, tests o config que
   referencie H2 (h2database, jdbc:h2:, kindb.mv.db, H2Dialect, spring.h2.console,
   ddl-auto update). H2 queda prohibido definitivamente.
2. **Toda persistencia es PostgreSQL Neon.**
   - Dev → rama `dev`. Prod → rama `main`. CI → rama efímera. Nunca la misma rama.
   - Conexión SIEMPRE con SSL (`?sslmode=require`) y, si es posible, pooled endpoint.
3. **Validar conexión con Neon ANTES de arrancar.**
   - `SELECT 1` contra la rama objetivo. Si falla → NO arrancar.
4. **Nunca iniciar Spring Boot si Flyway falla.**
   - `flyway migrate` debe pasar antes del arranque de la app. ddl-auto SIEMPRE `none`.
5. **Nunca modificar una migración aplicada.**
   - Chequeo de checksum de Flyway. Nuevas migraciones = **SIEMPRE V12+**, idempotentes, SQL PostgreSQL.
6. **Abortar inmediatamente si la base no responde.**
   - Exit no-zero, mensaje claro, SIN reintentos automáticos en bucle.
7. **Nunca entrar en bucles infinitos.**
   - Máx. 1 reintento manual tras revisar la causa. FAIL-FAST y reutilizar procesos vivos.
8. **Solo PostgreSQL.** Prohibidos otros motores (H2, SQLite, etc.) en cualquier entorno.
9. **Índices y JSONB:**
   - Índices GIN sobre columnas JSONB consultadas. FK siempre indexadas en el lado hijo.
   - Mantener `autovacuum` y `ANALYZE` (no desactivar).
10. **Datos:** respetar orden de FK y `setval` de secuencias al importar; validar con conteos.
```

---

## FASE K — Roadmap V2 (migración segura en 9 etapas)

> Cada etapa incluye **backup previo** (antes) y **backup posterior** (después) según §7.

### Etapa 1 — Crear Neon (PG 18)
- **Objetivo:** proyecto Neon + ramas `main` y `dev` en **PostgreSQL 18**.
- **Acciones:** crear proyecto; configurar rama dev; anotar endpoints (pooler/directo), roles y `SHOW server_version`.
- **Archivos afectados:** ninguno (solo cuentas/config Neon).
- **Riesgo:** bajo · **Tiempo:** 0.5–1 h
- **Rollback:** eliminar el proyecto/rama de Neon (o no crear aún la rama main).
- **Validación:** `psql` conecta a ambas ramas con SSL; versión = 18.

### Etapa 2 — Validar conexión (app ↔ Neon)
- **Objetivo:** demostrar conectividad JDBC desde el backend local a la rama dev sin cambiar aún el código.
- **Acciones:** setear `.env` con `JDBC_DATABASE_URL` de dev; prueba puntual `SELECT 1`; verificar `pg_isready` y pooled endpoint.
- **Archivos afectados:** `.env` (local, gitignored).
- **Riesgo:** bajo · **Tiempo:** 0.5 h
- **Rollback:** restaurar `.env` anterior (backup).
- **Validación:** conexión estable, sin errores SSL/pool.

### Etapa 3 — Flyway (11.x + PG 18)
- **Objetivo:** aplicar el esquema V1..V11 sobre la rama dev con Flyway 11.x.
- **Acciones:** override `flyway.version=11.x` en `pom.xml`, añadir `flyway-database-postgresql`; ejecutar `flyway migrate`; verificar `flyway_schema_history`.
- **Archivos afectados:** `pom.xml` (backup previo).
- **Riesgo:** **alto** (primer cambio real) · **Tiempo:** 0.5–1 día
- **Rollback:** revertir `pom.xml` (git) — la rama Neon dev se puede descartar/crear de nuevo.
- **Validación:** 11/11 migraciones, checksums OK, tablas/indexes/JSONB creados.

### Etapa 4 — Migración (datos, si aplica)
- **Objetivo:** portar datos H2 → Neon dev (solo si se decide conservar) según Fase F.
- **Acciones:** backup H2 → export → transformar → importar → setval → validar conteos.
- **Archivos afectados:** scripts temporales (fuera de repo o en `scripts/`).
- **Riesgo:** alto (integridad) · **Tiempo:** 0.5–1 día
- **Rollback:** descartar rama dev (flyway clean+migrate o recrear).
- **Validación:** conteos por tabla + smoke E2E dev.

### Etapa 5 — Eliminar H2
- **Objetivo:** H2 fuera del classpath y de la configuración.
- **Acciones:** quitar dependencia `com.h2database:h2`, `spring.h2.console`, dialecto H2; **backup** de `data/kindb.mv.db`; adaptar `scripts/reset-dev-db.*` a reset de Neon.
- **Archivos afectados:** `pom.xml`, `application.yml`, `scripts/reset-dev-db.*`, `.env`.
- **Riesgo:** medio · **Tiempo:** 2–4 h
- **Rollback:** git revert de pom/config + restaurar `.env`.
- **Validación:** búsqueda global `h2|H2|kindb|H2Dialect` = 0; arranque dev/test OK.

### Etapa 6 — Render → Neon (producción)
- **Objetivo:** prod apunta a Neon rama main (PG 18).
- **Acciones:** en `render.yaml`: eliminar `kin-db` + `fromDatabase`, definir `JDBC_DATABASE_URL` (o `DB_*`); **backup previo** del blueprint; validar en preview antes de producción.
- **Archivos afectados:** `render.yaml` + secrets del dashboard.
- **Riesgo:** **alto** (producción) · **Tiempo:** 0.5 día
- **Rollback:** re-deploy con blueprint anterior mientras `kin-db` exista.
- **Validación:** `/actuator/health` readiness `db` UP + smoke completo.

### Etapa 7 — Docker
- **Objetivo:** alinear Docker con el modelo Neon (fallback PG 18 o eliminación).
- **Acciones:** actualizar/eliminar `postgres-db` en compose; variables → `JDBC_DATABASE_URL`.
- **Archivos afectados:** `docker-compose.yml` (raíz y kin-database).
- **Riesgo:** bajo · **Tiempo:** 2–4 h
- **Rollback:** git revert.
- **Validación:** `docker compose up --build` (si se conserva) contra PG 18 local o Neon.

### Etapa 8 — Tests sobre PostgreSQL (Testcontainers PG 18)
- **Objetivo:** suite de tests 100% sobre PostgreSQL (nunca H2).
- **Acciones:** `application-test.yml` → Testcontainers `postgres:18`; ajustar tests que asuman H2; validar JSONB/UUID.
- **Archivos afectados:** `application-test.yml`, `pom.xml` (Testcontainers), tests.
- **Riesgo:** medio · **Tiempo:** 0.5–1 día
- **Rollback:** git revert del archivo test/profile.
- **Validación:** 902 tests verdes contra PG.

### Etapa 9 — Go Live
- **Objetivo:** aceptación final del modelo Neon-only + Spring Boot 3.5 + Flyway 11.
- **Acciones:** checklist completo, E2E Playwright, monitorización (§8), revisión de `flyway_schema_history`, alertas configuradas.
- **Archivos afectados:** ninguno (validación).
- **Riesgo:** bajo · **Tiempo:** 0.5 día
- **Rollback:** procedimiento de la Etapa 6.
- **Validación:** checklist 100 %.

---

## §7 — Estrategia profesional de Backups

### 7.1 Herramientas
- **`pg_dump`** (PostgreSQL) para copias lógicas del esquema+datos de cada rama Neon.
- **`pg_restore`** para restaurar desde formato personalizado (`-Fc`).
- **Backups automáticos de Neon** (PITR) como red de seguridad adicional (no reemplazan `pg_dump`).

### 7.2 Comandos de referencia (para ejecución futura)

```bash
# Backup de la rama (dev o main), formato custom comprimido
pg_dump "postgresql://<user>@<endpoint>/<db>?sslmode=require" \
  -Fc -f backups/kin_<rama>_<fecha>.dump

# Restauración a una rama/BD de destino (p. ej. rama efímera de validación)
pg_restore "postgresql://<user>@<endpoint>/<db>?sslmode=require" \
  -d <db> --clean --if-exists backups/kin_<rama>_<fecha>.dump

# Verificación de integridad
pg_restore -l backups/kin_<rama>_<fecha>.dump | tail -20
```

### 7.3 Reglas
- **Backup ANTES de cada etapa** que modifique esquema/datos (Etapas 3, 4, 5, 6, 7, 8).
- **Backup DESPUÉS de cada etapa** para anclar un punto de restauración estable.
- Retención: mínimo **7 backups** por rama (o según política de Neon).
- Backups **nunca** dentro del repositorio git; en bucket/almacenamiento aparte.
- Prueba de restauración **al menos 1 vez por trimestre** (restore a rama efímera y validar conteos).

### 7.4 Restauración y rollback
- Rollback de esquema en Neon: la rama es descartable → `flyway clean + migrate` o restaurar desde PITR/backup a una rama nueva.
- Rollback de datos: `pg_restore` del backup posterior a la última etapa correcta.
- Rollback de código: `git revert` por etapa (cada etapa es un commit atómico).

---

## §8 — Optimización PostgreSQL (recomendaciones)

### 8.1 Índices
- **FK siempre indexadas en el lado hijo** (evita seq scan y bloqueos en DELETE):
  `chat_messages.project_id`, `chat_messages.user_id`, `projects.user_id`, `viability_scores.project_id`, `user_subscriptions.user_id/plan_id`, `enterprise_document.project_id`, `project_context.project_id`, `interview_state.project_id`.
- Índices compuestos para patrones de acceso:
  - `(user_id, status)` en `projects` (ya existe) y `(user_id, created_at)`.
  - `(project_id, created_at)` en `chat_messages` (ya existe) → considerar DESC.
  - `(user_id, status)` en `user_subscriptions` (ya existe).
- **Índices parciales** si aplica (p. ej. `WHERE is_active` en `pricing_plans`, `WHERE status='ACTIVE'`).

### 8.2 JSONB y GIN
- Columnas JSONB consultables (ver §9) → **GIN** (`USING GIN (columna)`), ya presentes en `chat_messages.metadata` y `viability_scores.ai_insights`.
- Para busquedas por clave específica: GIN con operador `jsonb_path_ops` (más compacto y rápido) cuando las consultas usan `@>`.
- **Evitar** `->>`/`->` con filtrado sin índice en datos de alto volumen.

### 8.3 FK e integridad
- `ON DELETE CASCADE` correcto para datos hijos (users→projects→chat_messages).
- `ON DELETE SET NULL` en `users.current_plan_id` (ya correcto).
- `ON DELETE RESTRICT` en `user_subscriptions.plan_id` (ya correcto, integridad de facturación).

### 8.4 Consultas
- `EXPLAIN (ANALYZE, BUFFERS)` en las consultas calientes (login por email, listado de proyectos, historial de chat, suscripciones).
- Preferir consultas planas del repositorio JPA; evitar N+1 (usar `@EntityGraph`/`join fetch`; `default_batch_fetch_size=50` ya configurado).
- `OFFSET` → paginación con **keyset** (`WHERE id > ? ORDER BY id`) para listados grandes.

### 8.5 VACUUM / ANALYZE / autovacuum
- **Mantener autovacuum ACTIVO** (nunca desactivarlo). Neon gestiona el mantenimiento; ajustar `autovacuum_vacuum_scale_factor` si hay tablas de alta rotación (`chat_messages`, `webhook_events`).
- `ANALYZE` tras cargas masivas (Etapa 4) para refrescar estadísticas del planner.
- Monitorear `pg_stat_user_tables` (dead tuples, last_autovacuum, n_dead_tup).

### 8.6 Performance
- Tuning Hikari serverless (§6.4 del master: pool 5–10, `connection-init-sql=SELECT 1`, timeouts cortos).
- Pooled endpoint (`-pooler`) para el cómputo serverless.
- Evitar `LIKE '%x%'` sin `pg_trgm` si se busca texto (considerar extensión `pg_trgm` + índice GIN trigram para búsqueda en `projects.title`).
- Tamaño de `work_mem`/`shared_buffers`: Neon gestiona por proyecto; revisar si hay consultas de ordenación pesadas.

---

## §9 — Análisis JSONB (candidatas además de `pricing_plans.features`)

> **No se modifica ninguna entidad.** Recomendaciones arquitectónicas para decisión futura.

| Entidad / Columna | Estado actual | Recomendación | Motivo |
|---|---|---|---|
| `pricing_plans.features` | JSONB (V1+V8) | Mantener JSONB | Ya normalizado; lectura por cliente (despliegue de features en UI) |
| `chat_messages.metadata` | JSONB + GIN | Mantener | Metadata de IA/tokens ya JSONB |
| `viability_scores.ai_insights` | JSONB + GIN | Mantener | Insights del scoring ya JSONB |
| `project_context.context_data` | **TEXT** (serializado Jackson) | **JSONB** | El `ProjectContext` es un árbol JSON de dimensiones; JSONB permitiría consultas/validación y evita casts |
| `interview_state.state_data` | **TEXT** | **JSONB** | Estado de entrevista estructurado; mismos beneficios |
| `enterprise_document.metadata_json` | **TEXT** | **JSONB** | Metadatos del documento estructurados |
| `enterprise_project` (scores) | columnas individuales | Mantener columnas (no JSONB) | Ya tipadas y consultables; JSONB no aporta |
| `users` | — | (Opcional) columna `preferences`/`metadata` JSONB | Preferencias futuras de usuario sin migrar columnas |
| `webhook_events` | columnas | No aplica | Evento plano, sin JSON |

**Regla:** cualquier `TEXT` que en runtime contenga un JSON válido persistido por Jackson es candidata a JSONB. La transformación debe hacerse **en migraciones V12+** con validación `::jsonb` y **nunca** en caliente sobre rama main sin backup.

---

## §10 — Observabilidad

### 10.1 Integración
- **Micrometer + Actuator** (ya presente: `health, info, metrics, prometheus`; `micrometer-registry-prometheus` en pom).
- **Prometheus**: scrape `/api/v1/actuator/prometheus`.
- **Grafana**: dashboards conectados al datasource Prometheus.

### 10.2 Métricas a exponer
- **HikariCP** (`hikaricp.connections.*`, `hikaricp.connections.active/idle/pending`, `hikaricp.connection.timeout`): alertar sobre agotamiento del pool.
- **JDBC**: `jdbc.connections.*` y tiempos de ejecución (`jdbc.*` via micrometer si se habilita el instrumentado).
- **Flyway**: no expone métricas nativas; monitorizar **estado del esquema** vía health check custom (`/actuator/health` con indicador `flyway`) o consulta a `flyway_schema_history`.
- **JVM**: `jvm.memory.*`, `jvm.gc.*`, `jvm.threads.*`.
- **HTTP**: `http.server.requests.*` (latencia, errores, códigos).
- **DB**: métricas de Actuator `db` (pool/validación).

### 10.3 Health Checks (Actuator)
- Grupos actuales: `readiness` (readinessState + db), `liveness` (livenessState) → **mantener**.
- Añadir indicador custom de **Flyway** al readiness (esquema al día) y un chequeo de **conectividad Neon** (`SELECT 1`).
- Render usa `healthCheckPath=/api/v1/actuator/health` (readiness) → sin cambios.

### 10.4 Alertas (sugeridas)
- Pool Hikari: conexiones activas > 80 % del máximo durante > 5 min.
- Latencia P95 de `/chat`, `/chat/stream` y autenticación > umbral.
- Tasa de error 5xx > 1 % en 10 min.
- `db` readiness DOWN (indica fallo de conexión Neon).
- `flyway_schema_history` desalineado / migraciones pendientes.
- Dead tuples altos en tablas calientes (vacío de autovacuum).

### 10.5 Entregables de observabilidad
- Docker Compose local opcional: `prometheus + grafana` (perfil dev) — fuera del alcance de código en esta fase.
- Render: añadir `DATASOURCE_PROMETHEUS` (endpoint scrape) o Grafana Cloud.

---

## §11 — Skill `DATABASE_ARCHITECT` (reglas ampliadas)

(Contenido en Fase J — v2. Reglas añadidas respecto a V1: nunca modificar migración aplicada; siempre V12+; nunca iniciar Spring Boot si Flyway falla; validar conexión Neon antes de arrancar; abortar si la base no responde; sin bucles infinitos; solo PostgreSQL; H2 prohibido definitivamente.)

---

## §12 — Arquitectura futura (escalabilidad a medio/largo plazo)

### 12.1 Alta disponibilidad
- Neon con **branches y PITR**: punto de restauración instantáneo de la rama main.
- Múltiples cómputos por rama (replicas de lectura) si el workload lo exige; el pooled endpoint distribuye.
- Backup automatizado `pg_dump` + PITR de Neon como doble capa.

### 12.2 Escalabilidad
- **Serverless**: el cómputo de Neon escala verticalmente (autoscaling) según demanda; sin servidores que administrar.
- **Escalado horizontal**: réplicas de lectura para reportes/lecturas pesadas; el backend stateless ya es escalable en Render (múltiples instancias).
- `chat_messages`, `webhook_events`, `project_context` crecen: particionamiento/índices GIN y limpieza programada si el volumen lo requiere.

### 12.3 Costos
- Rama dev con autosuspend corto (cómputo en pausa = coste 0 de cómputo).
- Pooled endpoint limita conexiones concurrentes (menor footprint del cómputo).
- PITR y retención de backups ajustados a SLA (política de costes).

### 12.4 Neon Branches y entornos efímeros
- **CI/CD**: por PR → rama efímera Neon + `flyway migrate` + suite de tests contra esa rama → destrucción al terminar. Testcontainers PG 18 cubre lo local; Neon efímero cubre la paridad exacta de prod.
- **Preview environments** de Render/Next.js apuntando a ramas Neon de PR.

### 12.5 Escalado horizontal y observabilidad
- Backend sin estado → escala horizontal en Render sin fricción (sesiones JWT stateless).
- Observabilidad completa (§10) para escalar con confianza.

### 12.6 Seguridad
- SSL obligatorio; credenciales por rama con mínimo privilegio (`kin_app` solo DML).
- Secrets en Render/Neon, nunca en repo; rotación de credenciales documentada.
- CSP/HSTS ya activos en SecurityConfig; mantener.

### 12.7 Modernización futura (post-ciclo)
- **Spring Boot 4.x / Java 21+**: al exigir Flyway 13 (Java 21) en el futuro, el siguiente ciclo podrá migrar a Spring Boot 4.x + Java 21 de forma natural (la arquitectura de perfiles/datasource ya estará desacoplada de H2).
- **Migraciones de esquema** siempre vía Flyway V12+ (nunca editar aplicadas).

---

## §13 — Lista de decisiones arquitectónicas justificadas

| ID | Decisión | Justificación |
|---|---|---|
| AD-1 | **PostgreSQL 18 como objetivo final** | Ciclo de vida largo, mejoras de rendimiento/vacuum, alineado con Neon; soporte Flyway 11.x verificado. No quedarse en 16 por comodidad. |
| AD-2 | **Flyway 11.x + `flyway-database-postgresql`** | Único soporte oficial de PG 18; requiere Java 17 (compatible); API estable 10→11. |
| AD-3 | **Spring Boot 3.5.x en la misma migración** | Ventana de trabajo aprovechada; BOM actualizado (Hibernate 6.6, JDBC 42.7); sin coste marginal significativo; Java 17 sigue soportado. |
| AD-4 | **`ddl-auto=none` en todos los perfiles + Flyway como única fuente del esquema** | Elimina la divergencia dev/prod; esquema reproducible. |
| AD-5 | **Neon con ramas main/dev/efímeras** | Aislamiento dev/prod, entornos efímeros baratos, PITR. |
| AD-6 | **`JDBC_DATABASE_URL` como único punto de conexión** | Elimina deuda T8 (`DATABASE_URL` no consumida); un solo lugar para cambiar la cadena. |
| AD-7 | **Backups `pg_dump` antes/después de cada etapa** | Rollback de datos garantizado; ancla de restauración por etapa. |
| AD-8 | **Tests con Testcontainers PostgreSQL 18 (nunca H2)** | Paridad de motor en tests; elimina el riesgo de falsos verdes de H2. |
| AD-9 | **Java 17 (no subir aún)** | Flyway ≤ 12 y Spring Boot 3.5 lo soportan; subir a Java 21/Spring Boot 4 se documenta como siguiente ciclo. |
| AD-10 | **HikariCP tuned serverless** (pool 5–10, `SELECT 1`, timeouts cortos) | Evita conexiones muertas/ociosas en Neon; fallo rápido ante cómputo suspendido. |

---

## §14 — Checklist de ejecución

- [ ] Neon creado en PG 18; `SHOW server_version` = 18 en main y dev.
- [ ] `JDBC_DATABASE_URL` (pooled + `sslmode=require`) probada con psql y desde Spring (Etapa 2).
- [ ] `pom.xml`: `flyway.version=11.x` + `flyway-database-postgresql`; Spring Boot 3.5.x (Etapa 3/8).
- [ ] Flyway 11 aplica V1..V11 en dev (Etapa 3); `flyway_schema_history` consistente.
- [ ] `ddl-auto=none` en dev/test/prod; ninguna referencia H2 (Etapa 5).
- [ ] Backups `pg_dump` antes y después de cada etapa (§7).
- [ ] Render → Neon rama main con `JDBC_DATABASE_URL`; healthcheck readiness `db` UP (Etapa 6).
- [ ] Docker alineado (PG 18 fallback o eliminado) (Etapa 7).
- [ ] Tests verdes (902) sobre Testcontainers PG 18 (Etapa 8).
- [ ] Observabilidad: Prometheus scraping, métricas Hikari/JVM/HTTP, alertas configuradas (§10).
- [ ] GIN/índices/ANALYZE revisados tras la migración (§8).
- [ ] Skill `DATABASE_ARCHITECT` publicada (Fase J/§11).
- [ ] Documentación actualizada (`AGENTS.md`, README, CHANGELOG, TECH_DEBT → T8 cerrado).
- [ ] Rollback de producción documentado (Etapa 6) y probado en preview.

---

## §15 — Mejoras incorporadas respecto a V1 (resumen)

1. **PG 18 como objetivo** (V1 recomendaba PG 16 por compatibilidad de Flyway). Ventajas, riesgos, mitigaciones y estrategia de actualización documentados (Fase C).
2. **Flyway 11.x adoptado** con `flyway-database-postgresql` y override de `flyway.version`; análisis de compatibilidad con Spring Boot (Fase B).
3. **Spring Boot 3.5.x** incorporado a la migración: ventajas, riesgos, impacto, compatibilidad y rollback (Fase A + AD-3).
4. **Roadmap reordenado y más seguro** en 9 etapas (Neon → conexión → Flyway → migración → H2 off → Render → Docker → Tests → Go Live), cada una con objetivo, archivos, riesgo, tiempo, rollback y validación (Fase K).
5. **Estrategia profesional de backups** con `pg_dump`/`pg_restore`, backups antes y después de cada etapa, restauración y rollback (§7).
6. **Sección "Optimización PostgreSQL"** (índices, GIN/JSONB, FK, consultas, vacuum/analyze/autovacuum, estadísticas, performance) (§8).
7. **Sección "Observabilidad"** (Micrometer, Prometheus, Grafana, Actuator health, métricas Hikari/Flyway/JDBC, alertas) (§10).
8. **Análisis JSONB** con candidatas concretas más allá de `pricing_plans.features` (`project_context.context_data`, `interview_state.state_data`, `enterprise_document.metadata_json`) (§9).
9. **Skill `DATABASE_ARCHITECT` ampliada** con reglas obligatorias adicionales (V12+, nunca editar migraciones aplicadas, Flyway antes que el backend, abortar sin bucles, H2 prohibido) (Fase J/§11).
10. **Sección "Arquitectura futura"** (HA, escalabilidad, serverless, costos, Neon branches, CI/CD, entornos efímeros, escalado horizontal, observabilidad, seguridad, modernización Spring Boot 4/Java 21) (§12).
11. **Lista de decisiones arquitectónicas justificadas** (AD-1…AD-10) (§13).

---

*Este documento es el único entregable de esta revisión. No se modificó ningún archivo del repositorio, no se ejecutó Maven/Flyway/Docker/Render y no se crearon ramas ni commits.*
