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
| `MAIL_DEBUG_FALBACK_ALLOWLIST` | Lista permitida debug | `admin@kin-platform.com` | No |

### Frontend Base URL

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| `FRONTEND_BASE_URL` | URL base para enlaces en emails | `https://kin-platform.com` | Sí |

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

## Render - Configuración (render.yaml)

### Backend (kin-backend)

Variables a configurar en Dashboard de Render (no en código):

```yaml
envVars:
  - key: SPRING_PROFILES_ACTIVE
    value: render,shadow
  - key: JWT_SECRET
    sync: false
  - key: DEEPSEEK_API_KEY
    sync: false
  - key: STRIPE_SECRET_KEY
    sync: false
  - key: STRIPE_WEBHOOK_SECRET
    sync: false
  - key: DATABASE_URL
    fromDatabase:
      name: kin-db
      property: connectionString
  - key: DB_HOST
    fromDatabase:
      name: kin-db
      property: host
  - key: DB_PORT
    fromDatabase:
      name: kin-db
      property: port
  - key: DB_NAME
    fromDatabase:
      name: kin-db
      property: database
  - key: DATABASE_USER
    fromDatabase:
      name: kin-db
      property: user
  - key: DATABASE_PASSWORD
    fromDatabase:
      name: kin-db
      property: password
  - key: ALLOWED_ORIGINS
    value: "https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001"
  - key: APP_MAIL_ENABLED
    value: "true"
  - key: MAIL_HOST
    sync: false
  - key: MAIL_PORT
    value: "587"
  - key: MAIL_USERNAME
    sync: false
  - key: MAIL_PASSWORD
    sync: false
  - key: MAIL_FROM
    sync: false
  - key: MAIL_FROM_NAME
    value: "KIN Platform"
  - key: MAIL_DIAGNOSTIC_TO
    sync: false
  - key: MAIL_DEBUG_FALLBACK
    value: "false"
  - key: MAIL_DEBUG_FALLBACK_ALLOWLIST
    sync: false
  - key: FRONTEND_BASE_URL
    value: "https://kin-platform.com"
  - key: ALLOW_UNLIMITED_INVITES
    value: "true"
  - key: KIN_HEALTH_PHYSICIAN_ALLOW_UNLIMITED_INVITES
    value: "true"
  - key: KIN_HEALTH_PHYSICIAN_ENFORCE_INVITE_QUOTA
    value: "false"
  - key: CORS_ALLOWED_ORIGINS
    value: "https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001"
  - key: APP_SESSION_COOKIE_SECURE
    value: "true"
  - key: APP_SESSION_COOKIE_SAME_SITE
    value: "None"
  - key: FRONTEND_BASE_URL
    value: "https://kin-platform.com"
  - key: JWT_SECRET
    sync: false
  - key: DEEPSEEK_API_KEY
    sync: false
  - key: STRIPE_SECRET_KEY
    sync: false
  - key: STRIPE_WEBHOOK_SECRET
    sync: false
  - key: DATABASE_URL
    fromDatabase:
      name: kin-db
      property: connectionString
  - key: DATABASE_USER
    fromDatabase:
      name: kin-db
      property: user
  - key: DATABASE_PASSWORD
    fromDatabase:
      name: kin-db
      property: password
```

### Frontend Empresas (kin-frontend)

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

### Frontend Medical (kin-frontend-medical)

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

## Frontend - Configuración Local (.env.local)

```bash
# kin-frontend/.env.local
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_FRONTEND_NAME="KIN Empresas"
NEXT_PUBLIC_VERTICAL=empresas

# kin-frontend-medical/.env.local
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

Configurar en Dashboard de Render > Service > Environment:

### kin-backend
- `CORS_ALLOWED_ORIGINS` = `https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001`
- `APP_SESSION_COOKIE_SECURE` = `true`
- `APP_SESSION_COOKIE_SAME_SITE` = `None`
- `FRONTEND_BASE_URL` = `https://kin-platform.com`

### kin-frontend
- `NEXT_PUBLIC_API_URL` = `https://kin-backend-lwmy.onrender.com/api/v1`
- `NEXT_PUBLIC_FEEDBACK_URL` = `https://forms.example.com/feedback-piloto`
- `NEXT_PUBLIC_FRONTEND_NAME` = `KIN Empresas`
- `NEXT_PUBLIC_VERTICAL` = `empresas`

