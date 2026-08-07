# KIN_BACKEND_AUDITORIA_POSTGRESQL.md

> **Fase 1 — Auditoría y Plan de Migración H2 → PostgreSQL (Neon)**
> Modo: solo inspección. **No se modificó ningún archivo, no se ejecutó Maven, no se reinició backend, no se tocó Render/Neon/Flyway/Docker.**
> Fecha de auditoría: 2026-08-06 · Autor: Arquitecto de Software Senior / DBA PostgreSQL / DevOps Engineer / Spring Boot Expert.

---

## 1. Arquitectura actual del backend

| Capa | Tecnología | Versión | Evidencia |
|---|---|---|---|
| Framework | Spring Boot (starter-parent) | **3.2.5** | `pom.xml:9-14` |
| Lenguaje | Java (temurin) | **17** | `pom.xml:23` (`java.version=17`) |
| Build | Maven (wrapper `mvnw`) | wrapper Maven (JDK 17) | `kin-backend/mvnw` + `pom.xml` |
| Servidor | Tomcat embebido (servlet) | gestión de Spring Boot 3.2.5 | `application.yml:5-8` |
| Context path | `/api/v1` · puerto `8080` | — | `application.yml:6-8` |
| ORM | Hibernate (spring-data-jpa) | **6.4.4.Final** | runtime classpath (`hibernate-core-6.4.4.Final.jar`) |
| Migraciones | Flyway (flyway-core) | **9.22.3** | runtime classpath (`flyway-core-9.22.3.jar`) |
| DataSource pool | HikariCP | 5.0.1 | `application.yml:29-34` / runtime classpath |
| Security | Spring Security + JWT (jjwt) | Security 6.2.4 · jjwt **0.12.5** | `pom.xml:59-102`, `SecurityConfig.java` |
| OpenAPI | springdoc-openapi (Swagger UI) | **2.5.0** | `pom.xml:138-143` |
| Observabilidad | Actuator + Prometheus | gestión de Spring Boot | `pom.xml:104-114`, `application.yml:133-157` |
| IA | Spring AI (DeepSeek como modelo principal, OpenAI fallback) | **1.1.7** | `pom.xml:25,145-158`, `application.yml:98-126` |
| Pagos | Stripe (stripe-java) | 24.0.0 | `pom.xml:160-165` |
| Caché | Redis (opcional) | starter-data-redis (activo: `spring.cache.type=simple`) | `pom.xml:133-136`, `application.yml:82-83` |
| dotenv | springboot3-dotenv | 5.1.0 | `pom.xml:174-178` |
| Logs | logstash-logback-encoder (JSON en prod/render) | 7.4 | `pom.xml:116-121`, `logback-spring.xml` |

### Perfiles existentes

| Perfil | Archivo | DataSource | Uso |
|---|---|---|---|
| *(default / dev)* | `application.yml` | **H2 file** (`./data/kindb`) | Desarrollo local sin Docker |
| `test` | `application-test.yml` | H2 in-memory (`create-drop`) | Tests unitarios + E2E Playwright (backend `-Dspring-boot.run.profiles=test`) |
| `prod` | `application-prod.properties` | **PostgreSQL** | Producción |
| `render` | `application-render.properties` | PostgreSQL (hereda `prod` vía `spring.profiles.include=prod`) | Deploy Render |
| `enterprise` | `application-enterprise.properties` | PostgreSQL (hereda `prod`) | Deploy Enterprise/K8s |

> **No existe** `application-dev.yml`: el perfil de desarrollo ES el `application.yml` base (sin `spring.profiles.active`).
> **No existe** `bootstrap.yml` ni `application.properties`: la configuración vive en `application.yml` + los profiles `.properties`.

### DataSource, Hibernate, Flyway, Security, JWT, Docker, Actuator, Swagger — resumen

