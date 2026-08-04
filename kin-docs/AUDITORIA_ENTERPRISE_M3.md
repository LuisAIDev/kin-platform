# Auditoría del módulo Enterprise — Roadmap M3

> **Documento técnico pre-M3**. No se ha modificado código.
> Estado del repositorio: `main` @ `410fb8b` (working tree limpio).
> Alcance: `kin-backend` (`kin.enterprise` + `ai.enterprise.adapter`), `kin-enterprise-ui`, `kin-docs`, `docker-compose.yml`.
> Verificado contra código fuente y `git log`. Referencias `archivo:línea`.

---

## 1. Estado actual

### 1.1 Terminado (BUILD SUCCESS, 1786 tests backend, 55 tests UI, JaCoCo ≥95%)

| Área | Qué existe |
|---|---|
| **Contratos (M2A)** | Puertos `EnterpriseProjectRepository`, `DocumentRenderer`; contratos aditivos respetando `BASELINE_ARCHITECTURE.md` (no toca `KinMethod`, `EngineRegistry`, `ConversationOrchestrator` firmas públicas). |
| **Aggregate root (M2B)** | `EnterpriseProject` con máquina de estados `REQUESTED → RUNNING → COMPLETED \| FAILED` y versionado `(projectId, version)`. |
| **Value objects (M2C)** | 20+ VO con invariantes (`LeanCanvas`, `MarketPlan`, `FinancialPlan`, `Roadmap`, `RiskMatrix`, `KpiSet`, `InnovationPlan`, `EnterpriseScore`, etc.). |
| **8 engines (M2D)** | Motores 100% deterministas (`DefaultMarketEngine`, `DefaultFinancialPlanEngine`, `DefaultRoadmapEngine`, `DefaultKpiEngine`, `DefaultRiskPlanEngine`, `DefaultInnovationEngine`, `DefaultBusinessModelEngine`, `DefaultEnterpriseScoreEngine`), todos puros (sin I/O). |
| **Orquestación (M2E)** | `EnterpriseGenerationService` + `EnterpriseGenerationOrchestrator` (ciclo, progreso, eventos). |
| **Eventos (M2F)** | `EnterpriseProjectRequested/Generated/Failed` publicados en `DomainEventBus`; invocación aditiva desde `ConversationOrchestrator` (constructor sobrecargado, `ConversationOrchestrator.java:79-130,268-285`). |
| **JPA (M2G)** | `ai.enterprise.adapter` con tablas `enterprise_project`/`enterprise_document`, migración `V7__create_enterprise_project.sql`. |
| **Exportación (M2H)** | Renderers PDF/DOCX/PPTX (formato binario escrito a mano con JDK puro, sin librerías) + bundle ZIP + `EnterpriseExportService/Orchestrator`. |
| **REST + OpenAPI (M2I)** | 3 controllers (`EnterpriseController` 11 endpoints, `EnterpriseDashboardController`, `EnterpriseProgressController`) + springdoc. |
| **Dashboard + SSE (M2J)** | `EnterpriseProgressService` (SseEmitter + heartbeat) y `kin-enterprise-ui` (React 19 + Vite, 55 tests, ~96% cobertura). |
| **Contexto durable** | `EnterpriseGenerationRequest` porta `ProjectContext` real (cargado de `ContextRepository` en REST y en listener). |

### 1.2 Parcialmente terminado (código existente pero inactivo o incompleto)

