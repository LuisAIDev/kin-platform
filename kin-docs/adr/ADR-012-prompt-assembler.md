# ADR-012: PromptAssembler — transformador puro del ConsultingReport

**Estado**: Aprobado
**Fecha**: 2026-07-31
**Autor**: KIN Architecture Team

**Contexto**: En la arquitectura actual (Fase 5.4, ADR-011), el `ReportEngine` produce un `ConsultingReport` tipado, inmutable y determinista con 10 secciones + metadata. Sin embargo, el `PromptAssembler` actual (ADR-008) sigue construyendo el system prompt a partir de `ProjectContext`, `ConversationDecision` y dimensiones crudas, y su sección `CIERRE Y REPORTE` le pide al LLM que "GENERE UN INFORME PROFESIONAL COMPLETO" con 20+ secciones, scoring, recomendaciones, riesgos y oportunidades. Esto viola el principio **"Java decide, LLM comunica"**: el LLM **calcula, decide y estructura** el reporte, produciendo salida no tipada, no determinista, no versionable y no auditable.

La Fase 5.5 debe rediseñar `PromptAssembler` para que sea el **único puente** entre el dominio y el LLM, consumiendo **exclusivamente** el `ConsultingReport` ya calculado y transformándolo en un prompt estructurado que el LLM use **solo para explicar** de forma natural.

**Decisión**:

1. **`PromptAssembler` refactorizado** (`com.kinplatform.kin.ai`) — Fachada única stateless:
   - `assemble(PromptRequest) → String`
   - Delega a builder según `PromptType`.

2. **`PromptRequest`** — Record de entrada unificada:
   ```java
   record PromptRequest(
       ConsultingReport consultingReport,  // obligatorio para REPORT
       PromptType type,                    // CONVERSATION | REPORT
       ProjectContext context,             // solo para CONVERSATION
       ConversationDecision decision       // solo para CONVERSATION
   )
   ```

3. **`PromptType`** — Enum:
   - `CONVERSATION` — Fase exploratoria: el LLM pregunta, profundiza, guía.
   - `REPORT` — Fase de cierre: el LLM explica el `ConsultingReport`.

4. **`ConversationPromptBuilder`** — Construye prompt para conversación:
   - Personalidad (constante) + título/categoría/cobertura del proyecto + `INSTRUCCIÓN ESTRATÉGICA` (`decision.toStrategySnippet()`) + reglas de conversación/memoria/profundización (constantes).
   - **NO incluye** ninguna sección de reporte, scoring, recomendaciones, riesgos u oportunidades.

5. **`ReportPromptBuilder`** — Construye prompt para explicación del reporte:
   - Itera `ConsultingReport.sectionsInOrder()` (10 secciones en orden fijo: EXECUTIVE, SCORING, ANALYTIC, PROJECTION, AGGREGATE, METADATA).
   - Usa `SectionFormatter` por sección (10 formatters inyectados).
   - Añade instrucción fija final: *"Eres KIN. Explica el reporte anterior de forma natural, profesional y conversacional en español. No añadas secciones nuevas. No recalcules scores. No opines sobre viabilidad. Usa los datos tal cual están."*

6. **`SectionFormatter<T extends ReportSection>`** — Interfaz de formateo:
   - `format(T) → String` + `kind()` para ordenamiento.
   - 10 implementaciones stateless (una por sección): `ExecutiveSummaryFormatter`, `ScoresSectionFormatter`, `RecommendationsSectionFormatter`, `RisksSectionFormatter`, `OpportunitiesSectionFormatter`, `FinancialSectionFormatter`, `MarketSectionFormatter`, `InnovationSectionFormatter`, `NextStepsSectionFormatter`, `ReportMetadataFormatter`.
   - Producen Markdown ligero consistente. No calculan, solo presentan.

7. **`ConsultingReport`** (ya diseñado en Fase 5.4, ADR-011) — Gana método de conveniencia:
   - `sectionsInOrder()` → `List<ReportSection>` en orden fijo de presentación.

8. **`ConsultorStage`** — Se reposiciona tras `ReportStage` (novena etapa) y adapta la construcción de `PromptRequest` según `decision.shouldGenerateReport()`:
   - Modo CONVERSATION (`decision.action() != REPORT`): `PromptRequest.forConversation(context, decision)`
   - Modo REPORT (`decision.shouldGenerateReport()`): `PromptRequest.forReport(context.consultingReport())` (lanza `IllegalStateException` si el reporte aún no está presente en el contexto)

