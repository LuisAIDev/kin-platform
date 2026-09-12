# Auditoría de Arquitectura Multi-Producto – KIN Empresas y KIN Medical
**Fecha:** 12 de septiembre de 2026
**Auditado por:** Senior Software Architect

---

## 1. Resumen Ejecutivo

KIN Empresas y KIN Medical operan como **dos productos independientes que comparten un único repositorio de backend (`kin-backend`), una única base de datos PostgreSQL (`kin_platform` en Render) y un único despliegue de backend (`kin-backend-lwmy.onrender.com`)**. Los dos frontends (`kin-frontend` para Empresas y `kin-frontend-medical` para Medical) son directorios separados dentro del mismo repositorio Git (`https://github.com/LuisAIDev/kin-platform`), desplegados en plataformas distintas (Render para Empresas, Vercel para Medical).

La convivencia se logra mediante **un modelo de "multi-tenant implícito"** donde la columna `role` en la tabla `users` y el prefijo de rutas (`/empresas/**`, `/health/**`, `/medical/**`) actúan como mecanismos de separación lógica. Sin embargo, **no existe un campo explícito de tenant, platform o vertical** en la entidad `User`, lo que genera riesgos significativos de acoplamiento y potencial fuga de datos.

**Veredicto:** La arquitectura actual es funcional para un producto mínimo viable, pero presenta debilidades arquitectónicas serias que deben abordarse antes de escalar a producción masiva o agregar más verticales.

---

## 2. Infraestructura Compartida

### Backend
- **Repositorio único:** `https://github.com/LuisAIDev/kin-platform` con `kin-backend/` como subdirectorio
- **Despliegue:** Un único servicio en Render: `kin-backend` con `rootDir: kin-backend`
- **Runtime:** Docker, Java 17, Spring Boot 3.2.5
- **Puerto:** Servicio único `kin-backend-lwmy.onrender.com` atendiendo a ambos productos
- **Estructura de paquetes:** Todo en `com.kinplatform.*` sin separación por dominio de negocio

```
com.kinplatform.common.config.SecurityConfig          ← Compartido
com.kinplatform.common.security.JwtAuthenticationFilter ← Compartido
com.kinplatform.common.security.JwtService              ← Compartido
com.kinplatform.common.security.PatientAccess           ← Compartido
com.kinplatform.common.security.PhysicianAccess         ← Compartido
com.kinplatform.user.User                               ← Compartido
com.kinplatform.auth.AuthServiceImpl                    ← Compartido
```

**No existen paquetes separados** como `com.kinplatform.enterprise.*` vs `com.kinplatform.health.*`. Todo el código del backend es monolítico.

### Base de Datos
- **Una sola instancia PostgreSQL:** `kin-db` en Render, database `kin_platform`
- **Un solo esquema:** Todas las tablas en el esquema público
- **43 migraciones Flyway** (V1–V43) que construyen el esquema acumulativamente

### Despliegue
- **Backend:** `kin-backend-lwmy.onrender.com` (Render)
- **Frontend Empresas:** `kin-frontend` en Render → `kin-platform.com`
- **Frontend Medical:** `kin-frontend-medical` en Vercel → `www.kin-platform-medical.com`
- **Render.yaml:** Solo configura un servicio frontend (`kin-frontend`). El frontend Medical se despliega separadamente vía Vercel (no aparece en `render.yaml`)
- **Ambos frontends comparten la misma URL de API base:** `NEXT_PUBLIC_API_URL = https://kin-backend-lwmy.onrender.com/api/v1`

---

## 3. Aislamiento de Datos

### Tablas Compartidas (Usadas por Ambos Productos)

| Tabla | Uso | Campos críticos compartidos |
|---|---|---|
| **`users`** | **Ambos productos** | `email`, `password_hash`, `full_name`, `role`, `email_verified`, `is_active`, `credits`, `subscription`, `current_plan_id`, `date_of_birth`, `sex`, `health_data_consent`, `physician_verification_status`, `completed_projects` |
| **`pricing_plans`** | Ambos | `vertical` (EMPRESAS, SALUD_PERSONAL, SALUD_PROFESIONAL), `code`, `features` |
| **`user_subscriptions`** | Ambos | `user_id`, `pricing_plan_id`, `current_period_start`, `current_period_end` |
| **`audit_logs`** | Ambos | `user_id`, `action`, `resource_type`, `patient_id` |