| Hallazgo | Evidencia |
|---|---|
| **El ciclo automático es código muerto en producción.** `DefaultEnterpriseProjectTrigger` y `EnterpriseProjectRequestedListener` NO son beans Spring (clases planas sin anotaciones). El bean `conversationOrchestrator` se construye con el constructor de 5 args → `NO_OP_TRIGGER` (`KinConfig.java:571-579`). La emisión `enterpriseTrigger.request(...)` del orquestador nunca llega a ejecutar generación; `EnterpriseProjectRequested` viaja por un bus sin oyentes en producción. | `DefaultEnterpriseProjectTrigger.java:33`, `EnterpriseProjectRequestedListener.java:34`, `KinConfig.java:572-578`, `ConversationOrchestrator.java:52,60`. |
| **Los motores reciben resultados del pipeline SIEMPRE vacíos en producción.** Los 4 resultados reales (`RecommendationResult`, `RiskResult`, `OpportunityResult`, `KnowledgeResult`) llegan como `null → empty()` tanto desde REST como desde el listener. TAM/SAM/SOM = `0.0`, riesgos = vacío, oportunidades = vacío, recomendaciones = vacío. | `EnterpriseWebMapper.java:185`, `EnterpriseProjectRequestedListener.java:75-76`, `EnterpriseGenerationRequest.java:49-52`. |
| **Enterprise Score se calcula y se descarta.** El octavo motor corre pero su resultado se ignora; el aggregate no porta score; el dashboard lo devuelve siempre `null`; la entidad JPA tiene columnas `@Embedded score` sin poblar. | `EnterpriseGenerationService.java:322-326`, `EnterpriseWebMapper.java:235`, `EnterpriseProjectEntity.java:72-73`. |
| **SSE es 100% en memoria por JVM.** `ConcurrentHashMap<ProgressKey, List<Subscription>>` (`EnterpriseProgressService.java:40`). Sin persistencia, sin replay, sin cola; en multi-instancia cada nodo solo ve sus propios `save`. | `EnterpriseProgressService.java:40,90-96`. |
| **2 de 9 tipos de documento no se generan.** `EXECUTIVE_REPORT` y `DOFA` existen en el enum (`DocumentType.java:12-22`) pero el assembler solo produce 7. | `EnterpriseDocumentAssembler.java:68-81`, Javadoc `:33-34`. |
| **Firma heredada sin lógica.** `EnterpriseGenerationOrchestrator.generate(UUID)` lanza `UnsupportedOperationException` (compatibilidad binaria del Milestone 1). | `EnterpriseGenerationOrchestrator.java:90-94`. |
| **Checksum engañoso.** El prefijo dice `"sha256"` pero usa `content.hashCode()` (no criptográfico). | `EnterpriseDocumentAssembler.java:43,173-175`. |
| **UI no cubre toda la API.** No hay botón de generación (`POST /generate`), ni `GET /export` resumen, ni detalle de documento; el dashboard no se refresca con eventos SSE; el score (null hoy) no se muestra. | `kin-enterprise-ui/src/App.tsx:38-56,109-110`, `enterpriseApi.ts:47-54`. |

### 1.3 Aún no existe

| Carencia | Detalle |
|---|---|
| **Generación narrativa con IA (DeepSeek).** Los textos de los documentos son serializaciones de VO + marcadores `"Por definir"`; no hay ni una llamada a `AIResponder`/`PromptAssembler` en `kin.enterprise`. El Javadoc de `DefaultBusinessModelEngine.java:13` lo declara como intención ("Java decide, DeepSeek únicamente comunica"). | |
| **Consumo del pipeline real como fuente.** Enterprise no importa `KinMethod`, `PipelineContext` ni `Pipeline`; depende del pipeline de forma inversa (es el dominio conversacional quien conoce a Enterprise). | |
| **Documentación del módulo.** No existe ningún documento de Fase 10 / Enterprise / M2A–M2J / roadmap M3 en `kin-docs/`, `README.md` ni `CHANGELOG.md`. No existe **ADR-018**. El BASELINE dice "migraciones V1…V4" y el README "V1…V6", pero existe **V7** enterprise no documentada. | |
| **`kin-database/init.sql` desincronizado.** No contiene las tablas enterprise (V7); en Docker la crearía Flyway, pero la base de referencia no está sincronizada. | |
| **Integración del UI con el frontend principal.** `kin-frontend` tiene 0 referencias a enterprise; el UI enterprise corre en `:5173`, origin no permitido por CORS (por defecto `http://localhost:3000`), token en `localStorage` no compartido entre origins, sin login/session/401. No está en `docker-compose.yml` ni en README. | `SecurityConfig.java:35`, `CorsConfig.java:13`, `docker-compose.yml`. |
| **Persistencia de los resultados del pipeline en la request enterprise** (el paso de datos reales del turno, ver M3-C). | |
| **Pruebas E2E / integración de extremo a extremo** (chat → REPORT → enterprise → dashboard). Solo unitarias + mock. | |

