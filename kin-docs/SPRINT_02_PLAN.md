# Sprint 2 — Plan (KIN para IPS y clínicas)

## Objetivo
Hacer KIN vendible a IPS y clínicas: fundación institucional, facturación a
escala y operación multi-sede.

## Fases

| Fase | Entregable | Estado |
|---|---|---|
| 2.1 | Roles IPS_* + multi-sede + equipos médicos (V62–V64) | **En curso** |
| 2.2 | Flujo de invitación de miembros por email + asignación de roles IPS | Pendiente |
| 2.3 | RIPS reales: generación/validación XSD end-to-end | Pendiente |
| 2.4 | FEV-RIPS DIAN real (firma X.509 + envío) | Pendiente |
| 2.5 | MIPRES real (autorizaciones) | Pendiente |
| 2.6 | Glosas: parser por EPS específico + apelación automatizada | Pendiente |
| 2.7 | Cartera: pagos conciliados + reportes | Pendiente |
| 2.8 | Auditoría institucional (IPS_AUDITOR) + cumplimiento | Pendiente |

## Dependencias
- 2.1 → 2.2 (invitaciones).
- 2.1 → 2.6/2.7 (permisos de facturación por rol).
- 2.3/2.4/2.5 requieren credenciales DIAN/MIPRES y certificados (Sprint 3).

## Timeline estimado
- 2.1: 1 semana · 2.2: 1 semana · 2.3–2.5: 3–4 semanas · 2.6–2.7: 2 semanas · 2.8: 1 semana.

## Deuda técnica relacionada
- Suite de tests no 100% verde (grupos A/D/E/F en `TECH_DEBT.md`).
- Roles IPS_* no aplicados aún a `/billing/**` (hoy PHYSICIAN/ADMIN).
