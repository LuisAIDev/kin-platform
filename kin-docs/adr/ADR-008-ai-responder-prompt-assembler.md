# ADR-008: AIResponder (puerto) + PromptAssembler (servicio de dominio)

**Estado**: Aprobado
**Fecha**: 2026-07-30
**Autor**: KIN Architecture Team

**Contexto**: En KIN 2.0 Alpha 1 la construcción del system prompt (personalidad, instrucción estratégica, cierre/reporte) vivía embebida en `AiEngineService` (`ai/`), y la etapa `ConsultorStage` (dominio) dependía directamente del servicio concreto. Esto invertía la dirección de dependencias de la arquitectura limpia (el dominio apuntaba a la infraestructura) y hacía que la lógica de conversación más rica del sistema fuera difícil de testear y de reutilizar por otros proveedores.

**Decisión**: Introducir en el dominio (`com.kinplatform.kin.ai`) dos abstracciones y reposicionar `AiEngineService` como adaptador:

- **`AIResponder`** — puerto del proveedor de IA: `respond(AIRequest) → String` y `respondStream(AIRequest) → Flux<String>`. El dominio acepta `Flux` en la variante streaming porque la pila de IA es reactiva (Spring AI); es la excepción pragmática que preserva la semántica de backpressure del proveedor (documentada en el propio contrato).
- **`AIRequest`** — record inmutable (historial, mensaje del usuario, system prompt ya ensamblado).
- **`PromptAssembler`** — servicio de dominio puro (stateless, sin Spring) que centraliza la construcción del prompt: personalidad, datos del proyecto, `## INFORMACIÓN CONOCIDA DEL PROYECTO` (snippet del `ProjectContext`) e `## INSTRUCCIÓN ESTRATÉGICA` (snippet de la `ConversationDecision`).
- **`AiEngineService`** — pasa a implementar `AIResponder`: enruta la petición al `ProviderRouter` y aplica el fallback en español. Conserva los métodos legacy `generateAiResponse(...)`, `generateAiResponseStream(...)` y `buildSystemPrompt(...)` (usados por tests y por la compatibilidad del milestone).

`ConsultorStage` se construye con `(AIResponder, PromptAssembler)` y ya no conoce la implementación concreta.

**Alternativas consideradas**:

1. *Dejar `ConsultorStage` dependiendo de `AiEngineService`* — Rechazado: mantiene la inversión de dependencias.
2. *Extraer solo el prompt, sin puerto de respuesta* — Rechazado: el flujo streaming sigue acoplado a la clase concreta.
3. *Port a nivel de infraestructura (`ai.provider`) — Rechazado: el pipeline no debe depender del paquete `ai` de aplicación; el puerto vive en el dominio.

**Consecuencias**:
- Positivas: dirección de dependencias correcta (pipeline → dominio, `AiEngineService` → dominio); el prompt es testeable de forma aislada (`PromptAssemblerTest`); cualquier proveedor futuro implementa `AIResponder`; `ConsultorStage` es indiferente al proveedor en modo streaming.
- Negativas: nueva superficie de contrato; la excepción `Flux` en el puerto requiere documentación explícita (ya incluida en el contrato).

**Regla que modifica**: Inventario §2.5 y estabilidad §5.2 del `BASELINE_ARCHITECTURE.md` (v2.0.0-alpha.1) — `AiEngineService` pasa de "orquestador" a "adaptador de `AIResponder`".

**Cumplimiento**: Sin cambios en REST, SSE, eventos, frontend ni en la API pública de `AiEngineService` (los métodos legacy se conservan y sus tests siguen pasando).
