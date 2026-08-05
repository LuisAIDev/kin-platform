# KIN_SECURITY_GOVERNANCE_SPECIFICATION.md — FASE 18

**Especificación Oficial de Seguridad, Gobierno, Compliance y Trust de KIN.**
*Contrato oficial entre Security, Cybersecurity, Architecture, Backend, Frontend, DevOps, SRE, QA, IA, Enterprise, Product y CTO. 100 % consistente con F12–F17. No modifica arquitectura, DAG, ADR, contratos, puertos, eventos, Core congelado ni AI Boundary. Terminología oficial de F12–F17, sin sinónimos.*

---

## 1. Security Vision

**La seguridad es un atributo del producto**, no una fase. KIN protege: la arquitectura (Core congelado), los datos (datasets inmutables con confianza), los usuarios (planes y ownership), la IA (AI Boundary), los plugins y el Marketplace (vetting y firma), Enterprise (documentos) y la operación (SLOs y GitOps).

**Modelo de confianza:**
- **Zero Trust**: nunca confiar, siempre verificar (identidad en cada llamada, mTLS, mínimo privilegio).
- **Defense in Depth**: múltiples capas independientes de control.
- **Trust by Design / Security by Default**: lo inseguro no existe por defecto.
- **Privacy by Design**: PII minimizada, opt-in, residencia y retención desde el diseño.

---

## 2. Security Principles

| # | Principio |
|---|---|
| S1 | **Zero Trust** — cada llamada se autentica y autoriza; sin confianza implícita de red. |
| S2 | **Least Privilege** — mínimo acceso necesario para cada identidad/servicio. |
| S3 | **Default Deny** — lo no permitido está denegado por defecto. |
| S4 | **Defense in Depth** — capas independientes (red, app, datos, IA). |
| S5 | **Immutable Infrastructure** — artefactos inmutables, firmados, sin configuración manual (F17 §2). |
| S6 | **Secure by Design** — seguridad en el diseño (DoR), no al final. |
| S7 | **Privacy by Design** — PII minimizada y gobernada desde el modelo. |
| S8 | **Auditability** — toda decisión y acceso quedan registrados (`DecisionRecord`, `DataAccessAudited`). |
| S9 | **Deterministic Security** — las políticas de seguridad son datos evaluados determinísticamente (PolicyEngine), nunca heurísticas opacas. |

---

## 3. Identity & Access Management

| Tema | Definición |
|---|---|
| **Usuarios** | Aggregate `User` (existente); identidad UUID; roles: **FREE, PREMIUM, FACILITADOR, ADMIN**. |
| **Planes** | `PricingPlan` (pricing_plans) limita capacidad vía `PlanAccessPort` (CostEngine, F12A §4). |
| **Permisos** | RBAC por rol; **ABAC (futuro)** para acceso por atributos (país, dataset, feature). |
| **Ownership** | Todo recurso de proyecto pertenece a su usuario; el acceso se valida por ownership (gobernanza §10.8). |
| **JWT** | Emisión por `JwtService` (common.security); verificación en `JwtAuthenticationFilter`. |
| **Refresh / Revocation** | Tokens de refresco con rotación; revocación por sesión en incidentes. |
| **Enterprise / Facilitador / Admin** | Enterprise: módulo/BC con documentos por proyecto; FACILITADOR: asesoría con ownership explícito; ADMIN: operación y Marketplace. |

---

## 4. Authentication

| Política | Definición |
|---|---|
| JWT | `JwtService` firma y valida; expiración corta (acceso) + refresh con rotación. |
| Expiration / Rotation | Tokens con expiración; refresh rotativo; revocación inmediata ante incidente. |
| Ownership | La sesión autentica al `User`; nunca se asume identidad por datos del body. |
| Session Policy | Sesiones por usuario; revocación global en P0. |
| Password Policy | **Política de contraseñas**: requisitos de creación y gestión de credenciales de usuario — hash con sal (BCrypt/Argon2), longitud mínima, sin almacenamiento de texto plano. |
| MFA (futuro) | Habilitación para ADMIN/Enterprise en el roadmap de seguridad. |

---

## 5. Authorization

| Ámbito | Regla |
|---|---|
| Roles | RBAC: FREE/PREMIUM/FACILITADOR/ADMIN (capacidades por rol). |
| Policies | `DomainPolicy` evaluadas por `PolicyEngine` (datos, no branching). |
| Ownership | Proyectos: solo su dueño (o FACILITADOR autorizado). |
| **Project Security** | `Project` por `userId`; acceso negado a terceros. |
| **Enterprise Security** | `EnterpriseProject` por `(projectId, version)`; documentos del dueño. |
| **Country Security** | Datos de país: públicos y gobernados (datasets compartidos sin PII). |
| **Dataset Access** | Datasets inmutables de lectura; sin mutación desde el request path. |
| **Feature Access** | `FeatureRegistry` + plan (`PlanAccessPort`). |

