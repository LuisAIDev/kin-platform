# KIN — Matriz Maestra de Conocimiento (fuentes, categorías, países)

> **Documento autoritativo** de la adquisición de conocimiento externo. Consolida el estado real
> verificado (no diseño aspiracional) de todas las fuentes, su cobertura por **categoría de
> proyecto** y por **país/nivel**, con licencia, límites, actualización y estado. La fuente de
> verdad operativa es `kin-backend/src/main/resources/application-staging.yml`; el contrato de
> clave de caché y los adaptadores están en ADR-021, la allowlist global en ADR-023, el mapeo
> categoría→fuente en ADR-024 y la **Política de Fuentes** en ADR-025.
>
> **Estado del despliegue:** todo esto corre solo en el perfil `staging` (`KNOWLEDGE_EXTERNAL_ENABLED=true`
> únicamente ahí); **producción sigue con `KNOWLEDGE_EXTERNAL_ENABLED=false`**.

---

## 1. Principios que rigen la matriz

1. **Java decide**: la adquisición, validación, selección y caché son deterministas en Java; el
   LLM solo comunica resultados.
2. **Offline-first**: sin configuración no hay red; cualquier fallo degrada a `KnowledgeResult.empty()`.
3. **Allowlist única**: ningún dominio fuera de `kin.knowledge.allowed-domains` es consultable
   (`SourceConnectionGuard`, SSRF fail-closed, intacto).
4. **Verificación real**: una fuente solo entra a la matriz con prueba de red real (tests gated
   `KIN_TEST_REAL_NETWORK=true`) y su dato concreto documentado.
5. **Sin scraping**: toda fuente debe exponer API tabular real (JSON/XML/SDMX), nunca scraping de HTML.
6. **Reuso en vez de duplicación**: una fuente sirve a varias categorías vía `categories` (lista).

---

## 2. Matriz por fuente

Leyenda de estado: ✅ probada e integrada · 🟡 API verificada (integración/decoder pendiente) ·
⏳ pendiente de verificación · 🔴 descartada (motivo).

### 2.1 Nivel 1 — global (aplica a cualquier país)

| id | Institución | Formato (decoder) | Categorías | Licencia | Límites | Actualización | Estado |
|---|---|---|---|---|---|---|---|
| `worldbank-pib` | Banco Mundial — PIB `NY.GDP.MKTP.CD` (COL;LAC) | JSON (`WORLD_BANK_V2`) | contexto general | CC BY 4.0 | Sin key; volumen razonable | Anual | ✅ |
| `worldbank-inflacion` | Banco Mundial — Inflación `FP.CPI.TOTL.ZG` (COL;LAC) | JSON (`WORLD_BANK_V2`) | contexto general | CC BY 4.0 | Ídem | Anual | ✅ |
| `worldbank-desempleo` | Banco Mundial — Desempleo `SL.UEM.TOTL.ZS` (COL;LAC) | JSON (`WORLD_BANK_V2`) | contexto general | CC BY 4.0 | Ídem | Anual | ✅ |
| `ecb-usdeur` | BCE — Tipo de cambio USD/EUR | SDMX-JSON (`SDMX_JSON`) | contexto general | Pública (SDMX) | Sin key | Diaria | ✅ |

### 2.2 Nivel 2 — Colombia (`www.datos.gov.co`, SODA2/Socrata)

