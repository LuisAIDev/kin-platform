# ADR-021: Adquisición de Conocimiento Externo — Adaptadores de Producción (HTTP seguro + Redis caché con contrato de clave determinista)

**Estado**: **Aprobado** (adaptadores de infraestructura implementados y verificados — Fase 13/14; red real + allowlist verificadas)
**Fecha**: 2026-08-20
**Autor**: KIN Architecture Team

> **Alcance**: este ADR congela el **contrato de clave de caché Redis** (formato, datos en la
> clave, TTL, invalidación y aislamiento entre usuarios/proyectos) y documenta los
> **adaptadores de producción** de la adquisición de conocimiento externo (ADR-014): cliente HTTP
> seguro SSRF-safe, guardia de conexión con allowlist, fuente controlada de prueba y switch
> operativo offline-first. **Describe la implementación real** (no diseño aspiracional): las
> clases citadas existen en `com.kinplatform.ai.knowledge.adapter` y el contrato aditivo en
> `KnowledgeRepository`. No modifica ningún contrato congelado: es una **enmienda aditiva** de
> ADR-014 (infraestructura) y del baseline. **La allowlist global por niveles (Nivel 1/Nivel 2) y
> la metadata por fuente se documentan en ADR-023**; el presente ADR cubre la infraestructura y
> el contrato de clave de caché.

---

## Contexto

ADR-014 congeló el **bounded context de dominio `kin.knowledge`**: motor canonizado
(`KnowledgeEngine`, fase `KNOWLEDGE`/`DOMAIN`/50), `KnowledgeGateway`, `SourceRegistry`,
`SourceValidator` (reglas deterministas de validación), puertos `KnowledgeSource`/
`KnowledgeRepository` y la integración aditiva al pipeline (`KnowledgeStage`). El dominio es
100 % POJO y **nunca toca la red**; los conectores reales son adaptadores de infraestructura.

En el milestone actual la infraestructura de adaptadores existía con **mocks/estructura
preparada**: `HttpKnowledgeSourceAdapter` aceptaba un cliente HTTP inyectado (stub) y los tests
usaban servidores locales deterministas. Para producción se necesita:

1. **Red real segura**: un cliente HTTP que resuelva DNS, hable TLS y respete una **allowlist de
   dominios** con mitigación SSRF (bloqueo de rangos privados/loopback/link-local, literales de
   IP, DNS-rebinding fail-closed), timeouts acotados, límite de tamaño de respuesta, control de
   content-type y retry acotado.
2. **Caché distribuida opcional**: `KnowledgeRepository` con TTL para no repetir llamadas a la
   red en cada turno. El contrato congelado de ADR-014
   (`save(KnowledgeResult, Duration)` + `find(KnowledgeQuery)`) **no transporta la consulta**, por
   lo que un adaptador no podía derivar la misma clave usada en `find` (hit/miss cruzado). Se
   necesita un **contrato de clave determinista** y un método aditivo que alinee escritura y
   lectura.
3. **Operabilidad**: switches por variable de entorno, default **offline-first** (sin red, sin
   Redis), fuente controlada de prueba determinista para dev/test/E2E, y métricas de adaptador.

La numeración **ADR-021** está referenciada de forma consistente en la implementación
(`RedisKnowledgeRepository`, `SecureHttpClient`, `SourceConnectionGuard`,
`KnowledgeHttpAutoConfiguration`, `KinKnowledgeProperties`, `KnowledgeRepository`,
`application.yml`, `.env.example` y los tests) desde antes de la redacción de este ADR; este
documento la formaliza.

---

## Decisión

Se implementa la infraestructura de adaptadores de producción en
`com.kinplatform.ai.knowledge.adapter` (ADR-021, enmienda aditiva de ADR-014) con las siguientes
decisiones congeladas.

### D1. Contrato de clave de caché Redis (determinista)

Implementado en `RedisKnowledgeRepository` (adaptador de `KnowledgeRepository`). Se activa
únicamente con `kin.cache.redis.enabled=true` (default `false`).

