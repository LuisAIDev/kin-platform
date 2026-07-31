# BASELINE ARCHITECTURE — KIN 2.0 Alpha 1

> **Milestone**: `v2.0.0-alpha.1` — 30 de julio de 2026
> **Estado**: `ARCHITECTURE STABLE`
> **Commit**: `91426e5` — Branch: `main`
>
> Este documento es el **baseline contractual** del milestone: define qué existe hoy, qué está
> completo, qué contratos NO deben romperse, qué componente es estable/evolutivo/experimental
> y qué decisiones quedan congeladas. Ningún cambio sobre lo marcado como **Estable** procede
> sin una ADR aprobada.

---

## 1. Contexto

KIN es una plataforma full-stack de gestión de proyectos con evaluación asistida por IA.
Este milestone fija la base arquitectónica para las fases futuras: no añade funcionalidad
nueva de negocio, sino que estabiliza la infraestructura de dominio sobre la que se construirá
el resto de la plataforma.

| Capa | Stack |
|------|-------|
| Backend | Spring Boot 3.2.5 / Java 17 — Maven (`mvnw`) |
| Frontend | Next.js 16 App Router / TypeScript 5 strict / Tailwind CSS 4 |
| Base de datos | PostgreSQL (Docker, `ddl-auto: validate`) / H2 file-based en dev (`ddl-auto: update`) |
| IA | Ollama (`llama3.2`) con fallback en español (no requiere Ollama para dev/test) |

---

## 2. Qué existe hoy (inventario)

### 2.1 Arquitectura general

```
Clean Architecture + DDD Táctico + Pipeline Pattern + Event-Driven
```

| Bounded Context | Paquete | Responsabilidad |
|-----------------|---------|-----------------|
| `auth` | `com.kinplatform.auth` | Registro, login, emisión de JWT |
| `user` | `com.kinplatform.user` | Usuario, roles (FREE, PREMIUM, FACILITADOR, ADMIN) |
| `project` | `com.kinplatform.project` | CRUD + categorías, estado, scoring de viabilidad |
| `chat` | `com.kinplatform.chat` | Historial de mensajes, streaming SSE (`/chat/stream`), orquestación |
| `ai` | `com.kinplatform.ai` | Integración Ollama + fallback mock |
| `pricing` | `com.kinplatform.pricing` | Planes, Stripe |
| `reporting` | `com.kinplatform.kin.reporting` | Motores de dominio (scoring, recomendación, riesgo) |
| `engine` | `com.kinplatform.kin.engine` | Infraestructura común de motores (contrato estable) |
| `pipeline` | `com.kinplatform.kin.pipeline` | Pipeline de análisis + stages genéricos |
| `context` | `com.kinplatform.kin.context` | Tipos de dominio de contexto (ProjectContext, evaluación, decisión) |

### 2.2 Infraestructura de motores — `kin/engine` (CONTRATO CONGELADO)

| Tipo | Rol | Estable |
|------|-----|---------|
| `DomainEngine<E,R>` | Contrato funcional de motor: `execute(E) → R` + `metadata()` | ✅ |
| `EngineInput` | Record inmutable, base de entradas | ✅ |
| `EngineResult` | Record inmutable, base de resultados | ✅ |
| `EngineMetadata` | `id, name, version, phase, description, engineType, deterministic, inputRequirements, outputGuarantees` | ✅ |
| `EnginePhase` | `EXTRACT → ANALYZE → EVALUATE → RECOMMEND → DECIDE` | ✅ |
| `EngineType` | `SCORING, RECOMMENDATION, RISK, EVALUATION` | ✅ |
| `EngineRegistry` | Auto-descubrimiento via `List<DomainEngine>` + `get(id)` + `all()` | ✅ |
| `EngineExecutor` | Ejecución sequential/conditional/optional (parallel diseñado, no activo) | ✅ |
| `DeterministicId` | IDs deterministas para trazabilidad | ✅ |
| `EngineStage` | Stage genérico de pipeline que delega en motores | ✅ |

### 2.3 Motores de dominio — `kin/reporting`

| Motor | Input | Output | Notas |
|-------|-------|--------|-------|
| `ScoringEngine` | `ProjectContext` + `CompletenessEvaluation` | `ScoreResult` (totalScore, categoryScores, viabilityLabel, strengths, weaknesses) | En evolución: reemplazar heurística de longitud antes de KIN 2.5 |
| `RecommendationEngine` | `ProjectContext` + evaluación | `RecommendationResult` (recomendaciones deduplicadas y priorizadas) | ✅ estable como motor |
| `RiskEngine` | `ProjectContext` + evaluación | `RiskResult` (riesgos, severidad, probabilidad, score, riskLevel) | ✅ estable como motor |
| `RiskAssembler` | Analizadores de riesgo | `RiskResult` consolidado determinista | ✅ estable |

