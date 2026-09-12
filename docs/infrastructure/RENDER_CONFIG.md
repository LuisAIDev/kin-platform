# Configuración de Render - KIN Platform

## Resumen

Este documento describe la configuración de servicios en Render para KIN Platform.

## Servicios en Render

### 1. Base de Datos (kin-db)
- **Tipo**: PostgreSQL
- **Plan**: Starter
- **Nombre BD**: `kin_platform`
- **Usuario**: `kin_admin`
- **Región**: Oregon (US West) o la más cercana a usuarios

### 2. Backend API (kin-backend)
- **Tipo**: Web Service (Docker)
- **Runtime**: Docker
- **Repositorio**: `https://github.com/LuisAIDev/kin-platform`
- **Directorio raíz**: `kin-backend`
- **Contexto Docker**: `.`
- **Plan**: Starter
- **Health Check**: `/api/v1/actuator/health`
- **Auto Deploy**: `true`

#### Variables de Entorno (Configurar en Dashboard Render > Service > Environment)

| Key | Value / Source |
|-----|----------------|
| `SPRING_PROFILES_ACTIVE` | `render,shadow` |
| `JWT_SECRET` | **Secret** (sync: false) |
| `DEEPSEEK_API_KEY` | **Secret** (sync: false) |
| `STRIPE_SECRET_KEY` | **Secret** (sync: false) |
| `STRIPE_WEBHOOK_SECRET` | **Secret** (sync: false) |
| `DATABASE_URL` | `fromDatabase: kin-db, connectionString` |
| `DB_HOST` | `fromDatabase: kin-db, host` |
| `DB_PORT` | `fromDatabase: kin-db, port` |
| `DB_NAME` | `fromDatabase: kin-db, database` |
| `DATABASE_USER` | `fromDatabase: kin-db, user` |
| `DATABASE_PASSWORD` | `fromDatabase: kin-db, password` |
| `ALLOWED_ORIGINS` | `https://kin-frontend.onrender.com,https://kin-platform.com` |
| `APP_MAIL_ENABLED` | `true` |
| `MAIL_HOST` | *(sync: false)* |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | *(sync: false)* |
| `MAIL_PASSWORD` | *(sync: false)* |
| `MAIL_FROM` | *(sync: false)* |
| `MAIL_FROM_NAME` | `KIN Platform` |
| `MAIL_DIAGNOSTIC_TO` | *(sync: false)* |
| `MAIL_DEBUG_FALLBACK` | `false` |
| `MAIL_DEBUG_FALLBACK_ALLOWLIST` | *(sync: false)* |
| `FRONTEND_BASE_URL` | `https://kin-platform.com` |
| `ALLOW_UNLIMITED_INVITES` | `true` |
| `KIN_HEALTH_PHYSICIAN_ALLOW_UNLIMITED_INVITES` | `true` |
| `KIN_HEALTH_PHYSICIAN_ENFORCE_INVITE_QUOTA` | `false` |
| `CORS_ALLOWED_ORIGINS` | `https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001` |
| `APP_SESSION_COOKIE_SECURE` | `true` |
| `APP_SESSION_COOKIE_SAME_SITE` | `None` |
| `FRONTEND_BASE_URL` | `https://kin-platform.com` |
| `ALLOW_UNLIMITED_INVITES` | `true` |
| `KIN_HEALTH_PHYSICIAN_ALLOW_UNLIMITED_INVITES` | `true` |
| `KIN_HEALTH_PHYSICIAN_ENFORCE_INVITE_QUOTA` | `false` |

#### Secrets (sync: false - configurar en Dashboard)
- `JWT_SECRET`
- `DEEPSEEK_API_KEY`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `MAIL_HOST` (sync: false)
- `MAIL_PORT` = `587`
- `MAIL_USERNAME` (sync: false)
- `MAIL_PASSWORD` (sync: false)
- `MAIL_FROM` (sync: false)
- `MAIL_FROM_NAME` = `KIN Platform`
- `MAIL_DIAGNOSTIC_TO` (sync: false)
- `MAIL_DEBUG_FALLBACK` = `false`
- `MAIL_DEBUG_FALLBACK_ALLOWLIST` (sync: false)
- `JWT_SECRET` (sync: false)
- `DEEPSEEK_API_KEY` (sync: false)
- `STRIPE_SECRET_KEY` (sync: false)
- `STRIPE_WEBHOOK_SECRET` (sync: false)

