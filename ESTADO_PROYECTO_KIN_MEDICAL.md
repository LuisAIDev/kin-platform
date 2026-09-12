# Estado del Proyecto KIN Medical
**Fecha de Auditoría:** 11 de Septiembre de 2026
**Auditado por:** Senior Full-Stack Engineer / Auditor Técnico
**Repositorio:** https://github.com/LuisAIDev/kin-platform

---

## 1. Resumen Ejecutivo

KIN Medical es una plataforma SaaS B2B para el sector salud que permite a médicos, pacientes, clínicas, IPS, EPS y hospitales gestionar atención médica digital. El proyecto ha experimentado múltiples iteraciones de rediseño visual, mejora de UX y correcciones de infraestructura. El estado actual es **operativo y funcional** con build exitoso y deploy en producción.

**Estado actual:** ✅ BUILD SUCCESS (32/32 rutas frontend), deploy activo en Vercel/Render.

---

## 2. Arquitectura y Stack Tecnológico

### 2.1 Frontend (`kin-frontend-medical`)

| Tecnología | Versión | Detalle |
|---|---|---|
| **Next.js** | ^16.2.9 | App Router con Server Components |
| **React** | ^19.2.4 | Client Components |
| **React DOM** | ^19.2.4 | |
| **Tailwind CSS** | ^4.0.0 | Con `@tailwindcss/postcss` v4 |
| **TypeScript** | ^5 | |
| **lucide-react** | ^1.45.0 | Íconos (Eye, EyeOff, CheckCircle2, etc.) |
| **@tanstack/react-query** | ^5.0.0 | |
| **react-hook-form** | ^7.0.0 | |
| **zod** | ^3.0.0 | Validación de formularios |
| **jspdf** | ^2.5.0 | Generación de PDFs |
| **Vitest** | ^3.1.0 | Testing unitario |
| **Playwright** | ^1.61.0 | Testing E2E |

### 2.2 Backend (`kin-backend`)

| Tecnología | Versión | Detalle |
|---|---|---|
| **Spring Boot** | 3.2.5 | |
| **Java** | 17 | |
| **Base de datos** | PostgreSQL | A través de Flyway (migraciones V1-V30+) |
| **Spring Security** | 3.2.5 | JWT + Rate Limiting + Subscription Access |
| **Spring Data JPA** | | |
| **Maven** | | `pom.xml` |
| **Docker** | | Despliegue en Render |

### 2.3 Infraestructura

| Servicio | URL | Detalle |
|---|---|---|
| **Frontend (Vercel)** | `https://www.kin-platform-medical.com` | Producción |
| **Frontend Preview** | `https://kin-frontend-medical-*.vercel.app` | Despliegues preview |
| **Backend (Render)** | `https://kin-backend-lwmy.onrender.com` | Spring Boot + PostgreSQL |
| **API Base** | `/api/v1` | `context-path` en `application.yml` |

---

## 3. Funcionalidades Implementadas

### 3.1 Autenticación y Onboarding

| Funcionalidad | Ruta | Estado |
|---|---|---|
| **Login** | `/login` | ✅ Con PasswordInput (toggle mostrar/ocultar) |
| **Registro de Paciente** | `/register/salud/paciente` | ✅ Wizard 3 pasos + ValidatedInput |
| **Registro de Médico** | `/register/salud/medico` | ✅ Wizard 3 pasos + validación de cédula |
| **Selección de tipo** | `/register` | ✅ Tarjeta Paciente/Médico |
| **Verificación de correo** | `/auth/verify-email` y `/verify-email` | ✅ Token de URL, estados: loading/verified/error |
| **Reenvío de verificación** | `POST /auth/resend-verification` | ✅ |
| **Página de verificación de médico** | `/dashboard/physician/verification` | ✅ PENDING/APPROVED/REJECTED |

### 3.2 Componentes UI Reutilizables