---

## 2. Dependencias reales

### 2.1 Módulos que Enterprise necesita

| Dependencia | Uso real hoy | Tipo |
|---|---|---|
| `kin.context` → `ContextRepository` / `ProjectContext` | **SÍ** — carga el contexto durable para la generación (`EnterpriseController.java:213-215`, `EnterpriseProjectRequestedListener.java:71`). | Puerto de dominio (ya existe, ADR-007). |
| `kin.conversation` → `ConversationOrchestrator` + `EnterpriseProjectTrigger` | **SÍ (código, inactivo)** — el orquestador invoca el trigger tras un turno REPORT. | Puerto enterprise, dependencia inversa. |
| `kin.event` → `DomainEventBus` | **SÍ** — publica `EnterpriseProjectRequested/Generated/Failed` (bus en memoria, sin consumidores productivos). | Infraestructura existente. |
| `kin.reporting` → `RecommendationResult`, `RiskResult`, `OpportunityResult`, `ConsultingReport` | **Parcial** — tipos presentes en la request pero siempre `null` en producción. | Tipos de dominio existentes. |
| `kin.knowledge` → `KnowledgeResult` | **Parcial** — idem (TAM/SAM/SOM dependen de los `facts`). | Tipos de dominio existentes. |
| `kin.ai` → `AIResponder`, `PromptAssembler` | **NO** — no se usa IA en la generación. | Puertos de dominio (ADR-008/012). |
| `engine` → `DomainEngine`/`EngineRegistry` | **NO (intencional)** — los 8 motores enterprise se aíslan deliberadamente del registry para no registrarse (`EnterpriseWebConfig.java:17-26`). | Contrato congelado, no tocar. |

### 2.2 Módulos que ya existen

Todos: `kin.context` (ADR-007), `kin.event` (bus en memoria), `kin.reporting` completo, `kin.knowledge` completo, `kin.interview`, `kin.ai` (`AIResponder`, `PromptAssembler`, `PromptType`), `engine` (registry/executor), `pipeline` (`KinMethod`, `Pipeline`, `PipelineContext`, 12 stages), `conversation` (orquestador, `TurnResult`, `TurnDirective`).

### 2.3 Módulos que aún faltan (para M3)

| Falta | Necesidad |
|---|---|
| **Un puerto/forma de pasar resultados reales del turno a Enterprise** (extensión aditiva del trigger o carga desde repositorios). | M3-C |
| **Una extensión aditiva de `PromptType`/`PromptAssembler` para el modo ENTERPRISE** (narrativa del resumen ejecutivo) sin violar la frontera ADR-012. | M3-E |
| **Persistencia del score** (el aggregate debe portarlo y el mapper mapearlo). | M3-D |
| **Beans Spring de wiring** (trigger y listener) + pasar el trigger real al orquestador. | M3-B |
| **Migración Flyway V7 documentada + `init.sql` sincronizado + despliegue (Docker/CORS prod).** | M3-H |
| **Integración de UI en el dashboard principal** (o decisión de standalone con CORS/SSO). | M3-F |

---

## 3. Riesgos

### 3.1 Técnicos