| Aspecto | Decisión (implementada) |
|---------|--------------------------|
| **Prefijo** | `kin:knowledge:` — aísla el dominio de caché de cualquier otra clave del cluster |
| **Clave por consulta** | `kin:knowledge:q:<hex>` con `seed = topic + "|" + String.join(",", keywords)` y `<hex> = Integer.toHexString(seed.hashCode())` |
| **Clave por contenido (deduplicación)** | `kin:knowledge:c:<hex>` con `seed = (sourceId + "|" + claim)` por hecho, ordenados (`sorted()`) y unidos por `"|"` |
| **Datos en la clave** | Solo `topic` + keywords (consulta normalizada) o la tupla `(sourceId, claim)` por hecho. **Nunca** identidad de usuario ni de proyecto |
| **Valor** | `KnowledgeResult` serializado en JSON (Jackson, con soporte `OffsetDateTime`; lectura tolerante a `isEmpty()`/`factCount()`) |
| **TTL** | Default `Duration.ofHours(24)`; sobrescribible por llamada (`save(query, result, ttl)`). En el flujo del `KnowledgeOrchestrator`, el TTL efectivo es el **mínimo `maxAge` de las fuentes que aportaron hechos** (`KnowledgeResult.effectiveTtl()`, ADR-025 — TTL por fuente) y, si ningún hecho declara `maxAge`, la ventana de la `KnowledgeRequest` (`timeWindow()`, default 365 días) |
| **Invalidación** | Expiración por TTL; borrado explícito al detectar resultado vacío o JSON corrupto en `find`; la escritura de la misma clave (mismo `topic`+keywords o mismo contenido) sobrescribe (no duplica) |
| **Aislamiento** | Sin datos de usuario/proyecto: solo hechos públicos validados. El prefijo de dominio + la ausencia de datos privados es la estrategia de aislamiento |
| **Determinismo** | Misma consulta → misma clave (verificado por test `clavesDeterministas_sinDuplicadosAlGuardarDosVeces` y `claves_deberianSerDeterministas`) |

**Contrato aditivo del dominio** (`KnowledgeRepository`, método `default`):
`save(KnowledgeQuery, KnowledgeResult, Duration)` delega por defecto en
`save(KnowledgeResult, Duration)` — los adaptadores existentes no cambian su comportamiento
(verificado por `KnowledgeRepositoryDefaultSaveTest`). `RedisKnowledgeRepository` lo sobreescribe
para escribir la clave por consulta (la misma que lee `find`) y, además, la clave por contenido.

**Hit/miss y métricas**: `find` incrementa `kin.knowledge.adapter.cache.hit`/`miss`; ante
resultado vacío o error de parseo borra la clave y devuelve `Optional.empty()` (nunca lanza).

### D2. Cliente HTTP real y seguro (SSRF-safe)

`SecureHttpClient` implementa el seam `HttpKnowledgeSourceAdapter.HttpClient` usando el
`java.net.http.HttpClient` del JDK (sin dependencias nuevas). Protecciones **en Java** (el LLM
nunca decide destinos):

- **Guardia de conexión** `SourceConnectionGuard` (fail-closed), validada en cada URL y en cada
  salto de redirección:
  - protocolo **HTTPS obligatorio** (HTTP solo para hosts loopback si `allow-loopback=true`,
    exclusivo dev/test);
  - el host debe ser **nombre de dominio** (las literales de IP se rechazan siempre);
  - el host debe coincidir con la **allowlist** (igual o subdominio); con allowlist vacía solo se
    admiten loopback explícitos (offline-first);
  - **resolución DNS**: todas las direcciones resueltas deben ser públicas (bloquea
    10/8, 172.16/12, 192.168/16, 127/8, 169.254/16, 0.0.0.0/8, CGNAT 100.64/10, 192.0.0/24,
    198.18/15, ≥224.0.0.0); DNS-rebinding mitigado porque la validación usa las direcciones
    resueltas;
  - redirecciones revalidadas con las mismas reglas (límite 3 por defecto).
- **Timeouts acotados**: `connect-timeout` 1s y `request-timeout` 3s por defecto (nunca bloquea el
  pipeline indefinidamente).
- **Límites**: `max-response-bytes` (256 KiB por defecto; exceso = rechazo controlado),
  `max-redirects` (3), content-type permitido cuando la allowlist de tipos no está vacía
  (default `application/json`).
- **Retry acotado**: `retries` (1 por defecto) con backoff fijo (250 ms), solo para fallos seguros
  de reejecutar (errores de conexión/5xx); **nunca** 4xx ni tras éxito.
- **Offline-first**: ningún error escapa; se devuelve una respuesta marcada como fallo o `null` y
  el pipeline degrada con gracia (verificado por tests de timeout/5xx/404/exceso de tamaño/
  content-type/redirección a host no permitido).

