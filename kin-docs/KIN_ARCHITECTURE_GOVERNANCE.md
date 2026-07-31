# KIN ARCHITECTURE GOVERNANCE

> Constitución Arquitectónica del proyecto KIN.
> Aprobada el 30 de julio de 2026 — Vinculante para todo desarrollo futuro.
> Toda desviación requiere una ADR aprobada antes de implementar.

---

## ÍNDICE

1. [Principios Arquitectónicos](#1-principios-arquitectónicos)
2. [Reglas de Evolución](#2-reglas-de-evolución)
3. [Reglas para ADR](#3-reglas-para-adr)
4. [Definition of Done](#4-definition-of-done)
5. [Reglas para el Pipeline](#5-reglas-para-el-pipeline)
6. [Reglas para los Engines](#6-reglas-para-los-engines)
7. [Reglas para IA](#7-reglas-para-ia)
8. [Observabilidad](#8-observabilidad)
9. [Versionado](#9-versionado)
10. [Checklist Pre-PR](#10-checklist-pre-pr)
11. [Sanciones y Excepciones](#11-sanciones-y-excepciones)

---

## 1. PRINCIPIOS ARQUITECTÓNICOS

Ninguno de estos principios puede romperse. Son el núcleo de la identidad arquitectónica de KIN. Si una funcionalidad requiere violar uno, debe rediseñarse.

### 1.1 Pureza del Dominio

El paquete `kin/` NO puede depender de ningún otro paquete del proyecto. Sus únicas dependencias permitidas son:

- `java.*` (standard library)
- `org.slf4j` (logging — aceptado como utility transversal)

El paquete `kin/` NO puede contener:

- Anotaciones Spring (`@Service`, `@Component`, `@Autowired`, `@Transactional`, etc.)
- Anotaciones JPA (`@Entity`, `@Table`, `@Column`, `@ManyToOne`, etc.)
- Anotaciones de framework alguno
- DTOs
- Lógica de persistencia
- Lógica de serialización (JSON, XML)
- Lógica de presentación

### 1.2 Clean Architecture (adaptada a KIN)

```
kin/      → Domain Layer (0 dependencias del proyecto)
ai/       → Application Layer (depende de kin/)
chat/     → Application Layer (depende de kin/, ai/)
project/  → Application Layer (depende de kin/)
*/adapter/  → Infrastructure Layer (implementa interfaces de kin/)
common/config/ → Infrastructure Layer (wires everything)
```

- Las dependencias apuntan hacia adentro. El dominio está en el centro.
- La infraestructura implementa puertos definidos en el dominio.
- La aplicación nunca implementa lógica de dominio — solo orquesta.

### 1.3 DDD Ligero

- **Aggregate Roots**: `Project`, `ProjectContext`, `User`, `PricingPlan` — son las únicas entidades con identidad global.
- **Value Objects**: todo lo demás que sea inmutable: `ConversationDecision`, `ScoreResult`, `Message`, `AnalysisResult`, `CompletenessEvaluation`, `ScoringModel`, etc.
- **Domain Services**: comportamientos que no pertenecen naturalmente a una entidad: `ScoringEngine`, `CompletenessEvaluator`, `ConversationStrategist`.
- **Factories**: métodos estáticos `fromProject()`, `defaultModel()`, `empty()`, `ask()`, `generateReport()` — prefijo `from-` o `default-`.

### 1.4 SOLID

| Principio | Aplicación en KIN |
|-----------|-------------------|
| **SRP** (Single Responsibility) | Una clase = una razón para cambiar. PipelineStage solo ejecuta una etapa. ScoringEngine solo evalúa. |
| **OCP** (Open/Closed) | Nuevos comportamientos = nuevas implementaciones, no modificar las existentes. PipelineStage abierto a extensión. ProviderRouter acepta `List<AIProvider>`. |
| **LSP** (Liskov Substitution) | Cualquier `AIProvider` debe poder reemplazar a otro sin alterar el sistema. |
| **ISP** (Interface Segregation) | Interfaces pequeñas y focalizadas. `DomainEventBus` tiene 2 métodos. `PipelineStage` tiene 3. `AIProvider` tiene 3. |
| **DIP** (Dependency Inversion) | El dominio define interfaces (puertos). La infraestructura las implementa. `kin/` contiene `ContextAnalyzerPort`, `DomainEventBus`, `AIProvider`, `PipelineStage`. |

### 1.5 Composición antes que Herencia

- Preferir interfaces funcionales y composición de servicios.
- Las clases base abstractas solo están permitidas para eliminar duplicación REAL (no potencial) de implementación — ej: `AbstractAIProvider` para timeout/retry comunes.
- No existen jerarquías de herencia de más de 2 niveles.

### 1.6 Dependencias Unidireccionales

```
kin/ → nada del proyecto
ai/ → kin/
chat/ → kin/, ai/
project/ → kin/
common/config/ → kin/, ai/, chat/, project/
```

Ningún paquete puede depender de otro que esté en una capa superior. Verificar con `mvn dependency:analyze` o similar en CI.

### 1.7 Arquitectura Evolutiva

- Las decisiones arquitectónicas se documentan como ADR.
- Los componentes se clasifican como **Estable**, **Evolucionando**, o **Experimental** (ver ARQUITECTURA_BASE_KIN_2.0.md sección 8).
- Los componentes Estables solo cambian si hay ADR aprobada.
- Los componentes Experimentales pueden cambiar libremente pero deben estabilizarse en 2 fases.

### 1.8 Event-Driven cuando aporte valor

- Usar `DomainEventBus` para desacoplar contextos.
- NO usar eventos para flujos síncronos request-response.
- Los eventos se publican DESPUÉS de que la operación de negocio se completa.
- Los eventos llevan datos mínimos — los handlers obtienen datos adicionales vía repositorios.

### 1.9 Pipeline Desacoplado

- `Pipeline` solo sabe que existen `PipelineStage`.
- Cada `PipelineStage` recibe y devuelve `PipelineContext` — no conoce otros stages.
- No hay comunicación directa entre stages.
- El orden de stages se define en `KinConfig.java` (wiring), no es hardcodeado.

### 1.10 IA como Proveedor

- La IA es un proveedor externo, no una parte del dominio.
- `AIProvider` es un puerto. El dominio llama al puerto, no sabe qué proveedor responde.
- El `ProviderRouter` permite múltiples implementaciones con fallback.
- El dominio no contiene prompts ni lógica de prompting.

### 1.11 Java Toma las Decisiones de Negocio — el LLM Solo Comunica

> **Regla fundamental de KIN.**

- `CompletenessEvaluator` (Java) decide: ¿la información es suficiente?
- `ConversationStrategist` (Java) decide: ¿qué dimensión explorar?
- `ScoringEngine` (Java) decide: ¿qué score de viabilidad tiene el proyecto?
- `EventStage` (Java) decide: ¿qué eventos disparar?
- El LLM solo recibe una instrucción estratégica y genera texto conversacional o un reporte.
- El LLM NUNCA decide el flujo. NUNCA decide qué preguntar. NUNCA evalúa viabilidad.

---

## 2. REGLAS DE EVOLUCIÓN

### 2.1 Cuándo crear una nueva clase

**Válido cuando**:
- Representa un nuevo concepto del dominio (VO, Entity, Service).
- Implementa un puerto existente (nuevo adaptador de infraestructura).
- Extrae lógica de una clase que viola SRP (refactor).
- Es un DTO para un nuevo endpoint.

**Prohibido cuando**:
- Puede modelarse como un método de una clase existente sin violar SRP.
- Es una "clase de utilidad" con métodos estáticos (preferir funciones en el objeto que corresponde).
- Duplica funcionalidad existente.

### 2.2 Cuándo crear una interfaz

**Válido cuando**:
- Define un puerto en el dominio para ser implementado por infraestructura.
- Define un contrato entre Bounded Contexts.
- Permite múltiples implementaciones intercambiables (Strategy pattern).
- Es una interfaz funcional con un solo método (`@FunctionalInterface`).

**Prohibido cuando**:
- Tiene una sola implementación y no hay planes de tener otra. En ese caso, preferir clase concreta.
- Es una interfaz "marcadora" sin métodos.

### 2.3 Cuándo crear un nuevo Bounded Context

**Válido cuando**:
- Un conjunto de conceptos cohesivos alcanza ~10-15 clases y puede nombrarse independientemente.
- Existe una frontera natural (equipo diferente, ciclo de vida diferente, despliegue independiente potencial).
- El acoplamiento con otros contextos es bajo (comunicación vía eventos o DTOs).

**Prohibido cuando**:
- Tiene menos de 3 clases.
- Depende directamente de clases de otros contextos (no vía puertos).
- Su único propósito es "organizar mejor" sin justificación de frontera.

### 2.4 Cuándo modificar un Aggregate Root

**Válido cuando**:
- Agrega un nuevo campo con integridad transaccional (el campo debe ser consistente con el aggregate).
- Refactoriza métodos existentes sin cambiar el comportamiento observable.
- Agrega un método de consulta que no altera el estado.

**Prohibido cuando**:
- Agrega lógica que debería ser un Domain Service.
- Modifica su identificador (ID).
- Elimina un campo sin deprecación previa.
- Cambia el método de construcción (factory).

### 2.5 Cuándo agregar un Stage al Pipeline

**Válido cuando**:
- Procesa, enriquece, o transforma `PipelineContext` con una responsabilidad única y claramente definida.
- Puede nombrarse como "AlgoStage" y el nombre explica exactamente qué hace.
- Su `supports()` permite saltarlo condicionalmente.

**Prohibido cuando**:
- Depende de otro stage (el stage no puede asumir que otro stage se ejecutó antes — el `PipelineContext` es el único contrato).
- Realiza I/O no configurable (timeout, retry).
- Contiene lógica de presentación o serialización.
- Tiene side effects no registrados como Domain Events.

### 2.6 Cuándo introducir un nuevo Engine

Los Engines son Domain Services singulares. Deben cumplir:
- `XxxEngine` recibe inputs tipados, produce outputs tipados (records).
- `XxxEngine` no tiene estado mutable.
- `XxxEngine` puede tener dependencias de otros Domain Services o Ports.
- `XxxEngine` no depende de Application Services ni de infraestructura.

**Válido cuando**:
- Introduce un nuevo proceso de evaluación o transformación en el dominio.
- Se identifica una responsabilidad que no encaja en un Engine existente.

**Prohibido cuando**:
- Su única función es llamar a un repositorio (eso es un Application Service).
- Su salida es `void` o `null`.
- Depende de Spring, HTTP, JPA, o cualquier framework.

### 2.7 Cuándo crear nuevos eventos

**Válido cuando**:
- Representa un hecho del dominio que interesa a otros contextos (ej: `ProjectEvaluated`, `ConversationMilestoneReached`, `ReportGenerated`).
- El nombre del evento está en pasado (hecho consumado).
- Implementa `DomainEvent`: `type()` + `aggregateId()`.

**Prohibido cuando**:
- Es un comando (solicitud de acción) — los comandos no son eventos.
- Es notificación técnica (ej: `CacheClearedEvent`).
- No tiene ningún subscriber identificado.

---

## 3. REGLAS PARA ADR

### 3.1 Cuándo es obligatorio un ADR

Toda decisión en las siguientes categorías requiere un ADR **antes de implementar**:

| Categoría | Ejemplos |
|-----------|----------|
| **Cambios de arquitectura** | Agregar/eliminar capa, cambiar patrón arquitectónico, introducir CQRS, Event Sourcing. |
| **Cambios de dominio** | Nuevo Aggregate Root, nueva entidad con identidad, cambio en relaciones entre ARs. |
| **Cambios de contratos** | Modificar interfaz de `PipelineStage`, `AIProvider`, `DomainEventBus`, `KinMethod` (input/output). |
| **Cambios de persistencia** | Migrar de H2 a PostgreSQL, introducir Redis, cambiar ORM, agregar cache distribuida. |
| **Nuevos motores** | `ReportEngine`, `RecommendationEngine`, `KnowledgeEngine` — cada nuevo Engine requiere ADR. |
| **Nuevos proveedores IA** | Agregar Anthropic, Gemini, Mistral, o cualquier proveedor externo de LLM. |
| **Nuevos Bounded Contexts** | Separar un contexto existente o introducir uno nuevo. |
| **Violación de principio arquitectónico** | Cualquier decisión que viole las secciones 1.1-1.11 requiere ADR que justifique la violación. |
| **Cambios en Stability Classification** | Mover un componente de Estable a Evolucionando requiere ADR. |
| **Cambios en Governance** | Modificar este documento requiere ADR. |

### 3.2 Estructura de un ADR

```
# ADR-NNN: Título descriptivo

**Estado**: [Aprobado | Rechazado | Propuesto | Deprecado]
**Fecha**: YYYY-MM-DD
**Autor**: [Nombre]
**Contexto**: [Problema que motiva la decisión]
**Decisión**: [Qué se decidió hacer]
**Alternativas consideradas**: [Lista de opciones con pros/cons]
**Consecuencias**: [Impacto positivo y negativo]
**Regla que modifica**: [Si aplica: enlace a la sección de Governance que esta ADR modifica]
**Cumplimiento**: [¿Requiere cambios en otros componentes?]
```

### 3.3 Proceso de ADR

1. Crear ADR en `kin-docs/adr/` (naming: `NNN-titulo-breve.md`)
2. Estado inicial: `Propuesto`
3. Discusión en equipo (mínimo 1 día hábil)
4. Aprobación → estado: `Aprobado`
5. Implementación
6. Si se revierte → estado: `Deprecado`, nuevo ADR explica por qué

---

## 4. DEFINITION OF DONE

Toda funcionalidad, para considerarse completa, debe cumplir TODOS estos criterios:

### 4.1 Pruebas Unitarias

- Todo Domain Service debe tener prueba unitaria.
- Todo Pipeline Stage debe tener prueba unitaria.
- Todo Engine debe tener prueba unitaria.
- Cobertura mínima del código nuevo: **80%** (líneas).
- Usar JUnit 5 + Mockito + reactor-test para flujos reactivos.

### 4.2 Pruebas de Integración

- Todo nuevo endpoint REST debe tener prueba de integración (SpringBootTest + WebMvcTest).
- Todo nuevo flujo SSE debe tener prueba de integración.
- Las pruebas deben usar H2 en memoria (perfil `test`).

### 4.3 Documentación

- Los records y clases públicas del dominio deben tener JavaDoc explicando su responsabilidad.
- Los métodos públicos de servicios deben tener JavaDoc si su comportamiento no es obvio.
- Si la PR cambia la arquitectura, el UML correspondiente en `ARQUITECTURA_BASE_KIN_2.0.md` debe actualizarse.

### 4.4 ADR

- Si la funcionalidad cae en alguna categoría de la sección 3.1, debe tener ADR aprobada antes del merge.

### 4.5 Logs

- Toda operación importante debe tener log INFO al inicio y al final.
- Toda falla esperada debe tener log WARN con contexto.
- Toda falla inesperada debe tener log ERROR con stack trace completo.
- Los logs deben incluir `projectId` y `userId` cuando estén disponibles.

### 4.6 Observabilidad

- El pipeline debe exponer métricas: duración por etapa, cantidad de ejecuciones, errores.
- Los Domain Events deben ser trazables (ver sección 8).

### 4.7 Cobertura Mínima

| Componente | Cobertura unitaria | Cobertura integración |
|-----------|--------------------|-----------------------|
| Domain Services (`kin/*`) | ≥ 90% | — |
| Pipeline (`kin/pipeline/`) | ≥ 95% | ≥ 1 test end-to-end |
| Engines (`ScoringEngine`, etc.) | ≥ 95% | — |
| Application Services (`ai/`, `chat/`) | ≥ 80% | ≥ 1 test por endpoint |
| Infrastructure Adapters | ≥ 70% | ≥ 1 test por adapter |
| Controllers | — | ≥ 1 test por endpoint |

---

## 5. REGLAS PARA EL PIPELINE

### 5.1 Anatomía de una PipelineStage

```java
public interface PipelineStage {
    String name();
    boolean supports(PipelineContext context);
    PipelineContext execute(PipelineContext context);
}
```

### 5.2 Responsabilidades

**Una etapa SÍ puede**:
- Leer datos de `PipelineContext` (input).
- Invocar Domain Services, Ports, o Engines.
- Escribir resultados en `PipelineContext`.
- Agregar Domain Events a `PipelineContext.events()`.
- Usar `supports()` para decidir si ejecutarse según el estado del contexto.

**Una etapa NUNCA puede**:
- Depender de otra etapa directamente (solo se comunica via `PipelineContext`).
- Realizar operaciones bloqueantes sin timeout configurable.
- Llamar a la base de datos directamente (debe usar un Puerto).
- Modificar el objeto `ProjectContext` directamente (debe hacerlo via métodos públicos).
- Contener lógica de presentación, serialización, o formateo de respuesta.
- Lanzar excepciones unchecked sin manejo (ver sección 5.4).

### 5.3 Comunicación entre Etapas

```
PipelineContext es el ÚNICO canal de comunicación entre stages.

Stage A escribe → PipelineContext → Stage B lee

No hay memoria compartida entre stages.
No hay variables de instancia compartidas.
No hay paso de mensajes directo entre stages.
```

### 5.4 Manejo de Errores

- Cada stage debe manejar sus propias excepciones esperadas y registrar el error.
- Si un stage falla:
  1. Log WARN con stage name + contexto.
  2. El error se propaga al Pipeline, que debe NOTificar el error vía Domain Event.
  3. El Pipeline marca `context.completed()` y detiene la ejecución.
  4. El error no debe silenciarse — el KinMethod debe recibir un `KinMethodResult` con indicador de error.
- Timeout por stage: por defecto 30 segundos. Configurable por stage.

### 5.5 Pipeline Execution Model

```
foreach stage in stages:
    if stage.supports(ctx):
        try:
            ctx = stage.execute(ctx)
        catch (Exception e):
            publish(PipelineStageFailedEvent)
            ctx.markCompleted()
            break
    if ctx.completed():
        break
ctx.markCompleted()
```

---

## 6. REGLAS PARA LOS ENGINES

### 6.1 Contrato Único para Todos los Engines

> **Actualizado por ADR-005 (2026-07-30).** Todo engine implementa `DomainEngine<E, R>`:

```java
public interface DomainEngine<E extends EngineInput, R extends EngineResult> {
    EngineMetadata metadata();   // name, version, author, phase, type, priority, dependencies
    R evaluate(E input);
}
```

Donde:
- `E` extiende `EngineInput` (contrato común: `projectContext()`, `evaluation()`, `decision()`, `score()`), pero cada `XxxInput` es un record inmutable con tipado fuerte y sus propios campos.
- `R` extiende `EngineResult` (contrato común: `confidence()`, `explanation()`, `generatedBy()`, `engineVersion()`, `isEmpty()`), pero cada `XxxResult` conserva su tipo concreto.
- `metadata()` provee `EngineMetadata` (fase, tipo, prioridad, dependencias) para auto-descubrimiento y ejecución ordenada.
- Cada engine conserva su fábrica `static XxxResult empty()` (fallback seguro, nunca lanza por datos insuficientes).
- El motor **no depende** del `EngineExecutor`, el `EngineRegistry` ni Spring: son infraestructura que lo consume.

**Registro y ejecución**:
- `EngineRegistry` descubre automáticamente `List<DomainEngine<?, ?>>` inyectada por Spring (mismo patrón que `List<RiskAnalyzer>` / `ProviderRouter`). **Agregar un engine NUNCA modifica el registry.**
- `EngineExecutor` ejecuta: `execute`, `executeAll` (secuencial por prioridad), `executeIf` (condicional), `executeOptional`. El modo paralelo está diseñado pero no activo.
- `EngineStage<E, R>` integra un engine al pipeline por composición (nombre, engine, predicado `supports`, fábrica de input, escritor de resultado). `PipelineContext.engineResults()` lo persiste por nombre de engine sin campos nuevos.
- `DeterministicId.from(category, title, description)` genera ids deterministas compartidos (trazabilidad reproducible).

### 6.2 Engines Obligatorios

| Engine | Estado | Input | Output |
|--------|--------|-------|--------|
| `ScoringEngine` | ✅ Existente | `ProjectContext` + `CompletenessEvaluation` | `ScoreResult` |
| `RecommendationEngine` | ✅ Existente | `RecommendationInput` | `RecommendationResult` |
| `RiskEngine` | ✅ Existente | `RiskInput` | `RiskResult` |
| `ReportEngine` | 🔮 Futuro (KIN 2.2/3.0) | `ProjectContext` + `ScoreResult` | `Report` |
| `OpportunityEngine` | 🔮 Futuro (KIN 3.0) | `OpportunityInput` | `OpportunityResult` |
| `KnowledgeEngine` | 🔮 Futuro (KIN 3.0) | `KnowledgeQuery` | `KnowledgeResult` |
| `InnovationEngine` | 🔮 Futuro (KIN 3.0) | `InnovationInput` | `InnovationScore` |

### 6.3 Reglas Comunes

1. **Inmutabilidad**: Todo Engine es stateless. No tiene campos mutables.
2. **Auditabilidad**: Si el Engine produce un score, la salida debe incluir `explanation` o desglose.
3. **Fallback**: Si el Engine no puede evaluar, devuelve `empty()` — nunca lanza excepción por datos insuficientes.
4. **Configurabilidad**: Si el Engine tiene pesos, thresholds, o parámetros, se pasan como Model (VO) en el constructor — no están hardcodeados.
5. **Testabilidad**: El Engine debe ser testeable sin mocking de infraestructura.

---

## 7. REGLAS PARA IA

### 7.1 Arquitectura Definitiva de IA

```
┌─────────────────────────────────────────────┐
│              AI Layer                         │
│                                                │
│  ┌─────────────────────────────────┐         │
│  │  AiEngineService (App Service)  │         │
│  │  • elige approach (block/stream)│         │
│  │  • delega en ProviderRouter     │         │
│  │  • NO contiene prompts          │         │
│  └──────────────┬──────────────────┘         │
│                 │                             │
│                 ▼                             │
│  ┌─────────────────────────────────┐         │
│  │  ProviderRouter (App Service)   │         │
│  │  • itera proveedores con fallbak│         │
│  │  • NO conoce prompts            │         │
│  └──┬──────────┬──────────┬───────┘         │
│     ▼          ▼          ▼                  │
│  ┌──────┐ ┌──────┐ ┌──────┐                 │
│  │Deep- │ │OpenAI│ │(fut) │                 │
│  │Seek  │ │      │ │      │                 │
│  └──┬───┘ └──┬───┘ └──┬───┘                 │
│     │        │         │                     │
│     ▼        ▼         ▼                     │
│  ┌─────────────────────────────────┐         │
│  │  <<interface>> AIProvider       │         │
│  │  + generateBlocking()           │         │
│  │  + generateStream()             │         │
│  │  + providerName()               │         │
│  └─────────────────────────────────┘         │
│                                                │
│  Fuera de AI Layer:                            │
│  ┌─────────────────────────────────┐         │
│  │  PromptAssembler (App/Infra)    │         │
│  │  • recibe variables estructuradas│         │
│  │  • produce system prompt string │         │
│  │  • cada prompt tiene versión    │         │
│  └─────────────────────────────────┘         │
└─────────────────────────────────────────────┘
```

### 7.2 Reglas

1. **AIProvider es un puerto** — definido en `kin/` (o en un paquete de dominio si se mueve). El dominio puede depender de `AIProvider` pero NO de implementaciones concretas.
2. **PromptAssembler NO está en el dominio** — es un Application Service (o Infrastructure) que construye el system prompt a partir de parámetros estructurados.
3. **El dominio nunca contiene prompts** — ni system prompts, ni user prompts, ni templates. `AiEngineService.buildSystemPrompt()` debe migrarse a `PromptAssembler`.
4. **PromptTemplate** — archivo de texto o recurso externo (no código Java). Si el prompt cambia, no debe requerir recompilación.
5. **PromptVariables** — record tipado con los valores que el PromptAssembler inyecta en el template. Ejemplo:
   ```java
   public record PromptVariables(
       String projectTitle,
       String projectDescription,
       String projectCategory,
       ProjectContext context,
       ConversationDecision decision
   ) {}
   ```
6. **El LLM solo comunica** — la decisión estratégica (`ConversationDecision`) es generada por `ConversationStrategist` en Java. El LLM recibe la decisión como instrucción y genera texto. No decide.
7. **Nunca se pasa el `PipelineContext` completo al LLM** — solo la información necesaria para generar la respuesta.
8. **Versionado de prompts** — cada cambio en un prompt template debe incrementar su versión. El PromptAssembler debe registrar qué versión se usó en cada llamada (observabilidad).

---

## 8. OBSERVABILIDAD

### 8.1 Estándar de Logs

| Nivel | Cuándo usarlo |
|-------|---------------|
| `ERROR` | Fallo inesperado: excepción no manejada, conexión fallida, timeout. Incluir stack trace. |
| `WARN` | Fallo esperado: provider no responde (se usa fallback), stage skippeado, límite alcanzado. |
| `INFO` | Eventos importantes: inicio/fin de pipeline, decisión tomada, score calculado, mensaje guardado. |
| `DEBUG` | Datos internos: prompt generado, response LLM, valores de contexto. NO en producción sin configuración explícita. |
| `TRACE` | Flujo detallado: cada línea ejecutada en un algoritmo complejo. Solo para debugging local. |

### 8.2 Formato de Logs

```
[KIN] <projectId> <userId> <stage/component> <message> <durationMs>
```

Ejemplo:
```
[KIN] 550e8400-e29b-41d4-a716-446655440000 123e4567-e89b-12d3-a456-426614174000 ScoringEngine score=75 viability=ALTA duration=12ms
```

### 8.3 Métricas

| Métrica | Dónde | Tipo |
|---------|-------|------|
| `kin.pipeline.duration` | Pipeline completo | Timer (ms) |
| `kin.pipeline.stage.duration` | Por stage | Timer (ms) con tag `stage` |
| `kin.pipeline.stage.skipped` | Por stage | Counter con tag `stage` |
| `kin.scoring.result` | ScoringEngine | Value (0-100) con tag `viability` |
| `kin.ai.provider.response_time` | Por provider | Timer (ms) con tag `provider` |
| `kin.ai.provider.error` | Por provider | Counter con tag `provider` |
| `kin.events.published` | DomainEventBus | Counter con tag `eventType` |
| `kin.conversation.decision` | ConversationDecision | Counter con tag `action` |

### 8.4 Trazabilidad de Eventos

- Cada `DomainEvent` debe incluir `aggregateId` (projectId).
- El `InMemoryDomainEventBus` expone `publishedEvents()` para testing y debugging.
- Eventos críticos (ej: `ProjectEvaluated`, `ReportGenerated`) deben loguearse con el payload completo en DEBUG.

---

## 9. VERSIONADO

### 9.1 Esquema Semántico

```
KIN <MAJOR>.<MINOR>.<PATCH>
```

| Componente | Cuándo incrementar | Ejemplo |
|-----------|--------------------|---------|
| **MAJOR** | Cambio en la arquitectura que rompe contratos públicos (interfaces de dominio, API REST, eventos). | `2.0.0` → `3.0.0` |
| **MINOR** | Nueva funcionalidad que mantiene contratos. Nuevo Engine, nuevo Stage, nuevo endpoint. | `2.1.0` → `2.2.0` |
| **PATCH** | Bug fix, refactor interno, documentación, tests. No cambia API pública. | `2.0.1` → `2.0.2` |

### 9.2 Versión Arquitectónica

La **versión arquitectónica** es independiente de la versión del producto.

Se define como `ARCH-NNN` donde NNN es el número de revisión del documento `ARQUITECTURA_BASE_KIN_2.0.md`.

- `ARCH-001`: versión inicial (30 de julio de 2026).
- Se incrementa cuando se modifica el documento de arquitectura base.
- No se incrementa por ADR — las ADR son enmiendas a la arquitectura base.

### 9.3 Política de Deprecación

1. Los componentes se deprecan marcándolos con `@Deprecated` y JavaDoc explicando el reemplazo.
2. Un componente deprecated se mantiene por **2 fases MINOR** antes de eliminarse.
3. La eliminación requiere MAJOR bump.

---

## 10. CHECKLIST PRE-PR

Checklist obligatorio antes de abrir un Pull Request. El revisor verificará cada punto.

### 10.1 Arquitectura

- [ ] El nuevo código respeta la Clean Architecture (dependencias hacia adentro).
- [ ] `kin/` no importa ningún otro paquete del proyecto.
- [ ] `kin/` no tiene anotaciones Spring ni JPA.
- [ ] No se viola ningún Principio Arquitectónico (sección 1).
- [ ] Los componentes nuevos están clasificados como Estable/Evolucionando/Experimental.

### 10.2 DDD

- [ ] Value Objects son inmutables (records o clases defensivas).
- [ ] Aggregate Roots tienen identidad bien definida (UUID).
- [ ] Domain Services no tienen estado mutable.
- [ ] Factories son métodos estáticos dentro de la clase que producen.

### 10.3 Pipeline

- [ ] Nuevos stages implementan `PipelineStage`.
- [ ] `supports()` tiene una condición clara.
- [ ] `execute()` no depende de otros stages.
- [ ] Errores manejados (no excepciones sin captura).

### 10.4 Tests

- [ ] Cobertura unitaria ≥ 80% (nuevo código).
- [ ] Domain Services ≥ 90%.
- [ ] Pipeline Stages ≥ 95%.
- [ ] Engines ≥ 95%.
- [ ] Tests existentes siguen pasando.

### 10.5 Logs

- [ ] LOGS: operaciones importantes tienen INFO al inicio y fin.
- [ ] LOGS: fallos esperados tienen WARN.
- [ ] LOGS: fallos inesperados tienen ERROR con stack trace.
- [ ] LOGS: incluyen `projectId` y `userId`.

### 10.6 Documentación

- [ ] JavaDoc en records/clases públicas del dominio.
- [ ] Si cambia la arquitectura: UML actualizado.
- [ ] Si aplica: ADR aprobada antes del PR.

### 10.7 Gobernanza

- [ ] No se viola ninguna regla de este documento.
- [ ] Si se viola: ADR aprobada que lo justifica.
- [ ] Si es nuevo Engine: cumple el contrato de la sección 6.

### 10.8 Seguridad

- [ ] No se exponen IDs internos en URLs (usar UUIDs del aggregate).
- [ ] Validación de ownership: el usuario solo accede a sus propios recursos.
- [ ] No hay secrets hardcodeados (API keys, tokens, contraseñas).

---

## 11. SANCIONES Y EXCEPCIONES

### 11.1 Violaciones Leves

- No causan deuda técnica significativa.
- Ejemplos: log faltante, JavaDoc incompleto, cobertura ligeramente baja.
- **Consecuencia**: el PR se rechaza hasta corregir.

### 11.2 Violaciones Graves

- Violan principios arquitectónicos (sección 1), reglas de dominio (sección 2), o contratos públicos.
- Ejemplos: dominio dependiendo de infraestructura, LLM tomando decisiones de negocio, stage con side effects no registrados.
- **Consecuencia**: el PR se rechaza. Se requiere ADR para proponer el cambio. Si no hay ADR, el cambio no se mergea.

### 11.3 Violaciones Críticas

- Rompen la seguridad, la integridad de datos, o la estabilidad del pipeline.
- Ejemplos: exponer API keys, modificar datos sin transacción, pipeline sin manejo de errores.
- **Consecuencia**: reverteo inmediato. Revisión arquitectónica obligatoria.

### 11.4 Excepciones

Cualquier regla de este documento puede ser eximida temporalmente mediante:

1. ADR aprobada que documente la excepción, su justificación, y el plan para eliminar la deuda técnica.
2. Vigencia máxima de la excepción: **2 fases MINOR**.
3. Vencida la excepción sin corrección, se considera violación grave.

---

*Documento generado el 30 de julio de 2026.*
*Versión: KIN-GOV-001*
*Próxima revisión: al completar KIN 2.3*
*Próxima revisión mayor: al alcanzar KIN 3.0*
