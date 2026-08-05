# FASE 20.0 — Product Intelligence & AI Platform Intelligence

**Estado**: ✅ Implementado (100 % aditivo; sin tocar dominio ni ADR-012/014)
**Fecha**: 2026-08-05
**Autor**: KIN Architecture Team

> **Alcance**: inteligencia de producto Enterprise sobre analytics local (offline,
> sin IA, sin envío externo). Reutiliza `services/analytics`, `services/tokenEstimator`
> y `services/settings` existentes. El Knowledge Engine, el Prompt Builder y el
> dominio `kin.knowledge.*` no se modifican.

---

## 1. Arquitectura

```
src/services/intelligence/
  types.ts                 → modelos compartidos (UsageStatistics, Insights, Recomendaciones, Timeline, ProductMetrics)
  UsageStatistics.ts       → agregación de eventos por día/semana/mes y por tipo
  ConversationInsights.ts  → análisis determinista de conversaciones
  FeatureUsageTracker.ts   → adopción de funciones (localStorage + eventos)
  RecommendationEngine.ts  → recomendaciones por reglas
  ProductMetrics.ts        → retención, activación, engagement, embudo, timeline
  AnalyticsAggregator.ts   → fachada que computa toda la inteligencia
  exporters.ts             → exportación JSON / CSV / PDF (jsPDF)

src/hooks/useProductIntelligence.ts → hook que agrega y refresca métricas

src/components/insights/  → StatCard, FeatureAdoptionList, RecommendationList,
                            TimelineView, EnterpriseReportGenerator

src/app/dashboard/
  analytics/page.tsx        → dashboard de uso (KPIs + adopción)
  insights/page.tsx         → insights de conversación + timeline
  recommendations/page.tsx  → recomendaciones
  reports/page.tsx          → reportes y exportación
```

## 2. Componentes

| Servicio | Responsabilidad |
|---|---|
| `AnalyticsAggregator` | Fachada: `compute(events) → ProductIntelligence` |
| `UsageStatistics` | Conteos por periodo, mensajes, sesiones, tokens, coste, feedback, proyectos |
| `ConversationInsights` | Longitud promedio, preguntas, duración, intención, temas, calidad, satisfacción |
| `FeatureUsageTracker` | Registro/agregación de uso de funciones y detección de no usadas |
| `RecommendationEngine` | Próximos pasos, funciones no usadas, tips, IA Pro |
| `ProductMetrics` | Retención, activación, engagement, embudo de onboarding, timeline |
| `Exporters` | JSON / CSV / PDF de todas las métricas |

## 3. Flujo

1. `useProductIntelligence(feature)` registra la página visitada como feature y lee `analytics.events()`.
2. `AnalyticsAggregator.compute` agrega todo en un `ProductIntelligence` inmutable.
3. Las páginas `/dashboard/analytics|insights|recommendations|reports` renderizan KPIs, adopción, timeline y recomendaciones.
4. `/dashboard/reports` exporta JSON/CSV/PDF (jsPDF, componente separado de `PdfReportButton`).

## 4. Métricas

- **Uso**: diario, semanal, mensual · mensajes IA · sesiones · días activos.
- **IA**: tokens estimados (chars/4) · coste estimado · feedback 👍/👎 · intención predominante · temas frecuentes · calidad · satisfacción.
- **Producto**: retención · activación · engagement · embudo onboarding · uso por área.
- **Adopción**: funciones más/menos usadas, último uso, frecuencia.
- **Timeline**: eventos ordenados cronológicamente.

Todo **local** (`localStorage` + consola) — sin envío a terceros.

## 5. Casos de uso

- Dashboard de uso para el usuario final.
- Recomendaciones personalizadas (crear primer proyecto, descubrir funciones, IA Pro, feedback negativo).
- Reportes exportables (JSON/CSV/PDF) para revisión.
- Embudo de onboarding para activación.

## 6. Roadmap futuro

- Conectar los eventos de `analytics` a un endpoint propio (telemetría first-party).
- Consumir preferencias IA (proveedor/temperatura/longitud) en el backend.
- Resumen de conversación y re-engagement por inactividad.
- A/B testing del onboarding.

## 7. Validación

- Frontend: **205 tests** · cobertura **96.01 %** · ESLint 0 errores · tsc 0 errores · `next build` OK.
- Backend: `mvn verify` **BUILD SUCCESS** · **2189 tests** · cobertura OK.
- ADR-012 / ADR-014 intactas · dominio sin modificar · GitHub Actions funcionales.
