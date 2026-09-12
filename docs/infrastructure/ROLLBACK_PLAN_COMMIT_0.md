# Plan de Rollback - Commit 0 (Infraestructura de Dominios)

## Resumen
Este documento describe cómo revertir los cambios del Commit 0 (Infraestructura de Dominios) si algo falla durante o después del despliegue.

## Escenarios de Rollback

### 1. Rollback de DNS (Dominio Apunta Incorrectamente)

**Síntomas:**
- `kin-platform-medical.com` no resuelve o apunta a IP incorrecta
- Certificado SSL no válido
- Sitio no carga

**Pasos de Rollback:**
```bash
# 1. En proveedor DNS (Cloudflare, GoDaddy, Namecheap, etc.):
# Eliminar o modificar registros:
# - ELIMINAR: CNAME kin-platform-medical.com → cname.vercel-dns.com
# - ELIMINAR: CNAME www.kin-platform-medical.com → cname.vercel-dns.com
# (O restaurar valores anteriores si existían)

# 2. Verificar propagación (TTL bajo recomendado: 300s = 5 min)
dig kin-platform-medical.com CNAME
dig kin-platform-medical.com A

# 3. Verificar que el dominio anterior (si existía) funciona
curl -I https://kin-platform-medical.com  # Debe dar error 404/404 o redirigir según config anterior
```

**Tiempo estimado:** 5-30 minutos (dependiendo de TTL DNS)

---

### 2. Rollback de Proyecto Vercel (kin-frontend-medical)

**Síntomas:**
- Deploy fallido en Vercel
- Build falla
- Variables de entorno incorrectas
- Dominio apunta a deployment roto

**Pasos de Rollback:**

#### Opción A: Rollback a Deployment Anterior (Vercel Dashboard)
```bash
# 1. Ir a Vercel Dashboard > kin-frontend-medical > Deployments
# 2. Identificar deployment anterior exitoso (previo al fallo)
# 3. Click en "..." > "Promote to Production"
# 4. Confirmar
```

#### Opción B: Rollback via CLI
```bash
# Instalar Vercel CLI si no está instalado
npm i -g vercel

# Login
vercel login

# Listar deployments
vercel ls kin-frontend-medical

# Promover deployment anterior
vercel promote <deployment-url> --scope=<team-slug>
```

#### Opción C: Eliminar Proyecto Vercel (Extremo)
```bash
# Solo si el proyecto está completamente roto y no hay deployments previos
# En Vercel Dashboard > Project Settings > General > Delete Project
# Confirmar escritura del nombre del proyecto
```

**Tiempo estimado:** 2-5 minutos (Opción A), 5-10 min (Opción B), 2 min (Opción C)

---

### 3. Rollback Variables de Entorno (Vercel)

**Escenario:** Variables de entorno incorrectas causan build failures o runtime errors.

**Rollback en Vercel Dashboard:**
1. Ir a Project Settings > Environment Variables
2. Identificar variables cambiadas recientemente
3. Restaurar valores anteriores (historial no disponible en UI, usar backup manual)
4. Redeploy: `vercel --prod` o push a main

**Rollback via CLI:**
```bash
# Si tienes backup de variables (ej. .env.local.backup)
vercel env pull .env.local.backup --environment=production
vercel env pull .env.local --environment=production
vercel --prod
```

---

### 4. Rollback Backend (Render)

**Escenario:** Deploy de backend falla o introduce bug crítico.

#### Opción A: Rollback Deploy Anterior (Render Dashboard)
1. Ir a Render Dashboard > kin-backend > Deploys
2. Identificar deploy anterior exitoso (verde)
3. Click "..." > "Rollback to this deploy"
4. Confirmar

#### Opción B: Rollback via Git + Redeploy
```bash
# En local
git revert <commit-hash-problemático>
git push origin main

# Render auto-deploy triggerado automáticamente (autoDeploy: true)
```

#### Rollback Base de Datos (Solo si migración V41 aplicada y falla)
```bash
# SOLO si migración V41 aplicada y causa problemas irreversibles
# 1. Backup actual (pg_dump)
pg_dump -h <host> -U <user> -d kin_platform > backup_pre_rollback.sql

# 2. Revertir migración V41 (SOLO si es seguro)
# En Render Dashboard > kin-backend > Shell
# psql $DATABASE_URL -c "DROP COLUMN max_storage_mb, trial_days FROM pricing_plans;"

# 3. Rollback código
git revert <commit-migracion-v41>
git push origin main
```

---

### 5. Rollback DNS (Proveedor Externo)

**Proveedores Comunes:**

| Proveedor | Pasos Rollback |
|-----------|----------------|
| **Cloudflare** | DNS > Records > Edit/Delete CNAME/A record > Save |
| **GoDaddy** | DNS Management > Edit/Delete record > Save |
| **Namecheap** | Advanced DNS > Host Records > Edit/Delete |
| **Cloudflare** | DNS > Records > Edit/Delete > Save |
| **Route53** | Hosted Zones > Record Sets > Delete/Create |
| **Google Domains** | DNS > Custom Resource Records > Delete/Add |

