# Variables de Entorno - KIN Platform

## Backend (kin-backend)

### Variables Requeridas

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `SPRING_PROFILES_ACTIVE` | Perfiles activos de Spring | `render,shadow` | Sí |
| `JWT_SECRET` | Clave secreta para JWT (Base64, min 256 bits) | `base64...` | Sí |
| `DEEPSEEK_API_KEY` | API Key de DeepSeek | `sk-...` | Sí |
| `STRIPE_SECRET_KEY` | Secret key de Stripe | `sk_live_...` | Sí |
| `STRIPE_WEBHOOK_SECRET` | Secret del webhook de Stripe | `whsec_...` | Sí |
| `DATABASE_URL` | URL de conexión JDBC a PostgreSQL | `jdbc:postgresql://...` | Sí |
| `DATABASE_USER` | Usuario de la BD | `kin_admin` | Sí |
| `DATABASE_PASSWORD` | Contraseña de la BD | `********` | Sí |

### CORS

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos (comma-separated) | `https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001` | Sí |

### Configuración de Sesión

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `APP_SESSION_COOKIE_SECURE` | Cookie secure (true en prod HTTPS) | `true` | Sí |
| `APP_SESSION_COOKIE_SAME_SITE` | SameSite de la cookie | `None` | Sí |

### Frontend URL

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `FRONTEND_BASE_URL` | URL base del frontend para enlaces en emails | `https://kin-platform.com` | Sí |

### Configuración de Correo (Opcional - Solo si `APP_MAIL_ENABLED=true`)

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `APP_MAIL_ENABLED` | Habilitar envío de correos | `true` | Solo si se usa email |
| `MAIL_HOST` | Servidor SMTP | `smtp.sendgrid.net` | Si email habilitado |
| `MAIL_PORT` | Puerto SMTP | `587` | Si email habilitado |
| `MAIL_USERNAME` | Usuario SMTP | `apikey` | Si email habilitado |
| `MAIL_PASSWORD` | Contraseña SMTP | `SG.xxx` | Si email habilitado |
| `MAIL_FROM` | Email remitente | `noreply@kin-platform.com` | Si email habilitado |
| `MAIL_FROM_NAME` | Nombre remitente | `KIN Platform` | No |
| `MAIL_DIAGNOSTIC_TO` | Email para diagnóstico | `admin@kin-platform.com` | No |
| `MAIL_DEBUG_FALLBACK` | Fallback debug | `false` | No |
| `MAIL_DEBUG_FALLBACK_ALLOWLIST` | Lista permitida debug | `admin@kin-platform.com` | No |

### Frontend Base URL

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `FRONTEND_BASE_URL` | URL base del frontend para enlaces en emails | `https://kin-platform.com` | Sí |

### Stripe

| Variable | Descripción | Requerida |
|----------|-------------|-----------|
| `STRIPE_SECRET_KEY` | Secret key Stripe | Sí (planes pagos) |
| `STRIPE_WEBHOOK_SECRET` | Webhook secret | Sí (planes pagos) |

### Configuración de Invitaciones (Piloto)

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `ALLOW_UNLIMITED_INVITES` | Desactivar límites de invitaciones (piloto) | `true` | No (default false) |
| `KIN_HEALTH_PHYSICIAN_ALLOW_UNLIMITED_INVITES` | Ilimitado para médicos | `true` | No |
| `KIN_HEALTH_PHYSICIAN_ENFORCE_INVITE_QUOTA` | Forzar cuotas | `false` (piloto) | No |

---

## Frontend - Empresas (kin-frontend)

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `NEXT_PUBLIC_API_URL` | URL del backend API | `https://kin-backend-lwmy.onrender.com/api/v1` | Sí |
| `NEXT_PUBLIC_FEEDBACK_URL` | URL formulario feedback | `https://forms.example.com/feedback` | No |
| `NEXT_PUBLIC_FRONTEND_NAME` | Nombre del frontend | `KIN Empresas` | No |
| `NEXT_PUBLIC_VERTICAL` | Vertical por defecto | `empresas` | No |

---