| id | Institución / dataset | Decoder | Categorías | Licencia | Límites | Actualización | Estado |
|---|---|---|---|---|---|---|---|
| `trm-col` | Superfinanciera — TRM (`32sa-8pi3`) | `SODA_JSON` | contexto general | CC BY-SA 4.0 | Sin token; throttling anónimo por IP (429) | Diaria | ✅ |
| `pib-departamental` | DANE — PIB departamental (`kgyi-qc7j`) | `SODA_JSON` | contexto general | CC BY-SA 4.0 | Ídem | Anual | ✅ |
| `insumos-agricolas` | DANE — Índice insumos agrícolas (`gwbi-fnzs`) | `SODA_JSON` | AGROINDUSTRIA | CC BY-SA 4.0 | Ídem | Mensual | ✅ |
| `confecamaras-empresas` | Confecámaras — Empresas creadas (`8rxi-7swc`) | `SODA_JSON` | EMPRESARIAL, COMERCIO | CC BY-SA 4.0 | Ídem | Periódica | ✅ |
| `dane-exportaciones-cafe` | DANE/DIAN — Exportaciones de café (`5fct-ib9u`) | `SODA_JSON` | AGROINDUSTRIA, COMERCIO, LOGISTICA | CC BY-SA 4.0 | Ídem | Mensual | ✅ |
| `superfinanciera-cartera` | Superfinanciera — Cartera y crédito (`dugh-vkir`) | `SODA_JSON` | FINTECH, EMPRESARIAL | CC BY-SA 4.0 | Ídem | Trimestral | ✅ |
| `superfinanciera-tasas` | Superfinanciera — Tasas de captación (`axk9-g2nh`) | `SODA_JSON` | FINTECH, EMPRESARIAL | CC BY-SA 4.0 | Ídem | Periódica | ✅ |
| `finagro-desembolsos` | Finagro — Desembolsos redescuento (`bu9y-ytgi`) | `SODA_JSON` | AGROINDUSTRIA, EMPRESARIAL | CC BY-SA 4.0 | Ídem | Anual | ✅ |
| `saludatos-ths` | MinSalud — Talento humano en salud (`5atd-7cac`) | `SODA_JSON` | SALUD | CC BY-SA 4.0 | Ídem | Periódica | ✅ |
| `calidad-aire-colombia` | IDEAM — Calidad del aire (`g4t8-zkc3`) | `SODA_JSON` | MEDIO_AMBIENTE | CC BY-SA 4.0 | Ídem | Horaria/diaria | ✅ |
| `transporte-aereo-colombia` | Aerocivil — Tráfico aéreo (`djjf-g4q4`) | `SODA_JSON` | LOGISTICA | CC BY-SA 4.0 | Ídem | Mensual/anual | ✅ |
| `mintic-internet-fijo` | MinTIC — Accesos internet fijo (`n48w-gutb`) | `SODA_JSON` | TECNOLOGIA | CC BY-SA 4.0 | Ídem | Trimestral | ✅ |
| `mintic-internet-movil` | MinTIC — Tráfico internet móvil (`4z5v-cr6b`) | `SODA_JSON` | TECNOLOGIA | CC BY-SA 4.0 | Ídem | Trimestral | ✅ |
| `minciencias-proyectos` | MinCiencias — Proyectos de investigación (`6hgx-q9pi`) | `SODA_JSON` | INVESTIGACION | CC BY-SA 4.0 | Ídem | Anual (convocatorias) | ✅ |

### 2.3 Nivel 2 — España

| id | Institución | Decoder | Categorías | Licencia | Límites | Actualización | Estado |
|---|---|---|---|---|---|---|---|
| `ine-ipc` | INE — IPC nacional (`24077`) | `INE_JSON` | contexto general | Pública, sin key | Uso razonable | Mensual | ✅ |

---

## 3. Matriz por categoría de proyecto

El catálogo real de KIN tiene **19 categorías** (verificado en `V6__create_categories.sql` +
`V18__add_project_categories.sql`; seed de dev idéntico). Cada proyecto recibe al menos el
**contexto general** (Nivel 1 + `trm-col` + `pib-departamental` + `ine-ipc`).
Las categorías con `categories` específicas suman sus fuentes:

