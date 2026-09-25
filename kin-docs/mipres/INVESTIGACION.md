# Investigación MIPRES - MinSalud (Resolución 740 de 2024)

## Resumen Ejecutivo

MIPRES (Modulo de Prescripción de Tecnologías en Salud) es el aplicativo del Ministerio de Salud y Protección Social de Colombia para el reporte de prescripciones y suministros de tecnologías en salud **no financiadas con recursos de la UPC** (Medicamentos, Procedimientos, Dispositivos Médicos, Productos de Soporte Nutricional, Servicios Complementarios) — **MIPRES No PBSUPC**.

Normativa base: **Resolución 740 de 2024** (y normas modificatorias).

---

## 1. URLs Base

| Ambiente | Base URL | Swagger UI |
|----------|----------|------------|
| **Capacitación/Testing** | `https://tablas.sispro.gov.co/wsmipresnopbs/` | `https://tablas.sispro.gov.co/wsmipresnopbs/help` |
| **Producción** | `https://wsmipres.sispro.gov.co/WSMIPRESNOPBS/` | `https://wsmipres.sispro.gov.co/WSMIPRESNOPBS/help` |

> **Importante:** El NIT se ingresa **sin dígito de verificación** en todas las URLs.

---

## 2. Autenticación

### 2.1 Generar Token
El token se genera con el endpoint `GenerarToken` usando credenciales de `miSeguridadSocial.gov.co`:

```
POST /api/GenerarToken/{nit}/{pin_base64}
```

- **NIT**: Sin dígito de verificación
- **PIN**: Enviado en Base64 (proporcionado al registrar la entidad en `www.miseguridadsocial.gov.co`)
- **Duración**: 24 horas
- **Respuesta**: Token JSON para usar en todos los endpoints subsiguientes

**Ejemplo:**
```
https://tablas.sispro.gov.co/wsmipresnopbs/api/GenerarToken/8901100100/M x2VNzJl1h4mlNkr9LR51UY7hFYOv7MnOXq6Id2XwMk%3D
```

---

## 3. Endpoints Principales - Prescripción (MIPRES No PBS)

### 3.1 Consultar Prescripciones por Fecha
```
GET /api/Prescripcion/{nit}/{fecha}/{token}
```
**Parámetros:** NIT (sin DV), Fecha (AAAA-MM-DD), Token
**Respuesta:** Lista de prescripciones para la fecha e institución.

### 3.2 Consultar Prescripción por Paciente
```
GET /api/PrescripcionPaciente/{nit}/{fecha}/{token}/{tipodoc}/{numdoc}
```
**Parámetros:** NIT, Fecha, Token, TipoDoc (CC, RC, TI, CE, PA, NV, CD, SC, PR), NumDoc
**Respuesta:** Prescripción(es) del paciente en la fecha.

### 3.3 Consultar Prescripción por Número
```
GET /api/PrescripcionXNumero/{nit}/{token}/{NoPresc}
```
**Parámetros:** NIT, Token, Número de Prescripción (20 caracteres)
**Respuesta:** Información completa del anexo técnico.

### 3.3 Consultar Novedades de Prescripción
```
GET /api/NovedadesPrescripcion/{nit}/{fecha}/{token}
```
**Parámetros:** NIT, Fecha, Token
**Respuesta:** Lista de novedades (modificaciones, anulaciones) en la fecha.

### 3.4 Consultar Múltiples Prescripciones (Batch)
```
POST /api/PrescripcionXMany/{nit}/{token}
```
**Body:** Array de números de prescripción
**Respuesta:** Información completa de múltiples prescripciones.

---

## 4. Endpoints Principales - Suministro (Reporte de Entrega)

### 4.1 Registrar Suministro (PUT)
```
PUT /api/Suministro/{nit}/{token}
```
**Headers:** Content-Type: application/json
**Body:** JSON con estructura completa de suministro (ver Anexo Técnico Suministros v1.0)
**Respuesta:** Confirmación con ID de suministro generado.

### 4.2 Anular Suministro (PUT)
```
PUT /api/AnularSuministro/{nit}/{token}
```
**Body:** JSON con ID de suministro y motivo
**Respuesta:** Confirmación de anulación.

### 4.3 Consultar Suministros por Fecha
```
GET /api/SuministroXFecha/{nit}/{token}/{fecha}
```
**Parámetros:** NIT, Token, Fecha (AAAA-MM-DD)

### 4.4 Consultar Suministro por Prescripción
```
GET /api/SuministroXPrescripcion/{nit}/{token}/{noPres}
```

### 4.5 Consultar Suministro por ID
```
GET /api/SuministroXIdSuministro/{nit}/{token}/{idsuministro}
```

### 4.5 Consultar Múltiples Suministros (Batch)
```
POST /api/SuministroXMany/{nit}/{token}
```

---

## 5. Estructura de Datos - Suministro (JSON)

