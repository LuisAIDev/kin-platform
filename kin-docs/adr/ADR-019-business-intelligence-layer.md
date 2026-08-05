# ADR-019: Business Intelligence Layer (BIL) — Bounded Context de adquisición y consulta de inteligencia empresarial (Fase 11)

**Estado**: **Aprobado (diseño)** — sanciona la arquitectura de la Fase 11; la implementación
queda en el roadmap M1…M6.
**Fecha**: 2026-08-04
**Autor**: KIN Architecture Team

> **Alcance**: este ADR sanciona el **Bounded Context BusinessIntelligence (BIL)**: una capa
> aislada que resuelve el contexto geográfico (país/ciudad/región/moneda/idioma), adquiere
> datos de **providers por país y dominio** a través de **puertos**, cachea (TTL por dominio),
> fusiona, puntúa confianza y construye un **contexto estructurado para la IA**. **Java
> recopila los datos; el LLM (DeepSeek) únicamente redacta la respuesta en lenguaje
> natural.** Todos los cambios son **aditivos** y no modifican ningún contrato congelado de
> `BASELINE_ARCHITECTURE.md` ni la funcionalidad existente (Fase 10 cerrada, tag
> `v2.0.0-phase10`). Principio rector intacto: **Java decide. El LLM únicamente comunica.**

---

## Problema

KIN genera viabilidad de negocio con datos del proyecto y del pipeline, pero **no consulta
inteligencia empresarial externa de ningún país** (turismo, economía, demografía, negocios,
geografía, clima, finanzas). Para convertirse en una plataforma mundial necesita una capa que:

1. Detecte el contexto geográfico automáticamente (ej. "Cartagena" → Colombia → COP → Español).
2. Consulte múltiples fuentes por país y dominio sin acoplamiento a ninguna fuente concreta.
3. Consolide y puntúe confianza de cada dato (dato + fuente + fecha + score).
4. Entregue a la IA un contexto **ya procesado por Java**, prohibiendo que la IA consulte APIs.

El diseño debe respetar las reglas del repositorio: sin `if(country=="...")` ni
`switch(country)`; selección por providers y metadata; aislamiento de los contratos congelados.

---

## Contexto

- Fases 1–9 (ADR-001…017) y Fase 10 (ADR-018, `v2.0.0-phase10`) cerradas.
- Patrones de extensibilidad ya establecidos y reutilizables: auto-descubrimiento por
  `List<T>` (`RiskAnalyzer`, `OpportunityAnalyzer`, `SectionFormatter`, `KnowledgeSource`) y
  registros puros (`EngineRegistry`, `SourceRegistry`).
- La frontera ADR-012 (REPORT solo consume `ConsultingReport`; el LLM nunca ve fuentes crudas)
  y el ADR-013 (directiva en Java, `ResponseGuard`) permanecen intactos.
- El dominio `kin.*` es POJO puro (sin Spring); los adaptadores viven en `ai.*.adapter`; el
  wiring en composition roots (p. ej. `KinConfig`/`EnterpriseWebConfig`).

---

## Decisión

### 1. Bounded Context `BusinessIntelligence`

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Dominio | `com.kinplatform.kin.bil` | POJO puro (sin Spring): `geo` (CountryResolver + gazetteer), `provider` (8 puertos + Registry + Factory), `connector` (Orquestador + BusinessQuery), `cache` (puerto + TTL), `fusion` (Statistic/Provenance/DataFusionEngine), `confidence` (ConfidenceEngine), `service` (BusinessIntelligenceService), `aicontext` (AiContextBuilder), `web` (BilController + BilConfig) |
| Infraestructura | `com.kinplatform.ai.bil.adapter` | `CaffeineCacheEngine` (futuro `RedisCacheEngine`), `HttpConnector` (timeout/retry/circuit breaker), `ScraperConnector` (scraping ético), `CldrCountrySource`, y las implementaciones por país/dominio (`ColombiaTourismProvider`, `SpainTourismProvider`, `OpenStreetMapProvider`, …) |
| Frontend | (futuro) dashboard de consultas BIL | Reutiliza el stack existente; fuera de esta fase |

### 2. Aislamiento (cero cambios en contratos congelados)

- BIL **no se registra en `EngineRegistry`**, **no se añade al `Pipeline`** de 13 etapas y
  **no modifica** `KinMethod`, `ConversationOrchestrator`, `AIResponder`, `PromptAssembler`,
  `Flyway`, seguridad ni la API pública existente.
- La integración con la conversación es **aditiva y futura** (roadmap M6): el `AiContext`
  entraría por un overload de `PromptRequest` (patrón ADR-012/013) y se enmarcaría como dato
  procesado, nunca como fuente cruda.

### 3. Puertos (interfaces de dominio)

- `CountryResolver` — `resolve(String|lat,lon) → GeoResolution`.
- 8 puertos de provider: `TourismProvider`, `EconomyProvider`, `DemographyProvider`,
  `BusinessProvider`, `MapsProvider`, `WeatherProvider`, `FinanceProvider`,
  `KnowledgeProvider` (todos extienden el marcador `Provider` con `descriptor()`).
- `CacheEngine` — `get/put/evict` con TTL (independiente de Redis).
- `ConnectorPort` — adquisición externa implementada por adaptadores HTTP/scraping/knowledge.

### 4. Selección por metadata (nunca `if`/`switch` por país)

Cada provider declara `ProviderDescriptor{ id, domain, countries, sourceLevel, baseConfidence,
defaultTtl }`. `ProviderRegistry.select(domain, geo, minLevel)` filtra y ordena por metadata.
**Añadir un país = añadir una clase `@Component`.** KIN solo conoce interfaces.