- **DataSource**: HikariCP (máx. pool 10, min-idle 2, timeout 20 s) en dev y prod.
- **Hibernate**: `ddl-auto=update` (dev) / `create-drop` (test) / `none` (prod). `open-in-view=false`.
- **Flyway**: deshabilitado en dev/test, habilitado en prod (`baseline-on-migrate=true`).
- **Security**: stateless JWT (`SessionCreationPolicy.STATELESS`), `BCryptPasswordEncoder`, filtros `JwtAuthenticationFilter` + `RateLimitingFilter` + `SubscriptionAccessFilter`, CORS único en `SecurityConfig`, CSP/HSTS/Permissions-Policy.
- **JWT**: `jjwt 0.12.5`, secreto `${JWT_SECRET}`, expiración 86 400 000 ms (24 h).
- **Docker**: multi-stage backend (`maven:3.9-eclipse-temurin-17-alpine` → `eclipse-temurin:17-jre-alpine`), usuario no-root, HEALTHCHECK a `/api/v1/actuator/health`. `postgres:16-alpine` como base de datos de Compose.
- **Actuator**: expone `health, info, metrics, prometheus`; probes readiness/liveness; grupo `readiness` incluye `db`. Redis excluido del health agregado.
- **Swagger**: springdoc 2.5.0 → UI en `/api/v1/swagger-ui.html` (bajo el context-path).

---

## 2. Inventario completo de configuración

### 2.1 Backend (`kin-backend/src/main/resources/`)

| Archivo | Estado | Propósito |
|---|---|---|
| `application.yml` | ✅ existe | Config base = **dev con H2 file**. Datasource H2, JPA, Flyway deshabilitado, cache simple, JWT, Actuator, Stripe, DeepSeek/OpenAI |
| `application-test.yml` | ✅ existe | Test: H2 in-memory, `ddl-auto=create-drop`, rate-limit off |
| `application-dev.yml` | ❌ **NO existe** | El rol de dev lo cumple `application.yml` base |
| `application-prod.properties` | ✅ existe | Prod: **PostgreSQL**, `ddl-auto=none`, Flyway ON, driver `org.postgresql.Driver` |
| `application-render.properties` | ✅ existe | Render: `spring.profiles.include=prod` + health probes + compresión |
| `application-enterprise.properties` | ✅ existe | Enterprise: hereda prod + pool ampliado (20) + Redis opcional |
| `application.properties` | ❌ **NO existe** | Todo en `application.yml` |
| `bootstrap.yml` | ❌ **NO existe** | Sin configuración bootstrap |
| `logback-spring.xml` | ✅ existe | Logs JSON (prod/render) o consola (dev/test), con `correlationId` |

### 2.2 Raíz del repo y otros

| Archivo | Estado | Propósito |
|---|---|---|
| `pom.xml` | ✅ existe | Dependencias, plugins de calidad (Spotless/Checkstyle/PMD/SpotBugs/JaCoCo/OWASP) |
| `docker-compose.yml` (raíz) | ✅ existe | Postgres 16 + backend (perfil `prod`) + frontend |
| `kin-database/docker-compose.yml` | ✅ existe | Solo Postgres 16 (mismo esquema) |
| `kin-backend/Dockerfile` | ✅ existe | Multi-stage backend (build Maven → runtime JRE 17) |
| `kin-frontend/Dockerfile` | ✅ existe | Multi-stage Next.js (node:20-alpine) |
| `render.yaml` | ✅ existe | Blueprint Render: Postgres `kin-db` + backend + frontend |
| `.env` (raíz) | ✅ existe (gitignored) | Claves: `JWT_SECRET`, `POSTGRES_PASSWORD`, `DEEPSEEK_API_KEY` |
| `.env.example` | ✅ existe | Referencia de variables (sin valores reales) |
| `.env.local` (`kin-frontend/`) | ✅ existe | Variables del frontend (NEXT_PUBLIC_API_URL, etc.) |
| `kin-database/init.sql` | ✅ existe | **Referencia histórica, fuera de la ruta de despliegue** (el esquema lo crea Flyway V1..V11) |
| `kin-database/fix_pricing_plans_schema.sql` | ✅ existe | Script manual legacy; reemplazado por V5/V8 (ya no se usa) |