- **Generación en hilo asíncrono no controlado**: el listener actual captura `RuntimeException` y traga (`EnterpriseProjectRequestedListener.java:78-80`); sin un executor gestionado y un manejo de errores propagable, fallos silenciosos.
- **Doble disparo** si se cablea el trigger sin idempotencia: el REST `POST /generate` publica el evento y el ciclo conversacional también; hay que garantizar un único flujo (dedupe por `(projectId, version)`).
- **Documentos binarios escritos a mano**: PDF/DOCX/PPTX con JDK puro son frágiles ante cambios de contenido/unicode (PDF se normaliza a Latin-1, `PdfDocumentRenderer.java:118-133`). Riesgo de producción medio-alto si se escala.
- **`String.hashCode()` como checksum** — no apto para validación de integridad de artefactos.

### 3.2 Arquitectónicos

- **Regla congelada #2/#9 potencialmente violada**: los controllers y el `@Configuration` viven físicamente en `com.kinplatform.kin.*` (`EnterpriseWebConfig.java:28`, `EnterpriseController.java:63`, `EnterpriseApiExceptionHandler.java:38`), árbol que por contrato debe ser POJO puro sin Spring, con DTOs en `dto/` y config en `common/config/`. **Exige sanción por ADR o reubicación** (el usuario no quiere mover paquetes → ADR de excepción documentada).
- **BC Enterprise sin ADR-018** pese a caer en 4+ categorías de gobernanza (§3.1 de `KIN_ARCHITECTURE_GOVERNANCE.md`): nuevo Bounded Context, nuevos motores, cambio de persistencia (V7).
- **Frontera ADR-012**: si se quiere narrativa LLM, la extensión de `PromptType` debe ser **aditiva** (nuevo enum value o wrapper), nunca modificar el contrato REPORT que solo consume `ConsultingReport`.
- **Acoplamiento por tipos**: pasar `RecommendationResult`/`RiskResult`/etc. al trigger crea dependencia de Enterprise con 4 BC; debe hacerse con records de dominio existentes (no duplicados) y aditivamente.

### 3.3 Rendimiento

- **Latencia de LLM en generación async** (M3-E): la narrativa con DeepSeek debe correr fuera del turno del chat (nunca bloquear `POST /chat`), con timeout y fallback español garantizado (patrón `AiEngineService`).
- **SSE en memoria**: con N instancias, el cliente puede conectarse a una instancia que no ejecutó la generación y no recibir eventos; para multi-instancia haría falta un pub/sub real (fuera de M3, registrar como deuda).
- **Regeneración de versiones**: cada `generate` calcula 8 engines + 7 renderizados; sin caché de `inputHash` (hoy no se usa) se regenera todo.

### 3.4 Integración

- **CORS dual** (Security + Cors): añadir el origin enterprise a ambos o integrar el UI en el mismo origin (recomendado).
- **`init.sql` vs Flyway V7**: divergencia → despliegue desde cero en Docker puede dar esquemas distintos al dev.
- **Token/sesión**: el UI enterprise en `:5173` no comparte `localStorage` con `:3000`; sin login propio no hay sesión válida (salvo integración o proxy).
- **Tests**: no existe E2E backend→frontend; los 55 tests de UI mokean todo; la conexión REST→evento→listener solo se prueba de forma aislada en unit.

---

## 4. Roadmap M3 (milestones incrementales e independientes)

Cada milestone preserva: contratos congelados (BASELINE §4), arquitectura hexagonal/DDD, Open/Closed, sin deuda, sin duplicación. Orden de implementación sugerido = orden de numeración.

---

### M3-A — ADR-018 + Documentación del Bounded Context Enterprise

