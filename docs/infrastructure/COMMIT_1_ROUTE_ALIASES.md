# Commit 1 - Alias de Rutas en Backend

## Resumen

Este documento describe los alias de rutas añadidos al backend para soportar dos productos (KIN Empresas y KIN Medical) con prefijos de rutas claros, manteniendo compatibilidad hacia atrás.

## Objetivo

Añadir alias de rutas `@RequestMapping` con múltiples paths a controladores existentes, de modo que cada endpoint quede accesible desde ambos prefijos:

- **Nuevo**: `/api/v1/empresas/...` (KIN Empresas)
- **Nuevo**: `/api/v1/medical/health/...` (KIN Medical)
- **Legacy**: `/api/v1/...` (mantener compatibilidad)

## Cambios Implementados

### 1. Controladores Empresas (prefix: `/empresas/`)

| Controlador | Ruta Legacy | Ruta Nueva (Alias) | Endpoints Afectados |
|-------------|-------------|--------------------|---------------------|
| `EnterpriseController` | `/api/v1/enterprise/...` | `/api/v1/empresas/enterprise/...` | Todas las rutas enterprise |
| `EnterpriseProgressController` | `/api/v1/enterprise/...` | `/api/v1/empresas/enterprise/...` | SSE progress, streaming |
| `EnterpriseDashboardController` | `/api/v1/enterprise/...` | `/api/v1/empresas/enterprise/...` | Dashboard data |
| `EnterpriseInformationController` | `/api/v1/enterprise/...` | `/api/v1/empresas/enterprise/...` | Información del proyecto |
| `ProjectController` | `/api/v1/projects/...` | `/api/v1/empresas/projects/...` | CRUD proyectos |
| `CategoryController` | `/api/v1/categories` | `/api/v1/empresas/categories` | Listado categorías |

### 2. Controladores Medical (prefix: `/medical/health/`)

| Controlador | Ruta Legacy | Ruta Nueva (Alias) | Endpoints Afectados |
|-------------|-------------|--------------------|---------------------|
| `TelemedicineController` | `/api/v1/health/telemedicine/...` | `/api/v1/medical/health/telemedicine/...` | Citas, mensajes, consultas |
| `DocumentController` | `/api/v1/health/documents/...` | `/api/v1/medical/health/documents/...` | Subida, lista, descarga de documentos |
| `AuditPatientController` | `/api/v1/health/audit/...` | `/api/v1/medical/health/audit/...` | Logs de auditoría del paciente |
| `AuditAdminController` | `/api/v1/admin/health/audit/...` | `/api/v1/medical/admin/health/audit/...` | Logs de auditoría administrador |
| `DashboardController` | `/api/v1/health/dashboard/...` | `/api/v1/medical/health/dashboard/...` | Resumen, historial, perfil |
| `SchedulingController` | `/api/v1/health/scheduling/...` | `/api/v1/medical/health/scheduling/...` | Disponibilidad, citas |
| `DifferentialController` | `/api/v1/health/differential/...` | `/api/v1/medical/health/differential/...` | Diagnóstico diferencial |

### 3. Rutas de Mantenimiento (compatibilidad backward, sin cambios)

| Controlador | Ruta | Estado |
|-------------|------|--------|
| `AuthController` | `/api/v1/auth/...` | Mantiene compatibilidad total |
| `SubscriptionController` | `/api/v1/subscriptions/...` | Mantiene compatibilidad total |
| `PricingPlanController` | `/api/v1/pricing-plans/...` | Mantiene compatibilidad total |
| `Stripe Controllers` | `/api/v1/stripe/...` | Mantiene compatibilidad total |

## Patrones de Mapeo

### Pattern 1: Single Class-Level Alias

```java
@RestController
@RequestMapping({
    "/enterprise",           // legacy: /api/v1/enterprise/...
    "/empresas/enterprise"   // new:    /api/v1/empresas/enterprise/...
})
public class EnterpriseController { ... }
```

### Pattern 2: Multiple Context Paths

```java
@RestController
@RequestMapping({
    "/projects",             // legacy: /api/v1/projects/...
    "/empresas/projects"     // new:    /api/v1/empresas/projects/...
})
public class ProjectController { ... }
```

### Pattern 3: Category-Level Alias

```java
@RestController
@RequestMapping({
    "/categories",           // legacy
    "/empresas/categories"   // new
})
public class CategoryController { ... }
```

## Verificación de Endpoints

### Verificar Empresas

```bash
# Ruta legacy (debe funcionar)
curl -H "Authorization: Bearer $TOKEN" \
     https://kin-backend-lwmy.onrender.com/api/v1/enterprise/{projectId}

# Ruta nueva (debe funcionar después de Commit 1)
curl -H "Authorization: Bearer $TOKEN" \
     https://kin-backend-lwmy.onrender.com/api/v1/empresas/enterprise/{projectId}
```

### Verificar Medical

```bash
# Ruta legacy
curl -H "Authorization: Bearer $TOKEN" \
     https://kin-backend-lwmy.onrender.com/api/v1/health/telemedicine/{id}

# Ruta nueva
curl -H "Authorization: Bearer $TOKEN" \
     https://kin-backend-lwmy.onrender.com/api/v1/medical/health/telemedicine/{id}
```

### Verificar Backward Compatibility

```bash
# Asegurar que las rutas legacy siguen funcionando
curl -H "Authorization: Bearer $TOKEN" \
     https://kin-backend-lwmy.onrender.com/api/v1/enterprise/{projectId} \
     -o /tmp/legacy_enterprise.json

# Asegurar que las nuevas rutas también funcionan
curl -H "Authorization: Bearer $TOKEN" \
     https://kin-backend-lwmy.onrender.com/api/v1/empresas/enterprise/{projectId} \
     -o /tmp/new_enterprise.json

# Comparar respuestas (deben ser idénticas o similares)
diff /tmp/legacy_enterprise.json /tmp/new_enterprise.json
# Debería estar vacío o mostrar diferencias menores de formato
```

## Rollback Plan Commit 1

Si los alias de rutas causan problemas, se pueden revertir removiendo los paths nuevos de las anotaciones `@RequestMapping`:

```java
// De: @RequestMapping({"/enterprise", "/empresas/enterprise"})
// A: @RequestMapping("/enterprise")
```

Esto restauraría solo las rutas legacy y removía los nuevos prefijos.

## Verificación Post-Deploy

Después del despliegue, verificar:

1. **Healthchecks**: `GET /api/v1/actuator/health` → `{"status":"UP"}`
2. **Empresas routes**: Probar ambos prefijos acceden correctamente
3. **Medical routes**: Probar ambos prefijos acceden correctamente
4. **Auth routes**: `/api/v1/auth/me` sigue funcionando
5. **Stripe routes**: `/api/v1/stripe/create-checkout-session` sigue funcionando
6. **No 404 errors**: Ningún endpoint retorna 404 inesperado

## Dependencias

- **Backend**: kin-backend (spring-boot, spring MVC)
- **No breaking changes**: Todas las rutas legacy mantienen funcionalidad
- **Deploy**: Render auto-deploy con push a main

## Relación con otros Commitments

| Commit | Descripción | Estado |
|--------|-------------|--------|
| Commit 0 | Preparación infraestructura, DNS, dominios, docs | ✅ Completa |
| Commit 1 | Alias de rutas backend Empresas + Medical | ✅ Implementado |
| Commit 2 | [Próximo] | Por definir |

---

**Principio rector**: "Java decide. El LLM únicamente comunica."

**Fecha**: 2026-09-10
**Versión**: 1.0