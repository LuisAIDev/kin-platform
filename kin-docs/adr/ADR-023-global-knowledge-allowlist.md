# ADR-023: Allowlist global de fuentes de conocimiento externo (dos niveles) con metadata extensible

**Estado**: **Aprobado** (diseño e implementación de metadata + verificación de red real por país)
**Fecha**: 2026-08-20
**Autor**: KIN Architecture Team

> **Alcance**: este ADR define y congela el **diseño de la allowlist global por niveles**
> (Nivel 1 global + Nivel 2 nacional) de la adquisición de conocimiento externo (ADR-021), con
> **metadata por fuente** (nivel, región, categoría) para permitir en el futuro la selección
> automática por país del proyecto. Describe **fuentes verificadas con red real** y fuentes
> investigadas pero **pendientes**. No relaja ninguna protección SSRF (ADR-021) y mantiene
> `KNOWLEDGE_EXTERNAL_ENABLED=false` por defecto en producción. **El mapeo categoría de proyecto
> → fuentes (selección determinista) se documenta en ADR-024.**

---

## Contexto

KIN es una plataforma SaaS global. La capacidad de adquisición de conocimiento externo
(ADR-014/ADR-021) ya es segura (allowlist SSRF-safe, caché, offline-first), pero la lista de
dominios de las pruebas era un ejemplo técnico. Se necesita la **lista real** estructurada en dos
niveles:

- **Nivel 1 — cobertura global**: fuentes multilaterales que sirven a cualquier país (Banco
  Mundial, ECB, y —pendientes— IMF, OCDE, Naciones Unidas, CEPAL).
- **Nivel 2 — cobertura nacional**: la fuente oficial de estadísticas/economía de los mercados
  donde KIN tiene o espera usuarios (Colombia, España, México, Argentina, Chile, Perú, EE.UU.).

Además, la lista no debe ser "plana": cada fuente necesita **metadata** (nivel, región, categoría)
para que agregar un país nuevo sea una entrada de configuración y para que, en el futuro, el
sistema seleccione qué fuentes consultar según el país del proyecto.

---

## Decisión

### D1. Modelo de dos niveles

| Nivel | Criterio | Fuentes |
|-------|----------|---------|
| **1 — Global** | Cobertura mínima para cualquier usuario, sin importar su país | Banco Mundial, ECB (verificadas); IMF, OCDE, UN, CEPAL (pendientes) |
| **2 — Nacional** | Fuente oficial de cada mercado prioritario | Colombia, España, EE.UU. (verificadas); México, Argentina, Chile, Perú (pendientes) |

La **regla de selección futura** (no implementada ahora, solo habilitada por el diseño):
para un proyecto con país `CC`, se consultan las fuentes con `region == CC` (Nivel 2) más las de
`region == GLOBAL` o `level == 1` que cubran `CC` (Nivel 1). Un país sin Nivel 2 recurre
naturalmente a Nivel 1 (**respaldo global**).

### D2. Metadata por fuente (cambio mínimo, aditivo)

`KinKnowledgeProperties.SourceConfig` (ADR-021) se amplía con tres campos opcionales:

| Campo | Tipo | Valores | Significado |
|-------|------|---------|-------------|
| `level` | `int` | `1` / `2` | Nivel de la allowlist global |
| `region` | `String` | `GLOBAL`, `COL`, `ESP`, `MEX`, … | Región/país que sirve la fuente |
| `category` | `String` | `ECONOMY`, `TRADE`, `DEMOGRAPHY`, `REGULATION`, … | Categoría del dato |

Este es el **único cambio mínimo necesario** sobre la infraestructura de ADR-021:
`SourceRegistry` y `SourceValidator` (dominio) **no cambian** — operan sobre candidatos, no sobre
configuración. La selección por país se implementaría en el futuro en el cableado
(`KnowledgeStage`/`KnowledgeGateway`), leyendo el país del proyecto y filtrando por `region`, sin
tocar el dominio ni la guardia SSRF.

### D3. Fuentes verificadas con red real (2026-08-20)

