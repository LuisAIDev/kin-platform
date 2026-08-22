# ADR-025: Política de Fuentes de Conocimiento (gobernanza de la Matriz Maestra)

**Estado**: **Aprobado** (política de gobierno de la adquisición de conocimiento externo)
**Fecha**: 2026-08-20
**Autor**: KIN Architecture Team

> **Alcance**: este ADR congela la **Política de Fuentes** de la adquisición de conocimiento
> externo: cómo se incorpora, verifica, descarta y depreca una fuente; cómo se gobierna la
> allowlist; y qué estándares de configuración, código y calidad se exigen. La **Matriz Maestra
> de Conocimiento** (`kin-docs/MATRIZ_MAESTRA_CONOCIMIENTO.md`) es el inventario autoritativo de
> fuentes/categorías/países; este ADR es su gobernanza. No modifica la infraestructura de
> ADR-021/023/024; la extiende con reglas de proceso.

---

## Contexto

KIN ya integra 19 fuentes reales (4 globales, 14 colombianas, 1 española) que enriquecen 9 de 18
categorías de proyecto, con adaptadores SSRF-safe, caché Redis y selección por categoría
(ADR-021/023/024). A medida que crezca la matriz (más países, más categorías), la incorporación de
fuentes no puede ser ad-hoc: se necesita una **política explícita** que garantice que cada fuente
nueva cumpla los mismos estándares de seguridad, verificación y documentación, y que las
descartadas queden registradas con su motivo (evitando re-investigarlas).

---

## Decisión

### P1. Principios (no negociables)

1. **Java decide**: la adquisición, validación, selección y caché son deterministas en Java; el
   LLM nunca elige ni ejecuta consultas.
2. **Allowlist única**: ningún dominio fuera de `kin.knowledge.allowed-domains` es consultable;
   `SourceConnectionGuard` (SSRF fail-closed) no se modifica.
3. **Offline-first**: sin configuración no hay red; todo fallo degrada a `KnowledgeResult.empty()`.
4. **Verificación real obligatoria**: una fuente entra a la matriz solo con prueba de red real
   (test gated `KIN_TEST_REAL_NETWORK=true`) y un dato concreto documentado.
5. **Sin scraping**: API tabular real (JSON/XML/SDMX/SODA), nunca scraping de HTML.
6. **Producción apagada por defecto**: `KNOWLEDGE_EXTERNAL_ENABLED=false`; el perfil `staging` la
   activa solo en pruebas. La activación en producción exige revisión y aprobación explícita.

### P2. Ciclo de vida de una fuente

```
Investigar → Verificar (red real) → Integrar → Probar (gated) → Documentar → Activar (staging) → [Deprecar]
```

### P3. Checklist de incorporación (todo obligatorio)

Una fuente nueva **solo** se incorpora si cumple:

| # | Criterio | Evidencia requerida |
|---|---|---|
| 1 | API pública real (no scraping) con endpoint estable | URL + formato de respuesta |
| 2 | Permite consumo automatizado (términos/uso) | Documentar términos y licencia |
| 3 | Dominio incluido en la allowlist | `kin.knowledge.allowed-domains` |
| 4 | Metadata completa en `SourceConfig` | `id`, `name`, `base-url`, `format`, `query-param`, `fact-columns`/`landing-page`, `max-age`, `level`, `region`, `categories` |
| 5 | Decoder existente o nuevo en `KnowledgeHttpAutoConfiguration` | Si es nuevo: prueba unitaria del decoder |
| 6 | Consulta real produce hechos verificados | Test gated (`KIN_TEST_REAL_NETWORK=true`) con dato concreto en el reporte |
| 7 | Límites de uso documentados (rate limits, cuotas, throttling) | Documentar en la matriz |
| 8 | Actualización (diaria/mensual/anual/…) documentada | Documentar en la matriz |
| 9 | Licencia de los datos documentada (CC BY, CC BY-SA, …) | Documentar en la matriz |
| 10 | `mvn clean verify` en verde sin regresiones | Suite completa |