---

## 6. Secrets Management

- **Vault** central (SecurityGateway, F12A §7): cifrado **AES-GCM**, acceso por política.
- **Rotation**: ventana de solape; evento `SecretsRotated`; auditoría.
- **Certificates / mTLS**: gestión con rotación automática; mTLS interno entre servicios.
- **Plugin Signatures**: plugins firmados y verificados antes de cargar (F12A §13).
- **Provider Credentials**: credenciales de `ConnectorCatalog` viven en Vault; jamás en código/`.env` prod.

---

## 7. Cryptography

| Algoritmo | Uso |
|---|---|
| AES | Cifrado en reposo (Vault AES-GCM, datos PII) |
| TLS | Todo tráfico externo e interno (terminación Ingress + mTLS) |
| SHA | Integridad de artefactos (SBOM, digests) |
| JWT | Firma de tokens (HS256/RS256 según `JwtService`) |
| Password Hash | BCrypt/Argon2 con sal |
| Digital Signatures | Firmado de artefactos (cosign), plugins, SBOM |

---

## 8. AI Security

| Área | Control |
|---|---|
| **AI Boundary** | La IA solo redacta; jamás decide (F12B §7). |
| **AiContext** | Único insumo del LLM: datos procesados, sin fuentes crudas (frontera ADR-012). |
| **PII** | `PiiPolicy`/`RedactionPolicy` antes de invocar IA; PII de prueba negativa en QA (F16 §12). |
| **PromptAssembler** | Ensambla solo contexto gobernado; versión de prompt registrada. |
| **Prompt Injection** | El AiContext es estructurado y no contiene instrucciones del usuario sin gobernar; se trata como dato. |
| **Data Leakage** | `ContextBudgetPolicy` + RedactionPolicy; nunca `PipelineContext` completo (gobernanza §7.9). |
| **Hallucination Mitigation** | Todo dato tiene fuente/fecha/confianza; la IA no inventa números que no estén en el AiContext. |
| **ResponseGuard / ResponseFallback** | Validación de conformidad + respuesta segura sin motivos técnicos. |
| **AIToolRegistry / DecisionRecord** | Selección de modelo por política Java; `ModelSelected` registrado. |

---

## 9. Data Security

| Datos | Control |
|---|---|
| PII | `DataSensitivity`/`PiiFlag`, minimización, opt-in (Experience), retención, residencia. |
| Datasets | **Inmutables**, versionados, content-addressed (ID determinista); solo lectura. |
| Object Storage | Documentos Enterprise cifrados en reposo, acceso por ownership. |
| Redis | Solo datos efímeros/derivados; **nunca PII ni AiContext de usuario** (F12B §5). |
| PostgreSQL | Cifrado en reposo (volumen), PITR para DR, acceso por service account con mínimo privilegio. |
| Encryption | En reposo (AES) + en tránsito (TLS). |
| Versioning / Immutable Data | Datasets y series append-only; los snapshots se versionan. |

---

## 10. Enterprise Security

- **Document Generation / DOFA / Reports** (PDF/DOCX/PPTX): generados por `EnterpriseProject` del proyecto dueño; narrativa sobre datos procesados (frontera ADR-012/018).
- **Ownership**: `enterprise_document` pertenece a `(projectId)`; acceso solo del dueño.
- **Tenant Isolation**: Enterprise nunca expone datos de otros proyectos/usuarios; el feed BIL entra por puerto sin acoplamiento.

---

## 11. Marketplace Security

| Control | Definición |
|---|---|
| **Plugin Verification** | Firma digital verificada antes de cargar (F12A §13). |
| **SDK Validation** | `PluginDescriptor` validado (dominio, países, licencia, costo) en el arranque. |
| **Contract Tests** | CDCT obligatorios por plugin/conector. |
| **Sandbox (futuro)** | Aislamiento de classloader/límites de red en P3 (F12A §13). |
| **Vetting** | Revisión de firma, licencia y tests antes de `MarketplaceListed`. |
| **Registry** | `PluginRepository` con ciclo de vida **alineado a los eventos oficiales**: `registered` (`PluginRegistered`) → `verified` (`PluginVerified`) → `listed` (`MarketplaceListed`); la deprecación sigue la política oficial (2 fases MINOR, F12B §4) y se documenta con su reemplazo. |

---

## 12. API Security

| Control | Definición |
|---|---|
| REST | Recursos versionados (`/api/v1/...`), ownership en cada endpoint. |
| RFC7807 | Errores con `{type, title, status, detail, instance, queryId}` (F14 §10). |
| Rate Limiting | Por usuario/plan/IP; límites de `CostEngine` y cuotas de providers. |
| Validation | Validación de entrada en el borde (DTO), nunca en el dominio. |
| OpenAPI | Contrato expuesto y validado por CDCT. |
| Contract Versioning | SemVer + deprecación 2 fases (F12B §4). |
| Replay Protection | Idempotencia (eventos, requests de escritura) + expiración de tokens. |

