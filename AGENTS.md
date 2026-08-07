# KIN Platform — Agent Guide

> **Milestone: KIN 2.0 Alpha 1 — `ALPHA STABLE`** (release oficial `v2.0.0-alpha1`, commit `89b39b9`; núcleo inteligente completo: fases 5.4–5.6 sobre el milestone original `v2.0.0-alpha.1`), enmendado por la Fase 5.2.1 (ADR-006…009: runtime consolidado, contexto durable, scoring canonizado), la Fase 5.3 (ADR-010: OpportunityEngine), la **Fase 5.4 (ADR-011: ReportEngine)**, la **Fase 5.5 (ADR-012: PromptAssembler)**, la **Fase 5.6 (ADR-013: Conversation Orchestrator)**, la **Fase 6 (ADR-014: External Knowledge Acquisition — KnowledgeEngine + KnowledgeGateway + SourceValidator, cierre oficial E1…E7)** y la **Fase 7 (ADR-015: Strategic Interview Engine — InterviewEngine + InterviewBlueprint + AnswerValidator + InterviewStage + InterviewRepository, cierre oficial E1…E7)**.
> El contrato `kin/engine` y las APIs estables listadas en `kin-docs/BASELINE_ARCHITECTURE.md` NO pueden modificarse sin una ADR aprobada.
> Release notes: `kin-docs/releases/KIN_2_0_ALPHA_1.md`.

## Project
Full-stack project management platform with AI-guided viability assessment.

## Layout

| Directory | What |
|---|---|
| `kin-backend/` | Spring Boot 3.2.5 / Java 17 — Maven (wrapper: `mvnw`) |
| `kin-frontend/` | Next.js 16 App Router / TypeScript 5 strict / Tailwind CSS 4 |
| `kin-database/` | PostgreSQL init scripts (used by Docker) |
| `kin-docs/` | Architecture docs, ADRs (001…019), phase docs, release notes |

## Commands

```bash
# Backend dev (PostgreSQL local, perfil 'dev'; requiere Docker para la base)
# IMPORTANTE: usa SIEMPRE el script de arranque con guard FAIL-FAST.
# NUNCA ejecutes `mvnw spring-boot:run` directo: puede crear una segunda
# instancia. El script detecta KinApplication / Maven Wrapper / puerto 8080 y,
# si existe alguno, muestra "Backend ya está ejecutándose." y lo reutiliza.
# La base es PostgreSQL (docker compose up -d postgres-db); si no responde,
# el script informa el problema y se detiene.
powershell -ExecutionPolicy Bypass -File scripts/start-dev-backend.ps1   # Windows
bash scripts/start-dev-backend.sh                                         # Linux/macOS
# Solo si el script dice que NO hay backend en marcha, arranca una instancia.

# Frontend dev
cd kin-frontend && npm install && npm run dev  # http://localhost:3000

# Frontend lint
npm run lint                                   # ESLint 9 (core-web-vitals + TS)

# Full stack with Docker (PostgreSQL + backend + frontend)
docker compose up --build                      # from repo root
```

## Key quirks

- **Category catalog (SaaS-ready)**: the `ProjectCategory` enum was replaced by the `Category` entity / `categories` table. `Project.category` is `@ManyToOne → Category` (`category_id`). `GET /categories` returns only active categories ordered by `displayOrder`. `POST/PUT /projects` receive the category `code` string (backend resolves it; 400 if unknown). `ProjectResponse` exposes `category` (code), `categoryName` and `categoryColor`. Prod seeds via Flyway `V6__create_categories.sql`; dev (PostgreSQL, Flyway) seeds via `CategoryDataInitializer` (idempotente). The frontend `new/page.tsx` loads `GET /categories` (no hardcoded lists); badge color comes from `category.color`. The AI pipeline still receives the category as a String (SECTOR dimension).

