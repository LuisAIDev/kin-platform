# Sprint 2 — Plan (KIN para IPS y clínicas)

## Objetivo
Hacer KIN vendible a IPS y clínicas: fundación institucional, facturación a
escala y operación multi-sede.

## Fases

| Fase | Entregable | Estado |
|---|---|---|
| 2.1 | Roles IPS_* + multi-sede + equipos médicos (V62–V65) | **✅ COMPLETADA** |
| 2.2 | Catálogos CUPS/INVIMA (descarga, carga masiva, autocomplete) | **Estructura lista; carga de datos pendiente** |
| 2.3 | RIPS reales: generación/validación XSD end-to-end | **✅ 2.3a COMPLETADA** (validación Anexo Técnico 1); 2.3b DIAN pendiente |
| 2.4 | FEV-RIPS DIAN real (firma X.509 + envío) | Pendiente |
| 2.5 | MIPRES real (autorizaciones) | **✅ COMPLETADA** (2026-09-25) |
| 2.5a | HCE Core Res 839/1995: migración V75 (14 tablas) | **✅ COMPLETADA** (2026-09-25) |
| 2.6 | Glosas: parser por EPS específico + apelación automatizada | Pendiente |
| 2.7 | Cartera: pagos conciliados + reportes | Pendiente |
| 2.8 | Auditoría institucional (IPS_AUDITOR) + cumplimiento | Pendiente |

## Dependencias
- 2.1 → 2.2 (invitaciones).
- 2.1 → 2.6/2.7 (permisos de facturación por rol).
- 2.3/2.4/2.5 requieren credenciales DIAN/MIPRES y certificados (Sprint 3).

## Timeline estimado
- 2.1: 1 semana · 2.2: 1 semana · 2.3–2.5: 3–4 semanas · 2.6–2.7: 2 semanas · 2.8: 1 semana.

## Fase 2.3a — Validación RIPS según Resolución 2275 Anexo Técnico 1 — **✅ COMPLETADA (2026-09-24)**
- Duración: ~1 semana · Sin dependencias externas.
- Entregado:
  - `RipsValidator`: 11 campos sector salud + 6 reglas críticas de negocio
  - Tablas de referencia SISPRO (V71): `sispro_modalidad_pago`, `sispro_cobertura_plan`, `sispro_concepto_recaudo`, `sispro_tipo_id`
  - Integración en `RipsGenerationOrchestrator` pipeline (XSD + Business + RIPS rules)
  - 19 tests PASS (RipsValidatorTest) + 36 tests totales RIPS
  - Migración V71 con seed oficial Anexo Técnico 1
- Pendiente: XSD oficiales MinSalud (TD-CAT-4) + ajustar `RipsJsonToXmlMapper` a estructura oficial.

## Fase 2.5 — MIPRES (Autorizaciones MinSalud Resolución 740/2024) — **✅ COMPLETADA (2026-09-25)**
- Duración: ~1 semana · Sin dependencias externas para stubs.
- Entregado:
  - Tablas MIPRES (V74): `mipres_prescriptions`, `mipres_supplies`, `mipres_tokens`
  - `MipresHttpClient` (WebClient, perfil `prod`), `MipresTokenService` (cache 24h)
  - `MipresService` + `MipresController` (endpoints CRUD + reportes)
  - `MipresAuthorizationService` (NIT por organización)
  - Auditoría: `MIPRES_PRESCRIPTION_CREATE`, `MIPRES_SUPPLY_REPORT`, `MIPRES_SUPPLY_ANULLED`
  - 14 tests unitarios PASS (MipresServiceTest 6 + MipresControllerTest 8)
  - 41 tests RIPS sin regresiones
  - Deploy Render LIVE (commit ce696c7) · Health UP
- Pendiente para producción:
  - Configurar `MIPRES_NIT` + `MIPRES_PIN_BASE64` en perfil `prod` (proveídos por PO)
  - Tests de integración contra sandbox MIPRES (bloqueado por credenciales reales)

## Fase 2.3b — Firma DIAN (requiere trámites externos)
Acciones del PO (en paralelo):
- Contactar Certicámara (certificado X.509) — 2-3 semanas.
- Solicitar acceso sandbox DIAN (habilitación facturador electrónico) — 4-6 semanas.
- Descargar Anexo Técnico RIPS v2024 y Resolución 000042/2020.
Timeline estimado: 4-6 semanas.

## Deuda técnica relacionada
- Suite de tests no 100% verde (grupos A/D/E/F en `TECH_DEBT.md`).
- Roles IPS_* aplicados a `/billing/**` (commit `090cc21`).

## Fase 2.1 — CERRADA (2026-09-23)
- Roles IPS_* (V62) · Branches (V63) · OrganizationMembers (V64).
- Multi-tenant cableado: `users.organization_id` (V65) + `TenantContext` en `JwtAuthenticationFilter` (commit `02b65a1`).
- jsonb mapping corregido: 6 campos con `@JdbcTypeCode(SqlTypes.JSON)` (commit `367e863`).
- Smoke test producción: 8/8 (IPS_ADMIN crea sede 201; IPS_FACTURADOR billing 200; POST billing 403).
- Usuarios de prueba: `ips-admin-test@kin.internal`, `ips-facturador-test@kin.internal` (org demo `…0001`).

## Fase 2.5a — HCE Core (Resolución 839/1995): migración V75 — **✅ COMPLETADA (2026-09-25)**
- Entregado:
  - `V75__complete_hce_res_839_1995.sql` (544 líneas, opción C: sin rename de tablas legacy):
    14 tablas nuevas + 1 función (`update_updated_at_column`) + 14 triggers + 1 vista (`hce_complete_view`).
    - Tablas: `encounters`, `patient_identification`, `anamnesis`, `patient_history`, `physical_exam`,
      `diagnoses`, `treatment_plans`, `medical_orders`, `informed_consents`, `referrals`,
      `discharge_summaries`, `clinical_attachments`, `obstetric_history`, `surgical_history`.
    - `patient_profiles` y `patient_evolutions` se mantienen intactas (0 renames, 0 vistas de compatibilidad).
  - Verificado en Docker local (PostgreSQL 16) y en **Neon prod**: `flyway_schema_history version=75, success=t` (installed_on 2026-09-25 05:57:46), 14 tablas presentes.
- **Incidente de despliegue `c2a645a` (postmortem):**
  - **Causa raíz**: `MipresHttpClient` / `MipresTokenService` (`@Profile("prod")`) requieren un bean `WebClient` que nadie definía. `ce696c7` (MIPRES) introdujo los consumidores sin el `@Bean`. Los tests unitarios usaban mocks → no lo detectaron. `fabc088` (LIVE previo) no incluía MIPRES, por eso funcionaba.
  - **Fix**: `bf203fa` — `WebClientConfig` (bean `WebClient`) + `ApplicationContextTest` (`@SpringBootTest @ActiveProfiles("test")` con Testcontainers, verifica `contextLoads()` y la existencia del bean `WebClient`).
  - **Lección aprendida**: los tests con mocks NO detectan beans faltantes. `ApplicationContextTest` es **OBLIGATORIO** en cada PR que agregue `@Component`/`@Bean` (ver TD-CI-1/2 en `TECH_DEBT.md`).
  - **Evidencia de cierre**: Render LIVE `bf203fa` (build_time 2026-09-25T17:42:15Z), `/actuator/health` = 200 UP; MIPRES `POST /billing/mipres/prescriptions` = 403 sin token (ruta activa).