### Tablas Específicas de KIN Medical (Salud)

| Tabla | Descripción | Migración |
|---|---|---|
| `symptoms` | Catálogo de síntomas | V21 |
| `conditions` | Catálogo de condiciones médicas | V21 |
| `symptom_condition_relations` | Relaciones síntoma→condición con peso | V21 |
| `triage_consultations` | Historial de triajes por paciente | V21 |
| `differential_diagnoses` | Diagnóstico diferencial | V23 |
| `patient_profiles` | Perfiles médicos de pacientes | V24 |
| `physician_profiles` | Perfiles de médicos | V25 |
| `appointments` | Citas médicas | V30 |
| `clinical_alerts` | Alertas clínicas | V23 |
| `follow_up_plans` | Planes de seguimiento | V31 |
| `follow_up_tasks` | Tareas de seguimiento | V31 |
| `patient_evolutions` | Evolución del paciente | V31 |
| `clinical_documents` | Documentos clínicos | V34 |
| `ai_assist` | Asistencia IA médica | V35 |
| `automation_rules` | Reglas de automatización | V36 |
| `outbox_events` | Eventos de outbox | V19 |
| `processed_events` | Eventos procesados | V20 |
| `triage_share_links` | Enlaces compartidos de triaje | V39 |
| `password_reset_tokens` | Tokens de reset de contraseña | V16 |
| `email_verification_tokens` | Tokens de verificación de correo | V13 |

### Tablas Específicas de KIN Empresas (Proyectos)

| Tabla | Descripción | Migración |
|---|---|---|
| `projects` | Proyectos de negocio | V1 |
| `enterprise_project` | Datos extendidos de proyecto empresarial (scoring) | V7 |
| `enterprise_document` | Documentos generados por IA para proyectos | V7 |
| `project_context` | Contexto de entrevista para proyecto | V3 |
| `interview_state` | Estado de la entrevista de IA | V4 |
| `categories` | Categorías de proyectos | V6 |
| `project_reports` | Reportes generados | V12 |
| `project_documents` | Documentos de proyecto | V14 |
| `chat_messages` | Mensajes de chat de proyecto | V1 |
| `viability_scores` | Puntuación de viabilidad | V1 |
| `webhook_events` | Eventos de webhook | V11 |

### Estrategia de Multi-Tenancy

**No existe una estrategia formal de multi-tenancy.** La separación es implícita y se basa en:

1. **Prefijo de ruta en el backend** (`/empresas/**`, `/health/**`, `/medical/**`) en `SecurityConfig.java`
2. **Rol del usuario** (`role` en `users`) para determinar acceso
3. **Campo `vertical` en `pricing_plans`** para diferenciar planes
4. **Campos de extensión en `users`** (médicos tienen `health_data_consent`, empresarios tienen `completed_projects`)

**No hay:** `tenant_id`, `platform`, `vertical`, ni ningún campo discriminatorio en la tabla `users` que identifique a qué producto "pertenece" un usuario.

---

## 4. Autenticación y Roles

### Roles Existentes

```java
public enum UserRole {
    FREE,          // Ambos productos
    PREMIUM,       // Ambos productos
    FACILITADOR,   // Ambos productos (especial)
    PATIENT,       // Solo Medical (legacy)
    PHYSICIAN,     // Solo Medical (legacy)
    ADMIN;         // Ambos productos
}
```

**No existe `ROLE_ENTERPRISE_USER` ni `ROLE_CLIENT`.** Los usuarios empresariales usan los mismos roles genéricos (`FREE`, `PREMIUM`, `FACILITADOR`, `ADMIN`).

**Roles de Salud desacoplados** (ADR-039, ADR-040):
- `PhysicianAccess.isPhysician(user)`: Determina capacidad profesional desde `physician_verification_status = APPROVED`, **independiente de `users.role`**
- `PatientAccess.isPatient(user)`: Determina capacidad de paciente desde `health_data_consent = true` o `role = PATIENT`, **independiente de `users.role`**
- El `JwtAuthenticationFilter` construye authorities (`ROLE_PATIENT`, `ROLE_PHYSICIAN`) por request desde el estado persistido, sin modificar `users.role`