| Componente | Ubicación | Descripción |
|---|---|---|
| `PasswordInput` | `src/components/ui/PasswordInput.tsx` | Campo contraseña con toggle Eye/EyeOff, soporte para `validation` prop |
| `ValidatedInput` | `src/components/ui/ValidatedInput.tsx` | Input con validación en tiempo real, icono ✓/✗, mensajes de error |
| `RegisterWizard` | `src/components/auth/RegisterWizard.tsx` | Barra de progreso de 3 pasos con CheckCircle2/Circle |

### 3.3 Página Principal (Landing)

| Sección | Estado |
|---|---|
| **Hero** | ✅ "KIN Medical" (título gradiente), subtítulo, leyenda con 5 públicos |
| **Para Médicos** | ✅ 6 cards con íconos, hover, "Saber más →" |
| **Para Clínicas** | ✅ Badge "Próximamente", lista de features |
| **Para IPS/EPS** | ✅ Badge "Futuro", fondo gradient oscuro |
| **Planes** | ✅ 3 planes (Free/Pro/Clinic) con tooltip |
| **Contacto** | ✅ Email + CTA |
| **Visión** | ✅ "Una sola plataforma" |
| **Footer** | ✅ Consistente con KIN Empresas |

### 3.4 Paleta de Colores

| Color | Hex | Uso |
|---|---|---|
| `medical-500` | `#0EA5E9` | Primario (azul médico) |
| `medical-600` | `#0284C7` | Primario oscuro |
| `accent-500` | `#10B981` | Acento (verde salud) |
| `accent-600` | `#059669` | Acento oscuro |
| `neutral-900` | `#0F172A` | Texto principal |

### 3.5 Backend (Endpoints Principales)

| Endpoint | Método | Descripción |
|---|---|---|
| `/auth/login` | POST | Login con JWT |
| `/auth/register` | POST | Registro genérico |
| `/auth/register/patient` | POST | Registro de paciente |
| `/auth/register/physician` | POST | Registro de médico |
| `/auth/verify-email` | GET | Verificar token de correo |
| `/auth/resend-verification` | POST | Reenviar correo de verificación |
| `/auth/me` | GET | Usuario actual |
| `/auth/refresh` | POST | Refresh token |
| `/auth/logout` | POST | Cerrar sesión |
| `/health/physician/application` | POST/GET | Solicitud de capacidad profesional |
| `/health/physician/verification-status` | GET | Estado de verificación de médico |
| `/medical/physician/verification-status` | GET | Alias de verificación |
| `/health/patient/**` | GET/POST | Endpoints de paciente |
| `/health/triage/**` | GET/POST | Triaje digital |
| `/health/telemedicine/**` | GET/POST | Telemedicina |
| `/medical/**` | GET/POST | Alias de `/health/**` |

---

## 4. Historial de Cambios Reciente (Commits)

| Commit | Descripción |
|--------|-------------|
| `f6ff0f3` | feat(ui): crear ruta `/auth/verify-email` para verificación de correo |
| `2eb458b` | fix(backend): CORS con `allowedOriginPatterns`, agregar `*.vercel.app` |
| `171a014` | feat(ui): mejora del onboarding — wizard 3 pasos, ValidatedInput, página de verificación |
| `a0670d7` | feat(ui): añadir PasswordInput al formulario de login |
| `b5777f5` | feat(ui): botón mostrar/ocultar contraseña en formularios de registro |
| `0492f50` | feat(ui): rediseño profesional de KIN Medical (hero, paleta, cards, branding) |

---

## 5. Estado de la Infraestructura

### 5.1 Frontend (Vercel)
- **Producción:** `https://www.kin-platform-medical.com`
- **Preview:** `https://kin-frontend-medical-*.vercel.app`
- **Build:** 32/32 rutas prerenderizadas
- **Framework:** Next.js 16 + Turbopack

### 5.2 Backend (Render)
- **URL:** `https://kin-backend-lwmy.onrender.com`
- **Context path:** `/api/v1` (configurado en `application.yml`)
- **Base de datos:** PostgreSQL (Render starter)
- **Health check:** `/api/v1/actuator/health`
- **Docker:** Imagen buildeada desde `kin-backend`