- **Objetivo**: sancionar el BC Enterprise por gobernanza y dejar el módulo documentado (es requisito de gobierno ANTES de implementar; puede correr en paralelo con M3-B).
- **Alcance**: Redactar `ADR-018-enterprise-document-generation.md` (nuevo BC, 8 engines fuera del registry, migración V7, excepción documentada a la regla "kin.* POJO puro" para los controllers web del BC, puertos `EnterpriseProjectRepository`/`DocumentRenderer`). Actualizar `BASELINE_ARCHITECTURE.md` (§4.1/§4.3/§5.1), `README.md` (roadmap, comandos, env `VITE_API_URL`), `CHANGELOG.md` y release notes. Listar los 14 endpoints.
- **Riesgos**: proceso de gobernanza (mín. 1 día hábil de discusión); riesgo bajo de código.
- **Archivos involucrados**: `kin-docs/adr/ADR-018-enterprise-document-generation.md`, `kin-docs/BASELINE_ARCHITECTURE.md`, `README.md`, `CHANGELOG.md`, `kin-docs/ARQUITECTURA_BASE_KIN_2.0.md`.
- **Pruebas necesarias**: ninguna de código; revisión de consistencia de la doc (grep de rutas/endpoints contra código).
- **Criterio de aceptación**: ADR-018 aprobada; BASELINE/README/CHANGELOG reflejan Enterprise, V7 y los puertos; sin cambios de código.

---

### M3-B — Wiring del ciclo automático (conversación → generación)

- **Objetivo**: activar el código muerto. Al completar un turno REPORT real con `consultingReport`, la generación Enterprise se dispara de verdad.
- **Alcance**: Anotar `DefaultEnterpriseProjectTrigger` y `EnterpriseProjectRequestedListener` como beans Spring (o definirlos en `EnterpriseWebConfig`); pasar el trigger real al `ConversationOrchestrator` **vía el constructor sobrecargado ya existente** (6 args) en `KinConfig` — sin tocar la firma congelada `orchestrate/orchestrateStream`. Ejecutar la generación con un `Executor` gestionado (async con timeout) y propagar fallos al aggregate `FAILED` + evento `EnterpriseProjectFailed` (evitar tragar excepciones en el listener).
- **Riesgos**: doble disparo con `POST /generate` → resolver con idempotencia por `(projectId, version)`; hilos no gestionados → usar bean de executor; impacto en `ChatOrchestratorServiceImplTest` y `KinMethodTest` (asegurar que el constructor usado siga siendo el de 5 args en esos tests).
- **Archivos**: `KinConfig.java`, `EnterpriseWebConfig.java`, `DefaultEnterpriseProjectTrigger.java`, `EnterpriseProjectRequestedListener.java`, `EnterpriseGenerationService.java`, tests de contexto Spring.
- **Pruebas**: nuevo test de wiring (`@SpringBootTest` que el bean inyectado no sea NO-OP); test de integración REST/evento → generación; test de idempotencia ante doble disparo; conservar los 1786 tests verdes.
- **Criterio de aceptación**: un turno REPORT completo crea/persiste `EnterpriseProject` versionado, emite progreso por SSE y termina en `COMPLETED` o `FAILED`; `POST /generate` concurrente no duplica versión.

---

### M3-C — Resultados reales del pipeline en la generación