### P4. Criterios de descarte (motivo obligatorio)

Se descarta una fuente —y se registra el motivo en la matriz— si:

- No expone API tabular (solo HTML/SPA/descargas) → "scraping no permitido".
- La API requiere autenticación no pública / key propietaria no disponible → documentar la
  variable de entorno sin valor, marcar pendiente.
- La API devuelve `403 no row or column access to non-tabular tables` u otro estado que impida
  el consumo tabular.
- La API está deprecada o inestable (errores 404/204/500, datos no frescos).
- Los términos prohíben el uso comercial o automatizado.

### P5. Reuso y categorías múltiples

- Una fuente aplica a varias categorías vía `categories: [X, Y]` (p. ej. `superfinanciera-tasas`
  → `FINTECH, EMPRESARIAL`; `dane-exportaciones-cafe` → `AGROINDUSTRIA, COMERCIO, LOGISTICA`).
- **Reuso preferido sobre duplicación**: si una fuente existente ya cubre la necesidad de una
  categoría nueva, se amplía su lista de `categories` en vez de crear otra fuente.
- `categories` vacío = contexto general (aplica a todas las categorías).

### P6. Gobernanza de la allowlist

- La allowlist es la **única puerta de red**: cualquier dominio nuevo se agrega
  explícitamente a `allowed-domains` y se documenta en la matriz.
- `SourceConnectionGuard` (HTTPS obligatorio, rechazo de IPs privadas/loopback/link-local,
  DNS fail-closed, literales de IP rechazadas, redirecciones revalidadas) **no se modifica**.
- Cambios en la allowlist o en la guardia exigen ADR (contrato de seguridad).

### P7. Estándares de configuración (`SourceConfig`)

Cada entrada en `application-staging.yml` debe declarar: `id` único kebab-case, `name` legible,
`description` (opcional), `base-url` (con el filtro/`$limit` si aplica), `format` (`JSON_ITEMS`,
`WORLD_BANK_V2`, `SODA_JSON`, `INE_JSON`, `SDMX_JSON`), `query-param` (vacío si la URL es una
consulta fija), `fact-columns` (SODA), `landing-page` (URL de los hechos), `max-age`, `level`
(1/2), `region` (`GLOBAL`/código de país), `categories` (lista o vacío), `enabled`
(default `true`; `false` conserva la fuente configurada pero no se consulta) y `priority`
(default `0`; mayor valor se consulta antes, empate = orden de configuración).

### P8. Estándares de código y calidad

- Los decoders viven en `KnowledgeHttpAutoConfiguration` (infraestructura); el dominio
  (`kin.knowledge`) permanece 100 % POJO.
- Toda fuente nueva exige: test unitario del decoder (si aplica), test gated de red real que
  pruebe la fuente concreta, y suite completa verde.
- La caché (Redis) y el `SourceValidator` no cambian; si se detectan rechazos falsos
  (content-type/charset/subtipos `+json`), el fix se documenta y se añade test de regresión.

### P9. Deprecación y rollback

- Para desactivar una fuente en staging: eliminar su entrada de `application-staging.yml` (o
  quitar la categoría). Para desactivar todo: `KNOWLEDGE_EXTERNAL_ENABLED=false`.
- Rollback en producción: revertir la configuración del entorno; la allowlist vacía y el switch
  `false` son el estado seguro por defecto.
- La caché no se "contamina": ante config invalidada se puede `FLUSHALL` del dominio
  `kin:knowledge:*`.

### P10. Frescura y TTL por fuente (implementado)

- `SourceConfig.maxAge` define la **ventana de frescura** y el **TTL de caché sugerido** por
  fuente: datos diarios (TRM 12 h, ECB y calidad del aire 24 h), periódicos (tasas 7 d), mensuales
  (insumos, exportaciones, IPC 30 d) y estructurales/anuales (Finagro, MinCiencias 60 d).