### 2. Frontend Empresas (kin-frontend)

- **Tipo**: Web Service (Node.js)
- **Runtime**: Node
- **Repositorio**: `https://github.com/LuisAIDev/kin-platform`
- **Directorio raíz**: `kin-frontend`
- **Plan**: Starter
- **Build Command**: `npm ci && npm run build`
- **Start Command**: `npm start`
- **Auto Deploy**: `true`

#### Variables de Entorno

| Key | Value |
|-----|-------|
| `NEXT_PUBLIC_API_URL` | `https://kin-backend-lwmy.onrender.com/api/v1` |
| `NEXT_PUBLIC_FEEDBACK_URL` | `https://forms.example.com/feedback-piloto` |

### 3. Frontend Medical (kin-frontend-medical) - **NUEVO**

- **Tipo**: Web Service (Node.js)
- **Runtime**: Node
- **Repositorio**: `https://github.com/LuisAIDev/kin-platform`
- **Directorio raíz**: `kin-frontend-medical`
- **Plan**: Starter
- **Build Command**: `npm ci && npm run build`
- **Start Command**: `npm start`
- **Auto Deploy**: `true`

#### Variables de Entorno

| Key | Value |
|-----|-------|
| `NEXT_PUBLIC_API_URL` | `https://kin-backend-lwmy.onrender.com/api/v1` |
| `NEXT_PUBLIC_FEEDBACK_URL` | `https://forms.example.com/feedback-piloto` |
| `NEXT_PUBLIC_FRONTEND_NAME` | `KIN Medical` |
| `NEXT_PUBLIC_VERTICAL` | `medical` |

### 3. Base de Datos (kin-db)

- **Tipo**: PostgreSQL
- **Plan**: Starter
- **Nombre BD**: `kin_platform`
- **Usuario**: `kin_admin`

---

## Configuración de Dominios

### Dominios Principales

| Dominio | Servicio | Configuración |
|---------|----------|---------------|
| `kin-platform.com` | Frontend Empresas | Vercel → kin-frontend |
| `kin-platform-medical.com` | Frontend Medical | Vercel → kin-frontend-medical |
| `kin-backend-lwmy.onrender.com` | Backend API | Render → kin-backend |

### DNS (Configurar en proveedor DNS)

| Tipo | Nombre | Valor | TTL |
|-----|--------|-------|-----|
| CNAME | `kin-platform.com` | `cname.vercel-dns.com` | 3600 |
| CNAME | `www.kin-platform.com` | `cname.vercel-dns.com` | 3600 |
| CNAME | `kin-platform-medical.com` | `cname.vercel-dns.com` | 3600 |
| CNAME | `www.kin-platform-medical.com` | `cname.vercel-dns.com` | 3600 |
| CNAME | `kin-backend-lwmy.onrender.com` | (asignado por Render) | 3600 |

---

## Variables de Entorno por Ambiente

### Desarrollo Local

#### kin-frontend/.env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Empresas"
NEXT_PUBLIC_VERTICAL=empresas
```

#### kin-frontend-medical/.env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Medical"
NEXT_PUBLIC_VERTICAL=medical
```

### Backend (Local - application-dev.yml)

```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/kin_platform
    username: kin_admin
    password: kin_admin
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate

app:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:3001
  session:
    cookie-secure: false
    cookie-same-site: Lax
  frontend:
    base-url: http://localhost:3000
```

---

## Configuración de Vercel

### kin-frontend (Empresas)

**Project Settings > General**
- Framework Preset: Next.js
- Root Directory: `kin-frontend`
- Build Command: `npm ci && npm run build`
- Output Directory: `.next`
- Install Command: `npm ci`

**Environment Variables:**
```
NEXT_PUBLIC_API_URL=https://kin-backend-lwmy.onrender.com/api/v1
NEXT_PUBLIC_FEEDBACK_URL=https://forms.example.com/feedback-piloto
NEXT_PUBLIC_FRONTEND_NAME=KIN Empresas
NEXT_PUBLIC_VERTICAL=empresas
```

