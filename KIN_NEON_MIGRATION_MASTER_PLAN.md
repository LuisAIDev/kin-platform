# KIN_NEON_MIGRATION_MASTER_PLAN.md

> **KIN — Fase 2 · Plan Maestro de Migración Arquitectónica H2 → PostgreSQL Neon**
> Nivel: Enterprise · Documento de diseño y planificación.
> **Modo: solo diseño. No se modificó código, no se ejecutó Maven/Flyway, no se tocó Render/Neon/Docker. No se hicieron commits.**
> Fecha: 2026-08-06 · Basado en: `KIN_BACKEND_AUDITORIA_POSTGRESQL.md` (Fase 1, auditoría completa).

---

## 0. Resumen ejecutivo (TL;DR)

- **Decisión:** eliminar H2 de forma definitiva. Único motor de persistencia: **PostgreSQL sobre Neon** (desarrollo y producción).
- **Bloqueador técnico #1:** `flyway-core 9.22.3` (gestionada por Spring Boot 3.2.5) **NO soporta PostgreSQL 17/18**. Soporta oficialmente hasta PostgreSQL 16.
- **Decisión recomendada (riesgo mínimo):** crear/verificar las ramas Neon en **PostgreSQL 16** y mantener Flyway 9.22.3 sin cambios.
- **Decisión alternativa (si Neon ya está en PG 17/18):** migrar a **Flyway 11.x** + módulo `flyway-database-postgresql`, con validación de API contra Spring Boot 3.2.5 (ver Fase B).
- **Único punto de conexión a cambiar:** la cadena JDBC que hoy deriva `application-prod.properties` desde `DB_HOST/DB_PORT/DB_NAME` (o `JDBC_DATABASE_URL`). Render ya tiene ese override preparado.
- **Fuerza de trabajo:** 5 etapas (Fase K), ~2–4 días-hombre con validación en cada etapa, sin ventanas de indisponibilidad si se sigue el orden propuesto.

---

## FASE A — Compatibilidad completa del stack

### A.1 Matriz de compatibilidad

| Componente | Versión actual | PostgreSQL 16 | PostgreSQL 17 | PostgreSQL 18 | Neon |
|---|---|---|---|---|---|
| Spring Boot | 3.2.5 | ✅ | ✅ | ✅ | ✅ |
| Java | 17 | ✅ | ✅ | ✅ | — |
| Hibernate | 6.4.4.Final | ✅ (oficial) | ✅ (warning de versión) | ✅ (warning de versión) | ✅ |
| JDBC PostgreSQL | 42.6.2 | ✅ | ✅ | ✅ (retrocompatible) | ✅ |
| **Flyway** | **9.22.3** | ✅ (desde 9.22.0) | ⚠️ **No oficial** (≥ 10.11) | ❌ **No soportado** (≥ 11.0) | ⚠️ depende de versión de la rama |
| springdoc | 2.5.0 | ✅ | ✅ | ✅ | ✅ |
| HikariCP | 5.0.1 | ✅ | ✅ | ✅ | ⚠️ requiere tuning (ver A.4) |

### A.2 Verificaciones técnicas por componente

**Spring Boot 3.2.5**
- Compatible con PostgreSQL 16/17/18 y con Neon (mismo protocolo wire de PostgreSQL). Sin cambios de código.
- Gestiona versiones: Hibernate 6.4.4.Final, Flyway 9.22.3, PostgreSQL JDBC 42.6.2, H2 2.2.224.
- **Riesgo:** cualquier upgrade de Flyway por encima de lo gestionado (10/11) implica override en `pom.xml` y posible ajuste de la auto-configuración `FlywayAutoConfiguration`.

**Hibernate 6.4.4.Final**
- `PostgreSQLDialect` verifica versión mínima (8.2) → PG 16/17/18 pasan.
- Puede emitir un *warning* de "database version not explicitly supported" con PG 17/18, pero opera correctamente.
- **Problema conocido:** diferencias de tipos entre H2 y PG (`JSONB` vs `TEXT`, `BOOLEAN`, `TIMESTAMPTZ`). En Neon, `features` es JSONB y la entidad `PricingPlan.features` es `String` con `@JdbcTypeCode(SqlTypes.JSON)` → escribir JSON válido siempre. **No cambiar la entidad.**

**Flyway**
- Ver Fase B completa.

**PostgreSQL JDBC 42.6.2**
- Driver retrocompatible: funciona con PG 16/17/18.
- Neon requiere **SSL** → añadir `?sslmode=require` a la URL JDBC.

**Neon**
- Plataforma serverless (PostgreSQL gestionado). Wire-compatible. Exige:
  - SSL obligatorio.
  - **Pooled endpoint** (PgBouncer, sufijo `-pooler`) para conexiones de aplicaciones; conexión directa también disponible.
  - Cierre de conexiones ociosas (~5 min) y computo auto-suspendible (wake-up ~1 s en la primera conexión tras pausa).

