# Catálogo de categorías (SaaS-ready)

> Reemplazo del enum `ProjectCategory` por un catálogo persistente y administrable.
> Objetivo: agregar una nueva categoría/industria **como dato**, sin modificar Java ni React.

## Contexto

KIN se transforma en una plataforma SaaS de largo plazo donde constantemente aparecen nuevas
industrias. Las categorías de proyecto eran un enum Java (`ProjectCategory` con 4 valores) y el
frontend las duplicaba en arreglos hardcodeados (`CATEGORIES` en el formulario y `badgeColors.ts`).
Eso exigía un deploy por cada nueva categoría y duplicaba el dato en 3 capas.

## Diseño (SaaS-ready)

| Antes | Ahora |
|---|---|
| `enum ProjectCategory` (código) | Entidad `Category` → tabla `categories` (dato) |
| `Project.category` → `@Enumerated(EnumType.STRING)` columna `category` | `Project.category` → `@ManyToOne Category` (columna `category_id`) |
| `GET` inexistente | `GET /categories` (solo activas, ordenadas por `display_order`) |
| Frontend: `const CATEGORIES = [...]` + `badgeColors.ts` | Frontend: consume `GET /categories`; color desde `category.color` |

### Entidad

`com.kinplatform.project.Category` — tabla `categories`:

- `id` (UUID), `code` (único), `name`, `description`, `display_order`, `icon`, `color`,
  `active`, `created_at`, `updated_at`.

### Relación con Project

Se eligió **`@ManyToOne`** (relación JPA natural) sobre una columna string porque:

- Habilita integridad referencial (FK) y consultas derivadas (`findByUserIdAndCategory`).
- `@ManyToOne` es EAGER por defecto: evita `LazyInitializationException` al mapear DTOs.
- La columna `category_id` es **nullable** para no romper proyectos legacy (el backfill de la
  migración mapea `EMPRESARIAL/EMPRENDIMIENTO → Empresarial`, `SOCIAL → Impacto Social`;
  `PERSONAL` queda sin categoría hasta que el usuario la re-clasifique).

### Flujo del pipeline (IA)

El dominio (pipeline, `ProjectContext`) recibe la categoría como **String** (dimensión SECTOR).
`ChatOrchestratorServiceImpl` la envía como `project.getCategory().getName()` (nombre visible).
**La IA no se toca**: el refactor es 100 % capa de aplicación/infraestructura.

### Seed y migración

- **Producción (PostgreSQL)**: `kin-backend/src/main/resources/db/migration/V6__create_categories.sql`
  crea `categories`, siembra las 17 categorías con UUIDs fijos, agrega `category_id` a `projects`,
  hace backfill del enum legacy (soporta nombre `'EMPRESARIAL'` u ordinal `'1'`) y elimina la
  columna `category` (corrige la deuda del mapeo enum).
- **Dev (H2, `ddl-auto`, sin Flyway)**: `CategoryDataInitializer` (ApplicationRunner) siembra las
  mismas 17 categorías si la tabla está vacía (idempotente).
- `kin-database/init.sql` se actualizó al nuevo esquema (referencia para Docker).

### API

- `GET /categories` → `[{id, code, name, description, displayOrder, icon, color, active}]`
  (solo activas, orden por `displayOrder`).
- `POST /projects` / `PUT /projects/{id}` reciben `category` = **code** (ej. `"SALUD"`); el
  backend lo resuelve contra el catálogo (error 400 si no existe).
- `GET/PUT /projects` exponen `category` (code), `categoryName` y `categoryColor`.

## Frontend

- `services/projects.ts`: tipos `Category`, `Project.categoryName/categoryColor`, método
  `projectsService.getCategories()`.
- `app/dashboard/projects/new/page.tsx`: elimina `const CATEGORIES`; el `<select>` se llena con
  `GET /categories` (value=code, label=name).
- Badges: se eliminó `categoryBadge`/`categoryColors` de `utils/badgeColors.ts`; el badge usa
  `categoryColor` (hex) con estilo inline (`backgroundColor: color + "1A"`) y muestra
  `categoryName ?? category`. `statusBadge` se conserva.
- Sin listas hardcodeadas de categorías en el frontend.

## Deuda técnica resuelta

- Se eliminó el enum `ProjectCategory` y todo uso de `EnumType.ORDINAL` (el mapeo pasó a
  `category_id UUID` + FK). No quedan inconsistencias entre JPA y PostgreSQL.

## Tests (com.kinplatform.project)

- `CategoryServiceTest` (5): obtener activas, orden por `display_order`, exclusión de inactivas,
  `requireByCode` (ok + desconocido lanza), código nulo/vacío.
- `CategoryDataInitializerTest` (2): siembra 17 cuando la tabla está vacía; no siembra si ya hay datos.
- `ProjectServiceCategoryTest` (2): crear proyecto resuelve categoría por code (expone code/name/color);
  proyecto legacy sin categoría responde `null` sin errores.
- Ajustados: `ProjectContextSyncIntegrationTest`, `ChatOrchestratorServiceImplTest` (ahora usan la entidad `Category`).