**Domains:**
- `kin-platform.com` (Primary)
- `kin-frontend.vercel.app` (Vercel auto)

---

### kin-frontend-medical (NUEVO)

**Project Settings > General**
- Framework Preset: Next.js
- Root Directory: `kin-frontend-medical`
- Build Command: `npm ci && npm run build`
- Output Directory: `.next`
- Install Command: `npm ci`

**Environment Variables:**
```
NEXT_PUBLIC_API_URL=https://kin-backend-lwmy.onrender.com/api/v1
NEXT_PUBLIC_FEEDBACK_URL=https://forms.example.com/feedback-piloto
NEXT_PUBLIC_FRONTEND_NAME=KIN Medical
NEXT_PUBLIC_VERTICAL=medical
```

**Domains:**
- `kin-platform-medical.com` (Primary)
- `kin-frontend-medical.vercel.app` (Vercel auto)

---

## Configuración de DNS (Proveedor DNS)

### Registros Requeridos

| Tipo | Nombre | Valor | TTL |
|-----|--------|-------|-----|
| A | `@` | `76.76.21.21` (Vercel IP) | 3600 |
| CNAME | `www` | `cname.vercel-dns.com` | 3600 |
| CNAME | `kin-platform-medical.com` | `cname.vercel-dns.com` | 3600 |
| CNAME | `www.kin-platform-medical.com` | `cname.vercel-dns.com` | 3600 |

### Verificación DNS
```bash
# Verificar propagación
dig kin-platform-medical.com CNAME
dig kin-platform-medical.com A

# Verificar SSL
curl -I https://kin-platform-medical.com
# Debe responder con certificado válido (Let's Encrypt / Vercel)
```

---

## Render - Configuración Adicional

### Health Check
- **Path**: `/api/v1/actuator/health`
- **Intervalo**: 30s
- **Timeout**: 10s

### Auto Deploy
- **Enabled**: `true` (push a main = deploy automático)

### Health Check Path
- `/api/v1/actuator/health` (Backend)
- `/` (Frontend - Vercel maneja automáticamente)

---

## Variables de Entorno Sensibles (NO en código)

Configurar **SOLO en Dashboard de Render / Vercel** (nunca en código):

### Secrets (Render Dashboard > Service > Environment > Secret Files / Environment Variables)

| Servicio | Variable | Tipo | Dónde |
|----------|----------|------|-------|
| kin-backend | `JWT_SECRET` | Secret | Render Dashboard > kin-backend > Environment |
| kin-backend | `DEEPSEEK_API_KEY` | Secret | Render Dashboard |
| kin-backend | `STRIPE_SECRET_KEY` | Secret | Render Dashboard |
| kin-backend | `STRIPE_WEBHOOK_SECRET` | Secret | Render Dashboard |
| kin-backend | `JWT_SECRET` | Secret | Render Dashboard |
| kin-backend | `MAIL_HOST` | Secret | Render Dashboard |
| kin-backend | `MAIL_USERNAME` | Secret | Render Dashboard |
| kin-backend | `MAIL_PASSWORD` | Secret | Render Dashboard |
| kin-backend | `MAIL_PASSWORD` | Secret | Render Dashboard |
| kin-backend | `MAIL_FROM` | Secret | Render Dashboard |
| kin-backend | `MAIL_DEBUG_FALLBACK_ALLOWLIST` | Secret | Render Dashboard |
| kin-backend | `MAIL_DIAGNOSTIC_TO` | Secret | Render Dashboard |
| kin-backend | `MAIL_DEBUG_FALLBACK_ALLOWLIST` | Secret | Render Dashboard |
| kin-backend | `JWT_SECRET` | Secret | Render Dashboard |
| kin-backend | `DEEPSEEK_API_KEY` | Secret | Render Dashboard |
| kin-backend | `STRIPE_SECRET_KEY` | Secret | Render Dashboard |
| kin-backend | `STRIPE_WEBHOOK_SECRET` | Secret | Render Dashboard |
| kin-backend | `MAIL_HOST` | Secret | Render Dashboard |
| kin-backend | `MAIL_PORT` | Env Var | Render Dashboard |
| kin-backend | `MAIL_USERNAME` | Secret | Render Dashboard |
| kin-backend | `MAIL_PASSWORD` | Secret | Render Dashboard |
| kin-backend | `MAIL_FROM` | Secret | Render Dashboard |
| kin-backend | `MAIL_FROM_NAME` | Env Var | Render Dashboard |
| kin-backend | `MAIL_DIAGNOSTIC_TO` | Secret | Render Dashboard |
| kin-backend | `MAIL_DEBUG_FALLBACK` | Env Var | Render Dashboard |
| kin-backend | `MAIL_DEBUG_FALLBACK_ALLOWLIST` | Secret | Render Dashboard |
| kin-backend | `JWT_SECRET` | Secret | Render Dashboard |
| kin-backend | `DEEPSEEK_API_KEY` | Secret | Render Dashboard |
| kin-backend | `STRIPE_SECRET_KEY` | Secret | Render Dashboard |
| kin-backend | `STRIPE_WEBHOOK_SECRET` | Secret | Render Dashboard |
| kin-backend | `DATABASE_URL` | From Database | Render Dashboard |
| kin-backend | `DB_HOST` | From Database | Render Dashboard |
| kin-backend | `DB_PORT` | From Database | Render Dashboard |
| kin-backend | `DB_NAME` | From Database | Render Dashboard |
| kin-backend | `DATABASE_USER` | From Database | Render Dashboard |
| kin-backend | `DATABASE_PASSWORD` | From Database | Render Dashboard |