- **Objetivo**: que los motores trabajen con datos reales del turno (TAM/SAM/SOM, riesgos, oportunidades, recomendaciones) y no con `empty()`.
- **Alcance**: Extender **aditivamente** el puerto `EnterpriseProjectTrigger` (nuevo método `request(projectId, pipelineResults)` con un record contenedor de los 4 resultados de dominio ya existentes) y el evento `EnterpriseProjectRequested`; el `ConversationOrchestrator` ya dispone del `TurnResult` con el report — pasar los resultados que el turno tiene disponibles. Alternativa aceptable: el listener carga `RecommendationResult`/`RiskResult`/`OpportunityResult`/`KnowledgeResult` desde sus repositorios/estados si son durables. No duplicar tipos: importar los records existentes de `kin.reporting`/`kin.knowledge`.
- **Riesgos**: acoplamiento a 4 BC (mitigado con record contenedor en el puerto enterprise); cambios en `EnterpriseGenerationRequest` (aditivo, con default `empty()` para no romper REST); revisar que los motores ya usan esos campos (`DefaultMarketEngine.java:96-131`, `DefaultRiskPlanEngine.java:75`, `DefaultInnovationEngine.java:83-152`).
- **Archivos**: `EnterpriseProjectTrigger.java`, `DefaultEnterpriseProjectTrigger.java`, `ConversationOrchestrator.java` (invocación), `events/EnterpriseProjectRequested.java`, `EnterpriseGenerationRequest.java`, `EnterpriseWebMapper.java` (pasar resultados reales si el REST los tiene o dejarlos empty), tests de motores con datos reales.
- **Pruebas**: unit de los 8 engines con `KnowledgeResult`/`RiskResult`/`OpportunityResult`/`RecommendationResult` no vacíos; test de integración turno REPORT → generación con resultados poblados; verificar que TAM/SAM/SOM > 0 y riesgos reflejados.
- **Criterio de aceptación**: los documentos generados reflejan los datos reales del pipeline (sin `"Por definir"` ni `0.0` cuando el pipeline tenga datos); los 1786 tests siguen verdes.

---

### M3-D — Enterprise Score persistido y expuesto

- **Objetivo**: el score (8 dimensiones) deja de descartarse: se persiste, se devuelve y se muestra.
- **Alcance**: que `EnterpriseProject` porte el `EnterpriseScore` resultante; mapearlo en `EnterpriseProjectMapper` y en la entidad JPA (las columnas `@Embedded` ya existen); exponerlo en `EnterpriseWebMapper` (hoy `null`, `:235`) en dashboard/latest/version; actualizar el DTO `EnterpriseScoreSection`.
- **Riesgos**: migración de datos de versiones existentes (opcional: recomputar); bajo.
- **Archivos**: `EnterpriseProject.java`, `EnterpriseProjectEntity.java`, `EnterpriseProjectMapper.java`, `EnterpriseWebMapper.java`, `EnterpriseDashboardResponse.java`, `EnterpriseScoreSection.java`, `EnterpriseGenerationService.java:322-326`.
- **Pruebas**: mapper round-trip con score; `EnterpriseDashboardControllerTest` con score no-null; test de persistencia JPA.
- **Criterio de aceptación**: `GET .../dashboard` y `.../latest` devuelven score con 8 dimensiones y grado; el score sobrevive a un reinicio (H2 y Postgres).

---

### M3-E — Documentos narrativos con IA (DeepSeek) + EXECUTIVE_REPORT y DOFA

- **Objetivo**: completar los 9 tipos de documento; el `EXECUTIVE_REPORT` con narrativa generada por LLM (vía `AIResponder`), DOFA determinista (desde contexto + riesgos + oportunidades), manteniendo "Java decide, LLM comunica".
- **Alcance**: Implementar el DOFA de forma determinista (nuevo motor puro, DOFA = f(ProjectContext, RiskResult, OpportunityResult)); implementar `EXECUTIVE_REPORT` como motor + assembler con narrativa: **extensión aditiva** de `PromptType` (nuevo valor `ENTERPRISE_EXECUTIVE` o wrapper) y de `PromptAssembler` sin tocar la frontera ADR-012 (REPORT sigue consumiendo solo `ConsultingReport`); invocar `AIResponder.respond` dentro del flujo async con timeout y fallback español (reutilizar `AiEngineService` vía puerto `AIResponder`, no el servicio concreto); el assembler/`DocumentRenderer` debe soportar texto largo y salto de página (PDF hoy va sin compresión).
- **Riesgos**: **alto** — latencia LLM (nunca bloquear el turno del chat), frontera ADR-012 (requiere ADR-018 que lo habilite o ADR complementaria), determinismo del DOFA, renderizado de texto largo. Decisión de diseño a validar: ¿la narrativa se persiste como documento o como sección dentro del documento binario?
- **Archivos**: nuevos `DefaultDofaEngine` + `DefaultExecutiveReportEngine` (o assemblers), `DocumentType`, `EnterpriseDocumentAssembler.java`, `kin.ai` (`PromptType`, `PromptAssembler` — aditivo), `EnterpriseGenerationService.java` (inyectar `AIResponder`), `EnterpriseRendererFactory.java`/`PdfDocumentRenderer.java`, tests.
- **Pruebas**: unit del DOFA (determinista, sin I/O); unit de la extensión de prompt (additiva, no rompe tests de `PromptAssemblerTest`); mock de `AIResponder` para la narrativa (patrón de `AiEngineServiceTest`); fallback español.
- **Criterio de aceptación**: versiones generadas contienen 9 documentos; `EXECUTIVE_REPORT` con narrativa LLM (o fallback en español) y DOFA derivado de datos reales; los tests de `kin.ai` y `kin.conversation` siguen verdes (extensión aditiva).