### Manejo de JWT

```java
public String generateToken(UUID userId, String email, String role) {
    return Jwts.builder()
        .subject(email)
        .claim("userId", userId.toString())
        .claim("role", role)
        .claim("type", type)  // "access" o "refresh"
        .issuedAt(now)
        .expiration(...)
        .signWith(secretKey)
        .compact();
}
```

**Claims del JWT:**
- `userId` (UUID)
- `email` (String)
- `role` (String: FREE, PREMIUM, etc.)
- `type` ("access" o "refresh")

**⚠️ El JWT NO contiene:**
- Identificador de plataforma (`platform`, `tenant`, `vertical`)
- Indicador de qué producto originó el login
- Prefijo de rutas permitidas

Esto significa que **un JWT de KIN Empresas es técnicamente válido para acceder a endpoints de KIN Medical** si el rol lo permite.

### Separación de Cuentas

**Un mismo correo puede tener UNA sola cuenta en el sistema.** No existe separación de cuentas por producto:
- Un usuario con `role=FREE` puede tener `health_data_consent=true` (se convierte en paciente)
- Un usuario con `role=FACILITADOR` puede tener `physician_verification_status=APPROVED` (se convierte en médico)
- **No hay forma de que un usuario sea simultáneamente "empresario" y "paciente" en el mismo registro**, ya que `users` es una sola fila

El registro de paciente (`POST /api/v1/auth/register/patient`) y médico (`POST /api/v1/auth/register/physician`) crean un `User` con `role=PATIENT` o `role=PHYSICIAN`, reutilizando la misma tabla.

---

## 5. Enrutamiento y CORS

### Rutas de API

**Configuración en `SecurityConfig.java` (extraída):**

```
/empresas/enterprise/**    → FREE, PREMIUM, FACILITADOR, ADMIN
/empresas/projects/**      → FREE, PREMIUM, FACILITADOR, ADMIN
/empresas/categories/**    → FREE, PREMIUM, FACILITADOR, ADMIN

/health/**                 → FREE, PREMIUM, FACILITADOR, PATIENT, ADMIN (varios sub-routes)
/health/patient/**         → PATIENT, ADMIN
/health/physician/**       → PHYSICIAN, ADMIN
/health/dashboard/**       → FREE, PREMIUM, FACILITADOR, PATIENT, ADMIN
/health/triage/**          → FREE, PREMIUM, FACILITADOR, PATIENT, ADMIN

/medical/telemedicine/**   → FREE, PREMIUM, FACILITADOR, PATIENT, PHYSICIAN, ADMIN
/medical/dashboard/**      → FREE, PREMIUM, FACILITADOR, PATIENT, ADMIN
/medical/scheduling/**     → PATIENT, PHYSICIAN, ADMIN
/medical/differential/**   → FREE, PREMIUM, FACILITADOR, PATIENT, ADMIN
/medical/**                → PATIENT, PHYSICIAN, ADMIN (varios sub-routes)
```

**Patrón de enrutamiento:**
- **`kin-frontend` (Empresas)** usa `api.ts` sin prefijo: `GET /api/v1/auth/login`
- **`kin-frontend-medical` (Medical)** usa `medicalApi` con prefijo: `GET /api/v1/medical/...`

### Configuración de CORS

**`SecurityConfig.java`:**
```java
private static final List<String> GUARANTEED_ORIGINS = List.of(
    "https://www.kin-platform-medical.com",
    "https://kin-platform-medical.com",
    "https://kin-frontend-medical-*.vercel.app",
    "https://*.vercel.app"       // ⚠️ ¡CUIDADO! Permite CUALQUIER Vercel app
);
```

**`render.yaml` (env var `ALLOWED_ORIGINS`):**
```yaml
- key: ALLOWED_ORIGINS
  value: https://kin-frontend.onrender.com,https://kin-platform.com
```

**Resultado final efectivo:** El backend acepta conexiones CORS de:
1. `https://www.kin-platform-medical.com` ✓
2. `https://kin-platform-medical.com` ✓
3. `https://kin-frontend-medical-*.vercel.app` ✓
4. `https://*.vercel.app` ✓ ← **Cualquier app Vercel**
5. `https://kin-frontend.onrender.com` ← Configuración en Render, puede estar obsoleta
6. `https://kin-platform.com` ← Dominio empresarial