### A.3 Riesgos del stack

| Riesgo | Impacto | Prob. | Mitigación |
|---|---|---|---|
| Flyway 9.22.3 vs PG 17/18 (`Unsupported Database`) | Bloquea arranque | Alta | Rama Neon en PG 16, o upgrade de Flyway (Fase B) |
| API de Flyway 10/11 vs Spring Boot 3.2.5 | Fallo de auto-config | Media | Validar con smoke test; si falla, subir Spring Boot a 3.4.x/3.5.x |
| Desborde de pool Hikari en Neon | Conexiones muertas / lentitud | Media | Pooled endpoint + tuning (A.4) |
| JSONB `features` mal escrito por flujo no controlado | Excepción en insert/update | Baja | Validación en DataInitializer/CRUD (ya existente) |
| Diferencia de esquema H2 (dev) vs PG (prod) | Bug en prod | Alta (hoy) → Baja (post-migración) | Eliminar H2 = el mismo motor en ambos entornos |

### A.4 Recomendaciones HikariCP para Neon

| Parámetro | Valor recomendado | Motivo |
|---|---|---|
| `maximum-pool-size` | 5–10 (serverless) | Neon escala computo; pool grande innecesario |
| `minimum-idle` | 0–2 | Evitar mantener conexiones ociosas (Neon las cierra) |
| `connection-timeout` | 10 000 ms | Fallo rápido si el computo está suspendido |
| `idle-timeout` | 60 000 ms | < tiempo de idle de Neon (~5 min) |
| `max-lifetime` | 1 200 000 ms (20 min) | Renovar antes de que Neon/capa intermedia corte |
| `connection-init-sql` | `SELECT 1` | Validar conexión al crear (ya presente en `enterprise`) |

> Nota: `spring.datasource.hikari.connection-test-query` no es necesario; Hikari valida con `isValid()` por defecto. `connection-init-sql=SELECT 1` sí fuerza una validación real en el pooled endpoint.

---

## FASE B — Flyway 9.22.3 y PostgreSQL 18

### B.1 Respuesta directa

**NO. Flyway 9.22.3 no soporta PostgreSQL 17 ni 18.**

Matriz de soporte (comunidad Flyway):

| PostgreSQL | Flyway mínimo que lo soporta |
|---|---|
| ≤ 16 | Flyway 9.22.x (actual del proyecto) ✅ |
| 17 | Flyway **10.11.0+** |
| 18 | Flyway **11.0.0+** |

Flyway 9.22.3 registra `PostgreSQLDatabaseType` con rango de versiones soportadas hasta 16. Al conectar a una rama Neon que reporte 17.x/18.x lanza un error tipo `Unsupported Database: PostgreSQL 17.0/18.0` (o la recomendación de upgrade) y **aborta la migración/arranque**.

### B.2 Estrategia recomendada (orden de preferencia)

**Opción 1 — MANTENER Flyway 9.22.3 (recomendada, riesgo mínimo)**
- Elegir rama Neon (dev y prod) en **PostgreSQL 16**.
- Cero cambios de dependencias, cero riesgo de auto-config.
- Neon permite elegir la versión de PostgreSQL al crear el proyecto/rama; verificar con `SHOW server_version`.
- **Criterio:** válida si el proyecto Neon ya existe con PG 16 o puede recrearse así.

**Opción 2 — UPGRADE de Flyway (si Neon ya es PG 17/18)**
- En `pom.xml`: sobrescribir propiedad gestionada `flyway.version` a **11.x** (o 10.20.x si se quiere conservar API más cercana a 9).
- Añadir el módulo **`org.flywaydb:flyway-database-postgresql`** (obligatorio desde Flyway 10; en Flyway 9 el soporte PG iba dentro de `flyway-core`).
- Verificar compatibilidad de API con Spring Boot 3.2.5 (`FlywayAutoConfiguration` usa `FluentConfiguration`; en 10/11 la API núcleo se conserva, pero **debe validarse con un arranque de prueba**).
- **Fallback si hay incompatibilidad:** subir Spring Boot a 3.4.x/3.5.x (gestiona Flyway 10.20+/11.x) en la misma etapa.

**Opción 3 — No recomendada:** mantener PG 18 sin upgrade de Flyway. **Inválida.**

### B.3 Regla de oro (aplicar en la Fase 4 del Roadmap)

- No editar nunca una migración ya aplicada (checksum de Flyway).
- Agregar migraciones nuevas como `V12+` contra la rama DEV.
- Verificar `flyway_schema_history` (11 filas V1..V11) tras el primer `migrate` en Neon.