---

### M3-F — Integración del UI Enterprise en el dashboard principal

- **Objetivo**: Enterprise accesible desde `kin-frontend` (misma sesión, mismo origin), eliminando la app standalone de `:5173`.
- **Alcance**: Portar los componentes de `kin-enterprise-ui` a rutas del Next.js App Router (`/dashboard/projects/[id]/enterprise`), reutilizando `services/api.ts`, `SessionGuard` y el proxy; añadir botón/entrada desde `app/dashboard/projects/[id]/page.tsx`; conservar el hook SSE (`useEnterpriseProgress`) adaptado a Next. **Decisión a tomar**: portar a Next (recomendado, sin duplicar UI y con auth real) vs. servir el build Vite vía rewrite + arreglar CORS (más barato, pero mantiene dos apps). Si se elige portar, migrar los 55 tests a Vitest/Jest del monorepo frontend.
- **Riesgos**: coste del port; duplicación de componentes si no se reutiliza; CORS si se mantiene standalone; el proxy de Next solo protege `/dashboard/*`.
- **Archivos**: `kin-frontend/src/app/dashboard/projects/[id]/enterprise/*`, `kin-frontend/src/services/enterprise.ts`, componentes portados, `kin-frontend/src/components/...`, tests.
- **Pruebas**: portar los 55 tests; test de la nueva ruta con `SessionGuard`; test de que la entrada de navegación aparece en el proyecto.
- **Criterio de aceptación**: un usuario autenticado navega del dashboard a Enterprise, ve dashboard/versiones/documentos y descarga PDF/DOCX/PPTX, todo con su sesión real y sin errores CORS.

---

### M3-G — Acción de generación desde la UI

- **Objetivo**: el usuario inicia/regenera la generación desde la UI y ve el resultado en vivo.
- **Alcance**: botón "Generar plan" que llama `POST /enterprise/{projectId}/generate` (manejo de 201/202/409/422); el hook SSE, al recibir `COMPLETED`/`FAILED`, dispara refetch del dashboard (hoy no refresca: `App.tsx:38-56`); mostrar `failedReason` y errores persistentes.
- **Riesgos**: 409 (versión en vuelo) y 422 (sin contexto) deben tener mensajes claros; bajo.
- **Archivos**: `kin-frontend` enterprise components (GenerateButton), `useEnterpriseProgress.ts` (callback onComplete), `App/page` enterprise, `services/enterprise.ts`, tests.
- **Pruebas**: unit del botón (success, 409, 422); test de refetch tras evento terminal; test de visualización de `failedReason`.
- **Criterio de aceptación**: desde la UI se dispara una generación, la barra de progreso avanza vía SSE y al terminar el dashboard muestra los documentos/score sin recarga manual.

---

### M3-H — Infraestructura de producción

