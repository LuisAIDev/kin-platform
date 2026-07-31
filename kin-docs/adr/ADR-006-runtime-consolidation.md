# ADR-006: Consolidación del runtime — pipeline único vía KinMethod

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: Desde KIN 2.0 Alpha 1 existían dos flujos de conversación con lógica de negocio duplicada:

1. **Bloqueante** (`POST /chat`): `ChatOrchestratorServiceImpl` construye un `KinMethodCommand` y delega en `KinMethod.execute(...)` (limpio).
2. **Streaming** (`POST /chat/stream`, SSE): el orquestador ejecutaba análisis, evaluación y decisión *manualmente*, fuera del pipeline, porque necesitaba emitir tokens por SSE mientras el proveedor de IA respondía.

Esta duplicación violaba la regla 4 de Governance ("los Application Services orquestan, no implementan reglas de negocio") y la decisión congelada nº 5 ("`ChatOrchestratorServiceImpl` DEBE usar `KinMethod` en ambos flujos"), y era el riesgo R1 del baseline. Además, el `ProjectContext` vivía en un `ProjectContextService` en memoria (ver ADR-007), de modo que cada etapa dependía de un estado transitorio y no re-persistido.

**Decisión**: Hacer de `KinMethod` el **punto de entrada único del runtime** para ambos flujos:

- `KinMethod.execute(KinMethodCommand)` → `KinMethodResult` (bloqueante, flujo actual).
- `KinMethod.executeStream(KinMethodCommand)` → `Flux<String>` (streaming): ejecuta el pipeline completo de forma síncrona (todas las etapas deterministas), pero la etapa `ConsultorStage` —en modo streaming— **no bloquea**: deja el `Flux` de tokens del proveedor en `PipelineContext.aiResponseFlux`. El orquestador suscribe ese flux y emite el SSE.

Para esto:

- `PipelineContext` gana un flag `streaming` y un campo `aiResponseFlux`.
- `ConsultorStage` depende del puerto de dominio `AIResponder` y del `PromptAssembler` (ver ADR-008); decide entre `respond(...)` (bloqueante) y `respondStream(...)` (streaming) según el flag.
- `KinMethod` carga/crea el `ProjectContext` desde el `ContextRepository` (ver ADR-007), lo inyecta en el contexto del pipeline, re-persiste el contexto tras la ejecución y publica los eventos de dominio.
- `ChatOrchestratorServiceImpl` queda como **adaptador de I/O puro**: persiste mensajes y emite SSE; no contiene lógica de conversación.

**Alternativas consideradas**:

1. *Callback SSE dentro del pipeline* — Rechazado: acopla el dominio al HTTP y a `SseEmitter`, rompiendo "kin/ es 100 % POJO".
2. *Pipeline totalmente reactivo (Flux de contextos)* — Rechazado: invasivo; las etapas deterministas no necesitan reactividad y el `Flux` del proveedor debe poder escaparse al orquestador sin bloquear el hilo.
3. *Dejar el flujo streaming como estaba* — Rechazado: mantiene la deuda R1 y la duplicación de reglas de negocio.

**Consecuencias**:
- Positivas: una sola implementación de las reglas de conversación; `ChatOrchestratorServiceImpl` se reduce a I/O; el contrato SSE no cambia (mismos nombres de eventos y payloads); el contexto es durable y re-persistido en cada turno; los eventos se publican desde un solo lugar.
- Negativas: el pipeline streaming ejecuta las etapas deterministas de forma síncrona antes de suscribir el flux (los tokens tardan lo mismo en llegar; el coste de las etapas deterministas es despreciable frente al LLM).

**Regla que modifica**: Decisión congelada nº 5 y riesgo R1 del `BASELINE_ARCHITECTURE.md` (v2.0.0-alpha.1) — ahora cumplidos.

**Cumplimiento**: Sin cambios en REST, SSE, eventos, frontend ni contratos de `KinMethodCommand`/`KinMethodResult`. Los payloads SSE (`token`/`error`/`done`) y el orden de emisión se conservan (verificado por `ChatOrchestratorServiceImplTest`).
