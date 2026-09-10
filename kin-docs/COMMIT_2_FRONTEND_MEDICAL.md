# Commit 2 — Frontend KIN Medical (`kin-frontend-medical`)

## Contexto

La plataforma KIN sirve a dos productos con audiencias y narrativas distintas:

- **KIN Empresas** (`kin-frontend`): estructuración de proyectos con IA.
- **KIN Medical** (`kin-frontend-medical`): práctica clínica (pacientes y médicos).

Hasta ahora ambos convivían en `kin-frontend` con un selector de vertical. El
Commit 2 separa **visual y operativamente** el producto Medical en un frontend
independiente, manteniendo **un único backend** y la **misma autenticación**
(un usuario puede entrar a ambos productos con el mismo login).

## Arquitectura

```
kin-frontend-medical/
├── package.json, next.config.ts, tsconfig.json, postcss.config.mjs
├── playwright.config.ts, vitest.config.ts
├── public/
└── src/
    ├── app/
    │   ├── layout.tsx            # metadata KIN Medical
    │   ├── page.tsx              # landing B2B
    │   ├── (auth)/               # login, register, verify-email
    │   └── dashboard/
    │       ├── layout.tsx        # SessionGuard + RoleGuard + Sidebar + Header
    │       ├── patient/*         # 10 páginas
    │       ├── physician/*       # 7 páginas
    │       └── settings/
    ├── components/               # health, patient, physician, telemedicine, triage, followup, audit, ui, layout, auth
    ├── services/                 # cliente API + servicios Medical
    ├── hooks/, utils/, types/, styles/
```

## Decisiones técnicas

| Decisión | Detalle |
|----------|---------|
| **Proyecto independiente** | No monorepo ni paquete `@kin/ui` en esta fase; los componentes se **copian** y se adaptan. La extracción a paquete compartido queda para una fase posterior. |
| **Cliente API** | `services/api.ts` apunta a `NEXT_PUBLIC_API_URL` (`/api/v1`) y los servicios usan rutas `/medical/*` (alias por contexto del Commit 1). |
| **Sin selector de vertical** | Medical es siempre la vertical `salud`. `utils/roles.ts` fija `resolveVertical() → "salud"`. |
| **Capacidad profesional** | El acceso al portal médico se decide por `physicianCapability` (backend), no por `role === "PHYSICIAN"`. |
| **Tailwind v4** | `@import "tailwindcss"` + `@theme` en `styles/globals.css` (paleta azul médico + verde). |
| **Root Directory en Vercel** | `kin-frontend-medical`. |

## Rutas migradas

### Paciente (`/dashboard/patient/*`)
`health`, `triage`, `plans`, `invitations`, `messages`, `appointments`,
`schedule`, `followup`, `documents`, `audit`.

### Médico (`/dashboard/physician/*`)
`/` (Portal Médico), `plans`, `messages`, `schedule`, `appointments`,
`followup`, `documents`.

### Comunes
`/dashboard`, `/dashboard/settings`, `/dashboard/accept-invitation`,
`/login`, `/register`, `/register/salud/{paciente,medico}`, `/verify-email`.

## Landing page (`/`)

- **Hero**: "KIN Medical: infraestructura inteligente para tu práctica clínica."
- **Sección 1 — Médicos independientes**: 6 capacidades + CTA "Solicitar acceso como profesional".
- **Sección 2 — Clínicas y hospitales (próximamente)**: multi-médico, multi-sede, dashboards, reportes.
- **Sección 3 — IPS/EPS (futuro)**: redes de prestadores, autorizaciones, reportes.
- **Planes**: Medical Free ($0) · Medical Pro ($29) · Medical Clinic ($299, próximamente).
- **Footer**: enlaces y copyright.
- **CTAs**: `Soy Médico` → `/register/salud/medico` · `Soy Paciente` → `/register/salud/paciente` · `Quiero para mi Clínica` → `#contacto`.

## Tests E2E

Config: `playwright.config.ts` (baseURL `http://localhost:3001`, arranca `npm run dev`).

```bash
# Suite base (landing; sin backend)
npm run test:e2e

# Flujos autenticados (requieren backend + cuentas sembradas)
E2E_AUTH=1 \
E2E_PATIENT_EMAIL=paciente@test.com E2E_PATIENT_PASSWORD=Test123! \
E2E_PHYSICIAN_EMAIL=medico@test.com E2E_PHYSICIAN_PASSWORD=Test123! \
E2E_FREE_EMAIL=free@test.com E2E_FREE_PASSWORD=Test123! \
npm run test:e2e
```

Cobertura: landing + CTAs, login paciente/médico, usuario sin acceso, triaje
completo y portal médico.

## Despliegue (Vercel)

1. **Root Directory** = `kin-frontend-medical`.
2. **Framework Preset** = Next.js.
3. **Build Command** = `npm install && npm run build`; **Output** = `.next`.
4. **Variables**: `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_FRONTEND_NAME=KIN Medical`, `NEXT_PUBLIC_VERTICAL=medical`.
5. Dominio: `kin-platform-medical.com`.

## Próximos pasos

- **Commit 3** — Migración de rutas (alias por contexto, navegación cruzada).
- **Commit 4** — Limpieza de `kin-frontend` (retirar las rutas de paciente/médico migradas).