### 2.3 Migraciones Flyway (`classpath:db/migration`) — 11 migraciones

| Migración | Contenido |
|---|---|
| `V1__create_base_schema.sql` | Base: `users`, `projects`, `chat_messages`, `viability_scores`, `pricing_plans` (base) |
| `V2__add_viability_scoring_column.sql` | `pricing_plans.viability_scoring_detail` + CHECK |
| `V3__create_project_context.sql` | `project_context` (contexto durable) |
| `V4__create_interview_state.sql` | `interview_state` (entrevista estratégica) |
| `V5__complete_pricing_plans_schema.sql` | Columnas faltantes de `pricing_plans` (idempotente) |
| `V6__create_categories.sql` | `categories` + seed 17 categorías + FK `projects.category_id` + backfill |
| `V7__create_enterprise_project.sql` | `enterprise_project`, `enterprise_document` |
| `V8__enforce_pricing_plans_features_jsonb.sql` | Normaliza `features` como JSONB |
| `V9__add_current_plan_to_users.sql` | `users.current_plan_id` + FK a `pricing_plans` |
| `V10__create_user_subscriptions.sql` | `user_subscriptions` + CHECK de estado |
| `V11__create_webhook_events.sql` | `webhook_events` (idempotencia Stripe, `BIGSERIAL`) |

---

## 3. Dependencias

### ¿Existe dependencia H2? → **SÍ**

```xml
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>runtime</scope>
</dependency>
```
- Versión efectiva: **2.2.224** (gestionada por Spring Boot 3.2.5; confirmada en runtime classpath).
- Necesaria para dev (H2 file) y test (H2 mem).

### ¿Existe dependencia PostgreSQL? → **SÍ**

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```
- Versión efectiva: **42.6.2** (gestionada por Spring Boot 3.2.5; confirmada en runtime classpath).

### ¿Flyway? → **SÍ**

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
```
- Versión efectiva: **9.22.3** (gestionada por Spring Boot 3.2.5).
- En Flyway 9.x el soporte de PostgreSQL está **incluido en `flyway-core`** (no requiere el módulo `flyway-database-postgresql`, que sí es obligatorio a partir de Flyway 10 — importante si se actualiza).

### ¿Duplicadas / incompatibilidades?

- **Sin duplicados.** H2 y PostgreSQL coexisten con scope `runtime` — práctica estándar (driver disponible solo en tiempo de ejecución, nunca en compilación).
- **Sin incompatibilidad** en el arranque actual: el perfil activo decide el driver (`org.h2.Driver` por defecto, `org.postgresql.Driver` en prod). No se carga H2 en prod ni Postgres en dev.
- **Incompatibilidad latente (ver Fase 1 §5, §8):** `flyway-core 9.22.3` soporta oficialmente PostgreSQL ≤ 16. Si el objetivo es **PostgreSQL 17/18 (Neon)**, Flyway puede lanzar `Unsupported Database: PostgreSQL X`. Ver sección 10.

---

## 4. DataSource — dónde se configura `spring.datasource.*`

| Perfil | Archivo | `spring.datasource.url` | Driver |
|---|---|---|---|
| **Dev (default)** | `application.yml:24-34` | `jdbc:h2:file:./data/kindb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` | `org.h2.Driver` |
| **Test** | `application-test.yml:2-3` | `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1` | `org.h2.Driver` (heredado) |
| **Prod** | `application-prod.properties:10-13` | `${JDBC_DATABASE_URL:jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}` | `org.postgresql.Driver` |
| **Render** | `application-render.properties:6` (include `prod`) | hereda `prod` | PostgreSQL |
| **Enterprise** | `application-enterprise.properties:6` (include `prod`) | hereda `prod` | PostgreSQL |

