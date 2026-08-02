# 🎬 KIN — Guía de Demostración

> **Release**: `v1.0.0-phase8` — FASE 8 COMPLETADA
> **Caso de uso**: *"Quiero montar una cafetería"*
>
> Esta guía recorre el flujo completo de KIN: desde que el usuario escribe su idea hasta la
> respuesta de la IA con el **informe de viabilidad** enriquecido con conocimiento externo y
> fuentes citadas. Cada paso muestra qué hace el sistema internamente (pipeline de 13 etapas).

---

## 📖 Resumen del caso

**Usuario**: *"Quiero montar una cafetería especializada en café de origen colombiano, con
experiencia de degustación, en Cartagena."*

KIN recoge la idea, completa la información con una **entrevista estratégica**, adquiere
**conocimiento externo verificado** sobre el mercado, enriquece el análisis con esos hechos y
genera un **ConsultingReport** con score, recomendaciones, riesgos, oportunidades y **fuentes
citadas**.

---

## 👣 Recorrido paso a paso

### Paso 1 — El usuario escribe

El usuario envía su idea al chat del proyecto.

> **Sistema**: `POST /projects/{id}/chat` (o `/chat/stream` para streaming SSE).

```
Quiero montar una cafetería especializada en café de origen colombiano, con experiencia de degustación, en Cartagena.
```

> **TODO Screenshot** — captura del chat con el mensaje del usuario.

### Paso 2 — Pipeline

La solicitud entra al `ConversationOrchestrator` → `KinMethod` → **Pipeline de 13 etapas**.

> **Sistema**: se ejecutan las 13 etapas:
> `Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Enriquecimiento → Scoring →
> Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos`.

> **TODO Screenshot** — captura de los logs del pipeline (etapas ejecutadas).

### Paso 3 — Knowledge (Knowledge Engine)

KIN construye una `KnowledgeRequest` desde el `ProjectContext` y adquiere hechos externos
verificados (mercado del café, demanda, barreras de entrada).

> **Sistema**: `KnowledgeGateway` consulta las fuentes registradas, `SourceValidator` valida
> (HTTPS, dominio, frescura) y produce `KnowledgeResult` con `KnowledgeFact` y `SourceTrust`.

```
Hecho: "El consumo de café especial crece 12% anual en Colombia." [src-1, OFFICIAL_PUBLIC]
```

> **TODO Screenshot** — captura del `KnowledgeResult` en logs (hechos y validaciones).

### Paso 4 — Interview (Interview Engine)

Si faltan datos, la entrevista estratégica formula preguntas (Java decide qué y en qué orden).

> **Sistema**: `InterviewBlueprint` → siguiente pregunta → el LLM solo la formula
> (`## ENTREVISTA ESTRATÉGICA`).

```
¿Cuál es tu cliente objetivo y cuánto estimas invertir inicialmente?
```

> **TODO Screenshot** — captura de una pregunta de la entrevista en el chat.

### Paso 5 — Enrichment (Enrichment Engine)

`FactRanker` selecciona y pondera en Java los hechos relevantes por categoría (mercado,
innovación, financiero, competitivo) con score determinista (`SourceTrust` + frescura + cobertura).

> **Sistema**: `EnrichmentEngine` (prioridad 55) produce `EnrichmentResult` con `EvidenceRank` y
> `sourcesUsed`. Sin hechos, degrada a `EnrichmentResult.empty()` (offline-first).

```
MARKET: "El mercado del café especial crece 12% anual." score=0.94
```

> **TODO Screenshot** — captura del `EnrichmentResult` en logs.

### Paso 6 — Scoring (Scoring Engine)

`ScoringEngine` calcula el score de viabilidad por dimensión y categoría.

> **Sistema**: `ScoreResult` con `totalScore`, `viabilityLabel` y fortalezas/debilidades.

```
Score de viabilidad: 7.8/10 — VIABLE
```

> **TODO Screenshot** — captura del score en el reporte.

### Paso 7 — Recommendations (Recommendation Engine)

`RecommendationEngine` genera recomendaciones deduplicadas y priorizadas, enriquecidas con los
hechos relevantes.

> **Sistema**: `RecommendationInput.withEnrichment(...)`.

