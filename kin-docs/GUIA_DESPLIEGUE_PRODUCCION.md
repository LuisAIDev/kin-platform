# Guía de Despliegue en Producción — KIN Platform

## Arquitectura objetivo

| Capa | Tecnología | Entorno |
|------|------------|---------|
| Backend | Spring Boot 3.2.5 (Java 17) | Render |
| Base de datos | PostgreSQL 18 | Neon |
| Frontend | Next.js 16 (Node) | Render (static/SSR) |
| Caché (opcional) | Redis | Render Redis / Upstash |

## 1. Pre-requisitos

- Cuenta en Render y Neon.
- `.env` en la raíz del repo con todos los secretos (nunca versionar).

## 2. Base de datos (Neon)

1. Crear proyecto PostgreSQL en Neon.
2. Copiar `DATABASE_URL` (formato JDBC: `jdbc:postgresql://host.neon.tech/db?sslmode=require`).
3. El esquema se crea automáticamente por Flyway (V1..V28) en el primer arranque
   (`ddl-auto=none`; las migraciones son la fuente de verdad).

## 3. Variables de entorno críticas

| Variable | Requerida | Notas |
|----------|-----------|-------|
| `JWT_SECRET` | Sí | Base64 ≥ 32 bytes. Generar: `openssl rand -base64 32` |
| `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` | Sí | Neon |
| `DEEPSEEK_API_KEY` | Sí | Chat con IA |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | Opcional | Pagos |
| `APP_MAIL_ENABLED` | Sí | `true` en prod; configurar SMTP |
| `KIN_HEALTH_TELEMEDICINE_CRYPTO_SECRET` | Sí | Clave AES/GCM para mensajes. Generar: `openssl rand -hex 32` |
| `KIN_HEALTH_TRIAGE_CATALOG_CACHE_ENABLED` | No | `true` (default) |
| `ALLOWED_ORIGINS` | Sí | `https://kin-platform.com` (siempre garantizado por código) |

## 4. Desplegar el backend (Render)

1. **Web Service** → repositorio → rama `main`.
2. Build: `./mvnw clean package -DskipTests` (o confiar en el buildpack).
3. Start: `java -jar kin-backend/target/kin-backend-0.0.1-SNAPSHOT.jar`.
4. Profile: `SPRING_PROFILES_ACTIVE=prod`.
5. Configurar todas las variables de entorno (sección 3).
6. Verificar: `GET /api/v1/actuator/health` → `UP`; `GET /api/v1/actuator/health/healthModules` → estado de los módulos de salud.

## 5. Desplegar el frontend (Render)

1. **Static Site** o **Web Service** para Next.js.
2. Build: `npm install && npm run build`.
3. `NEXT_PUBLIC_API_URL=https://kin-backend-lwmy.onrender.com/api/v1` (URL real del backend).
4. `NEXT_PUBLIC_FEEDBACK_URL=<URL del formulario de feedback del piloto>` (opcional; el botón "Dar feedback" usa `mailto:` si se omite).
5. Verificar: cargar `/dashboard/projects` con sesión.

## 6. Post-despliegue

- [ ] Migraciones Flyway aplicadas (revisar logs de arranque).
- [ ] `GET /api/v1/actuator/metrics` expone métricas (incluidas `kin.health.*`).
- [ ] Healthchecks: `/api/v1/actuator/health`.
- [ ] Importar catálogo inicial si es necesario:
  `POST /api/v1/admin/health/catalog/import-from-external` (rol ADMIN).
- [ ] Revisar `GUIA_MONITOREO.md` para umbrales y alertas.

## 7. Despliegue de migraciones

- Las migraciones Flyway se ejecutan al arrancar; son idempotentes (V1..V28).
- Para regenerar la base local: `scripts/reset-dev-db.ps1` (exige confirmación `RESET`).
- En producción, no regenerar: las migraciones avanzan hacia delante.