---

## FASE C — Arquitectura definitiva (objetivo)

```
                        ┌───────────────────────┐
                        │         KIN           │
                        │ (UI Next.js, Vercel)  │
                        └───────────┬───────────┘
                                    │ HTTPS
                        ┌───────────▼───────────┐
                        │     Spring Boot 3.2   │
                        │  (kin-backend / 8080) │
                        │  Perfiles dev|test|   │
                        │  prod|render|enterprise│
                        └───────────┬───────────┘
                                    │ JDBC (SSL, pooled)
                        ┌───────────▼───────────┐
                        │  PostgreSQL NEON       │
                        │  ├─ rama main → prod   │
                        │  └─ rama dev  → dev    │
                        │  Flyway V1..V11        │
                        └───────────────────────┘
                        H2  →  ELIMINADO
```

**Principios inmutables:**
1. **Nunca más H2.** Ningún perfil, script o test referencia H2.
2. Único motor: **PostgreSQL (Neon)**.
3. **Flyway es la única fuente de verdad del esquema** en todos los entornos (`ddl-auto=none` siempre).
4. Dev y prod apuntan a **ramas distintas** de Neon (mismo proyecto), nunca a la misma rama.
5. `DATABASE_URL` (formato `postgresql://` de Render) **no se usa**; se usa `JDBC_DATABASE_URL` o `DB_HOST/DB_PORT/DB_NAME` (Fase G).

---

## FASE D — Perfiles Spring (diseño)

> Propuesta de estructura. **No se escriben los archivos todavía** (Fase 4 del Roadmap).

### D.1 Estructura objetivo

```
kin-backend/src/main/resources/
├── application.yml               # COMÚN (sin datasource propio de H2)
├── application-dev.yml           # NUEVO: Neon rama dev (sustituye al default H2)
├── application-test.yml          # MODIFICADO: PostgreSQL (Testcontainers o Neon) en vez de H2
├── application-prod.yml          # NUEVO/renombrado desde .properties: Neon rama main
├── application-render.properties # MANTENIDO (include prod) + apuntar a Neon
├── application-enterprise.properties # MANTENIDO (include prod)
└── db/migration/V1..V11          # SIN CAMBIOS (salvo nueva V12+ si hace falta)
```

### D.2 Contenido de cada perfil

**`application.yml` (común — se limpia de H2)**
- `server.port`, `server.servlet.context-path=/api/v1`, compresión.
- `spring.application.name`, `jackson`, `ai` (DeepSeek/OpenAI), `deepseek`, `stripe`, `jwt`, `springdotenv`.
- `management` (actuator: health, info, metrics, prometheus, probes, grupos readiness/liveness, redis excluido).
- `logging`.
- **Se elimina:** bloque `spring.datasource` (H2), `spring.h2.console`, `spring.jpa.database=H2`, dialecto H2, `ddl-auto: update`, `flyway.enabled: false`.
- **Común JPA/PG (por defecto, sobrescribible):** `spring.jpa.open-in-view=false`, `properties.hibernate.default_batch_fetch_size=50`, `format_sql`.
- `spring.flyway.locations=classpath:db/migration`, `baseline-on-migrate=true`.

**`application-dev.yml` (NUEVO — Neon rama dev)**
- `spring.profiles.active` no se define aquí (se activa vía script/env).
- Datasource: `JDBC_DATABASE_URL` (o `DB_HOST/DB_PORT/DB_NAME`) apuntando a la **rama dev de Neon**, `?sslmode=require`, driver `org.postgresql.Driver`.
- Hikari: pool 5, `connection-init-sql=SELECT 1`, timeouts de Neon (A.4).
- JPA: `ddl-auto=none`, dialecto `PostgreSQLDialect`, `show-sql=true`.
- Flyway: `enabled=true` (aplica V1..V11 sobre la rama vacía).
- `springdotenv.directory=../` (sigue leyendo `.env` de la raíz).

**`application-test.yml` (MODIFICADO — PostgreSQL en tests)**
- **Opción A (recomendada): Testcontainers** `postgres:16-alpine` (o la versión de Neon elegida) en `src/test`, con `spring.datasource.url` dinámica y `ddl-auto=none` + Flyway `clean` + `migrate` por test o una vez por suite.
- **Opción B:** apuntar los tests a una **rama efímera de Neon** (cierra el pool, flyway clean+migrate). Más lento y requiere red; se reserva para E2E.
- Rate-limit: se mantiene `app.rate-limit.enabled=false`.
- **Se elimina:** `jdbc:h2:mem:testdb`, `ddl-auto: create-drop`.
- Consecuencia: la suite de 902 tests actuales que asumen H2 deberá validarse contra PG (sobre todo tipos JSONB y UUID).

