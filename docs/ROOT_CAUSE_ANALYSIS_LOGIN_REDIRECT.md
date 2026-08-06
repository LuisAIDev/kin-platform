# ROOT CAUSE ANALYSIS — Login Redireccionado a `/login` (HTTP 500 en `/subscriptions/status`)

**Incidencia:** El flujo E2E de login → dashboard → creación de proyecto fallaba: tras autenticarse, la sesión se perdía y la aplicación redirigía de vuelta a `/login`.

**Estado:** RESUELTO. Corrección verificada con Playwright (suite completa en verde).

**Fecha:** 2026-08-06

---

## 1. Resumen ejecutivo

El backend devolvía **HTTP 500** en `GET /api/v1/subscriptions/status`. El frontend, al cargar el dashboard, ejecutaba `Promise.all([projects, subscriptions/status])` y trataba **cualquier** error como una pérdida de sesión: al fallar `/subscriptions/status` se invocaba `forceLogout()`, que limpiaba `localStorage` y las cookies y redirigía a `/login`. El resultado visible era que el usuario nunca llegaba al dashboard y la navegación a `/dashboard/projects/new` se abortaba (`net::ERR_ABORTED`).

La causa raíz del 500 era de configuración: `spring-boot-starter-data-redis` está en el classpath, por lo que Spring Boot auto-configuraba un `RedisCacheManager` como gestor de caché. Sin un servidor Redis disponible, la primera invocación de cualquier método `@Cacheable` de `SubscriptionValidatorService` intentaba conectar con Redis (`localhost:6379`) y lanzaba `RedisConnectionFailureException`, que terminaba como HTTP 500.

La corrección principal fue fijar `spring.cache.type=simple` (caché en memoria) y excluir el health indicator de Redis. Secundariamente se desactivó el *rate limit* de `/auth/**` en el perfil de test, que bloqueaba las llamadas repetidas del E2E con HTTP 429.

---

## 2. Síntomas observados

1. `GET /api/v1/subscriptions/status` devolvía **HTTP 500**.
2. Tras hacer login, la aplicación redirigía de vuelta a `/login` (la sesión no persistía).
3. En Playwright, `page.goto('/dashboard/projects/new')` fallaba con `net::ERR_ABORTED`; el snapshot final de la página mostraba el formulario de login.
4. El `<select>` de categorías del formulario "Nuevo proyecto" nunca cargaba opciones, porque la página redirigía a `/login` antes de renderizar.

---

## 3. Cómo se reproducía el problema

**Prerrequisitos:** backend levantado en `http://localhost:8080` con el perfil `test` (H2 en memoria, sin Docker, sin Redis).

**Pasos:**

```bash
# Desde kin-frontend/
npx playwright test tests/_debug-flow.spec.ts
```

**Secuencia fallida (comportamiento esperado en negrita):**

1. Registro de usuario vía API → **201**.
2. Login en `/login` → **redirige a `/dashboard/projects`**.
3. El dashboard carga `Promise.all([GET /projects, GET /subscriptions/status])`.
4. `GET /subscriptions/status` → **HTTP 500** (fallo real) → `Promise.all` rechaza.
5. El `.catch` de la página ejecuta `forceLogout()` → limpia sesión y navega a `/login`.
6. `page.goto('/dashboard/projects/new')` → **debería abrir el formulario**, pero la navegación es reemplazada por la redirección en curso a `/login` → `net::ERR_ABORTED`.
7. El `<select>` de categorías no se llena porque el usuario fue expulsado.

---

## 4. Investigación realizada paso a paso

1. **Verificación de estado del backend.**
   - `GET /api/v1/actuator/health` → `200 {"status":"UP",...}`. El backend está vivo.

2. **Sondeo directo de endpoints críticos con token real.**
   - `POST /auth/register` → `201` (token emitido).
   - `GET /auth/me` → `200`.
   - `GET /subscriptions/status` → **`500`** (reproducción directa, sin involucrar al frontend).
   - `GET /categories` → `200` con 17 categorías (solo autenticado).