**Pasos Genéricos:**
1. Acceder a panel DNS del registrador
2. Localizar registros para `kin-platform-medical.com` y `www.kin-platform-medical.com`
3. **Eliminar** registros CNAME/A creados para Vercel
4. **Restaurar** registros anteriores (si existían) o dejar en blanco
5. Guardar cambios
6. Esperar propagación (TTL típico 300-3600s)

**Verificación:**
```bash
# Verificar que ya NO resuelve a Vercel
dig kin-platform-medical.com CNAME
dig kin-platform-medical.com A

# Debe responder NXDOMAIN o IP anterior
```

---

### 6. Rollback Completo (Todo el Commit 0)

**Orden de Rollback Completo (Orden Inverso):**

```bash
# 1. Frontend Medical - Rollback Vercel (si deploy falló)
vercel promote <deployment-anterior> --scope=<team> --token=$VERCEL_TOKEN

# 2. Variables de entorno Vercel (Medical) - restaurar valores previos
# En Vercel Dashboard > Settings > Environment Variables
# Restaurar valores previos o eliminar variables añadidas

# 3. Variables Vercel (Empresas) - restaurar si se modificaron
# (Solo si se modificaron en Commit 0)

# 4. Rollback DNS
# Eliminar registros CNAME/A para kin-platform-medical.com
# Restaurar registros anteriores si existían

# 5. Variables Render (Backend) - si se modificaron CORS
# En Render Dashboard > kin-backend > Environment
# Restaurar CORS_ALLOWED_ORIGINS anterior

# 6. Verificar que kin-platform.com sigue funcionando
curl -I https://kin-platform.com
```

---

### 6. Verificación Post-Rollback

```bash
# 1. Verificar kin-platform.com sigue funcionando
curl -I https://kin-platform.com
# Debe responder 200/307

# 2. Verificar que kin-platform-medical.com NO responde (o 404)
curl -I https://kin-platform-medical.com
# Debe dar error DNS o 404

# 3. Verificar backend health
curl https://kin-backend-lwmy.onrender.com/api/v1/actuator/health
# {"status":"UP"}

# 4. Verificar frontend Empresas
curl -I https://kin-platform.com

# 5. Verificar acceso vertical en backend (después rollback)
# Verificar que kin-platform.com tiene acceso a Empresas
curl -H "Authorization: Bearer <token>" \
     https://kin-backend-lwmy.onrender.com/api/v1/auth/me \
     -o /tmp/user_me.json && jq '.verticalAccess' /tmp/user_me.json
# Debe devolver ["empresas"] o ["empresas","medical"]

# Verificar que kin-platform-medical.com NO tiene acceso (o rollback correcto)
# Si el rollback fue correcto, el dominio medical no debería responder o deberías
# obtener un error 401/404 al intentar acceder
curl -H "Authorization: Bearer <token>" \
     https://kin-backend-lwmy.onrender.com/api/v1/auth/me \
     -o /tmp/user_me_medical.json && jq '.verticalAccess' /tmp/user_me_medical.json
# Debe devolver ["medical"] (si usuario es médico) o array vacío/[] (si rollback correcto)

---

## Checklist de Rollback Completo

| Paso | Acción | Verificación |
|------|--------|--------------|
| 1 | Rollback Vercel Medical | `curl -I https://kin-platform-medical.com` → 404/DNS error |
| 2 | Restaurar vars Vercel Empresas | `vercel env ls kin-frontend` - valores originales |
| 3 | Rollback DNS | `dig kin-platform-medical.com` → NXDOMAIN o IP anterior |
| 4 | Verificar CORS backend | `curl -H "Origin: https://kin-platform.com" -X OPTIONS ...` |
| 5 | Healthcheck backend | `curl https://kin-backend-lwmy.onrender.com/api/v1/actuator/health` → UP |
| 6 | Frontend Empresas OK | `curl -I https://kin-platform.com` | 200/307 |
| 7 | Verificar verticals backend | `jq '.verticalAccess' /tmp/user_me.json` → ["empresas"] o ["empresas","medical"] |

---

### Contactos de Emergencia

| Rol | Contacto | Canal |
|-------|----------|-------|
| DevOps Lead | juan.cortes@kin-platform.com | Slack/Email |
| Backend Lead | maria.rojas@kin-platform.com | Slack/Email |
| Frontend Lead | carlitos.garcia@kin-platform.com | Slack/Email |
| DNS Admin | Cloudflare | Panel DNS / Soporte |
| Vercel Support | support@vercel.com | Email/Chat |
| Render Support | support@render.com | Email/Chat |

---

**Última actualización:** 2026-09-10
**Versión:** 1.0
**Autor:** Senior DevOps Engineer