**`application-prod.yml` (NUEVO/renombrado desde `.properties`)**
- Contenido equivalente al actual `application-prod.properties` (datasource PG, `ddl-auto=none`, Flyway ON, driver PG, Hikari, JWT, CORS, rate-limit trust-proxy, cookies Secure/SameSite=None), pero con la URL por defecto apuntando a **Neon rama main** (o usando `JDBC_DATABASE_URL`).
- `spring.jpa.properties.hibernate.dialect=PostgreSQLDialect`.
- Migrar de `.properties` a `.yml` es opcional (mejora de consistencia); si se renombra, se debe eliminar el archivo `.properties` en la misma etapa para evitar resolución ambigua.

**`application-render.properties` / `application-enterprise.properties`**
- Se mantienen con `spring.profiles.include=prod`; **solo cambian las variables de entorno** (Fase G/I), no la estructura.

---

## FASE E — Neon: organización recomendada

### E.1 Proyecto y ramas

```
Proyecto Neon: kin-db (o kin-neon)
├── Rama MAIN  → PRODUCCIÓN  (nunca tocar fuera de deploys; protegida)
└── Rama DEV   → DESARROLLO  (rama efímera hija de main, para el equipo)
    └── (rama efímera TEST/CI, creada on-demand por pipeline)
```

- **Branches de Neon** = snapshots instantáneos del storage (costo ~0, creación en segundos). Ideales para dev/CI.
- **Regla:** dev nunca escribe en `main`. La rama `dev` se resincroniza desde `main` cuando se desea (o se crea una nueva rama hija).
- **Desarrollo local** conecta a la **rama dev**; CI/E2E usan una **rama efímera** que se destruye al terminar.

### E.2 Credenciales

- Cada rama de Neon tiene su propio rol/password (p. ej. `neondb_owner` + password por rama, o un rol dedicado `kin_app` con privilegios mínimos sobre `public`).
- **Dev vs Prod deben usar credenciales distintas.**
- Las credenciales de Neon **nunca van al repositorio**: se inyectan en `.env` (local) y en el dashboard de Render (secrets).

### E.3 SSL y conexión

- SSL **obligatorio** en Neon → la URL JDBC debe incluir `?sslmode=require`.
- Formato JDBC (directo):
  `jdbc:postgresql://ep-xxxx.us-east-2.aws.neon.tech/kin_platform?sslmode=require`
- Formato JDBC (pooled — recomendado para la app):
  `jdbc:postgresql://ep-xxxx-pooler.us-east-2.aws.neon.tech/kin_platform?sslmode=require`
- **Pooler:** PgBouncer en modo transacción (sufijo `-pooler`). Usar **siempre** el pooled endpoint para Spring Boot (evita desbordar conexiones del computo serverless).

### E.4 Cadena JDBC definitiva

```
JDBC_DATABASE_URL=jdbc:postgresql://<endpoint-pooler>.<region>.aws.neon.tech/<db>?sslmode=require
```

- Usada por `application-prod.properties:10` (override) y por `application-dev.yml`.
- Alternativa equivalente: `DB_HOST=<endpoint-pooler>` + `DB_PORT=5432` + `DB_NAME=<db>`.

### E.5 Operación (para el plan de ejecución, no se ejecuta)

- `SHOW server_version;` para confirmar la versión de la rama (crítico para Fase B).
- Configurar autosuspend del computo de dev a un valor corto (ahorro) y de prod a "siempre encendido" si el SLA lo exige.
- Monitor: la primera conexión tras suspensión tarda ~1 s; reflejarlo en timeouts de la app.

---

## FASE F — Estrategia de migración de datos (H2 → Neon)

### F.0 Premisa

- El `data/kindb.mv.db` local es un artefacto **desechable** (gitignored). La **recomendación fuerte** es **NO migrar datos locales**: la rama DEV de Neon nace vacía y Flyway la crea desde cero.
- Solo se migra si hay datos locales que deban conservarse.

### F.1 Flujo recomendado (sin datos → camino limpio)

1. Backup/borrado del H2 local (los scripts existentes `reset-dev-db.*` dejarán de usarse).
2. Primera conexión a Neon rama dev con Flyway → esquema V1..V11.
3. `DataInitializer` y `CategoryDataInitializer` siembran planes de pricing y categorías (igual que hoy en H2).

### F.2 Flujo de migración de datos (si fueran necesarios)