- **PostgreSQL everywhere** (H2 eliminado): driver `org.postgresql:postgresql` activo. Dev usa el perfil `dev` (`application-dev.yml` → PostgreSQL local, Flyway V1..V11); test usa Testcontainers PostgreSQL 18; prod usa Neon/PostgreSQL.
- **Dev database is PostgreSQL** (H2 eliminado): el perfil `dev` usa `application-dev.yml` con `DATABASE_URL`/`DATABASE_USER`/`DATABASE_PASSWORD`, Flyway V1..V11 y `ddl-auto: none`. Para regenerar el esquema local usa `scripts/reset-dev-db.ps1` (Windows) o `.sh` (Linux/macOS): operación destructiva que exige confirmación `RESET` y solo se aplica a la base local.
- **CORS single source**: only `SecurityConfig.java` (bean `corsConfigurationSource`) configures CORS; the legacy MVC `CorsConfig` was removed (consolidación T12/K-506). Add new origins only in `SecurityConfig`.
- **AI engine** (`AiEngineService.java`) implements the domain port `AIResponder` (`com.kinplatform.kin.ai`) and routes through `ProviderRouter` (DeepSeek/OpenAI/Ollama) with a Spanish mock fallback on failure — safe to develop without a LLM running. Prompt construction lives in `PromptAssembler` (domain, `kin.ai`), not in the service.
- **PromptAssembler** is a pure façade (`kin.ai`): `assemble(PromptRequest) → String` delegates to `ConversationPromptBuilder` (CONVERSATION) or `ReportPromptBuilder` (REPORT, via 10 `SectionFormatter` in `kin.ai.prompt`). `PromptRequest.forConversation(...)` / `forReport(...)` enforce the ADR-012 boundary (REPORT only consumes `ConsultingReport`; raw sources are forbidden). `PromptRequest.forConversation(context, decision, directive)` is the additive ADR-013 overload; when a directive is present, `ConversationPromptBuilder` appends the `## DIRECTIVA DE COMUNICACIÓN` section (phase/mode/constraints). The additive ADR-015 overload `assemble(PromptRequest, InterviewResult)` propagates the interview result in CONVERSATION mode (REPORT ignores it), and `ConversationPromptBuilder.build(request, interviewResult)` appends the `## ENTREVISTA ESTRATÉGICA` section when there is a pending `InterviewDirective`.
- **Conversation Orchestrator** (ADR-013, `kin.conversation`, pure domain POJOs — no Spring): `ChatOrchestratorServiceImpl` delegates BOTH `/chat` and `/chat/stream` to `ConversationOrchestrator` (I/O only in the HTTP layer). Per turn: `HistoryWindow` caps history (default 20 messages; the current user message always stays), `DefaultTurnPolicy` decides the `TurnDirective` in Java BEFORE the pipeline from the previous decision (`ProjectContext.currentDecision()`), and the directive travels additively in `KinMethodCommand.directive` → `PipelineContext.turnDirective`. `ResponseGuard` validates the LLM response: blocking → the orchestrator emits `TurnResult.validation`; streaming → `ConsultorStage.attachStreamGuard` leaves `PipelineContext.responseValidation` (M3). **M1**: on the FIRST report-generation turn the pre-pipeline directive is derived from the previous (typically ASK) decision, e.g. `(EXPLORATION, ASK, QUESTION)`; later turns get `(REPORTING, REPORT, EXPLAIN_REPORT)`. `ConsultorStage` still uses `PromptRequest.forReport` (ADR-012 boundary intact).
- **Single runtime**: both `POST /chat` and `POST /chat/stream` go through `ConversationOrchestrator` → `KinMethod` (`execute` / `executeStream`). `ChatOrchestratorServiceImpl` is I/O only (persists messages, emits SSE). In streaming mode `ConsultorStage` leaves a `Flux<String>` in `PipelineContext.aiResponseFlux` and the orchestrator returns it to the SSE consumer.
- **Pipeline order**: `Analizador → Evaluador → Estratega → Entrevista → Conocimiento → Scoring → Recomendaciones → Riesgos → Oportunidades → Reporte → Consultor → Eventos` (12 stages). `InterviewStage` (ADR-015) runs between `StrategistStage` and `KnowledgeStage`, writes `PipelineContext.interviewResult`, persists state via `InterviewRepository` and applies the effective decision (ASK while incomplete / REPORT when complete). `KnowledgeStage` (ADR-014) runs after `InterviewStage`, writes `PipelineContext.knowledgeResult`; it builds a `KnowledgeRequest` from the `ProjectContext` and delegates to `KnowledgeEngine` (offline-first: `KnowledgeResult.empty()` if no source returns valid candidates). `ConsultorStage` runs after `ReportStage` so the LLM receives the `ConsultingReport`; it selects the conversation prompt with `## ENTREVISTA ESTRATÉGICA` when the interview is active with decision `ASK` (ADR-015), `PromptRequest.forReport(...)` when `decision.shouldGenerateReport()` (throws if the report is missing) and `forConversation(context, decision, turnDirective)` otherwise.
- **Strategic Interview Engine** (ADR-015, `kin.interview`, pure domain POJOs — no Spring): `InterviewStage` (between `StrategistStage` and `KnowledgeStage`) builds `InterviewInput` from `ProjectContext` + the turn's user message + the previous `InterviewState` (loaded via `InterviewRepository.findOrCreate`), runs `InterviewEngine` (`DomainEngine<InterviewInput, InterviewResult>`, phase `VALIDATION`), writes `PipelineContext.interviewResult` and persists the new state. `AnswerValidator` (deterministic) decides accept/refine/reject; `InterviewBlueprint` decides the next question (sequence per `AnalyzedDimension`, required/optional, follow-ups). Principle: **Java decide. El LLM únicamente formula preguntas.** While the interview is incomplete the effective decision is `ASK` (priority 9) and the analysis stages are skipped; when complete the decision is `REPORT` and the report is generated in the same turn. `ConsultorStage`/`PromptAssembler.assemble(request, interviewResult)` pass the `InterviewResult` to `ConversationPromptBuilder`, which appends the `## ENTREVISTA ESTRATÉGICA` section (topic + `AnswerRules`) only when there is a pending `InterviewDirective`. Adapter: `JpaInterviewRepository` (`ai.interview.adapter`, table `interview_state`).
- **Durable context**: `ProjectContext` is persisted per project via `ContextRepository` (port, `kin.context`) + `JpaContextRepository` (`ai.context.adapter`, JSON in table `project_context`). La tabla la crea Flyway V3 en todos los entornos (`ddl-auto: none`).
- **Auth middleware** is in `src/proxy.ts` (frontend middleware, not backend). Protects `/dashboard` and redirects `/login` when authenticated.
- **Tests** exist in `kin-backend/src/test/java/`. Run with `cd kin-backend && ./mvnw test`. Currently **902 tests**: 5 in `AiEngineServiceTest`, 5 in `ChatOrchestratorServiceImplTest` (SSE via `mockConstruction` of `ConversationOrchestrator`), 5 in `KinMethodTest` (full 12-stage pipeline, incl. REPORT prompt via `ArgumentCaptor` + directive propagation), 17 in `ConsultorStageTest` (incl. REPORT mode, guard streaming, directive framing, interview ASK/REPORT gating), 8 in `PromptAssemblerTest`, 3 in `PromptAssemblerInterviewTest`, 6 in `PromptRequestDirectiveTest`, 10 in `ReportPromptBuilderTest`, 11 in `ConversationPromptBuilderTest`, 3 in `ConversationPromptBuilderDirectiveTest`, 38 across the 10 `SectionFormatter` tests, 4 in `JpaContextRepositoryTest` (JSON round-trip), 7 in `ScoringEngineTest` + `ScoringStageTest`, 113 across `kin/conversation/` (enums/records, `HistoryWindowTest`, `ResponseGuardTest`, `DefaultTurnPolicyTest`, `ConversationOrchestratorTest`, `ConversationOrchestratorPipelineIntegrationTest`), 6 in `ConversationOrchestratorInterviewIntegrationTest`, **22 across `kin/knowledge/` (types/enums/records 8, `SourceRegistryTest`/`SourceValidatorTest`/`KnowledgeGatewayTest`/`KnowledgeEngineTest` in `engine/`, `KnowledgeStageTest` + `KnowledgeStagePipelineTest` in `stage/`)** plus **10 across `ai/knowledge/adapter/` (`CompositeKnowledgeSourceTest`, `HttpKnowledgeSourceAdapterTest`, `PublicApiConnectorTest`, `JdbcKnowledgeSourceTest`, `RagKnowledgeSourceTest`, `DocumentKnowledgeSourceTest`, `KnowledgeEngineAdapterIntegrationTest`, `KnowledgeAdapterGatewayPropagationTest`)**, plus `kin/engine/` (EngineRegistry/EngineExecutor/EngineMetadata/DeterministicId), `pipeline/stage/` (EngineStage, RecommendationStage, RiskStage, OpportunityStage, ReportStage) and `kin/reporting/` (RecommendationEngine/Result/Stage, RiskEngine/Result/Stage, RiskAssembler, RiskAnalyzers, OpportunityEngine/Result/Stage, OpportunityAssembler, OpportunityAnalyzers), `kin/reporting/report/` (ReportEngine, ReportBuilder, ConsultingReport, 10 SectionAssemblers, ReportStage), and **`kin/interview/` (tipos/enums/records + `InterviewStateTest`/`InterviewDecisionTest`/`InterviewDirectiveTest`/`AnswerRulesTest`/`AnswerValidationTest`/`InterviewEngineTest`/`InterviewBlueprintTest`/`AnswerValidatorTest`/`InterviewStageTest`/`InterviewStagePipelineTest` + adapter `ai/interview/` `JpaInterviewRepositoryTest`/`InterviewStateEntityTest`/`InterviewStateMapperTest`)**. JaCoCo is configured (`jacoco-maven-plugin`), report at `target/site/jacoco/index.html`. Domain coverage requirement: ≥90% instructions in `kin.reporting`, `kin.engine`, `kin.ai`, `kin.conversation`, `kin.knowledge` and `kin.interview` (current: **100% in `kin.conversation`**, **100% in `kin.knowledge`** (engine 99.47%, stage 100%), **100% in `ai.knowledge.adapter`**, **98.39% in `kin.interview` + `ai.interview.adapter`** (engine 95%, stage 100%, adapter 100%), 99.7% in `kin.ai`, 95.9% in `kin.ai.prompt`, 99.9% in `kin.ai.prompt.formatter`, 99.2% in `kin.reporting*` aggregate, 100% in `kin.reporting.opportunity`, 99.5% in `kin.reporting.risk`, 95.1% in `kin.scoring`, 99.1% in `kin.engine`, 99–100% in `kin.reporting.report`). Tests use Mockito + JUnit 5 + reactor-test; the AI engine tests exercise the mock fallback path (no LLM needed). No frontend tests or integration tests yet.
- **E2E tests** (`kin-frontend/tests/`): 3 login-flow tests with Playwright. Run with `cd kin-frontend && npx playwright test`. The webServer auto-starts only the Next.js frontend. **Before running, start the backend manually**: `cd kin-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test` (el perfil test usa PostgreSQL: Testcontainers o variables de entorno `SPRING_DATASOURCE_*`).
- **`.env` is gitignored** — never commit secrets. Copy `.env.example` to `.env`.
- **Frontend API URL**: controlled by `NEXT_PUBLIC_API_URL` env var (defaults to `http://localhost:8080/api/v1`).
- **Backend API prefix**: all endpoints under `/api/v1` (set via `server.servlet.context-path`).
- Frontend uses `@/*` path alias → `./src/*`.