**Resumen por tecnología/entorno:**

- **H2**: perfil **default (dev)** y **test**.
- **PostgreSQL**: perfiles **prod, render, enterprise** (todos derivan de `application-prod.properties`).
- **Render**: conecta vía `DB_HOST` / `DB_PORT` / `DB_NAME` / `DATABASE_USER` / `DATABASE_PASSWORD` (inyectados por `render.yaml` con `fromDatabase`), o override manual con `JDBC_DATABASE_URL`. **`DATABASE_URL` (formato `postgresql://`) se expone pero NO se consume** — deuda técnica documentada (T8).
- **Neon**: hoy **no se usa**; el objetivo de la migración es apuntar estos mismos placeholders a un endpoint de Neon (rama DEV), manteniendo `spring.profiles.include=prod` y solo cambiando las variables de entorno.

Hikari (idéntico en dev y prod): `maximum-pool-size=10`, `minimum-idle=2`, `idle-timeout=30000`, `max-lifetime=1800000`, `connection-timeout=20000`. Enterprise amplía a 20/5.

---

## 5. Flyway

| Atributo | Valor |
|---|---|
| Ubicación | `classpath:db/migration` (`spring.flyway.locations`) |
| Versión | **9.22.3** (gestionada por Spring Boot 3.2.5) |
| Estado (dev/test) | **Deshabilitado** (`spring.flyway.enabled=false` en `application.yml`) |
| Estado (prod/render/enterprise) | **Habilitado** (`spring.flyway.enabled=true` en `application-prod.properties`) |
| Baseline | **Sí**: `baseline-on-migrate=true`. **Sin** `baseline-version` explícito → baseline por defecto a versión 1 si la base no está vacía. |
| Nº de migraciones | **11** (V1…V11) |
| Chequeo de checksum | Sí (comportamiento por defecto de Flyway: valida checksum de migraciones aplicadas) |

### ¿Migraciones incompatibles con PostgreSQL?

**No.** Al contrario: las 11 migraciones son **100% específicas de PostgreSQL** y **ninguna es compatible con H2**:

- Extensiones: `CREATE EXTENSION IF NOT EXISTS "uuid-ossp"` (V1).
- Tipos: `TIMESTAMPTZ`, `JSONB`, `BIGSERIAL`, `DOUBLE PRECISION`, `NUMERIC(x,y)`.
- Sintaxis plpgsql: bloques `DO $$ ... $$` (V6, V9), consultas a `pg_constraint` e `information_schema`.
- Índices: `USING GIN (jsonb)` (V1).
- `ON CONFLICT (id) DO NOTHING` (V6) y `ALTER COLUMN ... TYPE JSONB USING ...` (V8).

Que no corran sobre H2 es **intencional y correcto**: el perfil dev NO ejecuta Flyway (usa `ddl-auto=update`), y el perfil prod ejecuta únicamente estas migraciones sobre una base PostgreSQL vacía. **No existe ninguna migración escrita para H2.**

> Riesgo asociado: el esquema de **dev (H2, generado por Hibernate)** y el de **prod (PostgreSQL, generado por Flyway)** se mantienen por caminos distintos. La fuente de verdad es el modelo JPA para H2 y las migraciones para PG (ver sección 8, R3).

---

## 6. Hibernate

| Atributo | Dev (application.yml) | Test (application-test.yml) | Prod (application-prod.properties) |
|---|---|---|---|
| `ddl-auto` | **update** | **create-drop** | **none** |
| Dialect | `org.hibernate.dialect.H2Dialect` | (heredado) | `org.hibernate.dialect.PostgreSQLDialect` |
| `show-sql` | **true** | (heredado) | **false** |
| `open-in-view` | false | — | false |
| `format_sql` | true | — | false |
| `default_batch_fetch_size` | 50 | — | 50 |
| `enable_lazy_load_no_trans` | false | — | false |
| `lob.non_contextual_creation` | true | — | true |