### 2.4 Pipeline — `kin/pipeline`

| Stage | Función |
|-------|---------|
| `Pipeline` | Orquesta la ejecución secuencial de stages |
| `PipelineStage` | Interfaz `name() + supports() + execute()` |
| `EngineStage` | Stage genérico delegando en `EngineExecutor` |
| `RecommendationStage` | Delega en `EngineStage` → `RecommendationEngine` |
| `RiskStage` | Delega en `EngineStage` → `RiskEngine` |
| `EventStage` | Publica eventos de dominio (en evolución, ver §5.2) |
| `PipelineContext` | Flujo de datos mutable entre stages + `engineResults` (engineId → EngineResult) |

### 2.5 AI — `ai/`

| Componente | Rol |
|------------|-----|
| `AIProvider` | Puerto `generateBlocking() + generateStream()` |
| `ProviderRouter` | Enrutamiento OCP-compliant entre proveedores |
| `AiEngineService` | Orquesta llamadas a IA con fallback a respuestas en español |
| `context.adapter.HeuristicContextAnalyzerAdapter` | Análisis heurístico (regex) — experimental |

### 2.6 Eventos — `kin/event`

| Componente | Rol | Estable |
|------------|-----|---------|
| `DomainEvent` | Interface `type() + aggregateId()` | ✅ |
| `DomainEventBus` | Interface `publish() + subscribe()` | ✅ |
| `InMemoryDomainEventBus` | Implementación en memoria (sin async/persistencia) | Experimental |
| Eventos concretos | `ConversationCompletedEvent`, `ProjectContextUpdatedEvent`, `QuestionGeneratedEvent`, `ReportGeneratedEvent`, `RiskDetectedEvent`, `ScoreCalculatedEvent` | ✅ tipos |

---

## 3. Qué está completo (criterio de aceptación del milestone)

- [x] **Fase 4.0** — Proveedor IA + capa de contexto de dominio.
- [x] **Fase 5.0** — `RecommendationEngine` (ADR-003).
- [x] **Fase 5.1** — `RiskEngine` + `RiskAssembler` (ADR-004).
- [x] **Fase 5.2** — Infraestructura común de motores (ADR-005): `kin/engine`, `EngineStage`, `KinConfig`.
- [x] Motores, inputs y results canonizados bajo el contrato `DomainEngine`.
- [x] `PipelineContext.engineResults` para resultados no canonizados.
- [x] **102 tests verdes** (`./mvnw clean test`), BUILD SUCCESS.
- [x] **Cobertura de dominio ≥ 90 %**: `kin.engine` 99,1 %, `kin.reporting` 96,2 %, `kin.reporting.risk` 99,6 %.
- [x] **5 ADRs** aprobadas (ADR-001 … ADR-005).
- [x] Documentación por fase (FASE5_0/5_1/5_2) + Gobernanza (§6) + AGENTS.md + CHANGELOG + Release Notes.

---

## 4. Contratos que NO deben romperse (API pública congelada)

> Cualquier cambio sobre estos contratos requiere una ADR aprobada.

### 4.1 Contratos de dominio

| Contrato | Contenido |
|----------|-----------|
| `DomainEngine` | `execute(E) → R`, `metadata() → EngineMetadata` |
| `EngineInput` / `EngineResult` | Records inmutables base |
| `EngineMetadata` | Campos actuales |
| `EnginePhase` | Orden y valores (`EXTRACT, ANALYZE, EVALUATE, RECOMMEND, DECIDE`) |
| `EngineType` | Valores actuales (`SCORING, RECOMMENDATION, RISK, EVALUATION`) |
| `EngineRegistry` | `get(id)`, `all()`, auto-descubrimiento |
| `EngineExecutor` | API de ejecución (sequential/conditional/optional) |
| `EngineStage` | `name()`, `supports()`, `execute()` |
| `RecommendationEngine` / `RecommendationResult` | Contrato público (input/output) |
| `RiskEngine` / `RiskResult` | Contrato público (input/output) |
| `KinMethod` | Fachada de dominio `execute(KinMethodCommand) → KinMethodResult` |
| `ConversationDecision.Action` | `ASK, REPORT, RECOMMEND, VALIDATE, SUMMARIZE, STOP, ESCALATE` |
| `AnalyzedDimension` | 14 dimensiones (se pueden agregar, no eliminar) |
| `DomainEvent` / `DomainEventBus` | `type()`, `aggregateId()`, `publish()`, `subscribe()` |
| `AIProvider` | `generateBlocking()`, `generateStream()` |
| `PipelineStage` / `Pipeline` | `name()`, `supports()`, `execute()` + algoritmo de ejecución |
| `ScoringModel` / `ScoreResult` | Records inmutables |
| `PipelineContext` | Acceso a campos + `engineResults` |