| Categoría | Fuentes específicas | Estado |
|---|---|---|
| Agroindustria | insumos-agricolas · dane-exportaciones-cafe · finagro-desembolsos | ✅ probada end-to-end |
| Fintech | superfinanciera-tasas · superfinanciera-cartera · finagro-desembolsos | ✅ probada end-to-end |
| Salud | saludatos-ths | ✅ probada end-to-end |
| Empresarial | confecamaras-empresas · tasas · cartera · finagro | ✅ probada end-to-end |
| Comercio | confecamaras-empresas · dane-exportaciones-cafe | ✅ probada end-to-end |
| Logística | transporte-aereo-colombia · dane-exportaciones-cafe (reuso) | ✅ probada end-to-end |
| Tecnología e Innovación | mintic-internet-fijo · mintic-internet-movil | ✅ probada end-to-end |
| Investigación | minciencias-proyectos | ✅ probada end-to-end |
| Medio Ambiente | calidad-aire-colombia | ✅ fuente probada |
| Industria | (DANE no publica IPI/manufacturera como dataset tabular) | 🔴 descartada |
| Gastronomía y Alimentos | (contexto general; insumos aplican parcial) | 🟡 contexto + parcial |
| Educación, Impacto Social, Gobierno, Turismo, Creatividad, Marketing Digital, Servicios, Otro | (solo contexto general) | ⏳ pendiente |

---

## 4. Matriz por país / nivel

| Nivel | Cobertura | Fuentes |
|---|---|---|
| 1 — Global | Cualquier país (macroeconomía, tipos de cambio) | Banco Mundial (PIB/inflación/desempleo), ECB |
| 2 — Colombia | Mercado base (14 datasets) | TRM, PIB deptal., insumos agrícolas, empresas, exportaciones café, cartera, tasas, Finagro, Saludatos, calidad del aire, transporte aéreo, internet fijo/móvil, MinCiencias |
| 2 — España | IPC nacional | INE |
| Pendiente (país) | México, Perú, Argentina, Chile, EE.UU. | Ver ADR-023 §D4 (token/API no disponible) |

---

## 5. Descartadas y pendientes (motivo, no se inventan fuentes)

| Fuente | Motivo |
|---|---|
| DTF (datos.gov.co `gtwv-4pxq`), Bancóldex (`cnuj-3h6z`), transporte marítimo (`2zze-26rz`) | Vista `403 no row or column access to non-tabular tables` |
| IPC nacional DANE, desempleo nacional, censo económico, IPI/manufacturera | No publicados como datasets tabulares abiertos en datos.gov.co |
| Banco de la República (API propia) | `suameca.banrep.gov.co` → `401` (requiere auth) |
| IMF, OECD, UN Comtrade, UNdata, CEPAL | SDMX/API no resuelven desde este entorno o requieren suscripción |
| INEGI/Banxico (México), BCRP (Perú), BCRA (Argentina), BCCh (Chile) | Requieren token o API deprecada/protegida |
| BLS (EE.UU.), Census (EE.UU.) | Requieren POST o key en algunos datasets |
| openFDA (EE.UU.), datos.gob.ar / datos.gob.cl (CKAN) | API verificada; decoder/integración pendiente |

---

## 6. Cómo se mantiene

1. **Config operativa**: `application-staging.yml` (fuentes + metadata, incluido `max-age` por
   fuente que define el **TTL de caché** — datos diarios 12–24 h, mensuales 30 d, estructurales
   60 d). Producción no la activa.
2. **Pruebas gated de red real**: `KIN_TEST_REAL_NETWORK=true` → `KnowledgeStagingSourcesTest`,
   `KnowledgeGlobalAllowlistTest`, `KnowledgeColombiaSourcesTest`, `KnowledgeCategoryMappingTest`,
   `KnowledgeHttpRealNetworkTest` (cada fuente debe producir hechos reales).
3. **Cobertura por categoría**: agregar una fuente = entrada de configuración con `categories`;
   el mecanismo (`CategoryAwareCompositeKnowledgeSource`) filtra automáticamente. `enabled=false`
   conserva la fuente sin consultarla; `priority` ordena la selección (mayor primero; ADR-025).
4. **Caché**: Redis opcional (`kin.cache.redis.enabled`), clave determinista
   `kin:knowledge:q:<hash>` (topic+keywords+categoría) y `kin:knowledge:c:<hash>`; **TTL por
   fuente** = mínimo `maxAge` de los hechos del resultado (ADR-025).
5. **Gobernanza**: ver ADR-025 (Política de Fuentes) para incorporar/descartar/deprecar fuentes.