### D3. Switch operativo y allowlist (offline-first por defecto)

Configuración en `KinKnowledgeProperties` (`prefix = kin.knowledge`), expuesta por variables de
entorno (ver `.env.example`):

| Variable | Default | Significado |
|----------|---------|-------------|
| `KNOWLEDGE_EXTERNAL_ENABLED` | `false` | Master switch del adaptador HTTP real |
| `KNOWLEDGE_ALLOWED_DOMAINS` | (vacío) | Allowlist de dominios (`comma-separated`); vacío = offline-first |
| `KNOWLEDGE_HTTP_BASE_URL` | (vacío) | URL base de la fuente autorizada (requerida si se habilita) |
| `KNOWLEDGE_HTTP_SOURCE_ID` / `KNOWLEDGE_HTTP_SOURCE_NAME` | `external-http` / `External Knowledge` | Identidad de la fuente |
| `KNOWLEDGE_HTTP_CONTENT_TYPES` | `application/json` | Tipos de contenido permitidos |
| `KNOWLEDGE_TEST_SOURCE_ENABLED` | `false` | Fuente controlada determinista para dev/test/E2E (sin red) |

**Regla de registro** (`KnowledgeHttpAutoConfiguration`, `@ConditionalOnProperty`): el bean
`externalHttpKnowledgeSource` (un `HttpKnowledgeSourceAdapter` con `SecureHttpClient` + guard) se
registra **solo** con `external-enabled=true`; `controlledTestKnowledgeSource` solo con
`test-source.enabled=true`. Los beans de `KnowledgeSource` se auto-descubren en `SourceRegistry`
(KinConfig); con las fuentes deshabilitadas el registro queda vacío y KIN degrada offline.
`SourceValidator` (KinConfig) recibe la allowlist; con allowlist vacía usa el modo estricto
(`SourceValidator.strict()`) y **ningún candidato externo entra al análisis**.

**Allowlist de referencia para producción** (documentada; deshabilitada por defecto): la lista
de dominios autorizados se define por operador vía `KNOWLEDGE_ALLOWED_DOMAINS`. Para la
verificación de red real de este ADR se usó `httpbin.org` (endpoint público estable de prueba,
documentado explícitamente en el test `KnowledgeHttpRealNetworkTest`); en un despliegue de
producción se sustituye por los dominios de las fuentes contratadas/verificadas (p. ej.
`datos.gob.es`, `open.fda.gov`, `api.worldbank.org` como ejemplos de fuentes públicas OCP — la
lista final la decide el operador y queda versionada en la configuración del entorno). **La
allowlist global definitiva por niveles y países está en ADR-023**, junto con la metadata por
fuente (`level`/`region`/`category`).

**Contrato de la fuente autorizada** (decoder JSON por defecto, `jsonItemsDecoder`):
`{"items":[{"content","url","publishedAt"}]}`. Fallos de parseo → lista vacía (offline-first).

### D4. Fuente controlada de prueba

`ControlledTestKnowledgeSource` (dev/test/E2E, sin red): devuelve candidatos deterministas con
URLs https y metadata trazable (`http_status`, `source_type`, `category`); para que pasen la
validación el operador debe incluir su dominio en la allowlist (**Java decide la confiabilidad,
nunca el LLM**).

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Clave de caché con identidad de usuario/proyecto** (`kin:knowledge:u:<userId>:...`) | Mezcla datos privados con hechos públicos y rompe la reutilización global de hechos verificados; el modelo de aislamiento elegido es **no guardar datos privados** (solo hechos públicos). Rechazada |
| **Clave de caché por URL/sin hash** | Claves largas e impredecibles y posible fuga de datos en nombres de clave; el hash determinista de la consulta normalizada es compacto y sin datos sensibles. Rechazada |
| **Librería HTTP de terceros (Apache/OkHttp/RestTemplate)** | El JDK `HttpClient` cubre timeouts, redirects manuales y streams sin dependencias nuevas (restricción ADR-014). Rechazada la dependencia |
| **Guard de conexión solo por allowlist, sin validación DNS/IP** | No mitiga DNS-rebinding ni hosts que resuelven a rangos privados; la resolución con validación de todas las IPs es fail-closed. Rechazada |
| **Caché siempre activa** | Sin servidor Redis disponible no debe romper el arranque ni el health global; se excluye el health indicator de Redis y el adaptador es opt-in. Rechazado |

---

## Consecuencias

### Positivas

