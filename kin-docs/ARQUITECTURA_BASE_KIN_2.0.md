# ARQUITECTURA BASE — KIN 2.0

> Documento contractual. Toda implementación futura debe respetar esta arquitectura.
> Cualquier desviación requiere una ADR (Architecture Decision Record) aprobada.

---

## ÍNDICE

1. [Filosofía Arquitectónica](#1-filosofía-arquitectónica)
2. [Bounded Contexts](#2-bounded-contexts)
3. [Clasificación DDD de Cada Componente](#3-clasificación-ddd)
4. [Mapa de Paquetes](#4-mapa-de-paquetes)
5. [Mapa de Dependencias](#5-mapa-de-dependencias)
6. [Diagramas UML](#6-diagramas-uml)
7. [Análisis de Riesgo — God Classes](#7-análisis-de-riesgo)
8. [Estabilidad de Componentes](#8-estabilidad)
9. [Roadmap hasta KIN 3.0](#9-roadmap)
10. [Reglas Arquitectónicas Absolutas](#10-reglas-absolutas)

---

## 1. FILOSOFÍA ARQUITECTÓNICA

```
Clean Architecture + DDD Táctico + Pipeline Pattern + Event-Driven
```

### Capas (de adentro hacia afuera)

| Capa | Contiene | Depende de |
|------|----------|------------|
| **Domain** (`kin/`) | Entidades, Value Objects, Domain Services, Aggregate Roots, Domain Events, Ports | Nada del proyecto |
| **Application** (`ai/`, `chat/`) | Application Services, DTOs, Orchestrators | Domain |
| **Infrastructure** (`ai/context/adapter/`, `common/security/`, `common/config/`) | Implementaciones de puertos, repositorios JPA, filtros, config de Spring | Domain + Application |
| **Web/API** (`*Controller.java`) | REST endpoints | Application |

### Principios rectores

1. **El dominio no sabe que Spring existe** — `kin/` es 100% POJO, sin anotaciones Spring, sin imports a infraestructura.
2. **Puertos (interfaces) en el dominio, adapters en infraestructura** — `ContextAnalyzerPort` está en `kin/context/`, su implementación `HeuristicContextAnalyzerAdapter` está en `ai/context/adapter/`.
3. **Eventos como contracts entre Bounded Contexts** — `DomainEventBus` en el dominio, implementaciones intercambiables.
4. **Pipeline como core del flujo de trabajo** — `KinMethod` ejecuta un `Pipeline` de etapas; cada etapa es un `PipelineStage` intercambiable.
5. **Inyección de dependencias por constructor** — ni setters, ni field injection, ni service locator.

---

## 2. BOUNDED CONTEXTS

### Propuesta oficial: 7 Bounded Contexts

```
┌─────────────────────────────────────────────────────────────┐
│                     KIN PLATFORM                              │
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │Consulting│  │Conversat.│  │   AI     │  │ Scoring  │    │
│  │ (Core)   │  │ (Core)   │  │ (Support)│  │ (Support)│    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘    │
│       │              │              │              │          │
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐    │
│  │ Reporting│  │Knowledge │  │  Auth    │  │Billing   │    │
│  │ (Future) │  │ (Future) │  │(Support) │  │(Support) │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
│                                                               │
│  ┌────────────────────────────────────────────────────┐      │
│  │              Infrastructure (Shared)               │      │
│  └────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 2.1 Consulting (Core Domain)

**Responsabilidad**: Evaluar la viabilidad de proyectos de innovación/emprendimiento usando el Método KIN.

**Componentes**:
- `KinMethod` — Aggregate Root, orquesta el pipeline
- `Pipeline`, `PipelineStage`, `PipelineContext` — Pipeline execution
- `ConversationDecision` — Value Object, qué hacer en cada paso
- Análisis de dimensiones, scoring de viabilidad

**Lenguaje ubicuo**: método KIN, pipeline, etapa, dimensión, viabilidad, scoring, decisión, reporte.

### 2.2 Conversation (Core Domain)

**Responsabilidad**: Gestionar el estado completo de la conversación con el usuario, extraer información del proyecto.

**Componentes**:
- `ProjectContext` — Aggregate Root, estado de la conversación
- `AnalyzedDimension` — Value Object (enum)
- `CompletenessEvaluation` — Value Object
- `CompletenessEvaluator` — Domain Service
- `ContextAnalyzerPort` — Puerto (implementado por infraestructura)
- `ExplorationStrategy`, `ConversationStrategist` — Domain Services
- `Message` — Value Object

### 2.3 AI (Supporting Subdomain)

**Responsabilidad**: Abstraer proveedores de IA (DeepSeek, OpenAI, etc.) con fallback automático.

**Componentes**:
- `AIProvider` — Puerto en el dominio
- `ProviderRouter` — Application Service (usa múltiples `AIProvider`)
- `DeepSeekProvider`, `OpenAIProvider` — Infrastructure Adapters
- `AiEngineService` — Application Service (facade)

### 2.4 Scoring (Supporting Subdomain)

**Responsabilidad**: Calcular score de viabilidad basado en dimensiones cubiertas y calidad de información.

**Componentes**:
- `ScoringEngine` — Domain Service
- `ScoringModel` — Value Object (configuración de pesos)
- `ScoreResult` — Value Object

### 2.5 Reporting (Future — Generic Subdomain)

**Responsabilidad**: Generar reportes estructurados de viabilidad en múltiples formatos.

**Componentes planificados**:
- `ReportEngine` — Domain Service
- `Report` — Aggregate Root
- `ReportSection` — Value Object
- `ReportRenderer` — Puerto

### 2.6 Knowledge (Future — Generic Subdomain)

**Responsabilidad**: Gestionar base de conocimiento de metodologías, benchmarks, casos de éxito.

**Componentes planificados**:
- `KnowledgeEngine` — Domain Service
- `KnowledgeEntry` — Entity
- `KnowledgeCategory` — Value Object

### 2.7 Auth / Billing / IAM (Supporting Subdomains)

**Responsabilidad**: Autenticación, roles, suscripciones, pagos (Stripe).

**Componentes actuales**: `auth/`, `pricing/`, `user/`, `common/security/`

### Mapa de paquetes propuesto (futuro)

```
com.kinplatform.consulting/     → Consulting (Core)
com.kinplatform.conversation/   → Conversation (Core)  
com.kinplatform.ai/             → AI (Supporting)
com.kinplatform.scoring/        → Scoring (Supporting)
com.kinplatform.reporting/      → Reporting (Future)
com.kinplatform.knowledge/      → Knowledge (Future)
com.kinplatform.auth/           → Auth (Supporting)
com.kinplatform.billing/        → Billing (Supporting)
com.kinplatform.user/           → User (Shared Kernel)
com.kinplatform.project/        → Project (Shared Kernel)
com.kinplatform.common/         → Shared Kernel (config, security, dto)
com.kinplatform.infrastructure/ → Adapters (persistence, ai providers)
```

**Nota**: Hoy conviven en `kin/` los contextos Consulting, Conversation, AI y Scoring porque son el core mínimo. En KIN 2.5+ se separarán en paquetes independientes.

---

## 3. CLASIFICACIÓN DDD

### 3.1 Entidades

| Componente | Contexto | Aggregate Root | ID | Persistencia |
|-----------|----------|---------------|----|--------------|
| `Project` | Project | Sí | UUID | JPA / PostgreSQL |
| `ProjectContext` | Conversation | Sí | UUID (projectId) | En memoria (hoy) |
| `User` | User | Sí | UUID | JPA / PostgreSQL |
| `ChatMessage` | Conversation | No | UUID | JPA / PostgreSQL |
| `PricingPlan` | Billing | Sí | UUID | JPA / PostgreSQL |
| `UserSubscription` | Billing | No | UUID | JPA / PostgreSQL |

### 3.2 Value Objects

| Componente | Contexto | Inmutable | Igualdad por |
|-----------|----------|-----------|---------------|
| `AnalyzedDimension` | Conversation | Sí | Nombre del enum |
| `ConversationDecision` | Consulting | Sí | Todos los campos |
| `CompletenessEvaluation` | Conversation | Sí | Todos los campos |
| `ScoreResult` | Scoring | Sí | Todos los campos |
| `ScoringModel` | Scoring | Sí | Versión |
| `KinMethodCommand` | Consulting | Sí | Todos los campos |
| `KinMethodResult` | Consulting | Sí | Todos los campos |
| `Message` | Conversation | Sí | Todos los campos |
| `AnalysisResult` | Conversation | Sí | Dimensiones extraídas |
| `EvaluationPolicies` | Conversation | Sí | Configuración |
| `ExplorationPriority` | Conversation | Sí | Prioridades |
| `PipelineContext` | Consulting | **NO** (mutable) | Referencia |

### 3.3 Domain Services

| Componente | Contexto | Opera sobre | Efectos |
|-----------|----------|-------------|---------|
| `CompletenessEvaluator` | Conversation | `ProjectContext` | Produce `CompletenessEvaluation` |
| `ConversationStrategist` | Conversation | `ProjectContext` + `CompletenessEvaluation` | Produce `ConversationDecision` |
| `ScoringEngine` | Scoring | `ProjectContext` + `CompletenessEvaluation` | Produce `ScoreResult` |
| `ExplorationStrategy` | Conversation | `ExplorationPriority` | Define próxima dimensión |

### 3.4 Application Services

| Componente | Contexto | Coordina |
|-----------|----------|----------|
| `KinMethod` | Consulting | Pipeline completo + Domain Events |
| `ChatOrchestratorServiceImpl` | Conversation | KinMethod o flujo inline (bloqueante vs streaming) |
| `AiEngineService` | AI | Llamada a proveedores AI con fallback |
| `ProviderRouter` | AI | Enrutamiento entre proveedores AI |
| `ChatServiceImpl` | Conversation | Persistencia de ChatMessage |
| `ProjectServiceImpl` | Project | CRUD de proyectos |
| `AuthServiceImpl` | Auth | Registro, login, JWT |
| `ProjectContextService` | Conversation | Caché de ProjectContext por proyecto |
| `SubscriptionValidatorService` | Billing | Validación de límites por plan |

### 3.5 Ports (interfaces en dominio)

| Puerto | Contexto | Implementación |
|--------|----------|---------------|
| `ContextAnalyzerPort` | Conversation | `HeuristicContextAnalyzerAdapter` |
| `DomainEventBus` | Consulting | `InMemoryDomainEventBus` |
| `AIProvider` | AI | `DeepSeekProvider`, `OpenAIProvider` |

### 3.6 Infrastructure Adapters

| Adaptador | Puerto que implementa | Tecnología |
|-----------|----------------------|------------|
| `HeuristicContextAnalyzerAdapter` | `ContextAnalyzerPort` | Regex, heurístico |
| `InMemoryDomainEventBus` | `DomainEventBus` | `ConcurrentHashMap` en memoria |
| `DeepSeekProvider` | `AIProvider` | Spring AI + OpenAI API compatibility |
| `OpenAIProvider` | `AIProvider` | Spring AI |
| `ChatMessageRepository` | (JPA impl.) | Spring Data JPA |
| `ProjectRepository` | (JPA impl.) | Spring Data JPA |
| `UserRepository` | (JPA impl.) | Spring Data JPA |

### 3.7 Factories

| Factory | Produce | Ubicación |
|---------|---------|-----------|
| `ProjectContext.fromProject()` | `ProjectContext` | `kin/context/ProjectContext.java` (static factory method) |
| `CompletenessEvaluation.empty()` | `CompletenessEvaluation` | `kin/context/CompletenessEvaluation.java` (static factory) |
| `ScoreResult.empty()` | `ScoreResult` | `kin/scoring/ScoreResult.java` (static factory) |
| `ConversationDecision.ask()` / `.generateReport()` / `.stop()` | `ConversationDecision` | `kin/decision/ConversationDecision.java` (static factories) |
| `ScoringModel.defaultModel()` | `ScoringModel` | `kin/scoring/ScoringModel.java` (static factory) |

---

## 4. MAPA DE PAQUETES

### Estado actual (después de correcciones)

```
com.kinplatform
├── KinApplication.java                      [Entry Point]
│
├── kin/                                     *** DOMINIO PURO ***
│   ├── KinMethod.java                       [Application Service]
│   ├── KinMethodCommand.java                [Value Object — record]
│   ├── KinMethodResult.java                 [Value Object — record]
│   ├── context/
│   │   ├── AnalyzedDimension.java           [Value Object — enum]
│   │   ├── AnalysisResult.java              [Value Object — record]
│   │   ├── CompletenessEvaluation.java      [Value Object — record]
│   │   ├── CompletenessEvaluator.java       [Domain Service]
│   │   ├── ContextAnalyzerPort.java         [Port — interface]
│   │   ├── EvaluationPolicies.java          [Value Object — record]
│   │   ├── ExplorationPriority.java         [Value Object — record]
│   │   ├── Message.java                     [Value Object — record]
│   │   ├── ProjectContext.java              [Aggregate Root — POJO mutable]
│   │   ├── ExplorationStrategy.java         [Domain Service — interface]
│   │   └── strategy/
│   │       ├── ConversationStrategist.java  [Domain Service]
│   │       └── DefaultExplorationStrategy.java [Domain Service]
│   ├── decision/
│   │   └── ConversationDecision.java        [Value Object — record]
│   ├── event/
│   │   ├── DomainEvent.java                 [Domain Event — interface]
│   │   ├── DomainEventBus.java              [Port — interface]
│   │   ├── EventHandler.java                [Functional Interface]
│   │   └── InMemoryDomainEventBus.java      [Infrastructure Adapter — en kin/ temporalmente]
│   ├── pipeline/
│   │   ├── Pipeline.java                    [Domain Service]
│   │   ├── PipelineContext.java             [Mutable context object — borderline VO]
│   │   ├── PipelineStage.java               [Port — interface]
│   │   └── stage/
│   │       ├── AnalyzerStage.java           [Pipeline Stage]
│   │       ├── ConsultorStage.java          [Pipeline Stage]
│   │       ├── EvaluatorStage.java          [Pipeline Stage]
│   │       ├── EventStage.java              [Pipeline Stage]
│   │       ├── ScoringStage.java            [Pipeline Stage]
│   │       └── StrategistStage.java         [Pipeline Stage]
│   └── scoring/
│       ├── ScoreResult.java                 [Value Object — record]
│       ├── ScoringEngine.java               [Domain Service]
│       └── ScoringModel.java                [Value Object]
│
├── ai/                                      *** APPLICATION / INFRASTRUCTURE ***
│   ├── AiEngineService.java                 [Application Service]
│   ├── provider/
│   │   ├── AIProvider.java                  [Port — interface]
│   │   ├── DeepSeekProvider.java            [Infrastructure Adapter]
│   │   ├── OpenAIProvider.java              [Infrastructure Adapter]
│   │   └── ProviderRouter.java              [Application Service]
│   └── context/
│       ├── ProjectContextService.java       [Application Service]
│       └── adapter/
│           └── HeuristicContextAnalyzerAdapter.java [Infrastructure Adapter]
│
├── chat/                                    *** APPLICATION / INFRASTRUCTURE ***
│   ├── ChatService.java                     [Application Service — interface]
│   ├── ChatServiceImpl.java                 [Application Service]
│   ├── ChatOrchestratorService.java         [Application Service — interface]
│   ├── ChatOrchestratorServiceImpl.java     [Application Service]
│   ├── ChatController.java                  [Web Adapter]
│   ├── ChatMessage.java                     [JPA Entity — Infrastructure]
│   ├── ChatMessageRepository.java           [Infrastructure Repository]
│   ├── MessageRole.java                     [Enum — Infrastructure]
│   └── dto/                                 [DTOs — Application]
│       ├── ChatMessageResponse.java
│       ├── ChatRequest.java
│       ├── ChatResponse.java
│       └── SaveMessageRequest.java
│
├── project/                                 *** APPLICATION / INFRASTRUCTURE ***
│   ├── Project.java                         [JPA Entity — Infrastructure]
│   ├── ProjectCategory.java                 [Enum — Infrastructure]
│   ├── ProjectStatus.java                   [Enum — Infrastructure]
│   ├── ProjectRepository.java               [Infrastructure Repository]
│   ├── ProjectService.java                  [Application Service — interface]
│   ├── ProjectServiceImpl.java              [Application Service]
│   ├── ProjectController.java               [Web Adapter]
│   ├── ProjectLimitExceededException.java   [Infrastructure Exception]
│   └── dto/                                 [DTOs — Application]
│       ├── CreateProjectRequest.java
│       ├── UpdateProjectRequest.java
│       └── ProjectResponse.java
│
├── auth/                                    *** APPLICATION / INFRASTRUCTURE ***
│   ├── AuthService.java                     [Application Service — interface]
│   ├── AuthServiceImpl.java                 [Application Service]
│   ├── AuthController.java                  [Web Adapter]
│   └── dto/                                 [DTOs]
│       ├── AuthResponse.java
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── UserDTO.java
│
├── user/                                    *** INFRASTRUCTURE ***
│   ├── User.java                            [JPA Entity]
│   ├── UserRole.java                        [Enum]
│   └── UserRepository.java                  [Repository]
│
├── pricing/                                 *** APPLICATION / INFRASTRUCTURE ***
│   ├── PricingPlan.java                     [JPA Entity]
│   ├── PricingPlanRepository.java
│   ├── UserSubscription.java                [JPA Entity]
│   ├── UserSubscriptionRepository.java
│   ├── PlanNotFoundException.java
│   ├── controller/
│   ├── dto/
│   └── service/
│       ├── SubscriptionService.java
│       ├── SubscriptionServiceImpl.java
│       ├── StripeService.java
│       └── SubscriptionValidatorService.java
│
├── common/                                  *** SHARED KERNEL ***
│   ├── GlobalExceptionHandler.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   ├── KinConfig.java                  [Wiring del dominio]
│   │   ├── AiContextConfig.java
│   │   ├── DeepSeekConfig.java
│   │   └── StripeConfig.java
│   ├── dto/
│   │   └── PageResponse.java
│   └── security/
│       ├── JwtService.java
│       ├── JwtAuthenticationFilter.java
│       ├── RateLimitingFilter.java
│       └── SubscriptionAccessFilter.java
│
├── stripe/                                  *** INFRASTRUCTURE ***
│   └── webhook/
│       └── StripeWebhookController.java
│
└── test/                                    *** WEB ADAPTER (for dev) ***
    └── TestAiController.java
```

### Observaciones sobre la organización actual

1. **`InMemoryDomainEventBus` está en `kin/event/`** — es una implementación en memoria, debería estar en infraestructura. Se mantiene aquí temporalmente por simplicidad hasta que se introduzca una implementación con RabbitMQ/Kafka.
2. **Los Pipeline Stages están en `kin/pipeline/stage/`** — son parte del dominio (son la implementación del patrón Pipeline). Correcto.
3. **`ProjectContextService` está en `ai/context/`** — es un Application Service. Correcto.
4. **`ChatMessage`, `Project`, `User` son JPA entities en sus respectivos paquetes** — mezclan responsabilidad de dominio con persistencia. En KIN 3.0 se separarán.

---

## 5. MAPA DE DEPENDENCIAS

### 5.1 Dependencias del Dominio (`kin/`)

```
kin/ → nada del proyecto
kin/ → java.util.*, org.slf4j (solo logging — aceptable en Java)
```

Verificación: `kin/` NO importa nada de `com.kinplatform.ai`, `com.kinplatform.chat`, `com.kinplatform.project`, `com.kinplatform.user`.

✅ **El dominio no depende de infraestructura.**

### 5.2 Dependencias de Application

```
ai/     → kin/, ProviderRouter, AIProvider
chat/   → ai/ (AiEngineService), kin/ (Message, KinMethod, KinMethodCommand, ProjectContext), project/, user/
project/ → user/, pricing/
auth/   → user/, common/security (JwtService)
pricing/ → user/
```

✅ **Application depende del dominio.**

### 5.3 Dependencias de Infraestructura

```
ai/provider/     → kin/ (Message), Spring AI
ai/context/      → kin/ (ProjectContext, etc.)
common/config/   → kin/ (todos los beans), ai/, Spring Security
common/security/ → user/, pricing/
```

✅ **Infraestructura implementa puertos del dominio.**

### 5.4 Verificación anti-circulares

```
kin/ ──→ nada ──→ kin/        ✓ Sin ciclos
ai/  ──→ kin/ ──→ nada        ✓ Sin ciclos
chat/ ──→ ai/, kin/, project/ → todo converge en kin/  ✓ Sin ciclos
common/config/ → kin/, ai/     ✓ Sin ciclos
```

✅ **No existen dependencias circulares.**

### 5.5 Mapa completo de dependencias

```
                    ┌─────────────┐
                    │  common/    │
                    │  config/    │──→ kin/, ai/, ai/provider/, ai/context/
                    │  security/  │──→ user/, pricing/
                    └─────────────┘
                          │
                          │ (wires beans)
                          ▼
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  auth/  │───→│  user/      │◄───│  project/   │───→│  chat/      │
└─────────┘    └─────────────┘    └──────┬──────┘    └──────┬──────┘
       │               │                 │                   │
       │               ▼                 ▼                   │
       │         ┌─────────────┐    ┌─────────────┐         │
       │         │  pricing/   │───→│  kin/ (DOMAIN)    │◄────┘
       │         └─────────────┘    └──────┬──────┘
       │                                   │
       │              ┌────────────────────┘
       │              ▼
       │    ┌─────────────────┐
       └───→│  ai/ (Application)  │
            │  ai/provider/    │──→ kin/ (via Message)
            │  ai/context/     │──→ kin/ (via ProjectContext, etc.)
            └─────────────────┘
```

---

## 6. DIAGRAMAS UML

### 6.1 UML de Dominio

```
┌────────────────────────────────────────────────────────────────────┐
│                        DOMAIN MODEL                                  │
│                                                                      │
│  ┌─────────────────────────────────┐                                │
│  │      ProjectContext (AR)        │                                │
│  ├─────────────────────────────────┤                                │
│  │ - data: EnumMap<AnalyzedDim,Str>│◄────────── uses ────────────┐  │
│  │ - dimensionsCovered: Set        │                              │  │
│  │ - currentDecision: ConvDecision │                              │  │
│  │ - exchangeCount: int            │                              │  │
│  │ - reportGenerated: boolean      │                              │  │
│  ├─────────────────────────────────┤                              │  │
│  │ + fromProject(title,desc,cat)   │                              │  │
│  │ + update(AnalysisResult)        │                              │  │
│  │ + isDimensionCovered(Dim): bool │                              │  │
│  │ + coverageRatio(): double       │                              │  │
│  │ + missingDimensions(): List     │                              │  │
│  │ + toPromptSnippet(): String     │                              │  │
│  │ + attachDecision(ConvDecision)  │                              │  │
│  └──────────────┬──────────────────┘                              │  │
│                 │                                                  │  │
│                 │ uses                                             │  │
│                 ▼                                                  │  │
│  ┌──────────────────────────┐     ┌─────────────────────────────┐  │  │
│  │  AnalyzedDimension (VO)  │     │  AnalysisResult (VO)        │  │  │
│  │  <<enum>>                │     │  extracted: Map<Dim, String>│  │  │
│  │  PROJECT_NAME, SECTOR,.. │     └─────────────────────────────┘  │  │
│  └──────────────────────────┘                                     │  │
│                                                                   │  │
│  ┌─────────────────────────────────────┐                          │  │
│  │   ConversationDecision (VO)         │                          │  │
│  ├─────────────────────────────────────┤                          │  │
│  │ action: Action (ASK/REPORT/STOP/..) │                          │  │
│  │ dimension: AnalyzedDimension        │                          │  │
│  │ priority: int                       │                          │  │
│  │ explanation: String                 │                          │  │
│  │ metadata: Map<String,Object>        │                          │  │
│  ├─────────────────────────────────────┤                          │  │
│  │ + ask(dim,p,exp): ConvDecision      │                          │  │
│  │ + generateReport(exp): ConvDecision │                          │  │
│  │ + stop(exp): ConvDecision           │                          │  │
│  │ + toStrategySnippet(): String       │                          │  │
│  └─────────────────────────────────────┘                          │  │
│                                                                   │  │
│  ┌──────────────────────────────────┐   ┌───────────────────────┐ │  │
│  │  CompletenessEvaluation (VO)     │   │  EvaluationPolicies   │ │  │
│  ├──────────────────────────────────┤   │  (VO - record)        │ │  │
│  │ coveragePercent: double          │   │ minCoverage, minConf  │ │  │
│  │ missingDimensions: List<Dim>     │   │ minDepth, minCritDim  │ │  │
│  │ confidenceScore: double          │   └───────────────────────┘ │  │
│  │ maturityLevel: MaturityLevel     │                              │  │
│  │ viabilityLevel: ViabilityLevel   │                              │  │
│  │ recommendationLevel: RecommLevel │                              │  │
│  └──────────────────────────────────┘                              │  │
│                                                                   │  │
│  ┌──────────────────────────────────┐   ┌───────────────────────┐ │  │
│  │  ScoringEngine (Domain Service)  │   │  ScoreResult (VO)    │ │  │
│  ├──────────────────────────────────┤   ├───────────────────────┤ │  │
│  │ + evaluate(ctx, eval): ScoreRes  │   │ totalScore, maxScore  │ │  │
│  │                                   │   │ viabilityLabel        │ │  │
│  └──────────────────────────────────┘   │ strengths/weaknesses  │  │  │
│                                         └───────────────────────┘ │  │
│                                                                   │  │
│  ┌──────────────────────────────────────────────┐                  │  │
│  │  Message (VO - record)                       │                  │  │
│  │  role: String, content: String               │                  │  │
│  └──────────────────────────────────────────────┘                  │  │
│                                                                   │  │
│  ┌─────────────────────────────────────┐                          │  │
│  │  <<interface>> ContextAnalyzerPort  │                          │  │
│  │  + analyze(msg, ctx): AnalysisRes   │                          │  │
│  └─────────────────────────────────────┘                          │  │
│                                                                   │  │
│  ┌─────────────────────────────────────┐                          │  │
│  │  <<interface>> DomainEvent          │                          │  │
│  │  + type(): String                   │                          │  │
│  │  + aggregateId(): Object            │                          │  │
│  └─────────────────────────────────────┘                          │  │
│                                                                   │  │
│  ┌─────────────────────────────────────┐                          │  │
│  │  <<interface>> DomainEventBus       │                          │  │
│  │  + publish(event)                   │                          │  │
│  │  + subscribe(type, handler)         │                          │  │
│  └─────────────────────────────────────┘                          │  │
└────────────────────────────────────────────────────────────────────┘
```

### 6.2 UML del Pipeline

```
┌──────────────────────────────────────────────────────────────┐
│                       PIPELINE                                 │
│                                                                │
│  ┌─────────────────┐                                          │
│  │  PipelineStage   │<<interface>>                             │
│  ├─────────────────┤                                          │
│  │ + name(): String                                            │
│  │ + supports(ctx): boolean                                    │
│  │ + execute(ctx): PipelineCtx                                 │
│  └────────┬────────┘                                          │
│           │ implements                                         │
│     ┌─────┼─────┬──────┬──────┬──────┬───────┐                │
│     ▼     ▼     ▼      ▼      ▼      ▼       ▼                │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌──────┐         │
│  │Ana-│ │Eval│ │Stra│ │Con-│ │Sco-│ │Even│ │(fut) │         │
│  │lyz-│ │uato│ │tegi│ │sult│ │ring│ │tSt-│ │      │         │
│  │er  │ │r   │ │st  │ │or  │ │    │ │age │ │      │         │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └──────┘         │
│                                                                │
│  ┌──────────────────────────────────────────────┐              │
│  │  PipelineContext (flujo entre stages)        │              │
│  ├──────────────────────────────────────────────┤              │
│  │ projectId, userId, userMessage               │              │
│  │ history: List<Message>                       │              │
│  │ projectTitle, projectDescription, cat        │              │
│  │ projectContext: ProjectContext               │ ← Analyzer   │
│  │ evaluation: CompletenessEvaluation           │ ← Evaluator  │
│  │ decision: ConversationDecision               │ ← Strategist │
│  │ aiResponse: String                           │ ← Consultor  │
│  │ scoreResult: ScoreResult                     │ ← Scoring    │
│  │ events: List<DomainEvent>                    │ ← Event      │
│  │ attributes: Map<String,Object>               │ (extensible) │
│  │ completed: boolean                           │              │
│  └──────────────────────────────────────────────┘              │
│                                                                │
│  Orden de ejecución:                                           │
│  Analyzer → Evaluator → Strategist → Consultor → Scoring → Event│
└──────────────────────────────────────────────────────────────┘
```

### 6.3 UML del Método KIN

```
┌─────────────────────────────────────────────────────────────┐
│                        KIN METHOD                             │
│                                                               │
│  KinMethodCommand ──→ KinMethod ──→ Pipeline ──→ KinMethodResult
│  (record)              │               │           (record)
│                        │               │
│                        │               ▼
│                        │         PipelineContext
│                        │               │
│                        ▼               ▼
│                   DomainEventBus ←─── events ──────┐
│                        │                           │
│                        ▼                           ▼
│                   Event handlers           (subscribers)
│                   (futuro: async)          (logging, etc.)
│                                                               │
│  FLUJO COMPLETO:                                               │
│  1. ChatOrchestratorServiceImpl recibe ChatRequest            │
│  2. Guarda mensaje del usuario (ChatMessage)                  │
│  3. Carga historial de conversación                           │
│  4. Construye KinMethodCommand                                │
│  5. kinMethod.execute(command)                                │
│     a. PipelineContext creado desde command                    │
│     b. Pipeline ejecuta todos los stages                      │
│        - AnalyzerStage: llama ContextAnalyzerPort             │
│        - EvaluatorStage: CompletenessEvaluator.evaluate()     │
│        - StrategistStage: ConversationStrategist.decide()     │
│        - ConsultorStage: AiEngineService.generateResponse()   │
│        - ScoringStage: ScoringEngine.evaluate()               │
│        - EventStage: recolecta DomainEvents                    │
│     c. Events publicados en DomainEventBus                    │
│  6. KinMethodResult devuelto                                  │
│  7. ChatOrchestratorServiceImpl guarda mensaje asistente      │
│  8. ChatResponse devuelto al cliente                          │
└─────────────────────────────────────────────────────────────┘
```

### 6.4 UML de Componentes

```
┌────────────────────────────────────────────────────────────────┐
│                     COMPONENT DIAGRAM                            │
│                                                                  │
│  ┌─────────┐     ┌──────────┐     ┌──────────┐                  │
│  │  REST    │────→│Orchestra-│────→│KinMethod │                  │
│  │  Chat    │     │torService│     │  +       │                  │
│  │Controller│     │  Impl    │     │Pipeline  │                  │
│  └─────────┘     └──────────┘     └────┬─────┘                  │
│                                        │                         │
│                   ┌────────────────────┼─────────────┐           │
│                   ▼                    ▼             ▼           │
│  ┌──────────────────┐    ┌─────────────────┐  ┌──────────┐      │
│  │  AiEngineService  │    │  DomainEventBus  │  │ScoringEng│      │
│  │  + ProviderRouter │    │  + InMemoryImpl  │  │  ine     │      │
│  └────────┬─────────┘    └─────────────────┘  └──────────┘      │
│           │                                                      │
│           ▼                                                      │
│  ┌──────────────────┐                                           │
│  │  ProviderRouter   │                                           │
│  └──┬───────────┬──┘                                           │
│     ▼           ▼                                               │
│  ┌────────┐ ┌────────┐                                          │
│  │DeepSeek│ │OpenAI  │                                          │
│  │Provider│ │Provider│                                          │
│  └────────┘ └────────┘                                          │
│                                                                  │
│  Servicios de infraestructura (Spring-managed):                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │ProjectContext │  │Completeness  │  │Conversation  │           │
│  │   Service     │  │  Evaluator   │  │  Strategist  │           │
│  └──────────────┘  └──────────────┘  └──────────────┘           │
└──────────────────────────────────────────────────────────────────┘
```

### 6.5 UML de Paquetes

```
┌──────────────────────────────────────────────────────────────┐
│                    PACKAGE DIAGRAM                              │
│                                                                │
│  ┌─────────────────────────────────────────────────────┐      │
│  │                  com.kinplatform                       │      │
│  │                                                       │      │
│  │  ┌──────────────────────────────────────────────┐    │      │
│  │  │  kin (DOMINIO PURO — sin Spring, sin JPA)    │    │      │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────────┐ │    │      │
│  │  │  │context/  │ │decision/ │ │pipeline/     │ │    │      │
│  │  │  │  VO/AR   │ │  VO      │ │  stage/      │ │    │      │
│  │  │  │  Service │ │          │ │  Pipeline,   │ │    │      │
│  │  │  │  Port    │ │          │ │  PipelineCtx │ │    │      │
│  │  │  └──────────┘ └──────────┘ └──────────────┘ │    │      │
│  │  │  ┌──────────┐ ┌──────────┐                   │    │      │
│  │  │  │event/    │ │scoring/  │                   │    │      │
│  │  │  │  Bus     │ │  Service │                   │    │      │
│  │  │  │  Events  │ │  Model   │                   │    │      │
│  │  │  └──────────┘ └──────────┘                   │    │      │
│  │  └──────────────────────────────────────────────┘    │      │
│  │                                                       │      │
│  │  ┌──────────────────────────────────────────────┐    │      │
│  │  │  ai (APPLICATION + INFRASTRUCTURE)            │    │      │
│  │  │  ┌──────────────┐ ┌──────────────────────┐   │    │      │
│  │  │  │provider/     │ │context/              │   │    │      │
│  │  │  │  Router      │ │  ProjectContextSvc   │   │    │      │
│  │  │  │  AIProvider  │ │  adapter/            │   │    │      │
│  │  │  │  DeepSeek    │ │  HeuristicAnalyzer   │   │    │      │
│  │  │  │  OpenAI      │ └──────────────────────┘   │    │      │
│  │  │  └──────────────┘                             │    │      │
│  │  │  ┌──────────────────────┐                     │    │      │
│  │  │  │  AiEngineService     │                     │    │      │
│  │  │  └──────────────────────┘                     │    │      │
│  │  └──────────────────────────────────────────────┘    │      │
│  │                                                       │      │
│  │  ┌──────────────────────────────────────────────┐    │      │
│  │  │  chat (APPLICATION + INFRASTRUCTURE)          │    │      │
│  │  │  ChatService, ChatOrchestratorService         │    │      │
│  │  │  ChatMessage (JPA), Repositories, DTOs        │    │      │
│  │  └──────────────────────────────────────────────┘    │      │
│  │                                                       │      │
│  │  ┌──────────────────────────────────────────────┐    │      │
│  │  │  common/config (INFRASTRUCTURE — WIRING)      │    │      │
│  │  │  KinConfig, AiContextConfig, SecurityConfig   │    │      │
│  │  └──────────────────────────────────────────────┘    │      │
│  └─────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────┘
```

---

## 7. ANÁLISIS DE RIESGO — GOD CLASSES

### 7.1 PipelineContext — RIESGO ALTO 🟠

**Síntomas actuales**: 82 líneas, 13 campos, getters/setters para cada uno. Es el bus de datos del pipeline.

**Riesgo**: Crece con cada nueva etapa del pipeline. Puede convertirse en un "God Context" que cargue con toda la información del sistema.

**Solución propuesta**:
- Los datos específicos de cada etapa deben vivir en `attributes: Map<String,Object>` en lugar de campos dedicados.
- Los campos `projectContext`, `evaluation`, `decision`, `aiResponse`, `scoreResult` son legítimos porque representan los **contratos entre etapas**. No deben eliminarse.
- **Límite**: si se agregan más de 3 campos nuevos en futuras fases, refactorizar a `Map<String,Object>` con typed accessors.

### 7.2 ProjectContext — RIESGO MEDIO 🟡

**Síntomas actuales**: 110 líneas, 5 campos, 15 métodos públicos. Mezcla estado (`data`, `dimensionsCovered`) con lógica (`computeCoverage`, `missingDimensions`).

**Riesgo**: Puede acumular demasiadas responsabilidades si se agregan más métodos de análisis.

**Solución propuesta**:
- `missingDimensions()` y `toPromptSnippet()` son cálculos derivados → podrían ser Domain Services si crecen.
- Si supera 20 métodos públicos, extraer `ProjectContextAnalyzer` como Domain Service separado.

### 7.3 ChatOrchestratorServiceImpl — RIESGO ALTO 🟠

**Síntomas actuales**: 211 líneas, dos flujos (bloqueante y streaming) con paths divergentes. Es el orquestador principal.

**Riesgo**: 
- El flujo bloqueante usa `KinMethod` (limpio).
- El flujo streaming **no usa KinMethod** — hace análisis, evaluación, decisión manualmente.  
- Esto crea dos caminos de ejecución que deben mantenerse sincronizados.

**Solución propuesta**:
- Refactorizar el flujo streaming para que también use `KinMethod` (con un callback SSE).
- Extraer la lógica de SSE en un helper `SseStreamHandler` separado.
- **Límite**: si supera 300 líneas, debe dividirse.

### 7.4 AiEngineService — RIESGO MEDIO 🟡

**Síntomas actuales**: 202 líneas, contiene el system prompt de 107 líneas inline.

**Riesgo**: El system prompt es lógica de dominio de AI que está acoplada al servicio. Cada cambio en el prompt requiere modificar la clase.

**Solución propuesta**:
- Extraer `buildSystemPrompt()` a un `PromptAssembler` separado (ya identificado como corrección pendiente).
- Extraer `buildStrategySnippet()` al mismo `PromptAssembler`.
- Una vez extraído, `AiEngineService` quedaría en ~50 líneas (estable).

### 7.5 ScoringEngine — RIESGO MEDIO 🟡

**Síntomas actuales**: `scoreDimension()` usa `length()` como heurística (0, 3, 5, 7, 10 según chars). `identifyStrengths()` y `identifyWeaknesses()` son strings fijos.

**Riesgo**: La heurística de longitud es frágil y no es auditable. Los strings de fortalezas/debilidades son difíciles de mantener.

**Solución propuesta**:
- Reemplazar heurística de longitud con evaluación semántica (futuro: AI-based).
- Extraer strings a un `ScoringDictionary` configurable.
- Agregar `ScoringExplanation` a `ScoreResult` para auditoría (corrección pendiente).

---

## 8. ESTABILIDAD

### 8.1 Componentes Estables (no deben cambiar en fases futuras)

| Componente | Razón |
|-----------|-------|
| `KinMethod` | Es la fachada principal del dominio. Su API `execute(KinMethodCommand) → KinMethodResult` es el contrato. |
| `KinMethodCommand` | Record inmutable — es el input contract. |
| `KinMethodResult` | Record inmutable — es el output contract. |
| `ConversationDecision` | Record inmutable — enum `Action` cubre todos los casos actuales. |
| `ConversationDecision.Action` | `ASK, REPORT, RECOMMEND, VALIDATE, SUMMARIZE, STOP, ESCALATE` — cubre el ciclo de vida completo. |
| `AnalyzedDimension` | Enum de 14 dimensiones. Pueden agregarse valores pero no eliminarse. |
| `DomainEvent` | Interface `type() + aggregateId()` es el contract base de eventos. |
| `DomainEventBus` | Interface `publish() + subscribe()` no cambiará. |
| `AIProvider` | Interface `generateBlocking() + generateStream()` es el contract con proveedores AI. |
| `PipelineStage` | Interface `name() + supports() + execute()` no cambiará. |
| `Pipeline` | Algoritmo de ejecución de stages no cambiará. |
| `ScoringModel` | Record inmutable con `weights + version + description`. |
| `ScoreResult` | Record inmutable con `totalScore + categoryScores + viabilityLabel + strengths + weaknesses`. |

### 8.2 Componentes en Evolución (cambiarán pero manteniendo API)

| Componente | Cambios esperados |
|-----------|-------------------|
| `ProjectContext` | Pueden agregarse métodos de consulta. No debe cambiar su API pública de actualización. |
| `CompletenessEvaluator` | Pueden refinarse los thresholds. La API `evaluate(ProjectContext) → CompletenessEvaluation` se mantiene. |
| `ConversationStrategist` | Pueden agregarse nuevas estrategias. API `decide(ctx, eval) → ConversationDecision` se mantiene. |
| `ScoringEngine` | Se reemplazará heurística de longitud. API `evaluate(ctx, eval) → ScoreResult` se mantiene. |
| `PipelineContext` | Se monitoreará contra God Class. API de acceso a campos se mantiene. |
| `EventStage` | Actualmente siempre dispara `ConversationCompleted`. Debe corregirse para disparar eventos basados en el flujo real. |

### 8.3 Componentes Experimentales (pueden cambiar significativamente)

| Componente | Razón |
|-----------|-------|
| `InMemoryDomainEventBus` | Implementación temporal. Será reemplazada por una versión con soporte async y persistencia. |
| `HeuristicContextAnalyzerAdapter` | Análisis basado en regex. Será reemplazado por NLP/AI más adelante. |
| `ChatOrchestratorServiceImpl` flujo streaming | Debe refactorizarse para usar KinMethod. Es el componente más inestable actualmente. |

---

## 9. ROADMAP

### KIN 2.0 — Fase actual (Correcciones arquitectónicas)

**Estado**: En progreso ✅

- [x] Domain restructured (`kin/context/` owns domain types)
- [x] JPA dependency removed from domain (`Message` record)
- [x] ProviderRouter OCP-compliant
- [ ] Provider deduplication (`AbstractAIProvider`)
- [ ] `PromptAssembler` extraction
- [ ] Pipeline error handling
- [ ] Scoring audit trail
- [ ] EventBus async abstraction
- [ ] Streaming path refactored to use `KinMethod`
- [ ] Unit tests for all corrections

### KIN 2.1 — Pipeline Estabilizado

**Objetivo**: Pipeline completamente funcional y testeado en ambos flujos.

- [ ] Pipeline error handling with retry/fail strategies
- [ ] EventStage fires correct events (not always ConversationCompleted)
- [ ] Pipeline timeout per stage
- [ ] Pipeline metrics (stage duration, success/failure rates)
- [ ] Streaming path uses KinMethod
- [ ] All 6 pipeline stages have unit tests
- [ ] Integration test: ChatController → Orchestrator → KinMethod → Pipeline → DB

### KIN 2.2 — Report Engine

**Objetivo**: Reportes estructurados de viabilidad.

- [ ] `ReportEngine` Domain Service
- [ ] `Report` Aggregate Root (sections, metadata)
- [ ] `ReportSection` value objects
- [ ] `ReportRenderer` port (HTML, PDF, Markdown adapters)
- [ ] Report persistence
- [ ] Report API endpoints (GET /projects/{id}/reports)
- [ ] System prompt simplification (mover lógica de generación de reporte a ReportEngine)

### KIN 2.3 — AI Provider Maturity

**Objetivo**: Proveedores AI robustos y extensibles.

- [ ] `AbstractAIProvider` base class (timeout, retry, logging)
- [ ] Provider health check / circuit breaker
- [ ] Provider fallback with escalation
- [ ] Token usage tracking per provider
- [ ] Cost tracking per project
- [ ] Prompt versioning (PromptAssembler v1 → v2)

### KIN 2.4 — Event-Driven & Async

**Objetivo**: Event Bus con soporte async y persistencia.

- [ ] AsyncDomainEventBus (Spring Events o RabbitMQ)
- [ ] Event persistence (outbox pattern)
- [ ] Event replay capability
- [ ] Event handlers for:
  - Notification when report ready
  - Context save on conversation milestone
  - Scoring audit trail

### KIN 2.5 — Context Analyzer NLP

**Objetivo**: Reemplazar análisis heurístico con NLP.

- [ ] AI-powered AnalyzerStage (usa LLM para extraer dimensiones)
- [ ] Confidence scoring per extracted dimension
- [ ] Multi-language support (español + inglés)
- [ ] Fallback: heurístico → NLP si falla

### KIN 3.0 — Platform Ready

**Objetivo**: Plataforma completa y estable.

- [ ] Knowledge Engine (base de conocimiento)
- [ ] Recommendation Engine (recomendaciones basadas en proyectos similares)
- [ ] Full async architecture
- [ ] Comprehensive metrics and monitoring
- [ ] Plugin system for custom pipeline stages
- [ ] Multi-tenant support
- [ ] Complete separation into 7 Bounded Contexts

---

## 10. REGLAS ABSOLUTAS

### 10.1 Reglas de dominio

1. **`kin/` NO debe importar nada de `com.kinplatform.*` excepto `kin.*` mismo.**
   - Excepción: `java.util.*`, `org.slf4j` (logging es infraestructura aceptable en Java).
2. **`kin/` NO debe tener anotaciones Spring:** ni `@Service`, `@Component`, `@Autowired`, `@Transactional`.
3. **`kin/` NO debe tener dependencias JPA:** ni `@Entity`, `@Table`, `@Column`, `@ManyToOne`.
4. **Toda comunicación entre Bounded Contexts debe ser por eventos o por interfaces en el dominio (puertos).**
5. **Los Value Objects deben ser inmutables:** usar `record` de Java o clases con constructor que copia defensivamente.
6. **`PipelineContext` es la única excepción a la inmutabilidad** — es mutable por diseño (flujo de datos entre stages).

### 10.2 Reglas de aplicación

7. **Los Application Services orquestan, no implementan lógica de dominio.**
8. **Un Application Service debe delegar en Domain Services, no contener `if/switch` que evalúe reglas de negocio.**
9. **Los DTOs viven en el paquete de aplicación (`dto/`), no en el dominio.**
10. **`ChatOrchestratorServiceImpl` DEBE usar `KinMethod` en ambos flujos (bloqueante y streaming).**

### 10.3 Reglas de infraestructura

11. **Los adapters implementan puertos del dominio, no al revés.**
12. **Los repositorios JPA son infraestructura. Su interfaz (si se define) debe estar en el dominio o en aplicación.**
13. **La configuración de Spring (`@Configuration`, `@Bean`) está en `common/config/`.**
14. **No hay `@Autowired` en field injection — solo constructor injection.**

### 10.4 Reglas de evolución

15. **Ningún componente nuevo puede violar esta arquitectura sin una ADR aprobada.**
16. **Cada nueva fase debe:**
    - Mantener compatibilidad de API REST
    - Mantener compatibilidad SSE streaming
    - No romper tests existentes
    - Agregar tests para nuevo código
17. **Cuando un componente alcanza el estado "Estable" (sección 8.1), su API pública no puede cambiar.**
18. **La heurística de longitud en ScoringEngine debe reemplazarse antes de KIN 2.5.**

---

*Documento generado el 30 de julio de 2026.*
*Versión: KIN 2.0-ARCH-001*
*Próxima revisión: al completar KIN 2.1*