### 5.3 CORS (SecurityConfig)
```java
// GUARANTEED_ORIGINS incluye:
- https://www.kin-platform-medical.com
- https://kin-platform-medical.com
- https://kin-frontend-medical-*.vercel.app
- https://*.vercel.app
// Usa allowedOriginPatterns (no allowedOrigins) para soportar wildcards
// allowedHeaders: List.of("*") para permitir todos los headers
```

### 5.4 Variables de Entorno Principales
| Variable | Valor |
|---|---|
| `NEXT_PUBLIC_API_URL` | `https://kin-backend-lwmy.onrender.com/api/v1` |
| `SPRING_PROFILES_ACTIVE` | `render,shadow` |
| `ALLOWED_ORIGINS` | `https://kin-frontend.onrender.com,https://kin-platform.com` |
| `MEDICAL_FRONTEND_BASE_URL` | `https://www.kin-platform-medical.com` |

---

## 6. Estado del Build y Tests

### 6.1 Frontend Build
```
✅ npm run build → BUILD SUCCESS
✅ 32/32 rutas prerenderizadas
✅ 0 errores TypeScript
```

### 6.2 Backend Build
```
⚠️ Maven build no verificado localmente (requiere JDK 17 + PostgreSQL)
⚠️ El pom.xml tiene 480 líneas de dependencias
```

### 6.3 Tests
- **Frontend:** Vitest unitarios (`npm run test`) y Playwright E2E (`npm run test:e2e`)
- **Backend:** Tests no verificados recientemente
- **Servicios con tests:** `auth.test.ts`, `projects.test.ts`, `chat.test.ts`, `documentChat.test.ts`, `analytics.test.ts`, `intelligence/*.test.ts`

---

## 7. Tareas Pendientes / Roadmap

### 7.1 Prioridad Alta
- [ ] **Rediseño de KIN Empresas** (`kin-frontend`) para mantener consistencia visual con KIN Medical
- [ ] **Verificar backend build** (`mvn clean package`) en entorno local o CI
- [ ] **Mejorar la validación de cédulas** — Integrar APIs de Registraduría/Secretaría de Salud (Opción B del plan original)
- [ ] **Tests E2E del flujo completo de registro** — Paciente y médico con Playwright

### 7.2 Prioridad Media
- [ ] **Extender idempotencia** en el backend (pendiente de la sesión anterior)
- [ ] **Marketing:** Preparar lanzamiento oficial con la nueva imagen visual
- [ ] **Dashboard del paciente** — Mejoras de UX adicionales
- [ ] **Portal médico** — Mejoras en la experiencia de seguimiento

### 7.3 Observaciones y Discrepancias
- ⚠️ **Ruta `ALLOWED_ORIGINS` en `render.yaml`** está en `https://kin-frontend.onrender.com,https://kin-platform.com` pero no incluye `https://www.kin-platform-medical.com` ni los dominios Vercel. El `GUARANTEED_ORIGINS` en `SecurityConfig.java` cubre esto, pero el valor en Render debería actualizarse si se despliega en nuevos dominios.
- ⚠️ **La ruta `/auth/verify-email`** en el frontend se resuelve a `/verify-email` (grupo `(auth)` no afecta URL). Se creó un wrapper en `/auth/verify-email` para compatibilidad. Verificar que el backend de correo envíe el enlace correcto.
- ⚠️ **El `next.config.ts`** solo tiene rewrite para `/api/medical/:path*`. No hay rewrite para `/api/auth/*`, lo que significa que las llamadas del frontend a `/api/auth/verify-email` van directamente al backend. Verificar que el `API_URL` en `api.ts` esté correctamente configurado.

---

## 8. Instrucciones para el Próximo Agente

### 8.1 Levantar el Proyecto Localmente

**Frontend:**
```bash
cd kin-frontend-medical
npm install
npm run dev        # Puerto 3001
npm run build      # Verificar BUILD SUCCESS
npm run test       # Tests unitarios
```