1. **Backup:** copiar `kin-backend/data/kindb.mv.db` a un archivo con fecha.
2. **Export:** con H2 (URL `jdbc:h2:file:./data/kindb`) ejecutar `SCRIPT TO 'kindb_export.sql'` (SQL DML/DDL de H2) **o** exportar por tablas a CSV (`CSVWRITE`).
3. **Transformación de tipos** (H2 → PG):
   - `VARCHAR/CLOB` → `TEXT`.
   - `BOOLEAN` → `BOOLEAN`.
   - `TIMESTAMP` → `TIMESTAMPTZ` (revisar zona horaria).
   - `UUID` → `UUID`.
   - `features` (JSON en texto) → cast `features::jsonb` (V8 ya lo hace en el esquema; el INSERT debe enviar JSON válido).
   - Columnas autoincrementales (`webhook_events.id BIGSERIAL`): al cargar con IDs explícitos, **reiniciar la secuencia** después: `SELECT setval('webhook_events_id_seq', (SELECT COALESCE(MAX(id),1) FROM webhook_events));`
4. **Orden de carga por FK:**
   `users → categories → pricing_plans → projects → chat_messages → viability_scores → project_context → interview_state → enterprise_project → enterprise_document → user_subscriptions → webhook_events`.
5. **Validación:** comparar conteos por tabla (`SELECT count(*)`) H2 vs Neon; validar `flyway_schema_history` (solo V1..V11, **sin** la tabla de datos importados metida en migraciones).

> **No se ejecuta nada en esta fase.** Es la Etapa 3 del Roadmap y solo aplica si se decide conservar datos locales.

---

## FASE G — Render: qué cambiará

### G.1 Cambios en `render.yaml` (futuros, no se ejecutan)

| Bloque | Hoy | Objetivo |
|---|---|---|
| `databases: kin-db` | Postgres gestionado de Render | **Se elimina** (la base pasa a ser Neon) o se conserva como fallback deshabilitado |
| Backend `SPRING_PROFILES_ACTIVE` | `render` | Se mantiene |
| `DATABASE_URL` (fromDatabase) | `connectionString` no-JDBC (no consumida) | **Se elimina** (deuda T8) |
| `DB_HOST/DB_PORT/DB_NAME` (fromDatabase) | apuntan a kin-db | **Se reemplazan** por el endpoint de Neon (rama main) |
| `DATABASE_USER/DATABASE_PASSWORD` (fromDatabase) | kin_admin | **Se reemplazan** por credenciales de la rama main de Neon |
| `JDBC_DATABASE_URL` | no existe | **Se añade** (opcional, más simple): cadena pooled de Neon con `?sslmode=require` |

### G.2 Comportamiento de la app (sin tocar código)

- `application-prod.properties:10` resuelve `JDBC_DATABASE_URL` si está definida; **esa sola variable** apuntará Render a Neon.
- `healthCheckPath=/api/v1/actuator/health` no cambia (el grupo readiness incluye `db`).
- CORS, JWT, cookies, rate-limit: sin cambios.

### G.3 Riesgo en Render

- Si se elimina `kin-db` y quedan `fromDatabase` huérfanos, Render falla el deploy → **eliminar el bloque de base y los `fromDatabase` de forma atómica**, y definir `JDBC_DATABASE_URL` en el mismo cambio.
- Validar en un **preview/service** de Render antes de tocar producción.

---

## FASE H — Docker: qué cambiará

| Elemento | Cambio necesario |
|---|---|
| `docker-compose.yml` (raíz) | Opcional. El servicio `postgres-db` (postgres:16-alpine) puede **mantenerse como fallback offline** o **eliminarse**. Si se elimina, quitar `depends_on`/redes relacionadas. `SPRING_PROFILES_ACTIVE=prod` se mantiene; `SPRING_DATASOURCE_URL/...` locales se sustituyen por `JDBC_DATABASE_URL` apuntando a Neon (solo para ensayos locales). |
| `kin-database/docker-compose.yml` | Idéntico al anterior (mismo criterio). |
| `kin-backend/Dockerfile` | **Sin cambios de base.** Solo cambia si la Etapa de Flyway añade el módulo `flyway-database-postgresql` (entra por el pom, no por el Dockerfile; el build Maven ya lo resuelve). |
| `kin-frontend/Dockerfile` | Sin cambios. |
| Healthcheck backend | `wget http://localhost:8080/api/v1/actuator/health` — sin cambios. |
| Variables | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (locales, actualmente apuntando al contenedor `postgres-db`) pasan a apuntar a Neon o se reemplazan por `JDBC_DATABASE_URL`. |

> Conclusión: **Docker no es obligatorio cambiar** si se mantiene como referencia local con PG 16; el cambio real es de variables. Si se opta por "todo Neon", el compose local se simplifica (solo backend+frontend, o se usa la rama dev de Neon directamente).

---

## FASE I — Variables de entorno

### I.1 Nuevas variables (aparecerán)