---

## 13. Infrastructure Security

| Capa | Control |
|---|---|
| Kubernetes | RBAC, NetworkPolicies, Pod Security, seccomp; nada manual en prod (GitOps). |
| Docker | Imágenes inmutables sin secrets; escaneo de vulnerabilidades. |
| Ingress | Terminación TLS, rate limit, **WAF** (Web Application Firewall: capa que filtra y mitiga ataques HTTP, p. ej. OWASP Top 10, antes de llegar al backend) para APIs públicas. |
| Network Policies | Aislamiento por servicio; solo rutas permitidas. |
| Vault | Secrets central, mTLS interno. |
| Redis/PostgreSQL | Acceso por service account, TLS, mínimo privilegio, backups. |
| GitOps (ArgoCD/Flux) | Manifiestos firmados, revisados y promovidos por Git (F17 §9). |

---

## 14. Compliance

| Marco | Estado objetivo |
|---|---|
| GDPR | Residencia, derecho al olvido, DPAs, consentimiento de experiencia. |
| Residency | Datos por región (F19); datasets por región. |
| PII | Minimización, retención, auditoría de acceso. |
| Audit | `DecisionRecord` + `DataAccessAudited` + compliance log de scraping. |
| SBOM | Por release, firmado (F17 §7). |
| OWASP | SAST/DAST/ASVS en el pipeline (F16 §11). |
| NIST 800-53 / ISO 27001 / SOC2 Ready | **Objetivos del roadmap de seguridad** (no requisitos obligatorios actuales): la postura y las evidencias se alinean progresivamente; la certificación es meta de roadmap con dueño y fecha, sin afectar la arquitectura vigente. |

---

## 15. Security Monitoring

| Área | Definición |
|---|---|
| Security Metrics | `bil.security.*` (accesos, intentos, rate limits, vetting) |
| Alerts | P0/P1/P2/P3 con severidad y runbook (F17 §13) |
| Threat Detection | Anomalías de acceso, rate-limit abuses, patrones de scraping |
| Audit Logs | `DataAccessAudited` + `DecisionRecord` (append-only) |
| Tracing | `queryId`/`traceId` en logs de seguridad |
| Incident Detection | SLO breach + detección de intrusiones → IR (cap. 17) |

---

## 16. Threat Model (STRIDE)

| Asset | Amenazas (STRIDE) | Mitigación |
|---|---|---|
| **API pública** | Spoofing (identidad falsa), Tampering, DoS, Repudiation | JWT, TLS, rate limit, logs de auditoría |
| **Datasets** | Tampering, Information Disclosure | Inmutables + content-addressed + residencia |
| **AiContext/LLM** | Tampering (prompt injection), Information Disclosure (leakage) | AI Boundary, RedactionPolicy, ContextBudgetPolicy |
| **Vault/Secrets** | Information Disclosure, Elevation | AES-GCM, rotación, mTLS, mínimo privilegio |
| **Marketplace/Plugins** | Tampering, Elevation, Malicious code | Firma, vetting, CDCT, sandbox futuro |
| **Enterprise docs** | Information Disclosure (tenant) | Ownership + cifrado en reposo + aislamiento |
| **Redis** | Tampering (cache poisoning) | Solo datos efímeros, nunca PII, TLS |

**Trust Boundaries**: Internet → Ingress → App → Datos; Vault/Secrets; LLM (externa, solo redacta); Marketplace (terceros, firmado).

---

## 17. Incident Response

| Etapa | Definición |
|---|---|
| Detección | Alertas de seguridad + SLO breach + threat detection |
| Clasificación | P0 (compromiso/fuga) → P4; impacto en datos/IA/tenants |
| Contención | Revocación de tokens, rotación de secrets, kill-switch de provider/plugin/feature (FeatureRegistry) |
| Recuperación | Restore (F17 §15), rollback determinista, vuelta a servicio |
| Postmortem | Blameless + RCA + acciones con dueño y fecha |
| Lessons Learned | Actualización de runbooks, threat model y checks |

---

## 18. Operational Security (DevSecOps)

- **Security Gates** en CI/CD: SAST, secrets scan, dependencias, DAST (F16 §11).
- **Pipeline Security**: artefactos firmados, SBOM, sin secrets en logs.
- **Release Security**: approval de seguridad en RC/GA (F12C §10).
- **Rollback Security**: rollback de una versión firmada, nunca de una configuración manual.
- **Feature Flags**: kill-switch de capacidades/provider/plugin como capa de contención.

---

## 19. Governance