| # | Fuente | Dominio | API/Formato | Estado | Límites |
|---|--------|---------|-------------|--------|---------|
| 1 | Banco Mundial | `api.worldbank.org` | `WORLD_BANK_V2` (JSON) | ✅ Verificada | Sin key; "volumen razonable"; datasets CC BY 4.0; idioma EN |
| 2 | BCE (ECB) | `data-api.ecb.europa.eu` | `SDMX_JSON` | ✅ Verificada | Pública, sin key; SDMX 2.1; idioma EN/… |
| 3 | Datos Abiertos Colombia | `www.datos.gov.co` | `SODA_JSON` (Socrata) | ✅ Verificada | Pública; throttling anónimo por IP (429 si se excede); licencias por dataset (TRM CC BY-SA 4.0); idioma ES |
| 4 | INE España | `servicios.ine.es` | `INE_JSON` | ✅ Verificada | Pública, sin key; uso razonable; idioma ES |
| 5 | openFDA (EE.UU.) | `api.fda.gov` | (JSON, decoder pendiente) | 🟡 API verificada | Pública, sin key para uso básico; idioma EN |
| 6 | Datos Abiertos Argentina | `datos.gob.ar` | CKAN (integración pendiente) | 🟡 API verificada | Pública; portales de datos abiertos; idioma ES |
| 7 | Datos Abiertos Chile | `datos.gob.cl` | CKAN (integración pendiente) | 🟡 API verificada | Pública; idioma ES |

### D4. Fuentes investigadas y descartadas / pendientes (con motivo)

| Fuente | Motivo |
|--------|--------|
| **IMF (FMI)** `api.imf.org` | SDMX no resuelve desde este entorno (`404 No such dataflow`, `204` vacío); pendiente de documentación/integración |
| **OECD (OCDE)** `stats.oecd.org` / `sdmx.oecd.org` | Dataflows SDMX devuelven `404`/redirects; pendiente |
| **UN Comtrade** `comtradeapi.un.org` | Endpoint público `404`; requiere suscripción/key para datos completos; pendiente |
| **UNdata** `data.un.org` | Servicio `500`/deprecado; pendiente |
| **CEPAL** `statistics.cepal.org` | Portal HTML, sin API pública limpia; pendiente |
| **México INEGI** | API requiere token; documentar `INEGI_TOKEN` (sin valor); pendiente |
| **México Banco de México (Banxico)** | API SIEA requiere token; documentar `BANXICO_TOKEN`; pendiente |
| **Argentina BCRA** | API v2 deprecada (410) y v3 `404`; pendiente |
| **Chile Banco Central (BCCh)** | API SIE `404` (no accesible); pendiente |
| **Chile INE / Perú BCRP** | `estadisticas.bcrp.gob.pe` protegido (HTML anti-bot); INE Chile sin API limpia; pendiente |
| **EE.UU. BLS** | API v2 requiere **POST** (el adaptador es GET); pendiente |
| **EE.UU. Census** | Algunos datasets requieren key (`Missing Key`); pendiente |
| **Perú INEI** | Sin API pública clara; pendiente |

### D5. Refinamientos de contenido (ADR-021/023)

Se corrigen dos rechazos falsos detectados con fuentes reales (sin relajar seguridad):

1. **Charset en Content-Type**: `SecureHttpClient` y `SourceValidator` ahora normalizan el
   `Content-Type` ignorando `; charset=...` (p. ej. `application/json; charset=utf-8`).
2. **Sub-tipos `+json`**: cuando `application/json` está permitido, se aceptan
   `application/*+json` (p. ej. `application/vnd.sdmx.data+json` de ECB, `application/problem+json`).
   La allowlist de dominios y la guardia SSRF **no cambian**.

### D6. Colombia ampliada (mercado base)

Todas las fuentes colombianas pasan por `www.datos.gov.co` (SODA2/Socrata, decoder `SODA_JSON`
ya existente; sin token, throttling anónimo por IP, licencias por dataset — el portal usa
predominantemente CC BY-SA 4.0). El Banco de la República **no expone API pública**
(`suameca.banrep.gov.co` → `401`); sus series se cubren vía datos.gov.co y el Banco Mundial.

**Datasets probados con red real (2026-08-20):**

| Dataset (ID) | Institución | Dato real obtenido | Frecuencia |
|---|---|---|---|
| TRM (`32sa-8pi3`) | Superfinanciera | TRM 3062.96 COP/USD (2026-08-21) | Diaria |
| PIB departamental (`kgyi-qc7j`) | DANE | PIB por departamento/actividad | Anual |
| Índice insumos agrícolas (`gwbi-fnzs`) | DANE | Índice 173.51 (jun-2026) | Mensual |
| Empresas creadas (`8rxi-7swc`) | Confecámaras | registro mercantil por municipio | Periódica |
| Exportaciones de café (`5fct-ib9u`) | DANE/DIAN | Café sin tostar → China: 3222 mil USD (2023) | Mensual |
| Cartera y crédito (`dugh-vkir`) | Superfinanciera | cartera por departamento | Trimestral |
| Tasas de captación (`axk9-g2nh`) | Superfinanciera | BBVA 12%, Banco Caja Social 12.99% | Periódica |
| Desembolsos redescuento (`bu9y-ytgi`) | Finagro | Antioquia/Agua: $24.2 mil millones | Anual |

