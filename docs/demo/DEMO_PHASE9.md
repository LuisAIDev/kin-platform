# 🎬 KIN — Demostración Fase 9 (Pipeline Estabilizado)

> **Release**: `v1.1.0-phase9` — FASE 9 COMPLETADA (KIN 2.1)
> **ADR**: 017 (pipeline resilience & response fallback) — **Aprobado**
>
> Esta guía recorre el **Pipeline Estabilizado**: desde el usuario hasta la respuesta final,
> pasando por la resiliencia del pipeline (retry/timeout/metrics), la semántica de eventos y el
> consumo de `ResponseValidation` con `ResponseFallback`.

---

## 📖 Resumen del caso

**Usuario**: *"Quiero montar una cafetería especializada en café de origen colombiano en
Cartagena."*

La plataforma recorre el pipeline de 13 etapas, genera el informe de viabilidad y valida la
respuesta del consultor antes de entregarla al usuario (con fallback seguro si fuera inválida).

---

## 👣 Recorrido paso a paso

### Paso 1 — El usuario escribe

El usuario envía su idea al chat del proyecto.

> **Sistema**: `POST /projects/{id}/chat` (o `/chat/stream` para streaming SSE).

```
Quiero montar una cafetería especializada en café de origen colombiano en Cartagena.
```

> **TODO Screenshot** — captura del chat con el mensaje del usuario.

### Paso 2 — ChatController

El endpoint REST recibe la petición, autentica al usuario (JWT) y delega en
`ChatOrchestratorServiceImpl`.

> **Sistema**: `ChatController.chat(...)` → `ChatOrchestratorServiceImpl.processMessage(...)`.

> **TODO Screenshot** — captura de los logs del controller (request recibido).

### Paso 3 — ConversationOrchestrator

El orquestador acota el historial (`HistoryWindow`), carga el contexto durable
(`ContextRepository`), decide la directiva de comunicación en Java (`TurnPolicy`) y construye el
`KinMethodCommand` con la directiva.

> **Sistema**: `ConversationOrchestrator.orchestrate(...)` → directiva de turno pre-pipeline.

> **TODO Screenshot** — captura de la directiva de turno en los logs.

### Paso 4 — KinMethod

El runtime único prepara el `PipelineContext` y ejecuta el pipeline.

> **Sistema**: `KinMethod.execute(...)` → `Pipeline.execute(ctx)`.

> **TODO Screenshot** — captura de los logs de KinMethod (project/user).

### Paso 5 — Pipeline (13 etapas)

El pipeline ejecuta las 13 etapas reales con resiliencia: cada stage corre con su política
(`StagePolicy`), medido con timeout y métricas (`StageExecutionStats`).

> **Sistema**: `Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Enriquecimiento →
> Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`.
> Métricas internas disponibles vía `Pipeline.metrics()`.

> **TODO Screenshot** — captura de los logs del pipeline (etapas + duración).

### Paso 6 — Knowledge (Knowledge Engine)

Adquiere hechos externos verificados (mercado del café, demanda) con `SourceValidator`.

> **TODO Screenshot** — captura del `KnowledgeResult` en logs.

### Paso 7 — Enrichment (Enrichment Engine)

`FactRanker` selecciona y pondera los hechos relevantes por categoría; el reporte cita las fuentes
(`SourcesSection`).

> **TODO Screenshot** — captura del `EnrichmentResult` en logs.

### Paso 8 — Scoring (Scoring Engine)

Score de viabilidad por dimensión.

> **TODO Screenshot** — captura del score en el reporte.

### Paso 9 — Recommendations (Recommendation Engine)

Recomendaciones deduplicadas y priorizadas.

> **TODO Screenshot** — captura de la sección de recomendaciones.

### Paso 10 — Risk (Risk Engine)

Riesgos con severidad/probabilidad; cada riesgo dispara un `RiskDetectedEvent`.

> **TODO Screenshot** — captura de la sección de riesgos y del evento `risk_detected`.

### Paso 11 — Opportunity (Opportunity Engine)

Oportunidades priorizadas.

> **TODO Screenshot** — captura de la sección de oportunidades.

### Paso 12 — Report (Report Engine)

`ReportEngine` orquesta los 11 `SectionAssembler` y produce el `ConsultingReport` (11 secciones).

> **TODO Screenshot** — captura del reporte completo.

### Paso 13 — Consultor (ConsultorStage)

Selecciona el prompt REPORT y pide la respuesta al LLM.

> **TODO Screenshot** — captura del prompt construido.

### Paso 14 — Response Validation

El orquestador valida la respuesta del LLM con `ResponseGuard` contra la directiva
(`ResponseValidation.accepted`).

> **Sistema**: bloqueante → `ConversationOrchestrator`; streaming → `ConsultorStage` →
> `PipelineContext.responseValidation`.

> **TODO Screenshot** — captura de la validación en los logs (accepted/rejected).

### Paso 15 — Response Fallback

Si la respuesta es rechazada (`accepted=false`): reintento acotado si la política lo permite o
**respuesta segura** determinista (`ResponseFallback`). Nunca se lanza excepción ni se devuelve null.

> **TODO Screenshot** — captura de una respuesta segura entregada por el fallback.

### Paso 16 — Respuesta Final

El usuario recibe la respuesta final (la respuesta validada del LLM o el fallback seguro), el
asistente la guarda y el evento `conversation_completed` se publica.

> **TODO Screenshot** — captura de la respuesta final en el chat.

---

## ✅ Flujo completo

```
Usuario
  ↓
ChatController
  ↓
ConversationOrchestrator
  ↓
KinMethod
  ↓
Pipeline (13 etapas)
  ↓
Knowledge → Enrichment → Scoring → Recommendations → Risk → Opportunity → Report
  ↓
Consultor
  ↓
Response Validation
  ↓
Response Fallback
  ↓
Respuesta Final
```

---

## 📸 Capturas pendientes

Todas las capturas están marcadas como **TODO Screenshot**. Se completarán en una iteración de
documentación posterior.

- [ ] Captura: chat con mensaje del usuario (Paso 1)
- [ ] Captura: logs del controller (Paso 2)
- [ ] Captura: directiva de turno (Paso 3)
- [ ] Captura: logs de KinMethod (Paso 4)
- [ ] Captura: logs del pipeline con duración (Paso 5)
- [ ] Captura: KnowledgeResult (Paso 6)
- [ ] Captura: EnrichmentResult (Paso 7)
- [ ] Captura: score de viabilidad (Paso 8)
- [ ] Captura: recomendaciones (Paso 9)
- [ ] Captura: riesgos + `risk_detected` (Paso 10)
- [ ] Captura: oportunidades (Paso 11)
- [ ] Captura: reporte completo (Paso 12)
- [ ] Captura: prompt construido (Paso 13)
- [ ] Captura: validación de respuesta (Paso 14)
- [ ] Captura: respuesta segura del fallback (Paso 15)
- [ ] Captura: respuesta final (Paso 16)
