# FASE 5 — Consolidación Arquitectónica Pre-Implementación

> Revisión final de la arquitectura antes de comenzar la implementación.
> Este documento modifica y refina el diseño de FASE5_DISENO_ARQUITECTONICO.md.
> Una vez aprobado, se convierte en el contrato definitivo para la implementación.

---

## ÍNDICE

1. [ConsultationResult — Contrato Único de Salida](#1-consultationresult)
2. [PipelineContext Rediseñado](#2-pipelinecontext-rediseñado)
3. [EngineRegistry y EngineExecutor](#3-engine-registry-y-executor)
4. [ReportRendererFactory](#4-reportrendererfactory)
5. [PromptAssembler Architecture](#5-promptassembler-architecture)
6. [Versionado de Reportes](#6-versionado-de-reportes)
7. [Modelo de Auditabilidad (Explanations)](#7-modelo-de-auditabilidad)
8. [Auditoría Arquitectónica Final](#8-auditoría-arquitectónica-final)
9. [Plan de Migración](#9-plan-de-migración)
10. [Documentos Resultantes](#10-documentos-resultantes)

---

## 1. CONSULTATIONRESULT

### 1.1 Problema

El diseño actual de Fase 5 tiene `PipelineContext` transportando múltiples objetos independientes (`recommendationSet`, `riskAssessment`, `opportunitySet`, `consultingReport`). Esto:

- Acopla los stages al conocer la estructura interna de `PipelineContext`
- Dificulta la evolución hacia Fase 6+ (cada nuevo engine agrega un campo)
- No hay un contrato formal de salida del proceso de consultoría

### 1.2 Solución: ConsultationResult

`ConsultationResult` es el **contrato único de salida** del proceso de consultoría. Representa el resultado completo de evaluar un proyecto.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      ConsultationResult                               │
│                                                                       │
│  Propósito: Representa el resultado COMPLETO del proceso de           │
│  consultoría para un proyecto. Es el contrato entre la Fase 5        │
│  y las Fases 6 y 7.                                                   │
│                                                                       │
│  Inmutable: Sí — record de Java                                       │
│  Ubicación: com.kinplatform.kin.reporting                             │
│  Ciclo de vida: Se construye al final del pipeline y se devuelve     │
│                 en KinMethodResult.                                   │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  public record ConsultationResult(                             │   │
│  │      UUID projectId,                                           │   │
│  │      UUID userId,                                              │   │
│  │      ConsultationMetadata metadata,                            │   │
│  │      ProjectContext projectContext,                            │   │
│  │      CompletenessEvaluation evaluation,                        │   │
│  │      ConversationDecision decision,                            │   │
│  │      ScoreResult score,                                        │   │
│  │      RecommendationResult recommendations,                     │   │
│  │      RiskResult risks,                                         │   │
│  │      OpportunityResult opportunities,                          │   │
│  │      ConsultingReport report,                                  │   │
│  │      List<DomainEvent> events                                  │   │
│  │  ) {                                                            │   │
│  │      public static ConsultationResult empty() { ... }          │   │
│  │      public boolean isComplete() { ... }                        │   │
│  │  }                                                              │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Beneficios

| Problema actual | Solución con ConsultationResult |
|----------------|--------------------------------|
| PipelineContext transporta datos no relacionados con la ejecución | PipelineContext solo tiene datos de **ejecución**. ConsultationResult tiene datos de **negocio**. |
| Stages deben escribir en PipelineContext | Stages escriben en un builder de ConsultationResult. |
| KinMethodResult debe extraer campos de PipelineContext | KinMethodResult contiene directamente ConsultationResult. |
| Fase 6+ agrega más campos a PipelineContext | ConsultationResult se expande. PipelineContext se congela. |
| No hay un contrato formal de salida | ConsultationResult ES el contrato. Documentado, versionado, testeable. |

### 1.4 PipelineContext vs ConsultationResult

```
PipelineContext (solo ejecución):
────────────────────────────────
- projectId, userId
- userMessage, history
- projectTitle, projectDescription, projectCategory
- currentStage
- completed
- attributes: Map<String,Object>
- errors: List<PipelineError>        (NUEVO)
- stageResults: Map<String,Object>   (NUEVO — resultados intermedios)

ConsultationResult (solo negocio):
─────────────────────────────────
- projectId, userId
- metadata (version, generatedAt, versions de cada engine)
- projectContext
- evaluation
- decision
- score
- recommendations
- risks
- opportunities
- report
- events
```

### 1.5 Impacto en KinMethodResult

```
KinMethodResult (Fase 5 — ACTUALIZADO):

public record KinMethodResult(
    ConsultationResult consultation   // ← ÚNICO campo de negocio
) {
    // Métodos de conveniencia (delegados):
    public ProjectContext projectContext() { return consultation.projectContext(); }
    public ConversationDecision decision() { return consultation.decision(); }
    public String aiResponse() { return consultation.report() != null
        ? consultation.report().llmExplanation() : null; }
    public ScoreResult score() { return consultation.score(); }
    public List<DomainEvent> events() { return consultation.events(); }
}
```

---

## 2. PIPELINECONTEXT REDISEÑADO

### 2.1 Arquitectura de Tres Capas

```
PipelineContext se divide en tres objetos con responsabilidades claras:

┌─────────────────────────────────────────────────────────────────────┐
│                      PIPELINE EXECUTION MODEL                        │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  ExecutionMetadata (inmutable — datos de contexto fijos)  │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - projectId: UUID                                       │       │
│  │  - userId: UUID                                          │       │
│  │  - userMessage: String                                   │       │
│  │  - history: List<Message>                                │       │
│  │  - projectTitle: String                                  │       │
│  │  - projectDescription: String                            │       │
│  │  - projectCategory: String                               │       │
│  │  - startedAt: OffsetDateTime                              │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      │ (se pasa al constructor y nunca cambia)      │
│                      ▼                                              │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PipelineContext (mutable — solo estado de ejecución)     │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - currentStage: String                                  │       │
│  │  - completed: boolean                                    │       │
│  │  - error: Optional<PipelineError>                        │       │
│  │  - stageResults: Map<String, Object>                     │       │
│  │  - attributes: Map<String, Object>                       │       │
│  │                                                          │       │
│  │  Métodos:                                                │       │
│  │  + setStageResult(name, value)                           │       │
│  │  + <T> getStageResult(name, type): Optional<T>           │       │
│  │  + markCompleted()                                       │       │
│  │  + fail(error)                                           │       │
│  │  + executionMetadata(): ExecutionMetadata                 │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      │ (los stages leen/escriben aquí)              │
│                      ▼                                              │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  ConsultationResult.Builder (mutable — construcción del  │       │
│  │  resultado de negocio)                                    │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  + projectContext(ctx)                                    │       │
│  │  + evaluation(eval)                                       │       │
│  │  + decision(decision)                                     │       │
│  │  + score(score)                                           │       │
│  │  + recommendations(recs)                                  │       │
│  │  + risks(risks)                                           │       │
│  │  + opportunities(opps)                                    │       │
│  │  + report(report)                                         │       │
│  │  + addEvent(event)                                        │       │
│  │  + build(): ConsultationResult                            │       │
│  └──────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Implementación Propuesta

```java
// ─── ExecutionMetadata (inmutable) ───
public record ExecutionMetadata(
    UUID projectId,
    UUID userId,
    String userMessage,
    List<Message> history,
    String projectTitle,
    String projectDescription,
    String projectCategory,
    OffsetDateTime startedAt
) {}

// ─── PipelineContext (mutable — solo ejecución) ───
public class PipelineContext {
    private final ExecutionMetadata executionMetadata;
    private final ConsultationResult.Builder resultBuilder;
    private String currentStage;
    private boolean completed;
    private PipelineError error;
    private final Map<String, Object> stageResults = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();

    public PipelineContext(ExecutionMetadata metadata) {
        this.executionMetadata = metadata;
        this.resultBuilder = ConsultationResult.builder()
            .projectId(metadata.projectId())
            .userId(metadata.userId());
    }

    // Execution metadata (inmutable — siempre disponible)
    public ExecutionMetadata executionMetadata() { return executionMetadata; }
    public UUID projectId() { return executionMetadata.projectId(); }
    // ... otros delegados

    // Consultation builder (para stages que producen datos de negocio)
    public ConsultationResult.Builder resultBuilder() { return resultBuilder; }

    // Stage execution
    public void currentStage(String stage) { this.currentStage = stage; }
    public String currentStage() { return currentStage; }
    public void markCompleted() { this.completed = true; }
    public boolean completed() { return completed; }
    public void fail(PipelineError error) { this.error = error; this.completed = true; }
    public Optional<PipelineError> error() { return Optional.ofNullable(error); }

    // Stage results (para datos intermedios entre stages)
    public void setStageResult(String key, Object value) { stageResults.put(key, value); }
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getStageResult(String key, Class<T> type) {
        var value = stageResults.get(key);
        if (type.isInstance(value)) return Optional.of((T) value);
        return Optional.empty();
    }

    // Generic attributes (para casos no previstos)
    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        var value = attributes.get(key);
        if (type.isInstance(value)) return Optional.of((T) value);
        return Optional.empty();
    }
}

// ─── PipelineError (inmutable) ───
public record PipelineError(
    String stage,
    String message,
    String exceptionType,
    String exceptionMessage
) {}

// ─── Pipeline (actualizado) ───
public class Pipeline {
    public PipelineContext execute(ExecutionMetadata metadata) {
        var ctx = new PipelineContext(metadata);
        // ... ejecución de stages ...
        return ctx;
    }
}
```

### 2.3 ¿Qué hace cada Stage ahora?

```
// Stage existente (ej: AnalyzerStage):
public PipelineContext execute(PipelineContext ctx) {
    var result = analyzer.analyze(ctx.userMessage(), /* ... */);
    ctx.resultBuilder().projectContext(updatedContext);
    ctx.setStageResult("analysis", result);
    return ctx;
}

// Stage nuevo (ej: RecommendationStage):
public PipelineContext execute(PipelineContext ctx) {
    var projectCtx = ctx.resultBuilder().getProjectContext(); // ← lee del builder
    var score = ctx.resultBuilder().getScore();
    var recommendations = recommendationEngine.evaluate(projectCtx, score);
    ctx.resultBuilder().recommendations(recommendations);
    return ctx;
}
```

### 2.4 Beneficios

| Riesgo original | Mitigación |
|----------------|------------|
| PipelineContext God Class | Dividido en 3 objetos. ExecutionMetadata inmutable. PipelineContext solo ejecución. ConsultationResult.Builder solo negocio. |
| Crecimiento indefinido | Nuevos engines = nuevos métodos en ConsultationResult.Builder, no nuevos campos en PipelineContext. |
| Stages escriben datos de negocio en contexto de ejecución | Ahora escriben en el builder específico. Separación clara de responsabilidades. |
| Dificultad para testear | PipelineContext se construye con metadata simple. ConsultationResult.Builder se testea independientemente. |

---

## 3. ENGINE REGISTRY Y EXECUTOR

### 3.1 Problema

Actualmente `KinMethod` conoce todos los engines. Cada nuevo engine requiere modificar `KinMethod` o el pipeline. Esto viola OCP.

### 3.2 Solución: EngineRegistry + EngineExecutor

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ENGINE REGISTRY PATTERN                            │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  <<interface>> DomainEngine<E, R>                        │       │
│  │  ├──────────────────────────────────────────────────────┤       │
│  │  + engineName(): String                                  │       │
│  │  + evaluate(E input): R                                  │       │
│  │  + supportedPhase(): EnginePhase                         │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      ▲ implements                                     │
│       ┌──────────────┼──────────────┐                               │
│       ▼              ▼              ▼                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐                        │
│  │Scoring   │ │Recommen- │ │   Risk       │                        │
│  │Engine    │ │dation    │ │   Engine     │                        │
│  │          │ │Engine    │ │              │                        │
│  └──────────┘ └──────────┘ └──────────────┘                        │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  EngineRegistry (Application Service)                    │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - engines: Map<String, DomainEngine<?,?>>               │       │
│  │  + register(engine): void                                │       │
│  │  + get(name): Optional<DomainEngine>                     │       │
│  │  + getAllByPhase(phase): List<DomainEngine>              │       │
│  │  + engineNames(): Set<String>                            │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      │                                                 │
│                      ▼                                                 │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  EngineExecutor (Domain Service)                         │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - registry: EngineRegistry                              │       │
│  │  + executePhase(phase, ctx): void                        │       │
│  │  + executeSingle(engineName, ctx): Optional<R>           │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  EnginePhase (enum)                                      │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  ANALYSIS, EVALUATION, STRATEGY, CONSULTATION,           │       │
│  │  SCORING, RECOMMENDATION, RISK, OPPORTUNITY,             │       │
│  │  KNOWLEDGE, INNOVATION, COMPETITION, FINANCIAL,          │       │
│  │  MARKET, VALIDATION, REPORTING, EXPLANATION              │       │
│  └──────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.3 Interfaces

```java
// ─── DomainEngine (contrato universal para cualquier engine) ───
public interface DomainEngine<E, R> {
    String engineName();
    EnginePhase supportedPhase();
    R evaluate(E input);
}

// ─── EngineRegistry ───
public class EngineRegistry {
    private final Map<String, DomainEngine<?, ?>> engines = new LinkedHashMap<>();

    public void register(DomainEngine<?, ?> engine) {
        engines.put(engine.engineName(), engine);
    }

    @SuppressWarnings("unchecked")
    public <E, R> Optional<DomainEngine<E, R>> get(String name) {
        var engine = engines.get(name);
        if (engine != null) return Optional.of((DomainEngine<E, R>) engine);
        return Optional.empty();
    }

    public List<DomainEngine<?, ?>> getAllByPhase(EnginePhase phase) {
        return engines.values().stream()
            .filter(e -> e.supportedPhase() == phase)
            .toList();
    }

    public Set<String> engineNames() {
        return engines.keySet();
    }
}

// ─── EngineExecutor ───
public class EngineExecutor {
    private final EngineRegistry registry;

    public EngineExecutor(EngineRegistry registry) {
        this.registry = registry;
    }

    public void executePhase(EnginePhase phase, PipelineContext ctx) {
        var engines = registry.getAllByPhase(phase);
        for (var engine : engines) {
            execute(engine, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private <E, R> void execute(DomainEngine<?, ?> engine, PipelineContext ctx) {
        var phase = engine.supportedPhase();
        var name = engine.engineName();

        try {
            // Cada engine define su input根据 el contexto
            // Esto puede refinarse — ver sección 3.4
            var input = buildInput(phase, ctx);
            var result = ((DomainEngine<E, R>) engine).evaluate((E) input);
            storeResult(phase, name, result, ctx);
        } catch (Exception e) {
            ctx.fail(new PipelineError(name, e.getMessage(),
                e.getClass().getName(), e.getMessage()));
        }
    }

    private Object buildInput(EnginePhase phase, PipelineContext ctx) {
        return switch (phase) {
            case SCORING -> new ScoringInput(
                ctx.resultBuilder().getProjectContext(),
                ctx.resultBuilder().getEvaluation()
            );
            case RECOMMENDATION -> new RecommendationInput(
                ctx.resultBuilder().getProjectContext(),
                ctx.resultBuilder().getScore()
            );
            case RISK -> new RiskInput(
                ctx.resultBuilder().getProjectContext()
            );
            // ...
        };
    }

    private void storeResult(EnginePhase phase, String name, Object result, PipelineContext ctx) {
        var builder = ctx.resultBuilder();
        switch (phase) {
            case SCORING -> builder.score((ScoreResult) result);
            case RECOMMENDATION -> builder.recommendations((RecommendationResult) result);
            case RISK -> builder.risks((RiskResult) result);
            // ...
        }
        ctx.setStageResult(name, result);
    }
}
```

### 3.4 Integración con el Pipeline

El pipeline ya no tiene stages fijos para cada engine. En su lugar:

```
// KinConfig.java:

@Bean
public EngineRegistry engineRegistry(
    List<DomainEngine<?, ?>> engines  // Spring auto-descubre todos los DomainEngine
) {
    var registry = new EngineRegistry();
    engines.forEach(registry::register);
    return registry;
}

@Bean
public EngineExecutor engineExecutor(EngineRegistry registry) {
    return new EngineExecutor(registry);
}

// El pipeline tiene un solo "EngineStage" genérico que ejecuta
// todos los engines registrados para la fase actual:
//
// Pipeline:
// 1. AnalyzerStage (existente)
// 2. EvaluatorStage (existente)
// 3. StrategistStage (existente)
// 4. ConsultorStage (existente)
// 5. EnginePhase.SCORING → ejecuta ScoringEngine automáticamente
// 6. EnginePhase.RECOMMENDATION → ejecuta RecommendationEngine
// 7. EnginePhase.RISK → ejecuta RiskEngine
// 8. EnginePhase.OPPORTUNITY → ejecuta OpportunityEngine
// 9. ReportStage
// 10. LlmExplanationStage
// 11. EventStage
```

### 3.5 Preparación para KIN 3.0

Para agregar un nuevo engine en KIN 3.0 (ej: `KnowledgeEngine`):

1. Implementar `DomainEngine<KnowledgeInput, KnowledgeResult>`
2. Anotar con `@Component`
3. Spring lo registra automáticamente en `EngineRegistry`
4. `EngineExecutor` lo ejecuta en la fase `KNOWLEDGE`
5. El pipeline no requiere modificación

**Sin EngineRegistry**: habría que crear un nuevo PipelineStage, agregarlo al Pipeline en KinConfig, y extender PipelineContext.

### 3.6 DomainEngine — Adaptación de Engines Existentes

```
// ScoringEngine se adapta al contrato DomainEngine:

@Component
public class ScoringEngineAdapter implements DomainEngine<ScoringInput, ScoreResult> {
    private final ScoringEngine scoringEngine;

    // Delegados:
    public String engineName() { return "scoring"; }
    public EnginePhase supportedPhase() { return EnginePhase.SCORING; }
    public ScoreResult evaluate(ScoringInput input) {
        return scoringEngine.evaluate(input.context(), input.evaluation());
    }
}

// El ScoringEngine original (puro, sin Spring) NO cambia.
// Solo se agrega un adapter @Component.
```

---

## 4. REPORT RENDERERFACTORY

### 4.1 Problema

Originalmente `ReportEngine` o un controller debía seleccionar el renderer mediante `if/switch` según el formato. Esto viola OCP.

### 4.2 Solución: RendererRegistry

```java
// ─── ReportRenderer (port — sin cambios) ───
public interface ReportRenderer {
    String render(ConsultingReport report);
    String formatName(); // "markdown", "html", "pdf", "json", "docx"
}

// ─── RendererRegistry (reemplaza a RendererFactory) ───
@Component
public class RendererRegistry {
    private final Map<String, ReportRenderer> renderers = new LinkedHashMap<>();

    public RendererRegistry(List<ReportRenderer> rendererList) {
        rendererList.forEach(r -> renderers.put(r.formatName(), r));
    }

    public ReportRenderer forFormat(String format) {
        var renderer = renderers.get(format.toLowerCase());
        if (renderer == null) {
            throw new IllegalArgumentException(
                "No renderer registered for format: " + format
                + ". Available: " + renderers.keySet());
        }
        return renderer;
    }

    public Set<String> availableFormats() {
        return renderers.keySet();
    }
}

// ─── Uso en controller ───
@GetMapping("/projects/{id}/report")
public ResponseEntity<String> getReport(
    @PathVariable UUID id,
    @RequestParam(defaultValue = "markdown") String format
) {
    var report = reportService.getReport(id);
    var renderer = rendererRegistry.forFormat(format);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(renderer.contentType()))
        .body(renderer.render(report));
}
```

### 4.3 Beneficios

- Agregar un nuevo renderer = crear clase que implemente `ReportRenderer` + `@Component`
- Spring lo registra automáticamente en `RendererRegistry`
- No requiere modificar ningún condicional
- El controller no sabe qué renderers existen

---

## 5. PROMPTASSEMBLER ARCHITECTURE

### 5.1 Prohibición

> **A partir de Fase 5, ningún servicio puede construir prompts manualmente concatenando strings.**
> Todo prompt debe pasar por `PromptAssembler`.

### 5.2 Arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                      PROMPT ASSEMBLER SYSTEM                          │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PromptTemplate (VO — inmutable)                         │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - templateId: String                                    │       │
│  │  - version: String                                       │       │
│  │  - content: String (template con placeholders)            │       │
│  │  - variables: Set<String> (placeholders esperados)        │       │
│  │  - category: PromptCategory                               │       │
│  │  + compile(variables): PromptResult                       │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      │                                                 │
│                      ▼                                                 │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PromptVariables (VO — inmutable)                        │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  Un record por tipo de prompt:                            │       │
│  │  - ConversationPromptVars(projectCtx, decision, history)  │       │
│  │  - ReportExplanationVars(consultingReport)                │       │
│  │  - ReportPromptVars(projectTitle, desc, category)        │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      │                                                 │
│                      ▼                                                 │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PromptAssembler (Application Service)                   │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - templateLoader: PromptTemplateLoader                  │       │
│  │  + assemble(category, variables): PromptResult           │       │
│  │  + assembleReportExplanation(report): PromptResult       │       │
│  │  + assembleConversation(ctx, decision): PromptResult     │       │
│  └──────────────────────────────────────────────────────────┘       │
│                      │                                                 │
│                      ▼                                                 │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PromptResult (VO — inmutable)                           │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - systemPrompt: String                                  │       │
│  │  - userPrompt: String                                    │       │
│  │  - templateId: String                                    │       │
│  │  - templateVersion: String                               │       │
│  │  - variablesUsed: Map<String, String>                    │       │
│  │  - assembledAt: OffsetDateTime                           │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PromptTemplateLoader (infrastructure)                   │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - load(templateId): PromptTemplate                     │       │
│  │  - loadByCategory(category): PromptTemplate              │       │
│  │  • Lee templates de archivos (classpath:/prompts/*.txt)  │       │
│  │  • Soporta versionado (v1, v2)                           │       │
│  │  • Cachea templates en memoria                           │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  PromptCategory (enum)                                   │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  CONVERSATION, REPORT_EXPLANATION, REPORT_GENERATION,    │       │
│  │  SCORING_EXPLANATION, RISK_EXPLANATION                   │       │
│  └──────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.3 Templates en Archivos

Cada prompt es un archivo de texto en `src/main/resources/prompts/`:

```
resources/prompts/
├── conversation/
│   ├── v1.txt
│   └── v2.txt
├── report-explanation/
│   ├── v1.txt
│   └── v2.txt
└── scoring-explanation/
    └── v1.txt
```

Ejemplo de `resources/prompts/report-explanation/v1.txt`:

```
Eres KIN, un consultor senior en innovación y emprendimiento.

Recibiste un reporte de viabilidad estructurado. Tu tarea es explicárselo
al emprendedor en lenguaje natural, como si estuvieras en una reunión.

DATOS DEL REPORTE:
{{consultingReport}}

INSTRUCCIONES:
- Explica el score general y qué significa.
- Destaca las fortalezas principales.
- Menciona los riesgos más importantes.
- Sugiere los próximos pasos.
- NO agregues información que no esté en el reporte.
- NO evalúes ni juzgues — solo explica.
- Usa un tono profesional pero cercano.
- Responde en español.
```

### 5.4 PromptResult

`PromptResult` es el resultado del ensamblado. Contiene el prompt listo para enviar al LLM **y** metadatos para trazabilidad.

```java
public record PromptResult(
    String systemPrompt,
    String userPrompt,
    String templateId,
    String templateVersion,
    Map<String, String> variablesUsed,
    OffsetDateTime assembledAt
) {
    public String fullPrompt() {
        return systemPrompt + "\n\n" + userPrompt;
    }
}
```

### 5.5 Uso

```java
// En AiEngineService (refactorizado):
public String generateBlocking(/*...*/) {
    var promptResult = promptAssembler.assembleConversation(
        projectContext, decision, history, userMessage
    );
    log.info("Prompt assembled: template={} v{} vars={}",
        promptResult.templateId(),
        promptResult.templateVersion(),
        promptResult.variablesUsed().size());

    return providerRouter.routeBlocking(
        history, promptResult.userPrompt(), promptResult.systemPrompt()
    );
}

// En LlmExplanationStage:
public PipelineContext execute(PipelineContext ctx) {
    var report = ctx.resultBuilder().peekReport();
    var promptResult = promptAssembler.assembleReportExplanation(report);
    var explanation = providerRouter.routeBlocking(
        List.of(), promptResult.userPrompt(), promptResult.systemPrompt()
    );
    ctx.resultBuilder().llmExplanation(explanation);
    return ctx;
}
```

---

## 6. VERSIONADO DE REPORTES

### 6.1 ConsultationMetadata

```java
public record ConsultationMetadata(
    String consultationVersion,      // "2.0.0"
    OffsetDateTime generatedAt,
    String generatedBy,              // "KIN-Engine"
    Map<String, String> engineVersions,  // {"scoring": "1.2.0", "risk": "1.0.0"}
    String pipelineVersion,          // "2.0.0"
    int pipelineStagesExecuted,
    long durationMs
) {
    public static ConsultationMetadata create(
            Map<String, String> engineVersions,
            int stagesExecuted,
            long durationMs
    ) {
        return new ConsultationMetadata(
            "2.0.0",
            OffsetDateTime.now(),
            "KIN-Engine",
            engineVersions,
            "2.0.0",
            stagesExecuted,
            durationMs
        );
    }
}
```

### 6.2 ConsultingReport Versioning

```java
public record ConsultingReport(
    UUID projectId,
    String reportVersion,            // "2.0.0"
    OffsetDateTime generatedAt,
    String generatedBy,              // "KIN-ReportEngine"
    Map<String, String> engineVersions,

    // Secciones (sin cambios):
    ExecutiveSummary executiveSummary,
    ScoresSection scores,
    RecommendationsSection recommendations,
    RisksSection risks,
    OpportunitiesSection opportunities,
    InnovationSection innovation,
    CompetitionSection competition,
    FinancialSection financial,
    MarketSection market,
    NextStepsSection nextSteps,

    // Explicación generada por LLM:
    String llmExplanation,

    ReportMetadata metadata
) {}
```

### 6.3 Política de Versiones

| Componente | Versión actual | Cuándo incrementar |
|-----------|---------------|-------------------|
| `ConsultationResult` | `2.0.0` | Cambio en campos principales |
| `ConsultingReport` | `2.0.0` | Cambio en secciones del reporte |
| `ScoringModel` | `1.0.0` | Cambio en pesos o algoritmo |
| `RecommendationModel` | `1.0.0` | Nuevas reglas de recomendación |
| `RiskModel` | `1.0.0` | Nuevas categorías de riesgo |
| `Pipeline` | `2.0.0` | Nuevos stages o cambio de orden |

---

## 7. MODELO DE AUDITABILIDAD

### 7.1 Problema

Hoy `ScoringEngine.scoreDimension()` usa `length()` y no puede explicar por qué asignó 5 puntos en lugar de 7. Los resultados no son auditables.

### 7.2 Solución: Explanation Model

```
Cada engine produce un resultado que incluye explicaciones detalladas.

┌─────────────────────────────────────────────────────────────────────┐
│                    EXPLANATION MODEL                                 │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  Explanation (VO — record)                               │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - category: String (ej: "dimension", "risk")            │       │
│  │  - item: String (ej: "PROBLEM", "COMPETITION")          │       │
│  │  - score: int                                            │       │
│  │  - maxScore: int                                         │       │
│  │  - reason: String (ej: "Dimension cubierta con 45 chars") │      │
│  │  - source: String (ej: "ScoringEngine.v1")              │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  ScoreResult (ACTUALIZADO con audit trail)               │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  (campos existentes)                                     │       │
│  │  + explanations: List<Explanation>    ← NUEVO            │       │
│  │  + modelVersion: String              ← NUEVO            │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  RiskResult (NUEVO)                                      │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - overallRiskLevel: RiskLevel                            │       │
│  │  - risks: List<Risk>                                     │       │
│  │  - explanations: List<Explanation>                        │       │
│  │  - modelVersion: String                                  │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  RecommendationResult (NUEVO)                            │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - recommendations: List<Recommendation>                  │       │
│  │  - priorityOrder: List<UUID>                             │       │
│  │  - explanations: List<Explanation>                        │       │
│  │  - modelVersion: String                                  │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  OpportunityResult (NUEVO)                               │       │
│  ├──────────────────────────────────────────────────────────┤       │
│  │  - opportunities: List<Opportunity>                      │       │
│  │  - quickWins: List<Opportunity>                          │       │
│  │  - explanations: List<Explanation>                        │       │
│  │  - modelVersion: String                                  │       │
│  └──────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.3 Ejemplo de Explicación

```java
// ScoreResult ahora incluye:
new ScoreResult(
    totalScore: 65,
    maxScore: 100,
    categoryScores: {"Problema": 10, "Solución": 7, ...},
    viabilityLabel: "MEDIA",
    strengths: ["Problema claramente definido"],
    weaknesses: ["Riesgos no evaluados"],
    explanation: "",
    explanations: List.of(
        new Explanation("dimension", "PROBLEM", 10, 10,
            "Dimensión cubierta. Valor de 120 caracteres supera threshold de 100.",
            "ScoringEngine.v1"),
        new Explanation("dimension", "SOLUTION", 7, 10,
            "Dimensión cubierta. Valor de 65 caracteres (threshold 50-100 → score 7).",
            "ScoringEngine.v1"),
        new Explanation("dimension", "COMPETITION", 0, 10,
            "Dimensión no cubierta. Score 0.",
            "ScoringEngine.v1")
    ),
    modelVersion: "1.0.0"
);

// RiskResult incluye:
new RiskResult(
    overallRiskLevel: RiskLevel.HIGH,
    risks: List.of(
        new Risk(/* ... */)
    ),
    explanations: List.of(
        new Explanation("risk", "COMPETITION", 8, 10,
            "Dimensión COMPETITION no cubierta. Riesgo competitivo alto.",
            "RiskEngine.v1"),
        new Explanation("risk", "REVENUE_MODEL", 5, 10,
            "Dimensión REVENUE_MODEL no cubierta. Riesgo financiero medio.",
            "RiskEngine.v1")
    ),
    modelVersion: "1.0.0"
);
```

### 7.4 Beneficios

- **Auditabilidad**: cada decisión de scoring tiene una explicación textual
- **Depuración**: se puede saber exactamente por qué un score es bajo
- **Mejora continua**: los explanations permiten identificar thresholds incorrectos
- **Confianza del usuario**: se puede mostrar "Por qué este score" en la UI
- **Sin LLM**: las explicaciones son generadas por Java, no alucinadas

---

## 8. AUDITORÍA ARQUITECTÓNICA FINAL

### 8.1 ¿Existe alguna futura God Class?

| Componente | Riesgo | Evaluación |
|-----------|--------|------------|
| `PipelineContext` | ❌ ALTO antes → ✅ MITIGADO | Dividido en 3 objetos. `PipelineContext` ahora solo tiene 6 campos de ejecución. `ConsultationResult.Builder` tiene 1 método por engine. |
| `ConsultationResult` | 🟡 MEDIO | 12 campos. Record inmutable. Si en Fase 6+ se agregan más de 5 campos, considerar agrupar en sub-objects. |
| `EngineRegistry` | 🟢 BAJO | Solo un `Map<String, DomainEngine>`. No crece significativamente. |
| `ChatOrchestratorServiceImpl` | ❌ ALTO antes → 🟡 MEDIO | El refactor a streaming con KinMethod reduce la divergencia. Si supera 300 líneas, extraer `SseStreamHandler`. |
| `ReportEngine` | 🟡 MEDIO | Orquesta 3 engines + construye 9 secciones. Si supera 250 líneas, extraer `SectionBuilder` factories. |

**Veredicto**: El riesgo de God Class en `PipelineContext` está mitigado. `ConsultationResult` y `ReportEngine` tienen límites definidos.

### 8.2 ¿Existe algún punto con alto acoplamiento?

| Punto | Acoplamiento | Evaluación |
|-------|-------------|------------|
| `EngineExecutor.buildInput()` | 🟡 MEDIO | Tiene un `switch` que conoce cada tipo de input. Alternativa: que cada engine declare su tipo de input (type-safe). Aceptable por ahora. Para Fase 6+ considerar `DomainEngine<E,R>` con tipo genérico E resuelto por reflexión. |
| `EngineExecutor.storeResult()` | 🟡 MEDIO | Similar — `switch` conoce cada fase. Aceptable. |
| `ConsultationResult.Builder` | 🟢 BAJO | Cada engine escribe su resultado en un método dedicado. Sin acoplamiento entre engines. |
| `Pipeline` + stages | 🟢 BAJO | No hay acoplamiento entre stages. Solo se comunican via `PipelineContext`. |

**Veredicto**: No hay acoplamiento peligroso. Los dos `switch` en `EngineExecutor` son mantenibles y localizados.

### 8.3 ¿Existe alguna violación de SOLID?

| Principio | Evaluación |
|-----------|------------|
| **SRP** | ✅ Cada clase tiene una responsabilidad única. `ConsultationResult.Builder` construye. `EngineRegistry` registra. `EngineExecutor` ejecuta. `Pipeline` orquesta. |
| **OCP** | ✅ `EngineRegistry` permite agregar engines sin modificar código existente. `RendererRegistry` igual. `PipelineStage` abierto a extensión. |
| **LSP** | ✅ `DomainEngine` es una interfaz funcional. Cualquier implementación puede reemplazar a otra. |
| **ISP** | ✅ `DomainEngine` tiene 3 métodos. `ReportRenderer` tiene 2. `PipelineStage` tiene 3. `DomainEventBus` tiene 2. |
| **DIP** | ✅ `ReportRenderer` es un port en el dominio. Las implementaciones están en infraestructura. `DomainEngine` es un port en el dominio. Los adapters (ScoringEngineAdapter) están en infraestructura. |

**Veredicto**: Sin violaciones de SOLID.

### 8.4 ¿Existe alguna dependencia que limite la evolución hacia KIN 3.0?

| Dependencia | Limitación | Evaluación |
|-------------|-----------|------------|
| `PipelineContext` conoce `ConsultationResult.Builder` | Media | Necesario para que los stages escriban resultados. Alternativa: stages devuelven Optional<R> y Pipeline los ensambla. |
| `EngineExecutor.buildInput()` conoce tipos concretos | Baja | El switch es mantenible. Para KIN 3.0 cada engine podría declarar su input type mediante genéricos. |
| `ConsultationResult.Builder` conoce todos los engines | Media | Cada nuevo engine agrega un método al Builder. Alternativa: `Map<String, Object>` con typed getters. |
| `InMemoryDomainEventBus` | Baja | Debe reemplazarse por versión async. No bloquea KIN 3.0. |

**Veredicto**: Sin dependencias bloqueantes. Los puntos identificados son refinamientos, no limitaciones.

### 8.5 ¿Existe algún componente que debería convertirse en un Bounded Context independiente?

| Componente | Recomendación |
|-----------|--------------|
| `ScoringEngine` | Puede separarse en KIN 3.0 si el modelo de scoring se vuelve complejo (múltiples modelos, pesos por industria, ML-based). Hoy es pequeño (~100 líneas). Se mantiene en `kin/reporting/`. |
| `RecommendationEngine` + `RiskEngine` + `OpportunityEngine` | Pueden separarse en KIN 3.0 si cada uno requiere equipo dedicado. Hoy comparten el mismo modelo de datos (`ProjectContext`, `ScoreResult`). Se mantienen juntos. |
| `PromptAssembler` + templates | Podría ser un `KIN.AI.Prompting` BC en KIN 3.0 si se introducen múltiples estrategias de prompting. Hoy es un Application Service. |
| `Renderers` | Son infraestructura. No necesitan ser un BC. |
| `ConsultingReport` | Es un VO en `kin/reporting/`. Si se agregan 10+ formatos de reporte, considerar `Reporting` BC separado. |

**Veredicto**: Hoy no hay necesidad de nuevos BC. La organización actual es adecuada. Para KIN 3.0, evaluar separación de `Scoring` y `Reporting`.

### 8.6 Resumen de Auditoría

```
┌─────────────────────────────────────────────────────────────┐
│                 AUDITORÍA ARQUITECTÓNICA — FASE 5            │
│                                                               │
│  God Classes:       ✅ Sin riesgo crítico                     │
│  Alto acoplamiento: ✅ Sin puntos peligrosos                  │
│  SOLID:             ✅ Sin violaciones                        │
│  KIN 3.0 blocker:   ✅ Sin dependencias bloqueantes          │
│  BC candidates:     🔶 Scoring, Reporting (futuro KIN 3.0)  │
│                                                               │
│  NOTA: Los dos switch en EngineExecutor son el único punto   │
│  con acoplamiento medio. Aceptable para Fase 5. Rediseñar   │
│  en Fase 6 si crece significativamente.                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. PLAN DE MIGRACIÓN

### 9.1 Orden de Implementación

```
Paso 1: Crear ConsultationResult y su Builder
Paso 2: Refactorizar PipelineContext (3 objetos)
Paso 3: Crear EngineRegistry + EngineExecutor + DomainEngine
Paso 4: Adaptar ScoringEngine → ScoringEngineAdapter
Paso 5: Implementar RecommendationEngine + RiskEngine + OpportunityEngine
Paso 6: Implementar ReportEngine con ConsultationResult.Builder
Paso 7: Implementar ReportRenderer + RendererRegistry
Paso 8: Implementar PromptAssembler + templates
Paso 9: Refactorizar AiEngineService → usa PromptAssembler
Paso 10: Implementar Pipeline stages (Fase 5)
Paso 11: Refactorizar ChatOrchestratorServiceImpl
Paso 12: Migrar tests
```

### 9.2 Backward Compatibility

**Ningún paso debe romper tests existentes.** Cada paso se implementa en paralelo con la funcionalidad existente. La migración es incremental:

1. Nuevos componentes se crean en nuevos paquetes
2. Los componentes viejos no se modifican hasta que los nuevos están probados
3. Se agregar tests para los nuevos componentes
4. Se verifica que los tests viejos sigan pasando
5. Se hace el switch: el pipeline nuevo reemplaza al viejo

---

## 10. DOCUMENTOS RESULTANTES

Al completar la Fase 5, los siguientes documentos deben estar actualizados:

| Documento | Estado |
|-----------|--------|
| `ARQUITECTURA_BASE_KIN_2.0.md` | Debe actualizarse con ConsultationResult, PipelineContext rediseñado, EngineRegistry |
| `KIN_ARCHITECTURE_GOVERNANCE.md` | Debe actualizarse con reglas de PromptAssembler, EngineRegistry, versionado |
| `FASE5_DISENO_ARQUITECTONICO.md` | Documento de diseño original (Fase 5) |
| `FASE5_CONSOLIDACION_ARQUITECTONICA.md` | **Este documento** — refinamientos pre-implementación |
| `kin-docs/adr/ADR-001-reporting-bc.md` | ADR del Bounded Context Reporting |
| `kin-docs/adr/ADR-002-pipeline-context.md` | ADR del nuevo PipelineContext |
| `kin-docs/adr/ADR-003-engine-registry.md` | ADR de EngineRegistry |
| `kin-docs/adr/ADR-004-prompt-assembler.md` | ADR de PromptAssembler |
| `kin-docs/adr/ADR-005-llm-role.md` | ADR: LLM solo comunica |

---

*Documento generado el 30 de julio de 2026.*
*Versión: FASE5-CONSOLIDACION-001*
*Estado: Pendiente de aprobación para implementación.*