## Backend packages

| Package | Responsibility |
|---|---|
| `auth` | Register, login, JWT issuance |
| `user` | User entity, roles (FREE, PREMIUM, FACILITADOR, ADMIN) |
| `project` | CRUD, `Category` catalog (SaaS-ready), status, viability scoring |
| `chat` | Message history, streaming SSE (`/chat/stream`), orchestration |
| `ai` | Adapters: `AiEngineService` (implements `AIResponder`), provider router, `context.adapter` (JPA durable context) |
| `kin.ai` | Domain AI: `AIResponder` port, `AIRequest`, `PromptRequest`/`PromptType`, `PromptAssembler` (pure façade) |
| `kin.ai.prompt` | `ConversationPromptBuilder`, `ReportPromptBuilder` (10 `SectionFormatter` en `kin.ai.prompt.formatter`) — frontera ADR-012: REPORT solo consume `ConsultingReport`; sección `## DIRECTIVA DE COMUNICACIÓN` cuando hay directiva (ADR-013) |
| `kin.conversation` | `ConversationOrchestrator` (fachada del ciclo de turno, ADR-013), `TurnPolicy`/`DefaultTurnPolicy` (directiva en Java), `ResponseGuard` (guardrail), `HistoryWindow` (presupuesto de contexto), tipos de turno (`ConversationTurn`, `TurnDirective`, `TurnResult`, `ResponseValidation`, `TurnConstraints`, `ConversationPhase`, `CommunicationMode`) |
| `engine` | Engine infrastructure: `DomainEngine<E,R>` contract, `EngineMetadata`, `EnginePhase`, `EngineRegistry` (auto-discovery via `List<DomainEngine>`), `EngineExecutor` (sequential/conditional/optional; parallel designed not active), `DeterministicId` |
| `pipeline.stage.EngineStage` | Generic engine stage (composition); `ScoringStage`/`RecommendationStage`/`RiskStage`/`OpportunityStage`/`ReportStage` delegate to it |
| `scoring` | Canonized `ScoringEngine` (implements `DomainEngine<ScoringInput, ScoreResult>`) |
| `reporting.opportunity` | `OpportunityEngine` (implements `DomainEngine<OpportunityInput, OpportunityResult>`) — coordinator over 8 auto-discovered `OpportunityAnalyzer` (market, innovation, technological, financial, competitive, scalability, automation, monetization) + `OpportunityAssembler` (shared confidence/priority/explanation). Priority 60, phase OPPORTUNITY |
| `reporting.report` | `ReportEngine` (implements `DomainEngine<ReportInput, ConsultingReport>`) — pure orchestrator over 10 `SectionAssembler`, `ReportBuilder`, `ConsultingReport` (10 sections). Priority 70, phase REPORTING |
| `kin.knowledge` | Domain knowledge (ADR-014, POJO puro): types (`KnowledgeRequest/Query/Candidate/Fact/Input/Result`, `SourceValidation`, `SourceTrust`, `KnowledgeSource`, `KnowledgeRepository`) |
| `kin.knowledge.engine` | `KnowledgeEngine` (implements `DomainEngine<KnowledgeInput, KnowledgeResult>` — phase KNOWLEDGE/DOMAIN/50), `KnowledgeGateway` (coordinador: `acquire(KnowledgeRequest) → KnowledgeResult`), `SourceRegistry` (auto-discovery vía `List<KnowledgeSource>`), `SourceValidator` (HTTPS/allowlist/HTTP status/content-type/freshness/dedup/trust, determinista) |
| `kin.knowledge.stage` | `KnowledgeStage` — pipeline stage aditivo (composición pura sobre `EngineStage`); escribe `PipelineContext.knowledgeResult` |
| `ai.knowledge.adapter` | Infrastructure adapters: `CompositeKnowledgeSource`, `HttpKnowledgeSourceAdapter`, `PublicApiConnector`, `JdbcKnowledgeSource`, `RagKnowledgeSource`, `DocumentKnowledgeSource` (todos implementan `KnowledgeSource`; mocks en tests, sin red) |
| `context` | Domain context types + `ContextRepository` port (durable `ProjectContext`) |
| `common.config` | CORS + Security filter chain (stateless JWT; `/test/**` requires ADMIN) + `KinConfig` wiring |
| `common.security` | `JwtService`, `JwtAuthenticationFilter` |