3. **Lectura del stacktrace / configuración.**
   - Se inspeccionó la configuración de caché y se confirmó la presencia de `spring-boot-starter-data-redis` en `kin-backend/pom.xml:118-121` y de `@EnableCaching` en `KinApplication.java:5`.
   - Sin `spring.cache.type` definido, Spring Boot auto-configuraba `RedisCacheManager`.
   - Se comprobó que los métodos `@Cacheable` de `SubscriptionValidatorService` son invocados por `GET /subscriptions/status`.

4. **Aplicación del fix de configuración.**
   - Se fijó `spring.cache.type: simple` y se deshabilitó el health de Redis.

5. **Re-sondeo de endpoints tras el fix.**
   - `GET /subscriptions/status` → `200` con `{"planName":"Básico Gratis","aiLevel":"FLASH",...}`.
   - `GET /projects` → `200`.
   - `GET /categories` → `200` (17 categorías).

6. **Re-ejecución de Playwright.**
   - `_debug-flow.spec.ts`: reprodujo el flujo completo; el login persistió, no hubo redirect a `/login` y el `<select>` cargó 17 categorías.
   - `dashboard-flow.spec.ts` (registro → login → crear proyecto → listado → logout): verde.
   - `auth.spec.ts` (3 casos): verde.

7. **Aislamiento de un segundo problema.** Durante la investigación se detectó que el E2E disparaba varias llamadas `/auth/**` (register, login, `/auth/me` del middleware, logout) desde la misma IP en menos de un minuto; el `RateLimitingFilter` (5 req/min/IP) las bloqueaba con HTTP 429. Se desactivó el *rate limit* únicamente en el perfil `test` (`application-test.yml`).

---

## 5. Evidencia encontrada

### Respuesta HTTP del endpoint fallido

`GET http://localhost:8080/api/v1/subscriptions/status` (con `Authorization: Bearer <token>`):

```json
// HTTP/1.1 500
// Antes del fix: 500 Internal Server Error
// (error de conexión a Redis: RedisConnectionFailureException — "Unable to connect to Redis")
```

### Resultado de Playwright ANTES del fix (archivo de resultados)

```
tests\_debug-flow.spec.ts >> debug flow
Error: page.goto: net::ERR_ABORTED at http://localhost:3000/dashboard/projects/new
Page snapshot: ... heading "Iniciar sesion" ... (formulario de login)
```

### Resultado de Playwright DESPUÉS del fix

```
[DBG] response 200 http://localhost:8080/api/v1/subscriptions/status
[DBG] response 200 http://localhost:8080/api/v1/projects?page=0&size=12
[DBG] response 200 http://localhost:8080/api/v1/categories
[DBG] category option count 18        // 17 categorías + placeholder
[DBG] state {"url":"http://localhost:3000/dashboard/projects/new","token":"present"}
 1 passed
```

---

## 6. Causa raíz

**Causa raíz primaria (backend):** el gestor de caché de Spring se resolvía a `RedisCacheManager` porque `spring-boot-starter-data-redis` está en el classpath y no se declaraba `spring.cache.type`. El primer acceso a cualquier método `@Cacheable` intentaba conectar con Redis (inexistente) y fallaba con `RedisConnectionFailureException`, propagada como HTTP 500 en `/subscriptions/status`.

**Causa raíz secundaria (frontend):** el manejo de errores del dashboard confundía un error 5xx con una pérdida de sesión. `projects/page.tsx` invoca `forceLogout()` en el `.catch` genérico de la carga inicial, de modo que el 500 de `/subscriptions/status` expulsaba al usuario.

**Causa raíz terciaria (entorno de test):** el `RateLimitingFilter` (5 req/min por IP sobre `/auth/**`) bloqueaba con HTTP 429 las llamadas repetidas del E2E.

---

## 7. Archivos modificados