- El adaptador estampa `maxAge` en cada candidato → `KnowledgeFact` lo conserva →
  `KnowledgeResult.effectiveTtl()` devuelve el **mínimo** de las fuentes del resultado (la fuente
  que cambia más seguido fija la expiración) → el `KnowledgeOrchestrator` guarda en Redis con ese
  TTL en vez de una ventana uniforme. Si ningún hecho declara `maxAge`, se usa la ventana de la
  solicitud (comportamiento previo).
- **Refuerzo a nivel repositorio**: `RedisKnowledgeRepository` aplica también el TTL dinámico al
  emitir el comando de expiración (`SETEX`): TTL efectivo = `min(ttl del llamador,
  effectiveTtl())` cuando los hechos declaran `maxAge`, y el `ttl` del llamador (o 24 h si es
  `null`) si no lo declaran. Así, aunque un llamador pase una ventana larga, la frescura de cada
  fuente se respeta de forma autónoma en la caché.

### P11. Modo sombra (Fase 1 del plan de activación)

- `kin.knowledge.shadow-enabled` (env `KNOWLEDGE_SHADOW_ENABLED`, default `false`). Con `true`, el
  `KnowledgeStage` **ejecuta el motor completo** (consulta real, validación, caché y métricas
  `kin.knowledge.adapter.*`) pero **suprime `knowledgeResult`** del contexto → el
  `EnrichmentStage` recibe vacío (`EnrichmentEngine` tolera `knowledge == null`) y **el usuario
  no ve ningún cambio**. Un log resumen por turno (`[shadow] ... sin propagacion`) permite
  revisar sin depender del endpoint de métricas.
- **Observabilidad (ADR-025, Fase 1)**: el timer de latencia expone **p50/p95/p99**
  (`kin_knowledge_adapter_latency_seconds{quantile=...}`) y se añadió el contador
  **`kin_knowledge_adapter_rate_limited_total`** para detectar respuestas **HTTP 429** (throttling
  anónimo por IP de datos.gov.co) + WARN por fuente.
- Activación en producción: **invisible para el usuario**; requiere aprobación explícita y un
  periodo de observación (métricas por fuente, latencia, caché hit/miss y 429). No sustituye a
  `KNOWLEDGE_EXTERNAL_ENABLED` (sombra consulta fuentes reales; la fase visible usa ese switch).

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Incorporación ad-hoc sin checklist** | Riesgo de fuentes sin verificar, scraping o duplicadas. Rechazada |
| **Registrar solo fuentes activas** | Sin el registro de descartadas se re-investiga lo ya resuelto. Rechazada: la matriz incluye descartadas con motivo |
| **Permitir scraping de portales oficiales** | Rompe la garantía de API tabular y es frágil. Rechazada |
| **Activar fuentes por defecto** | Viola el principio offline-first y la aprobación explícita. Rechazada |

---

## Consecuencias

### Positivas

- Proceso repetible y auditable para incorporar/descartar fuentes.
- La Matriz Maestra es la única referencia de cobertura (fuente, categoría, país, límites, licencia).
- Seguridad intacta: allowlist + SSRF sin cambios; producción apagada por defecto.
- Evita duplicación (reuso de categorías) y re-investigación (registro de descartes).

### Negativas

- Carga de proceso: cada fuente nueva requiere evidencia y tests gated (costo razonable).
- La política no acelera la aparición de fuentes oficiales tabulares donde no existen
  (categorías pendientes siguen pendientes hasta que haya API real).

---

## Regla que modifica

**ADR de gobernanza** sobre ADR-021/023/024: añade el proceso de ciclo de vida de fuentes, el
checklist de incorporación, los criterios de descarte y los estándares de configuración/calidad.
No modifica contratos congelados ni infraestructura.

## Cumplimiento

- Política aplicada retrospectivamente a las 19 fuentes integradas (todas cumplen el checklist:
  API real, allowlist, metadata, tests gated, licencia/límites/actualización documentados en la
  Matriz Maestra).
- Fuentes descartadas registradas con motivo (ver Matriz Maestra §5 y ADR-023 §D4).
- Suite completa backend **verde** (`./mvnw clean verify`).
- **Estado del ADR**: **Aprobado**.