**Descartadas (motivo técnico):**

| Fuente | Motivo |
|--------|--------|
| **DTF** (`gtwv-4pxq`) | `403 no row or column access to non-tabular tables` (vista no tabular) |
| **Bancóldex** (`cnuj-3h6z`) | Ídem `403` no tabular |
| **IPC nacional** | DANE no publica la serie nacional como dataset tabular en datos.gov.co |
| **Desempleo nacional** | No disponible como dataset abierto tabular |
| **Censo económico** | No publicado en datos.gov.co |
| **Censo población** | Solo versiones departamentales parciales (p. ej. Magdalena) |

**Escenario combinado verificado** (comercio exterior de café): TRM + exportaciones de café +
tasas de interés + desembolsos Finagro + PIB, en un solo análisis real (`KnowledgeColombiaSourcesTest`).
Caché Redis verificada con estos datasets: una consulta idéntica resuelve en ~1 s (hit de caché).

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Lista plana de dominios sin metadata** | No permite la futura selección por país ni distinguir Nivel 1/Nivel 2. Rechazada |
| **Selección por país ahora** (filtrar fuentes por país del proyecto en el pipeline) | El `ProjectContext` no expone país hoy; requiere decisión de producto. Se habilita el diseño sin implementarla |
| **Habilitar todas las fuentes verificadas en producción** | El usuario exige activación gradual y aprobación explícita; `KNOWLEDGE_EXTERNAL_ENABLED=false` se mantiene |
| **Aceptar cualquier content-type** | Debilitaría el control de formato. Rechazado: solo JSON y sub-tipos JSON |

---

## Consecuencias

### Positivas

- **Cobertura global real**: un proyecto en Colombia, España, México, EE.UU. o Ecuador obtiene
  hechos reales (verificado); los países sin Nivel 2 recurren a Nivel 1 sin fallar.
- **Extensible**: agregar un país = una entrada de configuración con metadata; la selección por
  país futura no requiere cambios de arquitectura ni de dominio.
- **Seguridad intacta**: la allowlist sigue siendo la única puerta; `SourceConnectionGuard` sin
  cambios; default deshabilitado en producción.
- **Transparencia**: cada fuente documentada con API, formato, estado y límites; las pendientes
  tienen motivo explícito.

### Negativas

- IMF/OECD/UN/CEPAL y varias nacionales siguen **pendientes**: la cobertura de Nivel 1 depende
  hoy del Banco Mundial y ECB.
- Los portales CKAN (Argentina/Chile) y openFDA tienen API verificada pero **decoder/integración
  pendiente**.
- Algunas fuentes nacionales (INEGI, Banxico) requieren token → variable de entorno documentada
  sin valor.
- Las series diarias de ECB generan muchos hechos; conviene acotar la ventana por configuración.

---

## Regla que modifica

**ADR aditivo** sobre ADR-021: amplía `SourceConfig` con `level`/`region`/`category`, añade los
formats `INE_JSON` y `SDMX_JSON`, y refina la aceptación de content-type (charset y `+json`) en
`SecureHttpClient` y `SourceValidator`. No modifica contratos congelados del dominio
(`SourceRegistry`, `SourceValidator` API, `KnowledgeEngine`, `SourceConnectionGuard` intactos).

## Cumplimiento

- **Verificación de red real** (tests gated por `KIN_TEST_REAL_NETWORK=true`):
  - `KnowledgeGlobalAllowlistTest` (7 tests): Colombia (1034 hechos), España (932, con INE),
    México (929, Nivel 1), EE.UU. (929, Nivel 1), **Ecuador sin Nivel 2 → 929 hechos de Nivel 1**,
    ECB solo, fuente rota degrada sin romper.
  - `KnowledgeStagingSourcesTest` (1 test): la config real de `application-staging.yml` produce
    536 hechos de las 8 fuentes.
  - `SecureHttpClientTest` (13) + `SourceValidatorTest` (28) + `KnowledgeMultiSourceDecoderTest`
    (10) + `SourceConnectionGuardTest` (11) verdes.
- **Caché Redis con datos reales**: tras una consulta real se pueblan `kin:knowledge:q:*` y
  `kin:knowledge:c:*`; una segunda consulta idéntica resuelve en ~1 s (hit de caché).
- **Producción**: `KNOWLEDGE_EXTERNAL_ENABLED=false` por defecto (application.yml); el perfil
  `staging` la activa solo en local/pruebas.
- Suite completa backend **verde** (`./mvnw clean verify`, BUILD SUCCESS).
- **Estado del ADR**: **Aprobado**.