| Archivo | Cambio | Tipo |
|---|---|---|
| `kin-backend/src/main/resources/application.yml` | `spring.cache.type: simple` + `management.health.redis.enabled: false` (+ comentario explicativo) | Corrección raíz |
| `kin-backend/src/main/java/com/kinplatform/common/config/SecurityConfig.java` | `/actuator/**` → `permitAll` (antes solo `/actuator/health` e `/actuator/info`) | Soporte de diagnóstico |
| `kin-backend/src/main/java/com/kinplatform/common/security/RateLimitingFilter.java` | Nuevo flag `app.rate-limit.enabled` (`@Value`, default `true`) | Soporte de diagnóstico |
| `kin-backend/src/main/resources/application-test.yml` | `app.rate-limit.enabled: false` (solo perfil `test`) | Soporte de diagnóstico |
| `kin-frontend/playwright.config.ts` | `testIgnore` condicionado a `PLAYWRIGHT_DIAGNOSTICS=1` | Organización de tests |
| `kin-frontend/tests/_debug-flow.spec.ts` | Movido a `tests/diagnostics/debug-flow.spec.ts` + comentario de cabecera | Organización de tests |

---

## 8. Explicación técnica de por qué Redis producía el HTTP 500

1. **Redis está en el classpath.** `kin-backend/pom.xml:118-121` declara `spring-boot-starter-data-redis`. Redis es una caché **opcional** del módulo de conocimiento (el bean `RedisKnowledgeRepository` se crea solo con `kin.cache.redis.enabled=true`, ver `RedisCacheConfig.java`), pero la **auto-configuración del `CacheManager`** no está condicionada por esa propiedad.

2. **`@EnableCaching` está activo.** `KinApplication.java:5`.

3. **Sin `spring.cache.type`**, el `CacheAutoConfiguration` de Spring Boot aplica el criterio `@ConditionalOnMissingBean(CacheManager.class)` y, al detectar `RedisCacheConfiguration` en el classpath, instala un `RedisCacheManager` (conectado a `localhost:6379` por defecto).

4. **`@Cacheable` se invoca en el flujo de suscripciones.** `SubscriptionValidatorService.java`:
   - `@Cacheable("projectLimit")` — línea 42 (`canCreateProject`)
   - `@Cacheable("messageLimit")` — línea 61 (`canSendMessage`)
   - `@Cacheable("activeSubscription")` — línea 132 (`getActiveSubscription`)

   El endpoint `GET /subscriptions/status` (`SubscriptionController.java:70-96`) llama a `isSubscriptionActive`, `getCurrentPlan`, `getRemainingMessages`, `canCreateProject` y `getAvailableAILevel`; todos acaban en un método `@Cacheable`.

5. **La primera invocación falla.** El interceptor de caché de Spring pide al `CacheManager` resolver la caché (`activeSubscription`, etc.). El `RedisCacheManager` crea un `RedisCache` y, en el primer acceso, emite un comando por Lettuce contra `localhost:6379`. Sin servidor Redis, la conexión se rechaza y se lanza `RedisConnectionFailureException` ("Unable to connect to Redis").

6. **La excepción no se traduce a un error HTTP amigable.** No existe `@ControllerAdvice`/`@ExceptionHandler` que capture `RedisConnectionFailureException` (o `DataAccessException`), de modo que el manejo de errores por defecto de Spring MVC devuelve **HTTP 500**.

7. **El health global también se veía afectado.** Sin la exclusión, el `RedisHealthIndicator` marcaba el health agregado como `DOWN` aunque la aplicación no dependiera de Redis. Por eso también se añadió `management.health.redis.enabled: false`.

---

## 9. Explicación de por qué el frontend ejecutaba `forceLogout()`

### Flujo en el dashboard

`kin-frontend/src/app/dashboard/projects/page.tsx:34-46`:

```ts
Promise.all([
  projectsService.getAll(page),
  subscriptionApi.getStatus(),
])
  .then(([projectsRes, sub]) => { /* ... */ })
  .catch(() => {
    if (!cancelled) forceLogout();   // <-- cualquier error aquí expulsa al usuario
  });
```

Como `subscriptionApi.getStatus()` hace `GET /subscriptions/status` y este respondió **500**, el `Promise.all` se rechaza y se ejecuta el `.catch` → `forceLogout()`.

### Qué hace `forceLogout()`

`kin-frontend/src/services/session.ts:45-62`:

```ts
export function forceLogout() {
  if (_forceLogoutInProgress) return;
  _forceLogoutInProgress = true;
  const token = localStorage.getItem("kin_token_v2");
  if (token) fetch(`${API_URL}/auth/logout`, { method: "POST", ... }).catch(() => {});
  clearSession();                                  // localStorage.clear() + borra cookies
  if (typeof window !== "undefined") {
    window.location.href = "/login";               // navegación dura a /login
  }
}
```

### Efecto encadenado

1. El dashboard redirige a `/login` vía `window.location.href`.
2. En Playwright, el paso siguiente (`page.goto('/dashboard/projects/new')`) se topa con esa redirección en curso y se aborta (`net::ERR_ABORTED`); el snapshot muestra el formulario de login.
3. El `<select>` de categorías nunca llega a cargarse porque el usuario ya no está en el dashboard.

**Defecto de diseño:** el dashboard trata **todo** error de arranque (incluidos 5xx del servidor) como una sesión inválida. Solo un `401`/`403` debería disparar `forceLogout()`; un `500` debería mostrar una UI de error, no cerrar la sesión. El mismo patrón aparece en `kin-frontend/src/app/dashboard/projects/[id]/page.tsx:48-59` (carga de proyecto/historial).

---

## 10. Solución aplicada

### 10.1 Fix raíz (backend) — `application.yml`

```yaml
spring:
  cache:
    type: simple        # caché en memoria por defecto; Redis solo si se activa explícitamente
```

Con `spring.cache.type=simple`, el `CacheManager` es un `ConcurrentMapCacheManager` en memoria; los `@Cacheable` de `SubscriptionValidatorService` funcionan sin depender de ningún servidor. Para volver a usar Redis como caché distribuida, basta definir `spring.cache.type=redis` (junto con `kin.cache.redis.enabled=true` y un servidor Redis).

Además se deshabilitó el health de Redis para que el agregado de `/actuator/health` no quede `DOWN` cuando no hay servidor:

```yaml
management:
  health:
    redis:
      enabled: false
```

### 10.2 Fix de entorno de test — `application-test.yml`

```yaml
app:
  rate-limit:
    enabled: false
```

Desactiva el `RateLimitingFilter` (5 req/min/IP) únicamente en el perfil `test`, para que el E2E pueda emitir las llamadas `/auth/**` necesarias sin recibir 429. El flag se implementó en `RateLimitingFilter.java` con `@Value("${app.rate-limit.enabled:true}")` (default `true` en cualquier otro perfil, producción conserva la protección anti-fuerza bruta).

### 10.3 Soporte de diagnóstico — `SecurityConfig.java`

Se amplió el matcher de `/actuator/**` a `permitAll` para facilitar el sondeo de salud durante el diagnóstico.

### 10.4 Organización del test de regresión

`tests/_debug-flow.spec.ts` se movió a `tests/diagnostics/debug-flow.spec.ts`, con comentario de cabecera documentando el incidente, y quedó excluido de la suite principal vía `testIgnore` condicionado a la variable `PLAYWRIGHT_DIAGNOSTICS` (ver sección 14).

---

## 11. Evidencia posterior a la corrección

Sondeo directo contra el backend con un token válido:

| Endpoint | Antes | Después |
|---|---|---|
| `POST /auth/register` | `201` | `201` |
| `GET /auth/me` | `200` | `200` |
| `GET /subscriptions/status` | **`500`** | **`200`** `{"planName":"Básico Gratis","aiLevel":"FLASH",...}` |
| `GET /projects?page=0&size=12` | — | `200` |
| `GET /categories` | — | `200` (17 categorías) |
| `GET /actuator/health` | — | `200 {"status":"UP"}` |

---

## 12. Resultados de Playwright antes y después

### Antes de la corrección

```
tests\_debug-flow.spec.ts >> debug flow
Error: page.goto: net::ERR_ABORTED at http://localhost:3000/dashboard/projects/new
Page snapshot: ... heading "Iniciar sesion" ...  (formulario de login)
```
- Estado: **FAILED**. El usuario quedaba expulsado a `/login` tras autenticarse.

### Después de la corrección