---

## 6. Frontend

### Repositorios

Ambos frontends están en **el mismo repositorio Git** (`https://github.com/LuisAIDev/kin-platform`):
- `kin-frontend/` → Empresas (Next.js 16, Tailwind 4, React 19)
- `kin-frontend-medical/` → Medical (Next.js 16, Tailwind 4, React 19)

**Misma estructura de directorios, mismas dependencias.** La única diferencia significativa es el contenido de `page.tsx` y los componentes de registro/onboarding.

### Componentes Compartidos

**Compartidos:**
- `src/services/api.ts` — Ambos usan el mismo `API_URL` base
- `src/styles/globals.css` — Ambos usan la misma paleta `medical-*` y `accent-*`
- `tailwind.config.ts` — Configuración idéntica
- `next.config.ts` — Rewrites para `/api/medical/*` y `/api/auth/*`
- `src/services/session.ts` — Gestión de sesión (probablemente idéntico)

**Diferentes:**
- `src/services/medicalApi.ts` (solo Medical) → Prefijo `/medical` a endpoints
- `src/app/page.tsx` — Landing pages completamente distintos
- `src/app/(auth)/register/` — Flujos de registro diferentes
- `src/components/ui/PasswordInput.tsx`, `ValidatedInput.tsx` — Pueden ser idénticos

### Duplicación de Código

**⚠️ Alta duplicación detectada:**
- `api.ts` en `kin-frontend` y `kin-frontend-medical` son **prácticamente idénticos** (mismo `API_URL`, misma lógica de `request()`, mismo `AUTH_ENDPOINTS`)
- La única diferencia significativa en `api.ts` es que `kin-frontend-medical` añade `medicalApi` con prefijo `/medical`
- El `package.json` de ambos es idéntico en dependencias

**Riesgo:** Si un equipo de desarrollo hace un cambio en `api.ts` de un frontend, debe replicarlo manualmente en el otro. Esto viola el principio DRY y aumenta la probabilidad de inconsistencias.

---

## 7. Riesgos Detectados

### ⚠️ RIESGO CRÍTICO 1: Ausencia de Tenant/Platform Identifier
**Severidad:** 🔴 Crítica
**Impacto:** Un usuario con `role=FREE` puede acceder a endpoints de `/health/triage/**` y `/empresas/projects/**` porque ambos permiten el rol `FREE`. No existe mecanismo para restringir que un usuario empresarial acceda a datos de salud o viceversa.
**Ejemplo:** Si una persona con `role=PREMIUM` (empresario) se registra como paciente con `health_data_consent=true`, puede acceder tanto a `/empresas/projects/**` como a `/health/triage/**`. El backend no puede distinguir que la intención era acceder como "empresario" o como "paciente".

### ⚠️ RIESGO CRÍTICO 2: Fuga de Datos a través de la Tabla `users`
**Severidad:** 🔴 Crítica
**Impacto:** La tabla `users` contiene campos de ambos dominios en la misma fila:
- `completed_projects`, `credits` (empresarial) vs `date_of_birth`, `health_data_consent`, `physician_verification_status` (salud)
- Una consulta `SELECT * FROM users` expone datos de salud de pacientes junto con datos empresariales
- **HIPAA/PD compliance:** Los datos de salud (`health_data_consent`, `date_of_birth`, `physician_verification_status`) están en la misma tabla que datos comerciales sin cifrado diferenciado

### ⚠️ RIESGO ALTO 3: JWT Sin Contexto de Plataforma
**Severidad:** 🟠 Alta
**Impacto:** El token JWT contiene solo `userId`, `email`, `role`. Un atacante podría:
- Usar un token de `role=FACILITADOR` para acceder a `/health/triage/**` (permitido)
- No hay forma de que el backend sepa si el request viene de `kin-platform.com` o `www.kin-platform-medical.com`
- El `JwtAuthenticationFilter` reconstruye authorities desde la BD en cada request, lo cual es correcto, pero no filtra por plataforma origen