### 4.2 Contratos de aplicación/API

| Contrato | Contenido |
|----------|-----------|
| API REST | Todos los endpoints bajo `/api/v1` |
| Streaming | `/chat/stream` con SSE |
| `AiEngineService` | Fallback en español garantizado si Ollama no responde |

### 4.3 Contratos de infraestructura

| Contrato | Contenido |
|----------|-----------|
| CORS dual | `CorsConfig.java` + `SecurityConfig.java` (agregar orígenes en ambos) |
| JWT | Filtro `JwtAuthenticationFilter` + `JwtService` (stateless) |
| Env vars | `NEXT_PUBLIC_API_URL`, `.env` (gitignored, copiar de `.env.example`) |

---

## 5. Matriz de estabilidad

### 5.1 ESTABLES — no deben cambiar en fases futuras

| Componente | Justificación |
|-----------|---------------|
| `kin.engine.DomainEngine` | Contrato funcional de todos los motores; congelado por ADR-005 |
| `kin.engine.EngineInput` / `EngineResult` | Records base de la infraestructura de motores |
| `kin.engine.EngineMetadata` | Metadatos completos para descubrimiento y trazabilidad |
| `kin.engine.EnginePhase` / `EngineType` | Enumeraciones del ciclo de motores |
| `kin.engine.EngineRegistry` | Auto-descubrimiento vía `List<DomainEngine>`; 96,9 % cobertura |
| `kin.engine.EngineExecutor` | Ejecución determinista; 100 % cobertura |
| `kin.engine.DeterministicId` | IDs reproducibles para tests y trazabilidad |
| `kin.pipeline.stage.EngineStage` | Stage genérico de composición; 100 % cobertura |
| `kin.reporting.RecommendationEngine` | Motor de recomendaciones ADR-003; 96,2 % cobertura |
| `kin.reporting.risk.RiskEngine` | Motor de riesgos ADR-004; 99,6 % cobertura |
| `kin.reporting.risk.RiskAssembler` | Consolidación determinista de riesgos; 100 % cobertura |
| `RecommendationResult` / `RiskResult` | Outputs inmutables consumidos por pipeline |
| `kin.reporting.ScoreResult` / `kin.scoring.ScoringModel` | Records inmutables |
| `KinMethod` / `KinMethodCommand` / `KinMethodResult` | Fachada de dominio (contrato KIN 2.0) |
| `ConversationDecision` (+`Action`) | Decisión de conversación, enum completo |
| `AnalyzedDimension` | 14 dimensiones (aditivo) |
| `DomainEvent` / `DomainEventBus` | Interfaces base del event-driven |
| `AIProvider` | Puertos `generateBlocking`/`generateStream` |
| `PipelineStage` / `Pipeline` | Interfaces y algoritmo de ejecución |
| `PipelineContext` | Flujo de datos entre stages + `engineResults` |

### 5.2 EN EVOLUCIÓN — cambiarán pero manteniendo la API pública

| Componente | Cambios esperados |
|-----------|-------------------|
| `kin.scoring.ScoringEngine` | Reemplazo de heurística de longitud (antes de KIN 2.5) manteniendo `evaluate(ctx, eval) → ScoreResult` |
| `ProjectContext` | Se pueden agregar métodos de consulta; no cambiar la API de actualización |
| `CompletenessEvaluator` | Refinamiento de thresholds; API se mantiene |
| `ConversationStrategist` | Nuevas estrategias; API `decide(...) → ConversationDecision` se mantiene |
| `EventStage` | Debe disparar eventos según el flujo real (hoy siempre `ConversationCompleted`) → KIN 2.1 |
| `AiEngineService` | Extracción de `PromptAssembler` y deduplicación de providers → KIN 2.3; API de servicio se mantiene |
| `kin.pipeline` | Error handling, timeout y métricas por stage → KIN 2.1; orden de stages y contratos se mantienen |

### 5.3 EXPERIMENTALES — pueden cambiar significativamente

| Componente | Razón |
|-----------|-------|
| `InMemoryDomainEventBus` | Implementación temporal sin async ni persistencia → será reemplazada (KIN 2.4) |
| `HeuristicContextAnalyzerAdapter` | Análisis regex → será reemplazado por NLP/AI (KIN 2.5) |
| `ChatOrchestratorServiceImpl` flujo streaming | Componente más inestable; debe refactorizarse para usar `KinMethod` (KIN 2.1) |
| `EngineExecutor.parallel` | Diseñado pero no activo; puede cambiar antes de activarse |
| `kin/event` tipos concretos | Pueden agregarse nuevos eventos; los existentes se mantienen |

---

## 6. Decisiones congeladas (Frozen Decisions)