**Naming strategy:** **no configurada explícitamente** en ningún archivo. Spring Boot usa por defecto `SpringPhysicalNamingStrategy` (camelCase → snake_case) y `CamelCaseToUnderscoresNamingStrategy` implícito. Las tablas/columnas en migraciones y entidades son consistentes con snake_case (`current_plan_id`, `user_subscriptions`, `project_context`, etc.).

---

## 7. Render (y cómo conecta hoy)

### Variables buscadas

| Variable | ¿Dónde se define? | ¿Se usa en el backend? |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `render.yaml:22-23` → valor **`render`** | Sí: activa `application-render.properties` (que incluye `prod`) |
| `DATABASE_URL` | `render.yaml:32-35` (`fromDatabase` → `connectionString`, formato `postgresql://...`) | **NO** (ver T8) |
| `SPRING_DATASOURCE_URL` | No definida en Render | No (en Render se deriva de `DB_HOST/DB_PORT/DB_NAME`) |
| `SPRING_DATASOURCE_USERNAME` | No definida en Render | No (se usa `DATABASE_USER`) |
| `SPRING_DATASOURCE_PASSWORD` | No definida en Render | No (se usa `DATABASE_PASSWORD`) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `render.yaml:36-47` (`fromDatabase`) | **Sí**: `application-prod.properties:10` |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `render.yaml:48-55` (`fromDatabase`) | **Sí**: `application-prod.properties:11-12` |
| `JDBC_DATABASE_URL` | No definida en Render (override manual opcional) | Sí: permite reemplazar la URL derivada |
| `JWT_SECRET`, `DEEPSEEK_API_KEY`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | `render.yaml:24-31` (`sync:false` → secretos del dashboard) | Sí |

### Cómo conecta Render a la base hoy

1. `render.yaml` crea la base gestionada `kin-db` (`databaseName: kin_platform`, `user: kin_admin`, plan starter).
2. Render inyecta `DB_HOST/DB_PORT/DB_NAME/DATABASE_USER/DATABASE_PASSWORD` (y `DATABASE_URL`, no consumida).
3. El backend corre con `SPRING_PROFILES_ACTIVE=render` → carga `application-render.properties` → incluye `prod` → `application-prod.properties` construye:
   `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}` con `DATABASE_USER`/`DATABASE_PASSWORD`.
4. `ddl-auto=none` + Flyway ON → Flyway aplica V1..V11 sobre la base vacía.

**Hoy Render NO está conectado a Neon**: usa su base gestionada. La migración a Neon consistirá en redirigir los placeholders anteriores (o definir `JDBC_DATABASE_URL`) al endpoint/pool de Neon.

---

## 8. Riesgos