### ⚠️ RIESGO ALTO 4: CORS con `*.vercel.app` Sillado
**Severidad:** 🟠 Alta
**Impacto:** `GUARANTEED_ORIGINS` incluye `https://*.vercel.app`, lo que permite **cualquier aplicación desplegada en Vercel** para hacer requests al backend. Un actor malintencionado podría desplegar un frontend malicioso en Vercel y acceder a la API.
**Mitigación parcial:** El backend verifica el `role` en cada request, por lo que un usuario sin permisos no puede hacer nada útil, pero la exposición de la API completa es un riesgo.

### ⚠️ RIESGO MEDIO 5: Acoplamiento de Esquema de Base de Datos
**Severidad:** 🟡 Media
**Impacto:** Cualquier migración que modifique `users` afecta a ambos productos. Ejemplo real: `V40__expand_users_role_check.sql` tuvo que corregir un `CHECK` que no incluía `PATIENT` y `PHYSICIAN`, causando `DataIntegrityViolationException` en el registro.
**Riesgo de regresión:** Agregar un campo como `enterprise_tier` o `health_specialty` a `users` afecta a ambos. No hay forma de hacer migraciones específicas por producto sin afectar al otro.

### ⚠️ RIESGO MEDIO 6: Render.yaml Confuso para Frontend Medical
**Severidad:** 🟡 Media
**Impacto:** El `render.yaml` despliega `kin-frontend` (que contiene código empresarial en `page.tsx`) pero configura `APP_FRONTEND_BASE_URL = https://www.kin-platform-medical.com`. El frontend Medical se despliega separadamente en Vercel, lo que genera confusión sobre qué servicio atiende qué dominio.
**Riesgo operativo:** Si alguien redeploys `render.yaml` sin entender esta distinción, el frontend Medical podría dejar de funcionar.

### ⚠️ RIESGO BAJO 7: Duplicación de Código Frontend
**Severidad:** 🟢 Baja
**Impacto:** `api.ts` es casi idéntico en ambos frontends. Cambios en un lado requieren replicación manual en el otro. No es un riesgo de seguridad pero afecta mantenibilidad.

### ⚠️ RIESGO BAJO 8: Sin Rate Limiting por Plataforma
**Severidad:** 🟢 Baja
**Impacto:** El `RateLimitingFilter` aplica un límite global sin distinguir entre platform. Un usuario empresarial podría agotar los limites de la API médica o viceversa.

---

## 8. Recomendaciones