```
1. Valida el punto de venta en la zona histórica de Cartagena.
2. Diferénciate con café de origen único y degustación.
```

> **TODO Screenshot** — captura de la sección de recomendaciones.

### Paso 8 — Risks (Risk Engine)

`RiskEngine` identifica riesgos con severidad, probabilidad y nivel, enriquecidos con evidencia.

> **Sistema**: `RiskInput.withEnrichment(...)`.

```
Riesgo alto: dependencia del turismo estacional en Cartagena.
Riesgo medio: márgenes ajustados por costo de grano especial.
```

> **TODO Screenshot** — captura de la sección de riesgos.

### Paso 9 — Opportunities (Opportunity Engine)

`OpportunityEngine` (8 analizadores) detecta oportunidades de capitalización del proyecto.

> **Sistema**: `OpportunityInput.withEnrichment(...)`.

```
Oportunidad: venta de grano tostado online (canal e-commerce).
Oportunidad: alianzas con hoteles boutique.
```

> **TODO Screenshot** — captura de la sección de oportunidades.

### Paso 10 — Consulting Report

`ReportEngine` orquesta los **11 `SectionAssembler`** y construye el `ConsultingReport` con la
**11.ª sección `SourcesSection`** (fuentes citadas con trazabilidad).

> **Sistema**: `ReportInput.withEnrichment(...)` → `ConsultingReport` (11 secciones).

```
Secciones: Resumen · Scores · Recomendaciones · Riesgos · Oportunidades · Financiero ·
Mercado · Innovación · Siguientes pasos · Fuentes · Metadata
```

> **TODO Screenshot** — captura del reporte completo en la UI.

### Paso 11 — Prompt final

`PromptAssembler` selecciona `PromptRequest.forReport(report)` y `ReportPromptBuilder` formatea
las 11 secciones (incluye `SourcesSectionFormatter` para las fuentes).

> **Sistema**: prompt REPORT con la instrucción fija *"Explica, no decidas"* y las fuentes
> citadas.

```
=== CONSULTING REPORT ===
...
## Fuentes Citadas
### 1. El mercado del café especial crece 12% anual.
**Fuente:** https://example.com/reporte
**Categoría:** Mercado
```

> **TODO Screenshot** — captura del prompt construido (modo debug).

### Paso 12 — Respuesta IA

El LLM (**DeepSeek**) recibe el prompt y responde de forma natural, explicando el análisis y las
fuentes ya seleccionadas por Java. El consultor valida la respuesta (`ResponseGuard`) y la entrega
al usuario.

> **Sistema**: `AIResponder.respond(...)` → respuesta final en el chat (o streaming SSE).

```
Tu idea es viable (7.8/10). El mercado del café especial crece 12% anual en Colombia
(fuente: src-1), así que te recomiendo enfocarte en café de origen único y validar el
punto de venta en la zona histórica. Tu mayor riesgo es la estacionalidad turística.
```

> **TODO Screenshot** — captura de la respuesta final de la IA.

---

## ✅ Flujo completo

```
Usuario ──► Frontend ──► Backend ──► ConversationOrchestrator ──► Pipeline (13 etapas)
                                                                    │
                                                    Knowledge → Enrichment → Scoring →
                                                    Recommendations → Risks → Opportunities →
                                                    Report (SourcesSection) → Prompt Builder → DeepSeek
                                                                    │
                                                            Respuesta IA ──► Usuario
```

---

## 📸 Capturas pendientes

Todas las capturas están marcadas como **TODO Screenshot** a lo largo de esta guía. Se completarán
en una iteración de documentación posterior de la release.

- [ ] Captura: chat con mensaje del usuario (Paso 1)
- [ ] Captura: logs del pipeline (Paso 2)
- [ ] Captura: KnowledgeResult (Paso 3)
- [ ] Captura: pregunta de entrevista (Paso 4)
- [ ] Captura: EnrichmentResult (Paso 5)
- [ ] Captura: score de viabilidad (Paso 6)
- [ ] Captura: recomendaciones (Paso 7)
- [ ] Captura: riesgos (Paso 8)
- [ ] Captura: oportunidades (Paso 9)
- [ ] Captura: reporte completo (Paso 10)
- [ ] Captura: prompt construido (Paso 11)
- [ ] Captura: respuesta final de la IA (Paso 12)
