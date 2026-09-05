# ADR-039: Desacoplamiento de la Capacidad Profesional (PHYSICIAN) de `users.role`

## Estado
**Aceptado** — 2026-09-04

## Contexto

KIN Salud necesita permitir que una persona que ya posee una cuenta KIN pueda convertirse en profesional de salud sin crear una segunda cuenta. El flujo original `POST /auth/register/physician` solo funcionaba con emails inexistentes: si el email ya existía, devolvía un 201 genérico sin crear nada ni enviar correo, y la UI afirmaba "te hemos enviado un correo" aunque no se hubiera enviado (falsa sensación de éxito).

El modelo previo usaba una única columna `users.role` para representar simultáneamente persona, vertical y rol funcional. Cambiar `role=FREE` a `role=PHYSICIAN` era incorrecto:
- degradaba la persona/vertical del usuario (perdía el hub Empresas y el auto-servicio de salud),
- no soportaba combinaciones (FREE+PREMIUM+PHYSICIAN, PATIENT+PHYSICIAN),
- el plan comercial ya vive separado en `user_subscriptions`/`pricing_plans` (`V9`, `V10`, `V37`), por lo que el `role` no debía usarse como marcador de facturación.

Principio rector aplicado: **"Java decide. El LLM únicamente comunica."** La capacidad profesional se deriva exclusivamente del estado persistido en backend; el frontend solo la representa.

## Decisión

**Alternativa B adoptada**: desacoplar la capacidad profesional `PHYSICIAN` de `users.role`.

1. **Una sola cuenta / una sola identidad**: el usuario conserva su persona (`role`), plan, suscripción y vertical. La capacidad profesional se añade como un atributo derivado.
2. **`PhysicianAccess.isPhysician(user)`** es la única fuente de verdad para decidir si un usuario tiene capacidad profesional. La capacidad se deriva de `users.physician_verification_status = APPROVED`, independientemente del rol. Para médicos legacy (`role=PHYSICIAN`), `null`/`APPROVED` conserva el comportamiento previo (provisionados/aprobados), y `PENDING`/`REJECTED` no otorgan capacidad.
3. **Authorities derivadas por request**: `JwtAuthenticationFilter` resuelve el `User` en cada request y construye `ROLE_<persona>` + `ROLE_PHYSICIAN` si `PhysicianAccess.isPhysician(user)`. No se confía en claims obsoletos del JWT (sin token stale); aprobar/rechazar surte efecto inmediato sin relogin. El `User` resuelto se reutiliza vía atributo de request para evitar consultas duplicadas.
4. **Flujo para cuenta existente**: `POST/GET /health/physician/application` (autenticado, cualquier persona) crea/consulta la solicitud; `physician_verification_status=PENDING` sin tocar `role`; un ADMIN decide con `POST /admin/users/physicians/{id}/approve|reject` (cualquier rol). Cada decisión se audita (`audit_logs`, acciones `PHYSICIAN_APPLICATION_*`).
5. **Registro de médico nuevo**: `register/physician` se conserva; ahora la respuesta 201 incluye `state` (ACCOUNT_ALREADY_VERIFIED / ACCOUNT_NOT_VERIFIED / PHYSICIAN_PENDING / PHYSICIAN_APPROVED / PHYSICIAN_REJECTED / NEW_REGISTRATION) para que la UI no mienta sobre el envío.
6. **resend-verification**: devuelve `status` (SENT / ALREADY_VERIFIED / COOLDOWN / NO_ACCOUNT) manteniendo HTTP 200.
7. **DTOs**: `UserDTO` y `AuthResponse` exponen `physicianCapability` derivada del backend.
8. **Seguridad**: `/health/physician/application/**` es accesible a cualquier usuario autenticado (no requiere `PHYSICIAN`); el resto de `/health/physician/**` sigue exigiendo capacidad/ADMIN.

## Arquitectura de capas