### Recomendación 1: Implementar Tenant Identification (Crítico)
**Acción:** Agregar un campo `platform` o `tenant_id` a la tabla `users` que identifique a qué producto "pertenece" cada usuario.

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS platform VARCHAR(20) DEFAULT 'EMPRESAS';
-- Valores posibles: 'EMPRESAS', 'SALUD_PERSONAL', 'SALUD_PROFESIONAL'
```

**Justificación:** Permitiría al backend restringir acceso: un usuario de `EMPRESAS` no puede acceder a `/health/**` y viceversa, incluso si tienen el mismo rol.

**Impacto:** Requiere migración (nuevo `V44__add_platform_column.sql`) y cambios en `JwtAuthenticationFilter` para validar el `platform` del token contra el prefijo de ruta.

### Recomendación 2: Cifrado Diferenciado de Datos de Salud (Crítico)
**Acción:** Separar los datos de salud en una tabla o esquema independiente con cifrado diferenciado (AES-256 para campos PHI).

```sql
-- Opción A: Tabla separada
CREATE TABLE health_data (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    date_of_birth DATE,
    sex VARCHAR(20),
    health_data_consent BOOLEAN,
    physician_verification_status VARCHAR(20),
    license_number VARCHAR(60),
    -- ... demás campos de salud
    encrypted_at TIMESTAMPTZ
);

-- Opción B: Esquema separado en PostgreSQL
CREATE SCHEMA health;
CREATE TABLE health.patient_profiles (...);
```

**Justificación:** Cumplimiento con HIPAA (si opera en EE.UU.) y protección de datos médicos. Los datos de salud no deberían compartir la misma tabla que datos comerciales.

### Recomendación 3: Añadir `platform` al JWT (Alto)
**Acción:** Agregar un claim `platform` al JWT que indique qué producto generó el token.

```java
public String generateToken(UUID userId, String email, String role, String platform) {
    return Jwts.builder()
        .subject(email)
        .claim("userId", userId.toString())
        .claim("role", role)
        .claim("platform", platform)  // ← NUEVO
        .claim("type", type)
        ...
}
```

**Justificación:** El `JwtAuthenticationFilter` podría validar que el `platform` en el JWT coincide con el prefijo de ruta solicitado (`/health/**` → `platform=SALUD`).

### Recomendación 4: Restringir CORS (Alto)
**Acción:** Reemplazar `https://*.vercel.app` con dominios específicos.

```java
private static final List<String> GUARANTEED_ORIGINS = List.of(
    "https://www.kin-platform-medical.com",
    "https://kin-platform.com",
    "https://kin-frontend-medical.vercel.app",          // Específico
    "https://kin-frontend.vercel.app"                    // Específico
);
```

**Justificación:** Eliminar el wildcard `*.vercel.app` para evitar acceso no autorizado desde cualquier proyecto Vercel.

### Recomendación 5: Separar Frontends en Repositorios (Media)
**Acción:** Mover `kin-frontend` y `kin-frontend-medical` a repositorios independientes (`kin-platform-enterprise`, `kin-platform-health`).

**Justificación:** Reducir acoplamiento, permitir despliegues independientes, evitar conflictos de merge, y facilitar la gestión de permisos de acceso al código.

### Recomendación 6: Compartir Código Base Común (Media)
**Acción:** Extraer `api.ts`, `PasswordInput.tsx`, `ValidatedInput.tsx`, y componentes comunes a un paquete compartido (`@kin/platform-common`) o symlinks monorepo.

**Justificación:** Eliminar duplicación de código y garantizar que cambios en componentes compartidos se propaguen automáticamente a ambos frontends.

### Recomendación 7: Evaluar Separación de Base de Datos (Media)
**Acción:** Para cuando haya producción significativa, considerar migrar a una arquitectura de base de datos separada por producto:
- `kin_platform_enterprise` → Datos empresariales
- `kin_platform_health` → Datos de salud (con cifrado)

**Justificación:** Aislamiento total de datos, cumplimiento regulatorio independiente, y la posibilidad de escalar cada base de datos según demanda.

**Sin embargo**, si el volumen de datos es bajo y los equipos son el mismo, el enfoque actual de base de datos compartida es suficiente si se implementan las recomendaciones 1-3.

---

## 9. Instrucciones para el Próximo Agente

1. **Verificar estado actual del `render.yaml`**: Confirmar que `kin-frontend` en Render es efectivamente el frontend empresarial y que `kin-frontend-medical` está correctamente desplegado en Vercel con `www.kin-platform-medical.com`.

2. **Revisar `AuthServiceImpl.java` completo** (líneas 50-416): Examinar cómo se construye el enlace de verificación de correo (`frontendBaseUrl + "/verify-email?token=" + token`) y confirmar que apunta al dominio correcto para cada producto.

3. **Buscar `EnterpriseController` o `ProjectController`**: Localizar los controladores de la ruta `/empresas/**` para verificar que no acceden a tablas de salud ni comparten datos sensibles con `users`.

4. **Verificar `HealthController`**: Confirmar que los endpoints `/health/**` validan `health_data_consent = true` antes de devolver datos médicos.

5. **Ejecutar el script de auditoría de datos**: Generar un reporte SQL que muestre cuántos usuarios tienen `health_data_consent = true` pero `role != PATIENT` (y viceversa) para evaluar la magnitud del riesgo de mezcla de datos.

6. **Documentar la estrategia de despliegue**: Crear un `DEPLOYMENT.md` que aclare cómo se despliega cada frontend, en qué plataforma, con qué dominio, y cómo se relacionan con el `render.yaml`.

7. **Evaluar implementación de `platform` column**: Si se acepta la Recomendación 1, el primer paso es crear una migración `V44__add_platform_column.sql` y un servicio de `PlatformAccess` similar a `PhysicianAccess` y `PatientAccess`.

---

*Documento generado como parte de la auditoría arquitectónica del ecosistema KIN. Toda la información se basa en el análisis del código fuente disponible en `https://github.com/LuisAIDev/kin-platform` al momento de la auditoría.*