| # | Riesgo | Impacto | Probabilidad | Mitigación |
|---|---|---|---|---|
| R1 | **Flyway 9.22.3 sin soporte oficial de PostgreSQL 17/18.** El soporte de PG 17 llegó con Flyway ≥ 10.11 y PG 18 con Flyway ≥ 11.x. Al conectar a una rama Neon que reporte PG 17/18, Flyway puede lanzar `Unsupported Database` y abortar el arranque. | **Alto** (bloquea arranque en dev/prod) | **Alta** (si la rama Neon usa PG 17/18) | Elegir en Neon una rama con la versión soportada (PG 16) **o** subir `flyway-core` a 10.x/11.x (requiere añadir `flyway-database-postgresql` y revalidar), **o** actualizar Spring Boot a una versión que gestione Flyway ≥ 10.20. Validar con un `flyway migrate` de prueba en la rama DEV |
| R2 | **`DATABASE_URL` de Render no es JDBC y no se consume (T8).** Al apuntar a Neon con `connectionString` se corre el riesgo de copiar `DATABASE_URL` y que el backend la ignore, quedándose con la base anterior o fallando. | **Medio** (configuración incorrecta en prod) | **Media** | Documentar/eliminar la dependencia de `DATABASE_URL`; usar `JDBC_DATABASE_URL` (formato `jdbc:postgresql://`) o mantener `DB_HOST/DB_PORT/DB_NAME`. Añadir una prueba de smoke que verifique contra qué base se conecta (ej. `SELECT current_database()`) |
| R3 | **Divergencia de esquema dev (H2/`ddl-auto=update`) vs prod (PG/Flyway).** Cambios de entidad que Hibernate aplica a H2 no tienen migración PG → falla en prod. | **Alto** | **Alta** | Regla: toda entidad nueva requiere su migración Flyway. Usar `validate`/test de migración en CI con una base PG efímera. La presente migración a dev-PG elimina este riesgo (mismo motor) |
| R4 | **Discrepancia de tipos H2 vs PG** (ej. `features` como `JSONB` en PG vs `TEXT` en la entidad; `context_data`/`state_data` como `TEXT`). Puede ocultar bugs que solo aparecen en PG. | **Medio** | **Media** | Al migrar dev a PG, correr la suite de tests de integración contra la rama DEV de Neon y alinear tipos JPA con el esquema PG |
| R5 | **Pérdida/duplicación de datos en la migración de datos H2 → Neon** (IDs, secuencias, JSONB, `webhook_events.id BIGSERIAL`). | **Alto** (pérdida de datos) | **Media** (si se migra el DB local) | Backup de `kin-backend/data/kindb.mv.db` antes de migrar; migración de datos con script parametrizado y validación por conteos; Neon permite branches para ensayar |
| R6 | **Rendimiento/pool en Neon serverless.** Neon desconecta conexiones ociosas y exige SSL; Hikari local (timeout 30 s idle, 30 min lifetime) puede acumular conexiones muertas. | **Medio** | **Media** | Usar el **pooled connection string** (puerto 5432 estándar o `-pooler`), SSL requerido (`?sslmode=require`), reducir `idle-timeout`/`max-lifetime`, validar conexión (`connection-init-sql=SELECT 1` ya existe en enterprise) |
| R7 | **Versión de PostgreSQL de la rama Neon desconocida/inestable** (ephemeral branches). | **Medio** | **Media** | Fijar la versión al crear la rama DEV; comprobar `SHOW server_version`; mantener rama de respaldo (parent) intacta |
| R8 | **Secreto `JWT_SECRET` real presente en `.env` local** (gitignored, pero legible en el workspace). | **Alto** (si se filtra) | **Baja** (local) | No exponer valores en logs/docs/commits; rotar el secreto si alguna vez se publica. No se reproducen valores en este documento |
| R9 | **Migraciones Flyway nuevas tras el baseline** → si una rama ya aplicó V1..V11 y se agrega V12 con chequeo de checksum, bases existentes fallan por checksum si se edita una migración ya aplicada. | **Medio** | **Baja** | Nunca editar una migración aplicada; agregar V12+; usar `baseline-on-migrate` solo sobre bases vacías/controladas |
| R10 | **`ddl-auto=update` residual en un entorno conectado a Neon** si se arranca dev con profile incompleto → Hibernate podría alterar el esquema PG. | **Alto** | **Media** | El nuevo `application-dev.yml` con `ddl-auto=none` + Flyway ON elimina el riesgo; comprobar que ningún script de dev sobrescriba `spring.profiles.active` |
| R11 | **`next build`/frontend** fuera de alcance pero afecta el flujo E2E que depende del backend (perfil `test` con H2 mem). No afecta a la migración. | Bajo | Baja | Mantener `application-test.yml` con H2 mem hasta definir la estrategia de tests sobre PG |

---

## 9. Plan de migración (diseño por fases)

> Este plan es **propuesta de diseño**; no se ejecuta nada en esta Fase 1.

### Fase 2 — Crear profile dev PostgreSQL