| Mecanismo | Rol |
|---|---|
| Architecture Board | Revisa RFC/ADR y decisiones de seguridad de diseño (F12C §2). |
| ADR | Cualquier cambio de seguridad con impacto en contratos requiere ADR (gobernanza §3.1). |
| Change Management | Tipos de cambio con flujo de aprobación (F12C §15: Security = revisión de seguridad + CTO). |
| Security Review | Todo cambio que toque Vault, auth, datos PII o IA exige revisión de seguridad. |
| Deprecation | Contratos/plugins deprecados con ventana (F12B §4). |
| Contract Governance | SemVer + CDCT protegen la compatibilidad de contratos de seguridad. |

---

## 20. Security KPIs

| KPI | Objetivo | Frecuencia |
|---|---|---|
| Cobertura de seguridad (SAST/DAST) | 100 % de releases | Por release |
| Secrets en reposo | 0 | Por PR |
| Vulnerabilidades críticas | 0 en prod | Mensual |
| Critical Findings | 0 abiertos > 1 sprint | Mensual |
| MTTR (P0/P1 seguridad) | ≤ 1 h | Mensual |
| Compliance (GDPR/residencia) | 0 violaciones | Mensual |
| Audit Score | scorecard de seguridad ≥ 9 | Trimestral |

---

## 21. Security Checklists

| Rol | Checklist |
|---|---|
| **Developer** | ¿sin secrets? · ¿validación de entrada? · ¿ownership? · ¿sin PII al LLM? · ¿ArchUnit/secrets scan? |
| **QA** | ¿casos negativos (PII, injection, RBAC)? · ¿chaos de Vault/Redis (F16 §10)? · ¿mutation en políticas? |
| **DevOps** | ¿imagen firmada + SBOM? · ¿Vault en prod? · ¿mTLS? · ¿NetworkPolicies? |
| **SRE** | ¿SLOs de seguridad medidos? · ¿runbooks actualizados? · ¿DR probado? |
| **Release** | ¿approval de seguridad? · ¿SBOM firmado? · ¿rollback seguro? · ¿flags de kill-switch? |
| **Incident** | ¿severidad correcta? · ¿contención ≤ 15 min (P0)? · ¿postmortem < 48 h? |
| **Architecture** | ¿ADR/RFC si aplica? · ¿threat model actualizado? · ¿sin violación del AI Boundary? |

---

## 22. Security Gates

| Gate | Requisito |
|---|---|
| **PR** | SAST, secrets scan, dependencias, sin PII en tests |
| **Merge** | cobertura sin drop + código aprobado por revisores |
| **Release** | DAST + SBOM firmado + approval de seguridad |
| **GA** | pilot verde + error budget + scorecard ≥ 8.0 |
| **LTS** | 1 año de soporte + matrix de compatibilidad |
| **Security Approval** | Firma de Security en RC/GA (no delegable) |

---

## 23. Long-Term Security Strategy (15 años)

1. **Evolución aditiva**: nuevas capacidades de seguridad entran por puertos/overloads nuevos; jamás tocan el Core.
2. **Compatibilidad**: SemVer + Contract Versioning + CDCT (F12B §4) protegen contratos de seguridad durante décadas.
3. **Zero Trust permanente**: mTLS, identidad por llamada y mínimo privilegio en todo el ciclo.
4. **Plugins/Marketplace**: firma + vetting + CDCT desde el día 1; sandbox cuando la escala lo exija.
5. **IA**: el AI Boundary y las políticas (`PiiPolicy`, `RedactionPolicy`, `ModelSelectionPolicy`) se endurecen sin cambiar la filosofía "Java decide, IA comunica".
6. **Escala mundial**: residencia por región (F19), cifrado por región, compliance multi-jurisdicción.
7. **Sin romper el Core**: cada control nuevo es una capa adicional de Defense in Depth, nunca una modificación del sistema congelado.

---

## 24. Conclusión

Esta especificación protege, durante más de 15 años:
- **la arquitectura** (Core congelado, DAG, contratos: nunca se tocan),
- **los datos** (datasets inmutables, PII gobernada, residencia, cifrado),
- **los usuarios** (ownership, RBAC, planes, MFA futuro),
- **la IA** (AI Boundary: Java decide, IA comunica; sin fuga ni inyección),
- **los plugins y el Marketplace** (firma, vetting, CDCT, sandbox futuro),
- **Enterprise** (documentos con ownership y aislamiento de tenant),
- **la operación** (GitOps, SLOs, DR, incident response),
- **el producto completo** (trust by design, defense in depth, auditabilidad total),

— sin modificar ninguno de los documentos F12–F17 y respetando la identidad fundacional de KIN: *Java decide, la IA comunica*.

---

*Especificación oficial de Seguridad y Gobierno de KIN (FASE 18). Documento formalizado conforme a la auditoría (H1–H3 aplicados). Compatible con F12–F17.*
