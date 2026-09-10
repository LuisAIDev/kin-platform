# Commit 1 — Alias de rutas Empresas + Medical

## Objetivo

Introducir rutas por contexto de producto (vertical) manteniendo **compatibilidad
total** con las rutas legacy existentes, y exponer en autenticación el acceso a
verticales del usuario como campo **derivado** (nunca persistido).

## Rutas: legacy + alias

Cada controlador conserva su ruta legacy y añade un alias por contexto. Ambas
rutas apuntan al **mismo** handler (misma respuesta).

### Empresas

| Controlador | Legacy | Alias |
|---|---|---|
| `EnterpriseController` | `/enterprise` | `/empresas/enterprise` |
| `EnterpriseDashboardController` | `/enterprise` | `/empresas/enterprise` |
| `EnterpriseInformationController` | `/enterprise` | `/empresas/enterprise` |
| `EnterpriseProgressController` | `/enterprise` | `/empresas/enterprise` |
| `ProjectController` | `/projects` | `/empresas/projects` |
| `CategoryController` | `/categories` | `/empresas/categories` |

### Medical

| Controlador | Legacy | Alias |
|---|---|---|
| `TelemedicineController` | `/health/telemedicine` | `/medical/telemedicine` |
| `DocumentController` | `/health/documents` | `/medical/documents` |
| `DashboardController` | `/health/dashboard` | `/medical/dashboard` |
| `SchedulingController` | `/health/scheduling` | `/medical/scheduling` |
| `DifferentialController` | `/health/differential` | `/medical/differential` |
| `AuditPatientController` | `/health/audit` | `/medical/audit` |
| `AuditAdminController` | `/admin/health/audit` | `/medical/admin/audit` |
| `AIAssistController` | `/health/aiassist` | `/medical/aiassist` |
| `AutomationController` | `/health/automation` | `/medical/automation` |
| `FollowUpController` | `/health/followup` | `/medical/followup` |
| `PhysicianController` | `/health/physician` | `/medical/physician` |
| `PatientConsentController` | `/health/patient/consent` | `/medical/patient/consent` |
| `PatientRelationshipController` | `/health/patient/relationships` | `/medical/patient/relationships` |
| `TriageController` | `/health/triage` | `/medical/triage` |
| `TriageExportController` | `/health/triage` | `/medical/triage` |

> El `context-path` es `/api/v1`; los matchers de `SecurityConfig` no lo
> incluyen (Spring Security lo omite).

## `verticalAccess` (campo derivado)

`verticalAccess` es un campo **calculado** por `AuthServiceImpl.computeVerticalAccess(User)`
y expuesto en `AuthResponse` y `UserDTO`. **No** existe en la entidad `User`.

Reglas:

- Roles `FREE`, `PREMIUM`, `FACILITADOR`, `ADMIN` → `"empresas"`.
- Roles `PATIENT`, `PHYSICIAN` **o** `PhysicianAccess.isPhysician(user) == true`
  (`physician_verification_status = APPROVED`) → `"medical"`.
- Un usuario puede tener ambos valores (p. ej. `FREE` + `APPROVED`).

## `SecurityConfig`

Nuevos matchers para `/empresas/**` y `/medical/**` con los roles de cada
vertical, replicando el patrón de las rutas legacy.

## Tests

- `RouteAliasMockMvcTest` — `MockMvc` standalone: legacy y alias devuelven
  status y body idénticos (categorías, proyectos, telemedicina, documentos).
- `RouteAliasMappingTest` — verifica por reflexión que los 21 controladores
  declaran ambas rutas.
- `VerticalAccessTest` — unitario: `verticalAccess` en `login` y `getCurrentUser`
  para cada rol/capacidad.

## Notas de compatibilidad

- No se elimina ninguna ruta legacy.
- No se modifica `users.role` ni la lógica de negocio.
- El cálculo de `verticalAccess` es determinista y no depende del cliente.