- **Red real segura y verificable**: se demuestra una llamada HTTP real a una fuente pública
  controlada (`https://httpbin.org/json`) a través de toda la cadena
  (`SecureHttpClient` → guard SSRF → `HttpKnowledgeSourceAdapter` → `SourceRegistry` →
  `SourceValidator` → `KnowledgeGateway` → `KnowledgeEngine`), con allowlist y sin que el LLM
  participe en ninguna decisión.
- **Caché distribuida opcional** con TTL, hit/miss, deduplicación por contenido e invalidación
  por expiración/borrado; probada contra **Redis real** (Testcontainers `redis:7-alpine`).
- **Offline-first garantizado**: allowlist vacía + `external-enabled=false` por defecto; fallos de
  red/5xx/timeout/SSRF degradan a `KnowledgeResult.empty()` sin romper el pipeline.
- **Operabilidad**: switches por variable de entorno, métricas Micrometer
  (`kin.knowledge.adapter.*`: requests, success, failure, timeout, rejected, cache.hit/miss,
  latency) y sin dependencias nuevas.
- **Coherencia documental**: la numeración ADR-021/022 ya estaba referenciada en el código; este
  ADR la formaliza sin renumerar.

### Negativas

- El default es offline-first: **sin configuración explícita de allowlist y master switch, la
  adquisición externa no aporta hechos** (por diseño).
- La exportación de trazas y la caché Redis añaden componentes opcionales al despliegue; el
  health agregado los excluye para no romper el arranque sin servidores.
- El test de red real (`KnowledgeHttpRealNetworkTest`) está **gated** por
  `KIN_TEST_REAL_NETWORK=true` (requiere salida a Internet), de modo que la suite por defecto
  permanece offline y determinista; debe ejecutarse explícitamente para validar la conectividad
  real.

---

## Regla que modifica

**ADR aditivo** sobre ADR-014 (infraestructura, no dominio): amplía `com.kinplatform.ai.knowledge.adapter`
con los adaptadores de producción (`SecureHttpClient`, `SourceConnectionGuard`,
`HttpKnowledgeSourceAdapter` real, `RedisKnowledgeRepository`, `RedisCacheConfig`,
`KinKnowledgeProperties`, `KnowledgeAdapterMetrics`, `KnowledgeHttpAutoConfiguration`,
`ControlledTestKnowledgeSource`) y amplía aditivamente el puerto `KnowledgeRepository` con el
método `default save(KnowledgeQuery, KnowledgeResult, Duration)`. **No modifica** contratos
congelados del baseline: `kin.knowledge` sigue 100 % POJO, `KnowledgeEngine`/`KnowledgeGateway`/
`SourceValidator`/`KnowledgeStage` intactos, y la frontera ADR-012 intacta.

## Cumplimiento

- **Etapa 1 (este ADR)**: documentación del diseño e implementación real. Sin cambios de contrato
  congelado.
- **Verificación realizada**:
  - Tests de integración del cliente HTTP real sobre servidor local determinista (SSRF, timeout,
    5xx, 404, retry, límite de tamaño, content-type, redirecciones): `SecureHttpClientTest`
    (11 tests) y `SourceConnectionGuardTest` (11 tests), **verdes**.
  - Trazabilidad end-to-end local (fuente controlada → pipeline): `KnowledgeHttpEndToEndTest`
    (3 tests), **verdes**.
  - Red real contra `https://httpbin.org/json` con allowlist `httpbin.org`:
    `KnowledgeHttpRealNetworkTest` (4 tests, `KIN_TEST_REAL_NETWORK=true`), **4/4 verdes** —
    incluye producción de hechos reales, rechazo por allowlist vacía, rechazo por dominio fuera
    de allowlist y degradación ante 5xx real.
  - Caché Redis real (Testcontainers): `RedisKnowledgeRepositoryTest` (5 tests) + null-safety
    (3 tests) + contrato aditivo del dominio (1 test), **verdes**.
  - Cableado condicional offline-first: `KnowledgeHttpAutoConfigurationTest` (6 tests), **verdes**.
  - Configuración real (application.yml): `TracingConfigurationTest` (3 tests), **verdes**.
  - Suite completa backend **verde** (`./mvnw clean verify`, BUILD SUCCESS) tras este ADR.
- **Estado del ADR**: **Aprobado** — decisiones congeladas. Cualquier cambio futuro en el
  contrato de clave, en la guardia SSRF o en los switches operativos exige una ADR propia.