El cuerpo del suministro (endpoint PUT `/Suministro`) requiere:

```json
{
  "nit": "8901100100",
  "token": "TOKEN_GENERADO",
  "suministro": {
    "idSuministro": "auto-generado",
    "numeroPrescripcion": "20241005126000000029",
    "direccionamiento": { ... },
    "programacion": { ... },
    "entrega": { ... },
    "reporteEntrega": { ... }
  }
}
```

Ver **Anexo Técnico Suministros v1.0** (Resolución 740/2024) para esquema completo:
- Direccionamiento (IPS, profesional, fechas, diagnóstico CIE-10)
- Programación (fecha, hora, profesional, sede)
- Entrega (responsable, observaciones)
- Reporte Entrega (cantidades, lote, vencimiento, CUPS, valor)

---

## 6. Códigos de Respuesta HTTP

| Código | Significado |
|--------|-------------|
| 200/201 | Éxito |
| 400 | Bad Request - datos errados o sintaxis incorrecta |
| 401 | No autorizado - token inválido/expirado |
| 404 | No encontrado - recurso no existe |
| 422 | Validación fallida - integridad referencial |
| 500 | Error interno del servidor |

---

## 7. Datos Maestros Requeridos (Catálogos SISPRO)

| Catálogo | Uso | Fuente |
|----------|-----|--------|
| `modalidadPago` | Modalidades de pago | SISPRO |
| `coberturaPlan` | Planes de beneficios | SISPRO |
| `conceptoRecaudo` | Conceptos de recaudo | SISPRO |
| `TipoIdPISIS` | Tipos de identificación | SISPRO |
| CUPS | Códigos de procedimientos/medicamentos | MinSalud |
| CIE-10 | Diagnósticos | OMS/MinSalud |
| VIAS_ADMINISTRACION | Vías de administración | MinSalud |
| UNIDADES_MEDIDA | Unidades de medida | MinSalud |

---

## 7. Flujo E2E Típico (IPS)

```
1. Médico prescribe en MIPRES Web → Genera N° Prescripción
2. IPS consulta prescripción → GET /PrescripcionXNumero
3. IPS programa entrega → PUT /Direccionamiento + PUT /Programacion
4. IPS entrega medicamento → PUT /Entrega
5. IPS reporta suministro final → PUT /Suministro (JSON completo)
6. MinSalud valida → Retorna ID Suministro
7. IPS consulta estado → GET /SuministroXPrescripcion
```

---

## 8. Documentos Oficiales de Referencia

| Documento | Enlace | Versión |
|-----------|--------|---------|
| Resolución 740/2024 | minsalud.gov.co | 30/04/2024 |
| Anexo Técnico Suministros | minsalud.gov.co | v1.0 JSON v3.9 |
| Documentación Web Services | minsalud.gov.co | v3.1 (Dic 2016) |
| Manual Usuario Prescripción | minsalud.gov.co | v1.1 (Ago 2024) |
| Swagger Testing | `tablas.sispro.gov.co/wsmipresnopbs/help` | - |

---

## 9. Credenciales y Acceso (Pendiente PO)

| Dato | Valor | Estado |
|------|-------|--------|
| NIT IPS (sin DV) | `PENDIENTE` | ❌ Requiere PO |
| PIN miSeguridadSocial | `PENDIENTE` | ❌ Requiere PO |
| Token de testing | `PENDIENTE` | ❌ Requiere PO |
| Acceso `miseguridadsocial.gov.co` | `PENDIENTE` | ❌ Requiere PO |

---

## 10. Riesgos y Bloqueantes Identificados

| Riesgo | Impacto | Mitigación |
|--------|---------|------------|
| Sin credenciales de testing | No se puede probar sandbox | PO debe proveer NIT+PIN |
| Token expira 24h | Requiere renovación automática | Implementar cache + refresh |
| Sin certificado X.509 para prod | Bloquea producción | PO debe gestionar Certicámara |
| Cambios en API MinSalud | Ruptura integración | Monitorear `help` endpoint |
| Rate limiting desconocido | Posible 429 | Implementar backoff exponencial |

---

## 11. Próximos Pasos Inmediatos

1. **PO proveer**: NIT IPS (sin DV), PIN miSeguridadSocial, acceso a `miSeguridadSocial.gov.co`
2. **Desarrollador**: Probar `GenerarToken` en sandbox `https://tablas.sispro.gov.co/wsmipresnopbs/`
2. **Desarrollador**: Explorar Swagger UI en `https://tablas.sispro.gov.co/wsmipresnopbs/help`
3. **Arquitectura**: Definir ADR-052 con decisión de diseño (WebClient, DTOs, retry policy)
4. **BD**: Crear migración V74 con tablas MIPRES

---

*Documento generado: 2026-09-24 | Investigación basada en documentación oficial MinSalud (Resolución 740/2024, Anexo Técnico Suministros v1.0, Manual Web Services v3.1)*