- **Objetivo**: Enterprise despliega igual que el resto del stack (Docker + Postgres + CORS prod).
- **Alcance**: Sincronizar `kin-database/init.sql` con la migración V7 (o documentar que Flyway la crea y ajustar `init.sql` al esquema mínimo); añadir servicio `kin-enterprise-ui` a `docker-compose.yml` (o integrarlo en el frontend según M3-F); añadir el origin de la UI y `:5173` en `app.cors.allowed-origins` (Security y Cors, dual); revisar `application-prod.properties`; verificar `checksum` Flyway en Postgres desde cero.
- **Riesgos**: divergencia `init.sql`/Flyway; checksum; puertos en conflicto; medio.
- **Archivos**: `kin-database/init.sql`, `docker-compose.yml`, `application-prod.properties`, `SecurityConfig.java`, `CorsConfig.java`, `.env.example`.
- **Pruebas**: `docker compose up --build` limpio; migración desde cero en Postgres; smoke test de los 3 controllers enterprise; curl del SSE.
- **Criterio de aceptación**: despliegue completo crea `enterprise_project`/`enterprise_document`, la UI es accesible con CORS correcto y la generación + exportación funcionan contra Postgres.

---

## 5. Tabla resumen

| Milestone | Estado | Prioridad | Complejidad | Riesgo |
|---|---|---|---|---|
| M3-A — ADR-018 + documentación | No iniciado | Alta (gate de gobernanza) | Baja | Bajo |
| M3-B — Wiring ciclo automático | No iniciado | Alta | Baja | Medio |
| M3-C — Resultados reales del pipeline | No iniciado | Alta | Media | Medio |
| M3-D — Score persistido y expuesto | No iniciado | Media | Baja | Bajo |
| M3-E — Narrativa IA + EXECUTIVE_REPORT/DOFA | No iniciado | Media | Alta | Alto |
| M3-F — Integración UI en kin-frontend | No iniciado | Media | Alta | Medio |
| M3-G — Acción de generación desde la UI | No iniciado | Media | Media | Bajo |
| M3-H — Infraestructura de producción | No iniciado | Alta | Media | Medio |

---

## 6. Recomendación final

**Implementar primero M3-B (Wiring del ciclo automático), precedido por M3-A (ADR-018) en paralelo.**

Razones:

1. **Es el habilitador de todo lo demás.** El trigger y el listener ya están escritos y testeados, pero son código muerto: `ConversationOrchestrator` usa `NO_OP_TRIGGER` (`KinConfig.java:571-579`). Sin M3-B, Enterprise jamás se dispara desde el flujo real de la conversación, por lo que "consumir el pipeline real" y "usar el contexto real" son solo teóricos.
2. **Relación coste/beneficio óptima.** Es el milestone con la mayor ganancia funcional por menor complejidad y riesgo (baja complejidad, riesgo medio controlable con idempotencia y un executor gestionado). Es puramente aditivo: usa el constructor sobrecargado ya existente del orquestador y no toca ningún contrato congelado.
3. **Desbloquea la cadena de valor.** M3-B habilita M3-C (datos reales del pipeline, donde están los mayores placeholders: `0.0` en TAM/SAM/SOM, riesgos vacíos, "Por definir") y M3-D; juntos convierten el módulo de "generador offline con datos simulados" en un generador que consume el contexto y los resultados reales del pipeline.
4. **M3-A debe correr en paralelo por gobernanza** (ADR obligatoria antes de implementar según `KIN_ARCHITECTURE_GOVERNANCE.md` §3.1), pero no bloquea la planificación: es documentación, bajo riesgo y puede revisarse mientras se implementa M3-B.

Después de M3-B → M3-C → M3-D, la "primera versión funcional de verdad" queda completada. M3-E (IA/narrativa) y M3-F/G (UI) añaden valor de producto; M3-H cierra el despliegue. **No abordar antes** M3-E por el riesgo de latencia/frontera ADR-012 sin primero tener el ciclo automático estable.