**Backend:**
```bash
cd kin-backend
# Requiere JDK 17 y PostgreSQL
mvn clean package
# O usar Docker:
docker-compose up
```

### 8.2 Estructura de Archivos Clave

```
kin-frontend-medical/
├── src/
│   ├── app/
│   │   ├── page.tsx                          # Landing page (rediseñada)
│   │   ├── layout.tsx                         # Con Inter font
│   │   ├── (auth)/
│   │   │   ├── login/page.tsx                 # Login con PasswordInput
│   │   │   ├── register/page.tsx              # Selección Paciente/Médico
│   │   │   ├── register/salud/
│   │   │   │   ├── paciente/page.tsx          # Wizard 3 pasos
│   │   │   │   └── medico/page.tsx            # Wizard 3 pasos
│   │   │   └── verify-email/page.tsx          # Verificación de correo
│   │   ├── auth/verify-email/page.tsx         # Wrapper /auth/verify-email
│   │   └── dashboard/physician/verification/page.tsx
│   ├── components/
│   │   ├── ui/
│   │   │   ├── PasswordInput.tsx              # Toggle show/hide password
│   │   │   └── ValidatedInput.tsx             # Validación en tiempo real
│   │   └── auth/
│   │       └── RegisterWizard.tsx             # Barra de progreso
│   ├── services/
│   │   ├── api.ts                             # API client con baseUrl
│   │   └── auth.ts                            # Servicios de autenticación
│   ├── styles/globals.css                     # Paleta medical/accent
│   └── app/public/logo-medical.svg            # Logo KIN Medical
├── tailwind.config.ts                         # Paleta extendida
└── next.config.ts                             # Rewrites para API

kin-backend/
├── src/main/java/com/kinplatform/
│   ├── auth/
│   │   ├── AuthController.java                # /auth/** endpoints
│   │   └── verification/VerifyEmailOutcome.java
│   ├── common/config/
│   │   └── SecurityConfig.java                # CORS + JWT + Security
│   ├── kin/health/physician/
│   │   └── api/
│   │       ├── PhysicianController.java         # /health/physician/**
│   │       └── PhysicianApplicationService.java
│   └── user/
│       ├── PhysicianVerificationStatus.java
│       └── UserRepository.java
├── src/main/resources/
│   ├── application.yml                        # context-path: /api/v1
│   ├── application-render.properties
│   └── db/migration/                          # Flyway V1-V30+
└── pom.xml                                    # Spring Boot 3.2.5, Java 17
```

### 8.3 Comandos Úteis

```bash
# Verificar build frontend
cd kin-frontend-medical && npm run build

# Verificar build backend
cd kin-backend && mvn clean package

# Ver historial de commits
git log --oneline -20

# Ver diff antes de commit
git diff --stat

# Deploy en Vercel
cd kin-frontend-medical && git push origin main

# Deploy en Render (backend)
cd kin-backend && git push origin main
```

### 8.4 Reglas de Desarrollo

- **NO tocar el backend** salvo que se indique explícitamente (solo añadir endpoints).
- **NO modificar `kin-frontend`** (Empresas) salvo que se indique.
- **Mantener la paleta de colores** `medical-*` (azul) y `accent-*` (verde).
- **Todos los CTAs y formularios** deben mantener funcionalidad intacta.
- **Build debe pasar** (`npm run build → BUILD SUCCESS`) antes de cualquier commit.
- **No hacer commit, push ni deploy** sin autorización explícita.

---

## 9. Metadatos del Documento

- **Última actualización:** 11 de Septiembre de 2026
- **Commits desde última auditoría:** 6 (`f6ff0f3` a `0492f50`)
- **Archivos modificados en total:** ~15 (frontend) + 1 (backend)
- **Rutas frontend:** 32 (todas prerenderizadas)
- **Rutas backend:** ~25+ endpoints documentados
- **Stack:** Next.js 16 + React 19 + Tailwind 4 + Spring Boot 3.2.5 + Java 17 + PostgreSQL

---

*Este documento debe actualizarse con cada iteración significativa del proyecto.*
