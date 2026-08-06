# Security Hardening Checklist — Proyecto KIN

> Checklist de endurecimiento de seguridad (estilo OWASP ASVS v4 + Zero-Trust), derivada de la **Segunda Auditoría Crítica**.
> Leyenda de estado: **[ ]** pendiente · **[x]** cumplido · **[~]** parcial.
> Evidencia de hallazgos en `TECHNICAL_DEBT_REGISTER.md` (T#) y `AUDITORIA_TECNICA_INTEGRAL.md` §16.

---

## V1 — Gestión de secretos y credenciales

| # | Control | Estado | Evidencia / Ticket |
|---|---|---|---|
| S1 | No hay secrets de producción commitidos en el repo | [ ] | **CRÍTICO**: JWT_SECRET en `frontend-ci.yml:99` (T5/K-401) |
| S2 | Secrets gestionados en secrets manager (GitHub Secrets / Render / SSM) | [ ] | `.env` gitignored, pero CI usa valor hardcodeado |
| S3 | Rotación periódica de JWT/claves (con ventana de doble clave) | [ ] | — |
| S4 | Detector de secretos en CI (gitleaks/trufflehog) bloqueante | [ ] | — |
| S5 | No hay claves API LLM en código/comentarios | [ ] | revisar ProviderRouter/DeepSeekConfig (usa env) |
| S6 | `DATABASE_URL` en formato JDBC y sin credenciales en repo | [ ] | T14/K-601 |

## V2 — Autenticación y sesión (ASVS 2, 3)

| # | Control | Estado | Evidencia / Ticket |
|---|---|---|---|
| S7 | Password policy (longitud ≥ 12, complejidad) | [ ] | `AuthServiceImpl.register` sin validación (T8/K-502) |
| S8 | No hay enumeración de cuentas en login/register | [ ] | login genérico OK; **register filtra email** (T8) |
| S9 | Rate limiting anti fuerza bruta por IP real (no spoofeable) | [ ] | `RateLimitingFilter` confía en header (T7/K-501) |
| S10 | Token de sesión NO accesible por JS (HttpOnly cookie Secure+SameSite) | [ ] | JWT en `localStorage` + cookie (T9/K-503) |
| S11 | Expiración de sesión corta + renovación | [ ] | JWT ~24h; sin refresh |
| S12 | Protección CSRF para mutaciones en estado (si cookies) | [ ] | depende de S10 |
| S13 | Logout invalida token/sesión server-side | [~] | stateless JWT; borrado client-side |

## V3 — Autorización (ASVS 4)

| # | Control | Estado | Evidencia |
|---|---|---|---|
| S14 | Autorización por capabilities (ownership en services) | [x] | `ChatOrchestratorServiceImpl.findProject` verifica owner (L224-226); `ChatService` idem |
| S15 | Autorización de `/admin/**` y `/test/**` por rol ADMIN | [x] | `SecurityConfig.java:54-60` |
| S16 | Autorización a nivel de método (@PreAuthorize/@Secured) en operaciones sensibles | [ ] | Solo URL matchers; sin anotaciones de método |
| S17 | Endpoints de billing/estado de suscripción protegidos | [x] | autenticados por defecto (`anyRequest().authenticated()`) |
| S18 | Sin IDOR en proyectos/mensajes/historial | [x] | validado en ChatService/ProjectService |

## V4 — Configuración y manejo de errores (ASVS 7)

| # | Control | Estado | Evidencia |
|---|---|---|---|
| S19 | No exponer detalles internos en respuestas de error | [ ] | `GlobalExceptionHandler` devuelve `ex.getMessage()` (T6/K-402) |
| S20 | `/actuator/**` restringido (roles o red interna) | [ ] | `permitAll` (T6/K-402) |
| S21 | Cabeceras de seguridad: HSTS, CSP, X-Content-Type-Options, Referrer-Policy | [~] | no configuradas explícitamente en proxy (K-506) |
| S22 | CORS minimalista y consistente (una sola fuente) | [ ] | dual config (T12/K-506) |
| S23 | Logs sin datos sensibles (mensajes de usuario, tokens, passwords) | [ ] | `ChatController` loguea el mensaje (T13/K-507) |

## V5 — Lógica de negocio y pagos (ASVS 8, 10)

| # | Control | Estado | Evidencia |
|---|---|---|---|
| S24 | Idempotencia de webhooks de pago (replay) | [ ] | Stripe webhook sin `event.id` dedup (T4/K-301) |
| S25 | Verificación de firma de webhook | [x] | `constructWebhookEvent` usa firma |
| S26 | Límites de negocio consistentes con caché (projectLimit) | [ ] | no se evicta al borrar (T2/K-203) |
| S27 | Rate limiting de endpoints sensibles (auth, AI) | [~] | solo `/auth/**` (T7/K-501) |

## V6 — Inyección, XSS, SSRF (ASVS 5, 6, 11)

| # | Control | Estado | Evidencia |
|---|---|---|---|
| S28 | SQL/JPA parameterizado (sin concatenación) | [x] | repos JPA/JdbcTemplate con binds |
| S29 | XSS mitigado (escapado React + CSP) | [~] | React escapa por defecto; CSP pendiente |
| S30 | SSRF en adquisición de conocimiento (SourceValidator/allowlist HTTPS) | [x] | `SourceValidator` exige HTTPS + allowlist (ADR-014) |
| S31 | Prompt injection mitigado (guardrails, decisión en Java) | [x] | `PromptGuardrail`, ResponseGuard (ADR-013) |
| S32 | Validación de entrada con Bean Validation en DTO | [~] | `@Valid` en chat; revisar resto |

## V7 — Datos y privacidad

| # | Control | Estado |
|---|---|---|
| S33 | Cifrado en tránsito (TLS) en prod | [ ] | Render/dominio TLS pendiente |
| S34 | Cifrado en reposo (volumen/tablas) | [ ] | — |
| S35 | GDPR/DPA: exportar y borrar datos | [ ] | sin endpoint de GDPR |
| S36 | Copias de seguridad y DR | [ ] | T24/K-703 |

---

## Prioridad de ejecución

1. **P0 (esta semana)**: S1, S2, S19, S20 (rotar secretos, cerrar actuator, errores genéricos).
2. **P1 (sprints 1-2)**: S9, S7, S8, S10, S23 (rate limit real, password, anti-enumeración, cookie, logs).
3. **P2 (sprints 3-4)**: S24, S27, S21, S22, S16.
4. **P3 (backlog)**: S33-S36.

Cada control con ticket asociado en `SPRINT_BACKLOG_ENTERPRISE.md`. Re-evaluar tras cada sprint con un escaneo (gitleaks + OWASP ZAP básico + revisión de endpoints).