- Crear `application-dev.yml` (o reutilizar `application-prod.properties`) con:
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/kin_platform_dev` (o el endpoint DEV de Neon).
  - `spring.jpa.hibernate.ddl-auto=none`, dialecto `PostgreSQLDialect`, `show-sql` según preferencia.
  - `spring.flyway.enabled=true`, `locations=classpath:db/migration`, `baseline-on-migrate=true`.
- Activar por defecto el perfil `dev` en dev (o documentar el arranque con `-Dspring-boot.run.profiles=dev`).
- Actualizar el script de arranque FAIL-FAST (`scripts/start-dev-backend.ps1/.sh`) para lanzar con el perfil `dev`.
- **Criterio de salida:** `mvnw spring-boot:run` (perfil `dev`) arranca contra PG local/Docker con Flyway V1..V11 aplicadas sin errores.

### Fase 3 — Conectar desarrollo a una rama DEV de Neon

- Crear en Neon un proyecto y una **rama `dev`** (idealmente con la misma versión de PostgreSQL que se validará; recomendación inicial PG 16 por compatibilidad con Flyway 9.22.3).
- Configurar credenciales en `.env` (dev): `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` o `JDBC_DATABASE_URL`.
- Probar conectividad (psql/`SELECT version()`), SSL (`sslmode=require`) y el pooled endpoint si se usa el pooler.
- **Criterio de salida:** el backend en `dev` arranca y responde `/actuator/health` UP con `db` ready contra la rama DEV de Neon; la base queda sin datos (vacía) y Flyway aplica el esquema.

### Fase 4 — Migrar Flyway

- Validar la versión de Flyway frente a la versión de PostgreSQL de la rama Neon (R1).
- Si PG 17/18: **upgrade de Flyway** (a 10.20+/11.x con `flyway-database-postgresql`) o fijar la rama a PG 16.
- Ejecutar `flyway migrate` de prueba contra la rama DEV; verificar `flyway_schema_history` (11 filas V1..V11) y checksums.
- Ejecutar la suite de tests de backend (902 tests) con el perfil `dev` apuntando a Neon DEV para detectar divergencias H2 vs PG.
- **Criterio de salida:** 11/11 migraciones aplicadas, history coherente, tests verdes contra Neon DEV.

### Fase 5 — Eliminar H2

- Eliminar la dependencia `com.h2database:h2` y la config `spring.h2.console` del `application.yml`.
- Migrar (si aplica) datos locales de `data/kindb.mv.db` → Neon DEV con script controlado (backup previo, R5).
- Actualizar `scripts/reset-dev-db.*` → renombrar/rediseñar a reset de Neon (reset = `flyway clean` + `migrate` sobre la rama DEV, nunca sobre la rama main).
- **Criterio de salida:** ningún `.properties`/`.yml` referencia H2; el proyecto arranca y testea solo sobre PostgreSQL.

### Fase 6 — Actualizar Docker

- Asegurar que `docker-compose.yml` (postgres:16-alpine) sea coherente con la versión de Neon elegida, o sustituir el servicio `postgres-db` por el endpoint de Neon como referencia.
- Actualizar `kin-backend/Dockerfile` si el upgrade de Flyway implica nueva dependencia.
- **Criterio de salida:** `docker compose up --build` arranca backend contra la base correcta y el healthcheck pasa.

### Fase 7 — Actualizar documentación

- `AGENTS.md`, `README.md`, `CHANGELOG.md` y los releases (`v1.0.0-phase8.md`, `v1.1.0-phase9.md`): cambiar "PostgreSQL 16 (prod) · H2 (dev)" por la nueva realidad (dev y prod sobre PostgreSQL/Neon).
- `kin-docs/`: registrar la decisión (ADR) de migración H2→Neon y actualizar `TECHNICAL_DEBT_REGISTER.md` (T8 y la columna `features` JSONB).
- **Criterio de salida:** la documentación no menciona H2 salvo como histórico.

### Fase 8 — Validación final

- Smoke test: `actuator/health` (readiness con `db` UP), login, creación de proyecto, chat, subscription, webhook.
- Verificar `SHOW server_version`, conteos de tablas, índices GIN y `flyway_schema_history`.
- Ejecutar los E2E de Playwright (backend arrancado con perfil dev/Neon).
- **Criterio de salida:** checklist de aceptación completo y despliegue de referencia actualizado.

---

## 10. Compatibilidad

| Componente | Versión | PostgreSQL 16 | PostgreSQL 17 | PostgreSQL 18 | Neon |
|---|---|---|---|---|---|
| Spring Boot | 3.2.5 | ✅ soportado | ✅ (funcional) | ✅ (funcional) | ✅ |
| Java | 17 | ✅ | ✅ | ✅ | — |
| Hibernate | 6.4.4.Final | ✅ soporte oficial | ✅ (con warning de versión) | ✅ (con warning de versión) | ✅ |
| JDBC PostgreSQL | 42.6.2 | ✅ | ✅ | ✅ (driver retrocompatible) | ✅ |
| **Flyway** | **9.22.3** | ✅ soporte oficial (desde 9.22.0) | ⚠️ **No oficial** (requiere ≥ 10.11) | ❌ **No soportado** (requiere ≥ 11.x) | ⚠️ depende de la versión reportada por la rama |
| springdoc | 2.5.0 | ✅ | ✅ | ✅ | — |

**Conclusiones de compatibilidad:**

1. **Spring Boot 3.2.5 + Java 17 + Hibernate 6.4.4 + JDBC 42.6.2** funcionan con PostgreSQL 16/17/18 y con Neon sin cambios.
2. **El cuello de botella es Flyway 9.22.3**: es compatible con **PostgreSQL 16** pero **no con 17/18**. Si la rama Neon se crea en PG 18, el arranque puede fallar.
3. **Neon es wire-compatible con PostgreSQL** (mismo protocolo); el endpoint `jdbc:postgresql://...` con SSL funciona sin cambios en el driver. Usar el pooled connection string para workloads serverless.
4. **Acción recomendada**: o bien fijar la rama Neon en **PostgreSQL 16** (cero cambios de dependencia), o bien **upgrade de Flyway** a 10.20+/11.x (añadiendo `flyway-database-postgresql`) si se desea PG 17/18. La primera opción es la de menor riesgo para esta base de código.