### Frontend (Vercel) - Secrets

| Proyecto | Variable | Valor |
|----------|----------|-------|
| kin-frontend | `NEXT_PUBLIC_API_URL` | `https://kin-backend-lwmy.onrender.com/api/v1` |
| kin-frontend | `NEXT_PUBLIC_FEEDBACK_URL` | `https://forms.example.com/feedback-piloto` |
| kin-frontend-medical | `NEXT_PUBLIC_API_URL` | `https://kin-backend-lwmy.onrender.com/api/v1` |
| kin-frontend-medical | `NEXT_PUBLIC_FEEDBACK_URL` | `https://forms.example.com/feedback-piloto` |

---

## Checklist de Despliegue

### Pre-Deploy
- [ ] Dominio `kin-platform-medical.com` registrado
- [ ] DNS configurado (CNAME a Vercel)
- [ ] Proyecto `kin-frontend-medical` creado en Vercel
- [ ] Variables de entorno configuradas en Vercel (Medical)
- [ ] Variables actualizadas en Vercel (Empresas)
- [ ] Variables de entorno en Render (Backend) actualizadas con nuevos CORS
- [ ] `render.yaml` actualizado si aplica
- [ ] DNS propagado (`dig kin-platform-medical.com`)

### Post-Deploy
- [ ] `curl -I https://kin-platform-medical.com` → 200/307/404 (no error TLS)
- [ ] `curl -I https://kin-platform.com` → OK
- [ ] Healthcheck backend: `curl https://kin-backend-lwmy.onrender.com/api/v1/actuator/health` → `{"status":"UP"}`
- [ ] Login Empresas funciona
- [ ] Login Medical funciona
- [ ] Flujo Stripe completo funciona

---

## Rollback Rápido (Si algo falla)

### Rollback Backend (Render)
```bash
# En Render Dashboard > kin-backend > Deploys
# Click en "..." del deploy anterior > "Rollback to this deploy"
```

### Rollback Frontend (Vercel)
```bash
# En Vercel Dashboard > Project > Deployments
# Click en "..." del deploy anterior > "Promote to Production"
```

### Rollback DNS (si dominio apunta mal)
```bash
# En proveedor DNS: cambiar CNAME a valor anterior
# TTL bajo (300s) para propagación rápida
```

---

## Monitoreo Post-Deploy

### Logs a Revisar
```bash
# Render Backend Logs
# Render Dashboard > kin-backend > Logs

# Vercel Logs
# Vercel Dashboard > Project > Functions > Logs
```

### Métricas Clave
- Healthcheck: `/api/v1/actuator/health` → `{"status":"UP"}`
- Latencia P95 < 500ms
- Error rate < 1%
- Memoria < 80%
- CPU < 70%