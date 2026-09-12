# ADR-042: Backend compartido para KIN Empresas y KIN Medical

## Estado
**Aceptado** — 2026-09-11

## Contexto
KIN Empresas y KIN Medical comparten un único backend (`kin-backend`), una única base de datos y una única tabla `users`.
Los frontends están completamente separados:
- KIN Empresas: `https://www.kin-platform.com` → solo módulos empresariales.
- KIN Medical: `https://www.kin-platform-medical.com` → solo módulos de paciente y médico.

Un usuario de KIN Empresas **no puede acceder a módulos de paciente/médico** porque esas rutas no existen en su frontend. El aislamiento real está en el frontend.

## Decisión
Mantener el backend compartido. El aislamiento se realiza en el frontend:
- Un usuario de KIN Empresas no ve módulos de paciente/médico (no existen en su frontend).
- Un usuario de KIN Medical no ve módulos empresariales.

La seguridad en el backend se basa en **capacidades**:
- `health_data_consent = true` → capacidad de paciente (ADR-040).
- `physician_verification_status = APPROVED` → capacidad de médico (ADR-039).
- `role = FREE/PREMIUM/FACILITADOR` → capacidad empresarial.

Un mismo correo puede tener múltiples capacidades. Esto es intencional y deseable.

## Consecuencias
- ✅ Simplicidad operativa: un solo backend, un solo deploy, una sola BD.
- ✅ Flexibilidad de usuario: una persona puede ser empresaria y paciente con el mismo correo.
- ✅ Seguridad real: la verificación de médicos la hace un ADMIN manualmente.
- ✅ El filtro JWT construye authorities por request desde `PatientAccess.isPatient()` y `PhysicianAccess.isPhysician()` sin verificar `platform`.
- ⚠️ Tarea pendiente V2.0: separar datos de salud en tabla propia (Habeas Data).

## Alternativas consideradas
- **Aislamiento estricto por `platform`**: rechazado porque rompe ADR-039/040 (un usuario EMPRESAS puede obtener capacidad de paciente/médico sin cambiar de `platform`). Además, el campo `request.getRequestURI()` incluye el context-path `/api/v1`, lo que hacía que el aislamiento fuera inefectivo en producción.
- **Bases de datos separadas**: pospuesto a V2.0.

## Claves de implementación
- `JwtAuthenticationFilter`: valida JWT, carga usuario, construye authorities desde capacidades. **No** verifica `platform` contra prefijo de ruta.
- `PlatformAccess`: marcado como `@Deprecated` — referencia histórica únicamente.
- `PatientAccess` y `PhysicianAccess`: siguen siendo las únicas fuentes de verdad para capacidades.
- El campo `platform` se conserva en `User` y como claim en JWT (útil para analítica/reportes).
- Migración `V44` (`add_platform_column`) se conserva (datos ya migrados).