| Capa | Elemento | Responsabilidad |
|------|----------|-----------------|
| Fuente de verdad | `com.kinplatform.common.security.PhysicianAccess` | Predicado central `isPhysician(user)` |
| Seguridad | `JwtAuthenticationFilter` + `SubscriptionAccessFilter` | Authorities por request + reuso del `User` |
| Solicitud | `PhysicianApplicationService` + `PhysicianController` (`/health/physician/application`) | Crear/consultar solicitud (PENDING), validar email verificado y duplicados |
| Admin | `AdminUserService`/`AdminUserController` | Listar solicitudes cross-rol y decidir (approve/reject con motivo), con auditoría |
| DTOs | `UserDTO`, `AuthResponse` | Exponer `physicianCapability` y `state` |
| Frontend | `roles.ts`, `RoleGuard`, `proxy.ts`, `session.ts` | Consumir `physicianCapability`; nunca inferir reglas de negocio |

### Predicado único `PhysicianAccess.isPhysician(user)`

| role | `physician_verification_status` | Capacidad |
|------|--------------------------------|-----------|
| PHYSICIAN | `null` (legacy/piloto) | **true** |
| PHYSICIAN | `APPROVED` | **true** |
| PHYSICIAN | `PENDING` / `REJECTED` | **false** |
| FREE/PREMIUM/PATIENT/FACILITADOR | `APPROVED` | **true** |
| FREE/PREMIUM/PATIENT/FACILITADOR | `PENDING` / `REJECTED` / `null` | **false** |

### Estados de registro (`POST /auth/register/physician`, HTTP 201)

| state | Significado | UI |
|-------|-------------|-----|
| `ACCOUNT_ALREADY_VERIFIED` | Email existe y verificado, sin solicitud | "Inicia sesión para solicitar el registro como profesional" |
| `ACCOUNT_NOT_VERIFIED` | Email existe, no verificado | "Debes verificar primero tu correo" |
| `PHYSICIAN_PENDING` | Solicitud pendiente | "Tu solicitud profesional está pendiente de revisión" |
| `PHYSICIAN_APPROVED` | Ya habilitado | "Esta cuenta ya está habilitada como profesional" |
| `PHYSICIAN_REJECTED` | Solicitud rechazada | "Tu solicitud fue rechazada" |
| `NEW_REGISTRATION` | Email nuevo; se envió correo | Flujo normal de verificación |

## Consecuencias

**Positivas**:
- Un usuario existente puede solicitar ser profesional sin duplicar cuenta ni perder persona/plan/vertical.
- Autorización efectiva inmediata (sin relogin) y revocación inmediata al aprobar/rechazar.
- UX veraz: el frontend nunca afirma "te hemos enviado un correo" si el backend no envió.
- Compatibilidad hacia atrás: médicos legacy, Empresas, PATIENT, Stripe y suscripciones intactos; sin cambios de esquema (no requiere migración; `physician_verification_status` de `V29` es el asiento).

**Negativas / trade-offs**:
- +1 consulta por request autenticado en el filtro (mitigada por índice único `users.email` y reuso del `User` en request-scope).
- `resend-verification` y `register/physician` revelan parcialmente existencia/estado de la cuenta (decisión de producto aprobada para que la UI no mienta); el HTTP se mantiene 200/201 (anti-enumeración de código).
- El frontend necesita un modelo "persona + capacidad" (navegación dual) en lugar de un solo `role`.

## Referencias

- `kin-docs/ADR-031_PHYSICIAN_PORTAL.md` (portal de médicos)
- `kin-docs/ADR-035_AUDIT.md` (auditoría; acciones `PHYSICIAN_APPLICATION_*`)
- Migraciones: `V13` (email_verified), `V29` (auto-registro salud / `physician_verification_status`), `V30` (relaciones médico-paciente), `V33` (audit_logs), `V37` (verticales de pricing)
- Claves de implementación: `PhysicianAccess`, `JwtAuthenticationFilter`, `PhysicianApplicationService`, `AdminUserService`, `AuthServiceImpl` (`registerPhysician`), `UserDTO`/`AuthResponse`