1. **`kin/` es 100 % POJO**: sin Spring, sin JPA, sin imports de `com.kinplatform.*` excepto `kin.*`.
2. **Toda comunicación entre bounded contexts** es por eventos o por puertos.
3. **Value Objects inmutables** (records de Java o copia defensiva). `PipelineContext` es la única excepción (mutable por diseño).
4. **Los Application Services orquestan, no implementan reglas de negocio**.
5. **`ChatOrchestratorServiceImpl` DEBE usar `KinMethod` en ambos flujos** (bloqueante y streaming) — regla absoluta #10.
6. **La infraestructura de motores (`kin/engine`) es un contrato congelado** — ADR-005.
7. **`RecommendationEngine` y `RiskEngine` son estables** — ADR-003 y ADR-004.
8. **La heurística de longitud en `ScoringEngine` debe reemplazarse antes de KIN 2.5** — regla absoluta #18.
9. **Cobertura de dominio ≥ 90 %** de instrucciones en `kin.reporting` y `kin.engine` (JaCoCo).
10. **Dev sin Docker**: H2 file-based + Flyway deshabilitado (`spring.flyway.enabled=false`); PostgreSQL solo en Docker.

---

## 7. Preparación para la siguiente fase (Fase 5.3)

> Análisis de preparación. NO implementación.

### 7.1 Estado de partida

La infraestructura de motores (`DomainEngine`, `EngineExecutor`, `EngineStage`) ya es reutilizable:
una Fase 5.3 puede añadir un nuevo motor (p. ej. `BusinessModelEngine`, `MarketEngine` o un
`ReportEngine`) o una nueva etapa de pipeline **sin tocar los contratos estables**.

### 7.2 Riesgos

| # | Riesgo | Severidad | Mitigación |
|---|--------|-----------|------------|
| R1 | `ChatOrchestratorServiceImpl` sigue sin usar `KinMethod` en streaming | Alta | Priorizar el refactor en la próxima fase; el milestone lo deja explícitamente fuera |
| R2 | `EventStage` no refleja el flujo real (siempre `ConversationCompleted`) | Media | Corregir junto al refactor de streaming (KIN 2.1) |
| R3 | Baja cobertura en infraestructura (auth, pricing, project, ai.provider) | Media | El requisito de ≥ 90 % aplica solo al dominio; ampliarlo requeriría planificar tests de infraestructura |
| R4 | Dependencia de Ollama no versionada para los proveedores AI | Media | El fallback en español garantiza el desarrollo; formalizar health check en KIN 2.3 |
| R5 | Script Flyway `V2` no portable a H2 | Baja | No bloquea; documentar y mantener `spring.flyway.enabled=false` en dev |
| R6 | Crecimiento de `PipelineContext` (riesgo God Class) | Media | Monitoreo continuo; la API de acceso se mantiene |

### 7.3 Bloqueantes

- **Ningún bloqueante** para iniciar la Fase 5.3. Los pendientes del roadmap (streaming, eventos,
  pipeline hardening, event bus async) son evolución, no bloqueo.

### 7.4 Dependencias

| Dependencia | Estado | Requerida para |
|-------------|--------|----------------|
| Ollama (`llama3.2`) | Opcional (fallback mock) | Pruebas de IA reales |
| PostgreSQL + Docker | No requerida en dev (H2) | Despliegue / validación de migraciones |
| Frontend (`npm install`) | Requerida | E2E y desarrollo UI |
| Backend (`./mvnw`) | Requerida | Tests y arranque |

### 7.5 Recomendaciones para la siguiente fase

1. **Prioridad 1**: refactorizar el flujo streaming de `ChatOrchestratorServiceImpl` para usar `KinMethod` (cierra R1 y habilita eliminar deuda del milestone).
2. **Prioridad 2**: corregir `EventStage` para disparar eventos según el flujo real.
3. **Prioridad 3**: añadir el nuevo motor/etapa de la Fase 5.3 sobre `kin/engine` reutilizando `EngineStage` — el contrato ya está listo para ello.
4. Mantener el requisito de ≥ 90 % de cobertura en `kin.reporting` y `kin.engine` al añadir código nuevo.
5. No romper ninguno de los contratos de §4 sin ADR.

---

## 8. Verificación

```bash
cd kin-backend && ./mvnw clean test          # 102 tests, 0 fallos, BUILD SUCCESS
# Cobertura: kin.engine 99,1 %, kin.reporting 96,2 %, kin.reporting.risk 99,6 %
git status                                   # working tree clean
git log --oneline -3                         # 91426e5 fix pricing schema / 6518010 feat phases 4-5.2
```

---

*Baseline KIN 2.0 Alpha 1 — contrato congelado. Cualquier desviación requiere ADR aprobada.*