9. **Frontera de pureza estricta (verificada en tests)**:
   | Permitido en `PromptAssembler` (REPORT) | Prohibido |
   |---|---|
   | Leer `ConsultingReport` y sus secciones | Acceder a `ProjectContext`, `ScoreResult`, `RecommendationResult`, `RiskResult`, `OpportunityResult` |
   | Formatear secciones en texto legible | Calcular scores, prioridades, confianzas, niveles |
   | Incluir `ReportMetadata` | Derivar nuevos riesgos/recomendaciones/oportunidades |
   | Aplicar plantillas de presentación | Aplicar umbrales de negocio |
   | Instrucción fija "Explica, no decidas" | Llamar al LLM |

10. **Sin cambios en contratos estables**:
    - `AIResponder`, `AIRequest`, `AiEngineService` — intactos (puerto y adaptador).
    - `ReportEngine`, `ConsultingReport`, `ReportSection`, `ReportSectionKind` — intactos (solo consumo).
    - `PipelineContext`, `KinMethod`, `KinMethodResult`, `EventStage` — intactos.
    - REST, SSE, frontend, eventos — intactos.

**Alternativas consideradas**:

1. *Mantener `PromptAssembler` actual y pedir al LLM que use `ConsultingReport` como referencia* — Rechazado: el prompt actual tiene 200+ líneas mezclando conversación y reporte; el LLM seguiría "decidiendo" estructura y contenido. No hay frontera clara.

2. *Pasar `ConsultingReport` como JSON crudo en el prompt* — Rechazado: ineficiente en tokens, difícil de leer para el LLM, pierde la presentación estructurada por secciones.

3. *Un solo builder con `if (type == REPORT)`* — Rechazado: viola SRP; `ConversationPromptBuilder` y `ReportPromptBuilder` tienen responsabilidades y dependencias distintas.

4. *Auto-descubrimiento `List<SectionFormatter>` con casts* — Rechazado: exige casts sin verificar y dispatch dinámico; inyección tipada ordenada es más auditable y testeable.

5. *Que el LLM genere el reporte y Java lo parse* — Rechazado: viola "Java decide, LLM comunica"; rompe determinismo, auditabilidad y versionado. El `ConsultingReport` ya existe y es la fuente de verdad.

6. *Incluir renderers (Markdown/HTML/PDF) en esta fase* — Rechazado: fuera de alcance; `PromptAssembler` solo produce el **prompt para el LLM**. Renderers son puerto futuro `ReportRenderer` (declarado en ADR-011).

**Consecuencias**:

- Positivas: Frontera clara "Java decide, LLM comunica"; `ConsultingReport` es la **única** fuente de verdad del reporte; salida del LLM es explicación natural, no cálculo; determinismo y auditabilidad preservados; extensibilidad OCP (cambiar formato de sección = cambiar un formatter); tests aislados por formatter.
- Negativas: **14 tipos nuevos/modificados** en `kin.ai` + `kin.ai.prompt`; `ConsultorStage` adapta lógica de construcción de `PromptRequest`; requiere tests de frontera para verificar que prompt REPORT no accede a fuentes crudas.

**Regla que modifica**: `KIN_ARCHITECTURE_GOVERNANCE.md` §6.2 — `PromptAssembler` pasa de "embebido en AiEngineService" (Fase 4) / "servicio de dominio con prompt mixto" (ADR-008) a "transformador puro del ConsultingReport" (Fase 5.5). `BASELINE_ARCHITECTURE.md` §4.1 — sin cambios en contratos congelados.

**Cumplimiento**: Sin cambios en REST, SSE, eventos, frontend, `AIResponder`/`AIRequest`/`AiEngineService`, `ReportEngine`/`ConsultingReport`, `PipelineContext`, `KinMethod`/`KinMethodResult`, `DomainEngine`/`EngineStage`/`EngineExecutor`/`EngineRegistry`, `DeterministicId`. Los 338 tests existentes siguen en verde y el paquete `kin.ai` cumple ≥90 % de cobertura de instrucciones en JaCoCo (100 % en `kin.ai`, 98.8 % en `kin.ai.prompt`, 99.9 % en `kin.ai.prompt.formatter`).