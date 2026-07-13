# KIN Platform — Agent Guide

## Project
Full-stack project management platform with AI-guided viability assessment.

## Layout

| Directory | What |
|---|---|
| `kin-backend/` | Spring Boot 3.2.5 / Java 17 — Maven (wrapper: `mvnw`) |
| `kin-frontend/` | Next.js 16 App Router / TypeScript 5 strict / Tailwind CSS 4 |
| `kin-database/` | PostgreSQL init scripts (used by Docker) |
| `kin-docs/` | (empty) |

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
- **AI engine** (`AiEngineService.java`) calls Ollama (`llama3.2`) but falls back to Spanish mock responses on failure — safe to develop without Ollama running.
- **Auth middleware** is in `src/proxy.ts` (frontend middleware, not backend). Protects `/dashboard` and redirects `/login` when authenticated.
- **No tests exist** yet (roadmap item).
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
| `ai` | Ollama integration + mock fallback |
| `common.config` | CORS + Security filter chain (stateless JWT) |
| `common.security` | `JwtService`, `JwtAuthenticationFilter` |

## Database

- **Dev (H2)**: `ddl-auto: update` — schema auto-created by JPA entities.
- **Prod (PostgreSQL, Docker)**: `ddl-auto: validate` — schema from `kin-database/init.sql`. Run `docker compose up --build` to initialize.