| Variable | Ámbito | Propósito |
|---|---|---|
| `JDBC_DATABASE_URL` | dev + Render | Cadena JDBC pooled de Neon con `?sslmode=require` (único punto de conexión) |
| `DB_HOST` | dev + Render | Host de Neon (rama correspondiente), p. ej. endpoint `-pooler` |
| `DB_PORT` | dev + Render | 5432 |
| `DB_NAME` | dev + Render | Nombre de la base en Neon |
| `DATABASE_USER` | dev + Render | Rol de la rama Neon |
| `DATABASE_PASSWORD` | dev + Render | Password de la rama Neon |
| `NEON_PROJECT_ID` / `NEON_BRANCH` | operativa (no de la app) | Referencia para scripts/CI |
| `NEON_API_KEY` | operativa (CI) | Solo si se automatizan ramas efímeras |

### I.2 Variables que desaparecerán

| Variable | Motivo |
|---|---|
| `POSTGRES_PASSWORD` (Docker local) | Ya no hay Postgres Docker obligatorio (o queda solo como fallback opcional). Si se elimina el compose de DB, se elimina. |
| `DATABASE_URL` (Render, formato `postgresql://`) | No consumida por la app (T8). Se elimina al quitar `kin-db`. |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (docker-compose local) | Reemplazadas por `JDBC_DATABASE_URL` o `DB_*`. |

### I.3 Variables que se mantienen

| Variable | Motivo |
|---|---|
| `JWT_SECRET` | Firma JWT (obligatoria) |
| `DEEPSEEK_API_KEY` | Proveedor IA principal |
| `OPENAI_API_KEY` | Fallback IA (opcional) |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | Pagos |
| `SPRING_PROFILES_ACTIVE` | Selección de perfil (`dev` local / `render` Render) |
| `PORT` | Puerto de Render |
| `ALLOWED_ORIGINS` | CORS |
| `KIN_REDIS_ENABLED` | Caché Redis opcional |
| `SPRINGDOTENV_DIRECTORY` | Carga de `.env` (default `../`) |

### I.4 Regla de seguridad

- **Nunca** versionar credenciales de Neon ni `JDBC_DATABASE_URL` con password.
- `.env` (gitignored) para dev; secrets del dashboard de Render para prod.

---

## FASE J — Skill propuesta: `DATABASE_ARCHITECT`

> Se propone crear esta Skill (contenido de referencia, **no se crea todavía**) para que cualquier agente del proyecto opere sobre el nuevo modelo de persistencia.

```markdown
# Skill: DATABASE_ARCHITECT

## Identidad
Actúa como arquitecto de base de datos de KIN.
Única base de datos permitida: PostgreSQL sobre Neon.

## Reglas obligatorias
1. NUNCA usar H2.
   - Prohibido crear/escribir perfiles, scripts, tests o config que referencie
     H2 (h2database, jdbc:h2:, kindb.mv.db, H2Dialect, spring.h2.console).
2. Toda persistencia es PostgreSQL Neon.
   - Dev → rama `dev` de Neon. Prod → rama `main` de Neon. Nunca la misma.
   - Conexión SIEMPRE con SSL (`?sslmode=require`) y, si es posible, pooled endpoint.
3. Verificar conexión ANTES de arrancar.
   - Ejecutar un healthcheck de BD (SELECT 1) contra la rama correspondiente
     antes de `spring-boot:run`. Si falla: NO arrancar.
4. Validar Flyway ANTES que el backend.
   - `flyway migrate` debe pasar contra la rama objetivo antes de iniciar la app.
   - ddl-auto SIEMPRE `none`. Flyway es la única fuente de verdad del esquema.
5. Si la base no responde: detener el proceso inmediatamente.
   - Abortar (exit no-zero), mostrar el error, y NO reintentar en bucle.
   - Máximo 1 reintento manual tras revisar la causa.
6. Nunca entrar en bucles.
   - Sin reintentos automáticos infinitos, sin arranques en cascada.
   - Si hay un segundo proceso vivo: FAIL-FAST y reutilizar (misma regla que
     el guard de arranque del backend).
7. Migraciones:
   - Nuevas migraciones = V12+, idempotentes, SQL de PostgreSQL.
   - Nunca editar una migración ya aplicada (checksum de Flyway).
8. Datos:
   - Respetar el orden de FK y las secuencias (setval) al importar.
   - validar con conteos y `flyway_schema_history`.
```

**Integración sugerida:** vincular la Skill a la tarea de arranque del backend (reutilizar `scripts/start-dev-backend.*`) para que la verificación de Neon sustituya a la antigua lógica de H2, manteniendo el guard FAIL-FAST.

---

## FASE K — Roadmap de migración

> Orden de ejecución recomendado. Cada etapa es reversible de forma independiente (Rollback). Duración: ~2–4 días-hombre.

