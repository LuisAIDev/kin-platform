# KIN 2.0 Alpha 1 — GitHub Release

> Contenido listo para publicar. `gh` no estaba disponible en el entorno de generación,
> por lo que se entrega el cuerpo del Release junto con los comandos para publicarlo en un solo paso.

## Datos del Release

| Campo | Valor |
|-------|-------|
| Repositorio | `LuisAIDev/kin-platform` |
| Tag | `v2.0.0-alpha.1` (ya publicado en `origin`) |
| Título | **KIN 2.0 Alpha 1** |
| Target | `main` (commit `5dde0d3`) |

## Cómo publicarlo

### Opción A — GitHub CLI (recomendado)

```bash
# Instalar gh (si no está): https://cli.github.com
gh release create v2.0.0-alpha.1 \
  --repo LuisAIDev/kin-platform \
  --title "KIN 2.0 Alpha 1" \
  --notes-file kin-docs/releases/GITHUB_RELEASE_KIN_2_0_ALPHA_1.md
```

### Opción B — API REST (token con scope `repo`)

```bash
TOKEN="<TU_PAT>"
gh_cmd() { :; }
BODY=$(Get-Content -Raw "kin-docs/releases/GITHUB_RELEASE_KIN_2_0_ALPHA_1.md" -Encoding UTF8)
$json = @{ tag_name="v2.0.0-alpha.1"; name="KIN 2.0 Alpha 1"; target_commitish="main"; body=$BODY } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "https://api.github.com/repos/LuisAIDev/kin-platform/releases" -Headers @{ Authorization="Bearer $env:GITHUB_TOKEN"; Accept="application/vnd.github+json" } -ContentType "application/json" -Body $json
```

---

## Cuerpo del Release

```markdown
# KIN 2.0 Alpha 1 🎯

**Primer hito oficial de la arquitectura KIN 2.0 — estado `ARCHITECTURE STABLE`** (30 de julio de 2026).

La base arquitectónica de la plataforma queda **congelada** y lista para construir sobre ella
las fases futuras. Este hito cierra las fases **4.0, 5.0, 5.1 y 5.2** sin añadir funcionalidad
nueva: es el cierre oficial del contrato de dominio sobre el que se desarrollará el resto.

## ✨ Novedades

### Infraestructura de motores (`kin/engine`) — contrato estable
- `DomainEngine<E,R>`, `EngineInput`, `EngineResult`, `EngineMetadata`, `EnginePhase`, `EngineType`.
- `EngineRegistry` (auto-descubrimiento), `EngineExecutor` (secuencial/condicional/opcional), `DeterministicId`.
- `EngineStage`: stage genérico de pipeline que delega en cualquier motor.

### Motores de dominio (`kin/reporting`)
- **RecommendationEngine**: recomendaciones accionables, deduplicadas y priorizadas (ADR-003).
- **RiskEngine + RiskAssembler**: riesgos por categoría con severidad y probabilidad (ADR-004).
- **ScoringEngine**: scoring de viabilidad por dimensiones.

### Pipeline
- `RecommendationStage` y `RiskStage` delegando en `EngineStage` (composición).
- `PipelineContext.engineResults`: resultados no canonizados disponibles en el flujo.

### Configuración
- `KinConfig`: beans de `EngineRegistry` y `EngineExecutor` con auto-descubrimiento.

## 🧪 Calidad

| Métrica | Valor |
|---------|-------|
| Tests | **102** (0 fallos) — `./mvnw clean test` → BUILD SUCCESS |
| Cobertura `kin.engine` | **99,1 %** instrucciones / **100 %** ramas |
| Cobertura `kin.reporting` | **96,2 %** instrucciones |
| Cobertura `kin.reporting.risk` | **99,6 %** instrucciones / **98,6 %** ramas |
| ADRs | 5 aprobadas (ADR-001 … ADR-005) |

## 📚 Documentación
- **`BASELINE_ARCHITECTURE.md`** — baseline contractual del milestone: inventario, contratos que no deben romperse, matriz de estabilidad y preparación para la siguiente fase.
- **Release notes** — `kin-docs/releases/KIN_2_0_ALPHA_1.md`.
- **CHANGELOG** — sección `v2.0.0-alpha.1`.
- Documentación por fase: `FASE5_0`, `FASE5_1`, `FASE5_2`.

## 🚧 Problemas conocidos
- Script Flyway `V2__add_viability_scoring_column.sql` no portable a H2 → dev usa `spring.flyway.enabled=false`.
- `ChatOrchestratorServiceImpl` aún no usa `KinMethod` en streaming (planificado KIN 2.1).
- `EventStage` dispara `ConversationCompleted` de forma fija (KIN 2.1).
- `InMemoryDomainEventBus` sin async ni persistencia (KIN 2.4).
- Heurística de longitud en `ScoringEngine` por reemplazar antes de KIN 2.5.

## 🗺️ Siguiente
- Fase 5.3 sobre la infraestructura estable (`kin/engine` listo para nuevos motores).
- Refactor streaming → `KinMethod`, corrección de `EventStage`, hardening del pipeline.

## ✅ Verificación
```bash
cd kin-backend && ./mvnw clean test   # 102 tests, 0 fallos, BUILD SUCCESS
git tag -l "v2.0.0-alpha.1"           # tag presente
```
```