### 5. Niveles de fuente

`SourceLevel`: `OFFICIAL(1)` (ministerios, institutos, open government), `OPEN_DATA(2)`
(portales/APIs/datasets abiertos), `ETHICAL_SCRAPING(3)` (solo si no hay API oficial;
siempre respetando `robots.txt`, sin bypass de autenticación, con politeness delay y cache).
El nivel alimenta directamente al `ConfidenceEngine`.

### 6. Cache, fusión y confianza

- **CacheEngine**: TTL por dominio (maps 24 h, turismo 7 d, economía 30 d, clima 30 min, …),
  override por `ProviderDescriptor.defaultTtl`. Caffeine hoy; Redis (distribuido) en M5.
- **DataFusionEngine**: normaliza a `Statistic(metric, value, unit, currency, geo, period)`,
  deduplica, resuelve conflictos (nivel → confianza → frescura → mayoría) y consolida con
  `Provenance`.
- **ConfidenceEngine** (determinista): `base(sourceLevel) − freshnessDecay + agreementBonus −
  validationMalus`, 0–100. BIL entrega **SIEMPRE** dato + fuente + fecha + confidence.

### 7. AI Context Builder

`AiContextBuilder` transforma `BusinessIntelligenceResult` + `GeoResolution` en `AiContext`
estructurado y acotado (ciudad, país, turistas, cruceros, competidores, ingreso promedio,
hoteles, flujo peatonal, confidence). Ese contexto es el único insumo del LLM para redactar.
**La IA jamás consulta APIs.**

### 8. Fuera de alcance de este ADR (roadmap de implementación)

- M1 núcleo de dominio, M2 infraestructura de providers, M3 orquestación/servicio/REST/AiContext,
  M4 portafolio de países + KnowledgeProvider, M5 Redis/observabilidad/resiliencia/scraping
  formalizado, M6 integración aditiva con la conversación e i18n.

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Resolver país con `if`/`switch`** | Viola el principio de diseño explícito; imposible de escalar a N países; rompe Open/Closed. Selección por metadata. |
| **Un solo `GlobalProvider` gigante** | Interfaces pequeñas por dominio (ISP) + composición: un provider por país/dominio es testeable e intercambiable. |
| **La IA consulta las APIs directamente** | Rompe el principio rector "Java decide, LLM comunica" y la frontera ADR-012; sin auditoría de fuentes ni confianza. |
| **Acoplar BIL al pipeline (etapa nueva)** | Violaría los contratos congelados y la regla de aislamiento; el pipeline de 13 etapas no se modifica. Integración solo aditiva en M6. |
| **Cache sin puerto (Caffeine directo)** | Acoplamiento a una tecnología; un puerto permite Redis sin tocar dominio. |
| **Scraping como primera opción** | Riesgo legal y de licencias; el scraping es nivel 3, solo cuando no hay API oficial, con política ética. |

---

## Consecuencias

### Positivas

- KIN consulta inteligencia empresarial mundial de forma extensible (Open/Closed).
- Cero cambios en contratos congelados; BIL vive en paquetes nuevos.
- Cada dato es auditable: dato + fuente + fecha + confidence.
- Java controla la calidad (fusión + confianza); el LLM solo redacta.
- Cache y aislamiento evitan degradar el pipeline y el coste de llamadas externas.

### Negativas

- Coste de operación de providers externos (llamadas, cuotas) — mitigado por cache/TTL.
- Complejidad nueva (fan-out, circuit breaker, fusión) — mitigada por puertos y defaults.
- Dependencia de disponibilidad de fuentes externas — mitigada por fallback y fail-open.

---

## Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|-----------|
| R1 | Cambio/discontinuación de APIs externas | Alta | Puertos + adaptadores sustituibles; cache y fallback |
| R2 | Licencias de datos | Alta | Niveles 1/2 prioritarios; política de scraping ético + compliance log |
| R3 | Latencia externa | Media | Timeout/circuit breaker por provider; paralelismo acotado |
| R4 | Inconsistencia de datos entre países | Media | Normalización CLDR + fusión determinista + confianza |
| R5 | Costo de llamadas | Media | TTL por dominio; límites de cuota por provider |
| R6 | Datos obsoletos | Media | Frescura por dominio + decay de confianza por edad |
| R7 | Dependencia de un proveedor de mapas | Baja | Interfaz única `MapsProvider` + implementaciones múltiples |
| R8 | Acoplamiento accidental con el pipeline | Baja | BC aislado; integración solo aditiva en M6 |

---

## Criterios de aceptación (diseño)

- [x] ADR-019 **Aprobado (diseño)**; `FASE11_BUSINESS_INTELLIGENCE_LAYER.md` con diagramas,
      bounded contexts, paquetes, interfaces, flujo, estrategias y roadmap.
- [x] BIL aislado: paquetes `kin.bil` + `ai.bil.adapter`; cero cambios en contratos congelados.
- [x] Selección de providers por metadata; prohibido `if`/`switch` por país.
- [x] 8 puertos por dominio; niveles de fuente 1/2/3; política de scraping ético.
- [x] `CacheEngine` con TTL por dominio (Caffeine ahora, Redis futuro).
- [x] `DataFusionEngine` + `ConfidenceEngine` determinista; entrega dato+fuente+fecha+score.
- [x] `AiContextBuilder`: Java recopila, DeepSeek solo redacta.

---

## Estado

**APROBADO (DISEÑO)** — La Business Intelligence Layer (Fase 11) queda diseñada y sancionada.
Este ADR **no modifica contratos congelados ni funcionalidad existente**. La implementación
sigue el roadmap M1…M6 y será sanccionada por ADRs complementarios (por fase) al concretarse.