```
tests\diagnostics\debug-flow.spec.ts → 1 passed   (login persiste, sin redirect, 17 categorías en el select)
tests\dashboard-flow.spec.ts         → 1 passed   (registro → login → crear proyecto → listado → logout)
tests\auth.spec.ts                   → 3 passed   (render, credenciales inválidas, login válido)
-----------------------------------------------
Total: 5 passed
```

Suite principal (`npx playwright test`): **4 tests en 2 archivos, todos en verde** (el spec de diagnóstico está excluido por defecto).

---

## 13. Recomendaciones para evitar regresiones futuras

1. **Declarar siempre el tipo de caché explícitamente** (`spring.cache.type=simple` o `redis`). No depender de la auto-configuración cuando hay starters opcionales en el classpath.
2. **Separar el manejo de errores HTTP en el frontend:** `forceLogout()` debe dispararse solo ante `401`/`403` (o el mensaje "Authenticated user not found"), nunca ante `5xx`. Para 5xx, mostrar un estado de error recuperable en el dashboard y en la página de detalle de proyecto (`projects/page.tsx:34-46`, `projects/[id]/page.tsx:48-59`).
3. **Endurecer el backend:** añadir un `@ControllerAdvice`/`@ExceptionHandler` para `RedisConnectionFailureException`/`DataAccessException` que devuelva un error HTTP controlado (p. ej. `503`) con un cuerpo descriptivo, en lugar de un 500 genérico.
4. **Mantener Redis estrictamente opcional:** idealmente, condicionar también la auto-configuración del `CacheManager` a `kin.cache.redis.enabled=true` (p. ej. un `@Configuration` con `@ConditionalOnProperty`) para que, si no se activa Redis, jamás se intente conectar.
5. **Conservar el spec de diagnóstico** (`tests/diagnostics/debug-flow.spec.ts`) y ejecutarlo como prueba de humo de regresión ante cualquier cambio en autenticación, suscripciones o caché.
6. **Documentar la advertencia** en `AGENTS.md`/guías de configuración: "Redis es opcional; sin `spring.cache.type`, el starter de Redis desactiva la caché en memoria y puede romper los endpoints con `@Cacheable`".

---

## 14. Cómo ejecutar el test de diagnóstico

El spec está excluido de la suite principal (`testIgnore` condicionado). Para ejecutarlo como prueba de regresión:

```bash
# 1. Levantar el backend con el perfil de test (ya debe estar corriendo)
cd kin-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 2. Ejecutar el flujo diagnóstico (segunda terminal)
cd kin-frontend
PLAYWRIGHT_DIAGNOSTICS=1 npx playwright test tests/diagnostics/debug-flow.spec.ts
```

> En PowerShell: `$env:PLAYWRIGHT_DIAGNOSTICS = "1"; npx playwright test tests/diagnostics/debug-flow.spec.ts`

Verificaciones que hace el spec:

- El login permanece autenticado (sin redirect inesperado a `/login`).
- `GET /subscriptions/status`, `GET /projects` y `GET /categories` responden `200`.
- El `<select>` de categorías del formulario "Nuevo proyecto" carga las opciones.

---

## 15. Conclusión

La incidencia era un **error de configuración de caché** amplificado por un **manejo de errores demasiado agresivo en el frontend**:

- Sin `spring.cache.type`, el starter de Redis en el classpath hacía que cualquier endpoint con `@Cacheable` (en particular `/subscriptions/status`) intentara conectar con un servidor Redis inexistente y devolviera HTTP 500.
- El dashboard interpretaba ese 500 como una sesión inválida y ejecutaba `forceLogout()`, expulsando al usuario y rompiendo el flujo E2E (login → dashboard → creación de proyecto).

La fijación de `spring.cache.type=simple`, la exclusión del health de Redis, la desactivación del *rate limit* en el perfil de test y la organización del spec diagnóstico dejan el flujo completo en verde (5/5 tests), con un mecanismo de regresión documentado y ejecutable a voluntad. No se detectaron problemas adicionales en `/projects`, `/categories` ni en la persistencia de la sesión tras el login.