## Database

- **Dev (PostgreSQL)**: perfil `dev` — `application-dev.yml`, Flyway V1..V11 (`ddl-auto: none`). Base local: `docker compose up -d postgres-db`.
- **Test (PostgreSQL 18)**: Testcontainers en los tests de integración; Flyway V1..V11.
- **Prod (PostgreSQL, Docker/Neon)**: Flyway habilitado (`ddl-auto: none`) — migraciones V1…V11. Run `docker compose up --build` to initialize.

### Dev PostgreSQL — reset (importante)

En desarrollo el backend usa **PostgreSQL local** (perfil `dev`, `application-dev.yml`).
Flyway administra el esquema V1..V11; Hibernate usa `ddl-auto: none` (nunca modifica
el esquema silenciosamente).

**Regla de oro: NUNCA modificar entidades para adaptarlas a la base.** La base dev es un
artefacto local desechable; las entidades son la fuente de verdad. Para regenerarla:

```bash
# Windows
scripts/reset-dev-db.ps1

# Linux / macOS
bash scripts/reset-dev-db.sh
```

Los scripts detienen la app si está corriendo, ELIMINAN el esquema `public` de la base
local (exigen confirmación `RESET`, solo base localhost) y vuelven a arrancar Spring Boot
con el perfil `dev`; Flyway recrea V1..V11 desde cero y los seeds (`DataInitializer`,
`CategoryDataInitializer`).