## Frontend - Medical (kin-frontend-medical)

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `NEXT_PUBLIC_API_URL` | URL del backend API | `https://kin-backend-lwmy.onrender.com/api/v1` | Sí |
| `NEXT_PUBLIC_FEEDBACK_URL` | URL formulario feedback | `https://forms.example.com/feedback` | No |
| `NEXT_PUBLIC_FRONTEND_NAME` | Nombre del frontend | `KIN Medical` | No |
| `NEXT_PUBLIC_VERTICAL` | Vertical por defecto | `medical` | No |

---

## Frontend - Configuración Local (.env.local)

### kin-frontend/.env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Empresas"
NEXT_PUBLIC_VERTICAL=empresas
```

### kin-frontend-medical/.env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Medical"
NEXT_PUBLIC_VERTICAL=medical
```

---

## Vercel - Variables de Entorno

### kin-frontend (Empresas)
```
NEXT_PUBLIC_API_URL=https://kin-backend-lwmy.onrender.com/api/v1
NEXT_PUBLIC_FEEDBACK_URL=https://forms.example.com/feedback-piloto
NEXT_PUBLIC_FRONTEND_NAME=KIN Empresas
NEXT_PUBLIC_VERTICAL=empresas
```

### kin-frontend-medical (Medical)
```
NEXT_PUBLIC_API_URL=https://kin-backend-lwmy.onrender.com/api/v1
NEXT_PUBLIC_FEEDBACK_URL=https://forms.example.com/feedback-piloto
NEXT_PUBLIC_FRONTEND_NAME=KIN Medical
NEXT_PUBLIC_VERTICAL=medical
```

---

## Render - Variables de Entorno (Dashboard)

### kin-backend
| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `render,shadow` |
| `JWT_SECRET` | (sync: false) |
| `DEEPSEEK_API_KEY` | (sync: false) |
| `STRIPE_SECRET_KEY` | (sync: false) |
| `STRIPE_WEBHOOK_SECRET` | (sync: false) |
| `DATABASE_URL` | fromDatabase: kin-db, connectionString |
| `DB_HOST` | fromDatabase: kin-db, host |
| `DB_PORT` | fromDatabase: kin-db, port |
| `DB_NAME` | fromDatabase: kin-db, database |
| `DATABASE_USER` | fromDatabase: kin-db, user |
| `DATABASE_PASSWORD` | fromDatabase: kin-db, password |
| `ALLOWED_ORIGINS` | `https://kin-frontend.onrender.com,https://kin-platform.com` |
| `APP_MAIL_ENABLED` | `true` |
| `MAIL_HOST` | (sync: false) |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | (sync: false) |
| `MAIL_PASSWORD` | (sync: false) |
| `MAIL_FROM` | (sync: false) |
| `MAIL_FROM_NAME` | `KIN Platform` |
| `MAIL_DIAGNOSTIC_TO` | (sync: false) |
| `MAIL_DEBUG_FALLBACK` | `false` |
| `MAIL_DEBUG_FALLBACK_ALLOWLIST` | (sync: false) |
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

### kin-frontend (Empresas)
```yaml
envVars:
  - key: NEXT_PUBLIC_API_URL
    value: https://kin-backend-lwmy.onrender.com/api/v1
  - key: NEXT_PUBLIC_FEEDBACK_URL
    value: https://forms.example.com/feedback-piloto
  - key: NEXT_PUBLIC_FRONTEND_NAME
    value: "KIN Empresas"
  - key: NEXT_PUBLIC_VERTICAL
    value: empresas
```

### kin-frontend-medical
```yaml
envVars:
  - key: NEXT_PUBLIC_API_URL
    value: https://kin-backend-lwmy.onrender.com/api/v1
  - key: NEXT_PUBLIC_FEEDBACK_URL
    value: https://forms.example.com/feedback-piloto
  - key: NEXT_PUBLIC_FRONTEND_NAME
    value: "KIN Medical"
  - key: NEXT_PUBLIC_VERTICAL
    value: medical
```

---

## Variables Locales (.env.local)

### kin-frontend/.env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Empresas"
NEXT_PUBLIC_VERTICAL=empresas
```

### kin-frontend-medical/.env.local
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Medical"
NEXT_PUBLIC_VERTICAL=medical
```