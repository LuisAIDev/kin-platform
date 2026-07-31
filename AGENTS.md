# KIN Platform — Agent Guide

> **Milestone: KIN 2.0 Alpha 1 — `ARCHITECTURE STABLE`** (tag `v2.0.0-alpha.1`), enmendado por la Fase 5.2.1 (ADR-006…009: runtime consolidado, contexto durable, scoring canonizado).
> El contrato `kin/engine` y las APIs estables listadas en `kin-docs/BASELINE_ARCHITECTURE.md` NO pueden modificarse sin una ADR aprobada.

## Project
Full-stack project management platform with AI-guided viability assessment.

## Layout

| Directory | What |
|---|---|
| `kin-backend/` | Spring Boot 3.2.5 / Java 17 — Maven (wrapper: `mvnw`) |
| `kin-frontend/` | Next.js 16 App Router / TypeScript 5 strict / Tailwind CSS 4 |
| `kin-database/` | PostgreSQL init scripts (used by Docker) |
| `kin-docs/` | Architecture docs, ADRs (001…009), phase docs, release notes |

## Commands

```bash
# Backend dev (H2 file-based, no Docker needed)
cd kin-backend && ./mvnw spring-boot:run      # http://localhost:8080/api/v1

# Frontend dev
cd kin-frontend && npm install && npm run dev  # http://localhost:3000

# Frontend lint
npm run lint                                   # ESLint 9 (core-web-vitals + TS)

# Full stack with Docker (PostgreSQL + backend + frontend)
docker compose up --build                      # from repo root
```

## Key quirks

- **PostgreSQL dependency is commented out** in `pom.xml`. Dev uses H2 file-based (`data/kindb`). Only Docker deployment uses PostgreSQL.
- **Dual CORS config**: both `CorsConfig.java` and `SecurityConfig.java` configure CORS. `SecurityConfig` takes precedence (Spring Security filter chain). Add new origins to both.
- **AI engine** (`AiEngineService.java`) implements the domain port `AIResponder` (`com.kinplatform.kin.ai`) and routes through `ProviderRouter` (DeepSeek/OpenAI/Ollama) with a Spanish mock fallback on failure — safe to develop without a LLM running. Prompt construction lives in `PromptAssembler` (domain, `kin.ai`), not in the service.
- **Single runtime**: both `POST /chat` and `POST /chat/stream` go through `KinMethod` (`execute` / `executeStream`). `ChatOrchestratorServiceImpl` is I/O only (persists messages, emits SSE). In streaming mode `ConsultorStage` leaves a `Flux<String>` in `PipelineContext.aiResponseFlux` and the orchestrator subscribes to it.
- **Durable context**: `ProjectContext` is persisted per project via `ContextRepository` (port, `kin.context`) + `JpaContextRepository` (`ai.context.adapter`, JSON in table `project_context`). Dev auto-creates the table with `ddl-auto: update`; prod uses Flyway V3 + `init.sql`.
- **Auth middleware** is in `src/proxy.ts` (frontend middleware, not backend). Protects `/dashboard` and redirects `/login` when authenticated.
- **Tests** exist in `kin-backend/src/test/java/`. Run with `cd kin-backend && ./mvnw test`. Currently 130 tests: 5 in `AiEngineServiceTest`, 5 in `ChatOrchestratorServiceImplTest` (SSE via `mockConstruction`), 3 in `KinMethodTest` (full 8-stage pipeline), 5 in `PromptAssemblerTest`, 5 in `ConsultorStageTest`, 4 in `JpaContextRepositoryTest` (JSON round-trip), 7 in `ScoringEngineTest` + `ScoringStageTest`, plus `kin/engine/` (EngineRegistry/EngineExecutor/EngineMetadata/DeterministicId), `pipeline/stage/` (EngineStage, RecommendationStage, RiskStage) and `kin/reporting/` (RecommendationEngine/Result/Stage, RiskEngine/Result/Stage, RiskAssembler, RiskAnalyzers). JaCoCo is configured (`jacoco-maven-plugin`), report at `target/site/jacoco/index.html`. Domain coverage requirement: ≥90% instructions in `kin.reporting` and `kin.engine` (current: 99.1% in `kin.engine`, 96.2% in `kin.reporting`, 99.6% in `kin.reporting.risk`, 95.1% in `kin.scoring`). Tests use Mockito + JUnit 5 + reactor-test; the AI engine tests exercise the mock fallback path (no LLM needed). No frontend tests or integration tests yet.
- **E2E tests** (`kin-frontend/tests/`): 3 login-flow tests with Playwright. Run with `cd kin-frontend && npx playwright test`. The webServer auto-starts only the Next.js frontend. **Before running, start the backend manually**: `cd kin-backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=test` (H2 in-memory, no Docker needed).
- **`.env` is gitignored** — never commit secrets. Copy `.env.example` to `.env`.
- **Frontend API URL**: controlled by `NEXT_PUBLIC_API_URL` env var (defaults to `http://localhost:8080/api/v1`).
- **Backend API prefix**: all endpoints under `/api/v1` (set via `server.servlet.context-path`).
- Frontend uses `@/*` path alias → `./src/*`.

## Backend packages

| Package | Responsibility |
|---|---|
| `auth` | Register, login, JWT issuance |
| `user` | User entity, roles (FREE, PREMIUM, FACILITADOR, ADMIN) |
| `project` | CRUD + categories, status, viability scoring |
| `chat` | Message history, streaming SSE (`/chat/stream`), orchestration |
| `ai` | Adapters: `AiEngineService` (implements `AIResponder`), provider router, `context.adapter` (JPA durable context) |
| `kin.ai` | Domain AI: `AIResponder` port, `AIRequest`, `PromptAssembler` |
| `engine` | Engine infrastructure: `DomainEngine<E,R>` contract, `EngineMetadata`, `EnginePhase`, `EngineRegistry` (auto-discovery via `List<DomainEngine>`), `EngineExecutor` (sequential/conditional/optional; parallel designed not active), `DeterministicId` |
| `pipeline.stage.EngineStage` | Generic engine stage (composition); `ScoringStage`/`RecommendationStage`/`RiskStage` delegate to it |
| `scoring` | Canonized `ScoringEngine` (implements `DomainEngine<ScoringInput, ScoreResult>`) |
| `context` | Domain context types + `ContextRepository` port (durable `ProjectContext`) |
| `common.config` | CORS + Security filter chain (stateless JWT; `/test/**` requires ADMIN) + `KinConfig` wiring |
| `common.security` | `JwtService`, `JwtAuthenticationFilter` |

## Database

- **Dev (H2)**: `ddl-auto: update` + Flyway disabled — schema auto-created by JPA entities (incl. `project_context`).
- **Prod (PostgreSQL, Docker)**: Flyway enabled (`ddl-auto: none`) — migrations V1…V3 + `kin-database/init.sql`. Run `docker compose up --build` to initialize.