### Etapa 1 — Decisión de versión de PostgreSQL (pre-requisito)
- **Objetivo:** fijar PG 16 (mantener Flyway 9.22.3) o PG 17/18 (planear upgrade de Flyway).
- **Acciones:** consultar `SHOW server_version` en el proyecto Neon existente; confirmar si puede crearse la rama en 16.
- **Archivos afectados:** ninguno (solo decisión/checklist).
- **Riesgo:** bajo · **Tiempo:** 0.5 h
- **Rollback:** N/A (no hay cambio).
- **Validación:** versión documentada y decisión firmada.

### Etapa 2 — Perfil dev + conexión a Neon rama DEV
- **Objetivo:** el backend arranca en local contra Neon DEV con Flyway V1..V11.
- **Acciones:** crear `application-dev.yml` (Fase D), setear `.env` con `JDBC_DATABASE_URL`/`DB_*` de la rama dev, actualizar script de arranque a perfil `dev`, `ddl-auto=none`.
- **Archivos afectados:** `application-dev.yml` (nuevo), `application.yml` (quitar H2/ddl-auto), `scripts/start-dev-backend.ps1/.sh`, `.env`, `AGENTS.md`.
- **Riesgo:** medio (migración de esquema y de tipos) · **Tiempo:** 0.5–1 día
- **Rollback:** restaurar `application.yml` H2 y `.env` (backup previo).
- **Validación:** `/actuator/health` UP con `db` ready; `flyway_schema_history` con V1..V11; smoke de login.

### Etapa 3 — Migración de datos (opcional, solo si hay datos locales que conservar)
- **Objetivo:** portar datos H2 → Neon DEV (si aplica).
- **Acciones:** Fase F (backup → export → transformar → importar → setval → validar).
- **Archivos afectados:** solo scripts temporales de migración (fuera del repo o en `scripts/`).
- **Riesgo:** alto (integridad de datos) · **Tiempo:** 0.5–1 día
- **Rollback:** Neon DEV es una rama: se recrea desde cero (flyway clean+migrate) o se descarta la rama.
- **Validación:** conteos por tabla y smoke E2E en dev.

### Etapa 4 — Flyway (si se decide PG 17/18)
- **Objetivo:** habilitar Flyway 11.x (+ módulo `flyway-database-postgresql`) o confirmar PG 16 sin cambios.
- **Acciones:** override `flyway.version` en `pom.xml`, añadir módulo PG, validar arranque; fallback a Spring Boot 3.4/3.5 si hay incompatibilidad de API.
- **Archivos afectados:** `pom.xml` (+ posible `application-*`).
- **Riesgo:** medio-alto · **Tiempo:** 0.5–1 día
- **Rollback:** revertir el override del pom (git) y restaurar la versión previa.
- **Validación:** `flyway migrate` limpio en Neon DEV + suite de tests.

### Etapa 5 — Tests sobre PostgreSQL
- **Objetivo:** eliminar H2 de los tests (Testcontainers PG 16, o rama efímera Neon).
- **Acciones:** `application-test.yml` → PG; actualizar test de contexto; corregir discrepancias de tipos (JSONB/UUID).
- **Archivos afectados:** `application-test.yml`, `pom.xml` (Testcontainers), tests.
- **Riesgo:** medio · **Tiempo:** 0.5–1 día
- **Rollback:** mantener el archivo `application-test.yml` anterior (git).
- **Validación:** 902 tests verdes contra PG.

### Etapa 6 — Eliminación definitiva de H2
- **Objetivo:** borrar H2 del classpath y de la configuración.
- **Acciones:** eliminar dependencia `com.h2database:h2`, `spring.h2.console`, dialecto H2, `data/kindb.mv.db` (backup previo), y adaptar `scripts/reset-dev-db.*` → reset de Neon.
- **Archivos afectados:** `pom.xml`, `application.yml`, `scripts/reset-dev-db.*`.
- **Riesgo:** medio (referencias residuales) · **Tiempo:** 2–4 h
- **Rollback:** revertir pom/config (git).
- **Validación:** búsqueda global `h2|H2|kindb|H2Dialect` = 0 resultados funcionales; arranque con perfil dev/test.

### Etapa 7 — Render → Neon (producción)
- **Objetivo:** producción apunta a Neon rama main.
- **Acciones:** en `render.yaml`: eliminar `kin-db` + `fromDatabase`, añadir `JDBC_DATABASE_URL` (o `DB_*`) de la rama main; validar en preview antes de producción.
- **Archivos afectados:** `render.yaml` (y secrets en dashboard de Render).
- **Riesgo:** alto (producción) · **Tiempo:** 0.5 día
- **Rollback:** re-deploy con el blueprint anterior (kin-db) mientras exista.
- **Validación:** `/actuator/health` readiness `db` UP en Render + smoke de flujo completo.