---

## 11. Resultado final

- **Estado actual:** el backend usa H2 en dev/test y PostgreSQL en prod/render/enterprise. Flyway (9.22.3, deshabilitado en dev, habilitado en prod) gestiona 11 migraciones 100% PostgreSQL.
- **La migración a Neon es viable** con bajo esfuerzo si se mantiene **PostgreSQL 16** en la rama Neon. Con PG 17/18 requiere **upgrade de Flyway** (R1).
- **Prerrequisito crítico:** `application-prod.properties` deriva la URL de `DB_HOST/DB_PORT/DB_NAME/DATABASE_USER/DATABASE_PASSWORD`; apuntar esos valores a Neon (o usar `JDBC_DATABASE_URL`) es la única pieza de configuración que cambia la conexión. `DATABASE_URL` de Render no se consume (T8).
- **No se modificó ningún archivo.** Este documento es el entregable de la Fase 1. Las Fases 2–8 quedan diseñadas y pendientes de aprobación.

---

*Anexo: archivos inspeccionados (solo lectura): `pom.xml`, `application.yml`, `application-test.yml`, `application-prod.properties`, `application-render.properties`, `application-enterprise.properties`, `logback-spring.xml`, 11 migraciones V1..V11, `docker-compose.yml` (raíz y kin-database), `Dockerfile` (backend y frontend), `render.yaml`, `.env.example`, `.env` (solo nombres de claves; los valores no se reproducen), `kin-database/init.sql`, `fix_pricing_plans_schema.sql`, `SecurityConfig.java`, `DataInitializer.java`, `CategoryDataInitializer.java`, runtime classpath del proceso Java activo.*