### kin-frontend-medical
- `NEXT_PUBLIC_API_URL` = `https://kin-backend-lwmy.onrender.com/api/v1`
- `NEXT_PUBLIC_FEEDBACK_URL` = `https://forms.example.com/feedback-piloto`
- `NEXT_PUBLIC_FRONTEND_NAME` = `KIN Medical`
- `NEXT_PUBLIC_VERTICAL` = `medical`
```

Now let me create the remaining documentation files:
1. CORS_CONFIG.md
2. AUTH_ME_EXTENDED.md
2. ROLLBACK_PLAN_COMMIT_0.md
2. COMMIT_0_CHECKLIST.md
2. RENDER_CONFIG.md
<tool_call>
<function=write>
<parameter=content>
# Configuración CORS - KIN Platform

## Resumen

La configuración CORS permite que los frontends (kin-platform.com, kin-platform-medical.com, previews de Vercel, y localhost) accedan al backend API.

## Configuración en Backend

### Variable de Entorno: `CORS_ALLOWED_ORIGINS`

```yaml
# render.yaml / variable de entorno
CORS_ALLOWED_ORIGINS: "https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001"
```

### En application.yml / application-prod.properties

```yaml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001}
```

### En SecurityConfig.java

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    var origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    
    // Garantía: los orígenes del frontend nunca se pierden
    for (String origin : GUARANTEED_ORIGINS) {
        if (!origins.contains(origin)) {
            origins.add(origin);
        }
    }
    
    var config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

## Orígenes Permitidos (Producción)

| Origen | Descripción |
|--------|-------------|
| `https://kin-platform.com` | Frontend Empresas (producción) |
| `https://kin-platform-medical.com` | Frontend Medical (producción) |
| `https://kin-frontend.vercel.app` | Preview deployments Empresas (Vercel) |
| `https://kin-frontend-medical.vercel.app` | Preview deployments Medical (Vercel) |
| `http://localhost:3000` | Desarrollo local Empresas |
| `http://localhost:3001` | Desarrollo local Medical |

## Orígenes Garantizados (Siempre presentes)

```java
private static final List<String> GUARANTEED_ORIGINS = List.of(
    "https://kin-platform.com",
    "https://kin-platform-medical.com"
);
```

Estos orígenes **nunca se eliminan** aunque `CORS_ALLOWED_ORIGINS` no los incluya.

## Configuración en Render (Dashboard)

En Render Dashboard > Service > Environment Variables:

| Key | Value |
|-----|-------|
| `CORS_ALLOWED_ORIGINS` | `https://kin-platform.com,https://kin-platform-medical.com,https://kin-frontend.vercel.app,https://kin-frontend-medical.vercel.app,http://localhost:3000,http://localhost:3001` |

## Desarrollo Local

### .env.local (kin-frontend)
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

## Verificación

### Verificar CORS en desarrollo
```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type,Authorization" \
     -X OPTIONS http://localhost:8080/api/v1/health/triage
```

Debe responder con headers:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: Authorization,Content-Type
Access-Control-Allow-Credentials: true
```

### Verificar en producción
```bash
curl -H "Origin: https://kin-platform.com" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type,Authorization" \
     -X OPTIONS https://kin-backend-lwmy.onrender.com/api/v1/health/triage
```

Debe responder con:
```
Access-Control-Allow-Origin: https://kin-platform.com
```

## Troubleshooting

### Error: "CORS policy: No 'Access-Control-Allow-Origin' header"
1. Verificar que el origen está en `CORS_ALLOWED_ORIGINS`
2. Verificar que el origen está en `GUARANTEED_ORIGINS` (SecurityConfig.java)
3. Verificar que el backend recibió el header `Origin` correcto
3. Verificar que la petición no es `OPTIONS` sin `Access-Control-Request-Method`

### Error: "Credential is not allowed"
- Verificar `config.setAllowCredentials(true)`
- Verificar que el origen no es `*` (wildcard no permitido con credentials)

### Error en producción pero no en local
- Verificar que `CORS_ALLOWED_ORIGINS` está configurado en Render Dashboard
- Verificar que el dominio está en `GUARANTEED_ORIGINS` en SecurityConfig.java
- Verificar que no hay proxy/CDN quitando headers CORS
```