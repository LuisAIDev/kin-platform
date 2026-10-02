# Módulo Licensing — KIN On-Premise

Sistema de licenciamiento firmado con RSA para despliegues On-Premise.

## Estado

- 12/12 tests pasan (unitarios + integración + ciclo persistente + negativos)
- Endpoint REST verificado con MockMvc
- Contexto Spring carga sin errores

## Propósito

KIN se instala en infraestructura del cliente (IPS/clínica) y solo funciona
si tiene licencia válida firmada por KIN.

## Variables de entorno

| Variable | Descripción | Default | Requerida en prod |
|----------|-------------|---------|-------------------|
| `KIN_LICENSE_PATH` | Ruta al archivo license.key | `/opt/kin/config/license.key` | ✅ |
| `KIN_PRIVATE_KEY_PATH` | Ruta a la clave privada RSA | `config/license-keys/kin-private.pem` | ✅ (solo admin) |
| `KIN_LICENSE_FAIL_FAST` | Bloquear KIN sin licencia | `false` | ✅ Debe ser `true` |

**IMPORTANTE:** activar `KIN_LICENSE_FAIL_FAST=true` en producción.

## Endpoints REST

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| POST | `/admin/licensing/generate` | ADMIN | Genera licencia |
| GET | `/admin/licensing/status` | ADMIN, IPS_ADMIN | Estado actual |
| POST | `/admin/licensing/reload` | ADMIN | Recarga desde disco |

## Flujo de generación

1. PO obtiene hash del servidor de la IPS
2. PO llama `POST /admin/licensing/generate`
3. KIN devuelve `.key` en el body HTTP (NO persiste a disco)
4. PO guarda el archivo manualmente en `KIN_LICENSE_PATH` de la IPS
5. IPS reinicia KIN → valida licencia al arrancar

## Módulos disponibles

HCE, TRIAJE, OCR, TELEMEDICINA, AGENDA, RIPS

## Tests

    cd kin-backend
    .\mvnw.cmd test -Dtest="LicenseSignerTest,LicenseIntegrationTest,LicenseAdminControllerTest,LicenseLifecycleTest"

Requiere Docker corriendo (Testcontainers → PostgreSQL).

## Limitaciones conocidas

Ver `kin-docs/TECH_DEBT.md` (TD-LIC-1 a TD-LIC-5).

---

Última actualización: 2026-10-01
Estado: Módulo operativo (12/12 tests)