### Etapa 8 — Docker y documentación
- **Objetivo:** alinear Docker y docs con el nuevo modelo.
- **Acciones:** decidir mantener/eliminar `postgres-db` en compose; actualizar `README.md`, `CHANGELOG.md`, releases, `AGENTS.md`, `TECHNICAL_DEBT_REGISTER.md` (cerrar T8), crear la Skill `DATABASE_ARCHITECT`.
- **Archivos afectados:** `docker-compose.yml`, `kin-database/docker-compose.yml`, docs.
- **Riesgo:** bajo · **Tiempo:** 2–4 h
- **Rollback:** git revert de docs.
- **Validación:** `docker compose up --build` (si se conserva) y revisión de docs.

### Etapa 9 — Validación final y go-live
- **Objetivo:** aceptación final del modelo Neon-only.
- **Acciones:** checklist completo (§ Rollback y § Checklist), E2E Playwright, revisión de `flyway_schema_history`, monitor de conexiones del pooler.
- **Archivos afectados:** ninguno (validación).
- **Riesgo:** bajo · **Tiempo:** 0.5 día
- **Rollback:** procedimiento de la Etapa 7.
- **Validación:** checklist al 100 %.

---

## ROLLBACK (estrategia global)

1. **Cada etapa es reversible** con `git revert` de los archivos de esa etapa (pom, application-*, scripts, render.yaml, docker-compose) y restauración del `.env` desde backup.
2. **Neon:** las ramas son descartables → "rollback de base" = descartar la rama dev y recrearla desde main (flyway clean + migrate), o restaurar un *point-in-time* de Neon si existe.
3. **Producción:** mientras la Etapa 7 no se haga, `kin-db` de Render sigue existiendo → el rollback de prod es re-deployar el blueprint anterior (restaurando `fromDatabase`).
4. **Regla:** **backup de `data/kindb.mv.db` antes de la Etapa 6** y de cada `.env` antes de modificarlo. Los backups se guardan fuera del repo.
5. **Nunca se ejecuta `flyway clean` sobre la rama main/producción.**

---

## CHECKLIST de ejecución (a completar en cada etapa)

- [ ] Versión de PostgreSQL de la rama Neon confirmada (`SHOW server_version`).
- [ ] `JDBC_DATABASE_URL` (pooled + `sslmode=require`) probada con `psql`.
- [ ] Perfil correcto activado (`dev` local / `render` prod).
- [ ] `ddl-auto=none` en todos los perfiles; Flyway ON.
- [ ] `flyway_schema_history` = V1..V11 tras primer migrate en cada rama.
- [ ] `/actuator/health` readiness con `db` UP.
- [ ] Smoke: login, proyecto, chat, suscripción, webhook.
- [ ] Suite de tests verdes (902) sobre PostgreSQL.
- [ ] Búsqueda global sin referencias funcionales a H2.
- [ ] Secretos (Neon, JWT) fuera del repo; `.env` gitignored.
- [ ] Documentación y Skill `DATABASE_ARCHITECT` publicadas.
- [ ] Backup de `kindb.mv.db` y `.env` realizados antes de cambios destructivos.

---

## Archivos que cambiará la migración (referencia)

| Archivo | Acción prevista |
|---|---|
| `pom.xml` | quitar H2; posible upgrade Flyway + `flyway-database-postgresql`; posible Testcontainers |
| `application.yml` | quitar datasource H2, `spring.h2.console`, `ddl-auto: update` |
| `application-dev.yml` | **nuevo** — Neon rama dev |
| `application-test.yml` | **modificado** — PostgreSQL en vez de H2 |
| `application-prod.yml` | renombrado/reescrito desde `.properties` |
| `application-render.properties` / `application-enterprise.properties` | sin cambios estructurales |
| `scripts/start-dev-backend.*` y `scripts/reset-dev-db.*` | perfil `dev`, reset de Neon, guard FAIL-FAST con verificación de BD |
| `render.yaml` | eliminar `kin-db`/`fromDatabase`; `JDBC_DATABASE_URL` a Neon |
| `docker-compose.yml` (raíz y `kin-database/`) | eliminar/quitar `postgres-db` o apuntarlo a Neon |
| `.env` / `.env.example` | credenciales Neon; eliminar `POSTGRES_PASSWORD` |
| `AGENTS.md`, `README.md`, `CHANGELOG.md`, releases, `TECHNICAL_DEBT_REGISTER.md` | documentación Neon-only |
| Skill `DATABASE_ARCHITECT` | nuevo (Fase J) |

---

*Este documento es el único entregable de la Fase 2. No se modificó ningún archivo del repositorio, no se ejecutó Maven/Flyway, y no se tocó Render/Neon/Docker.*
