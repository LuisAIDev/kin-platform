# ADR-040: Desacoplamiento de la Capacidad de Paciente de `users.role`

## Estado
**Aceptado** — 2026-09-05

## Contexto

Tras ADR-039, la capacidad profesional (PHYSICIAN) quedó desacoplada de `users.role`: un usuario FREE/PREMIUM/PATIENT con `physician_verification_status = APPROVED` obtiene `ROLE_PHYSICIAN` sin cambiar de persona. El modelo de paciente, en cambio, seguía atado a `users.role = 'PATIENT'`.

Esto genera una asimetría observable:

- Un usuario puede usar todas las funcionalidades de paciente (triaje, historial, dashboard de salud) desde cualquier persona.
- Pero el médico no puede invitarlo: `RelationshipService.invitePatient` exigía `user.getRole() == UserRole.PATIENT` y respondía **404 "Paciente no registrado en KIN"** incluso para cuentas existentes y funcionales (p. ej. `luisgue.11@hotmail.com`, rol `FREE`).

Además, el alta de pacientes (`role=PATIENT`) estuvo bloqueada en producción hasta el fix de `users_role_check` (V40), por lo que casi no existían cuentas `PATIENT`: el médico no podía invitar a nadie registrado antes del fix.

Principio rector aplicado (igual que ADR-039): **"Java decide. El LLM únicamente comunica."** La capacidad de paciente se deriva exclusivamente del estado persistido en backend; el frontend solo la representa.

## Decisión

**Opción A adoptada**: desacoplar la capacidad de paciente de `users.role`, reutilizando `users.health_data_consent = true` (migración V29) como indicador de capacidad.

1. **`PatientAccess.isPatient(user)`** es la única fuente de verdad para decidir si un usuario tiene capacidad de paciente. Regla:
   - `role = PATIENT` → paciente (legacy, compatibilidad hacia atrás).
   - cualquier otra persona (FREE/PREMIUM/FACILITADOR/PHYSICIAN/ADMIN) → paciente solo si `health_data_consent = true`.
2. **Authorities derivadas por request**: `JwtAuthenticationFilter` resuelve el `User` en cada request y construye `ROLE_<persona>` + `ROLE_PHYSICIAN` (si `PhysicianAccess.isPhysician`) + `ROLE_PATIENT` (si `PatientAccess.isPatient`). La persona `PATIENT` ya no añade `ROLE_PATIENT` por rol: la añade `PatientAccess` (evita duplicados y centraliza la regla).
3. **Invitación**: `RelationshipService.invitePatient` reemplaza el filtro `role == PATIENT` por `PatientAccess::isPatient`. El flujo de aceptación no cambia: el destinatario con `ROLE_PATIENT` (ahora derivado) puede ver y aceptar la invitación vía `/health/patient/relationships/**`.
4. **Sin backfill**: los usuarios existentes solo son pacientes si ya tienen `health_data_consent = true` (registro de paciente, de médico o solicitud de capacidad profesional). Nadie se convierte retroactivamente sin consentimiento.
5. **Sin migración** (el asiento es `health_data_consent`, V29). Sin cambios de esquema.

### Predicado único `PatientAccess.isPatient(user)`

| role | `health_data_consent` | Capacidad de paciente |
|------|------------------------|-----------------------|
| PATIENT | cualquiera | **true** (legacy) |
| FREE/PREMIUM/FACILITADOR/PHYSICIAN/ADMIN | `true` | **true** |
| FREE/PREMIUM/FACILITADOR/PHYSICIAN/ADMIN | `false` / `null` | **false** |

Nota semántica: todo auto-registro de paciente y de médico, y toda solicitud de capacidad profesional, exige consentimiento de datos de salud (`health_data_consent = true`, `AuthServiceImpl` y `PhysicianApplicationService`). Por lo tanto, con la Opción A, un médico también puede ser "paciente" (puede ser invitado por otro médico) si dio consentimiento — consistente con el modelo multi-capacidad (FREE+PATIENT+PHYSICIAN).

## Arquitectura de capas

| Capa | Elemento | Responsabilidad |
|------|----------|-----------------|
| Fuente de verdad | `com.kinplatform.common.security.PatientAccess` | Predicado central `isPatient(user)` (espejo de `PhysicianAccess`) |
| Seguridad | `JwtAuthenticationFilter` | Authorities por request: `ROLE_<persona>` (FREE/PREMIUM/FACILITADOR/ADMIN) + `ROLE_PHYSICIAN` + `ROLE_PATIENT` |
| Invitación | `RelationshipService.invitePatient` | Usa `PatientAccess.isPatient` para el destinatario (el invitador sigue exigiendo `role = PHYSICIAN`, sin cambios en este ADR) |
| Seguridad de rutas | `SecurityConfig` | Sin cambios: `/health/patient/**` ya permite `PATIENT`/`ADMIN`; el derivar `ROLE_PATIENT` activa esos matchers para personas con consentimiento |
| Aceptación | `PatientRelationshipController` (`/health/patient/relationships/**`) | Sin cambios; el paciente (con `ROLE_PATIENT`) acepta/rechaza su invitación |

## Consecuencias

**Positivas**:
- Un médico puede invitar a cualquier usuario con capacidad de paciente (independientemente de `role`), resolviendo el 404 reportado.
- Simetría con ADR-039: "persona" (`role`) y "capacidades" (PHYSICIAN/PATIENT) son atributos derivados independientes.
- Autorización efectiva inmediata: si un usuario da/retira el consentimiento de datos de salud, `ROLE_PATIENT` cambia sin relogin.
- Compatibilidad hacia atrás: pacientes `role=PATIENT` legacy intactos; Empresas, Stripe y suscripciones intactos; sin migración.

**Negativas / trade-offs**:
- `health_data_consent` no era un marcador de "persona paciente" puro (los médicos y solicitantes también lo tienen `true`): con Opción A, cualquier médico con consentimiento puede ser invitado como paciente. Aceptado como decisión de producto (multi-capacidad).
- No hay backfill: cuentas antiguas que usaron salud sin consentimiento persistido no son invitable hasta que no se defina una acción de datos (fuera de alcance).
- El invitador sigue exigiéndose `role = PHYSICIAN` en `RelationshipService` (asimetría del lado médico pendiente, NO incluida en este ADR para no alterar el comportamiento de médicos de ADR-039).

## Referencias

- `kin-docs/ADR-039_PHYSICIAN_CAPABILITY_DECOUPLING.md` (espejo: capacidad profesional desacoplada)
- `kin-docs/ADR-031_PHYSICIAN_PORTAL.md` (portal de médicos; ciclo de relación V30)
- Migraciones: `V29` (`health_data_consent`, `physician_verification_status`), `V30` (relaciones médico-paciente), `V40` (fix `users_role_check`)
- Claves de implementación: `PatientAccess`, `JwtAuthenticationFilter`, `RelationshipService.invitePatient`, `PhysicianAccess`
