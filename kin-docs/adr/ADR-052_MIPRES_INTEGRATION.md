# ADR-052: Integración MIPRES con MinSalud (Resolución 740/2024)

## Estado
Propuesto

## Contexto

La Resolución 740 de 2024 del Ministerio de Salud y Protección Social establece la obligatoriedad de reportar a través de servicios REST (MIPRES No PBSUPC) las prescripciones y suministros de tecnologías en salud **no financiadas con recursos de la UPC**:

- Medicamentos
- Procedimientos
- Dispositivos Médicos
- Productos de Soporte Nutricional
- Servicios Complementarios

Actualmente KIN tiene:
- Interface `MipresClient` con métodos básicos
- `MipresStubClient` para testing (perfil `!prod`)
- `MipresHttpClient` placeholder (perfil `prod`) que lanza `UnsupportedOperationException`

**Gap:** Falta implementación real del cliente HTTP contra los endpoints oficiales de MinSalud (WSMIPRESNOPBS).

---

## Decisión

Implementar cliente MIPRES productivo usando **Spring WebClient** con las siguientes características:

### 1. Cliente HTTP
- **WebClient** (Spring WebFlux) - reactivo, no bloqueante
- Configuración por ambiente: `application-{prod,test}.yml`
- Base URLs configurables:
  - Testing: `https://tablas.sispro.gov.co/wsmipresnopbs/`
  - Prod: `https://wsmipres.sispro.gov.co/WSMIPRESNOPBS/`

### 2. Autenticación (Token Management)
- **GenerarToken** al arranque y cada 23h (token dura 24h)
- Cache en memoria (`ConcurrentHashMap`) con TTL
- Renovación automática background job (`@Scheduled`)
- Credenciales en variables de entorno (NIT, PIN Base64)

### 3. DTOs y Serialización
- Records inmutables para Request/Response
- Jackson para JSON (camelCase ↔ snake_case según API)
- Validación Bean Validation (`@NotNull`, `@Size`, etc.)

### 4. Resiliencia
- **Retry exponencial** (3 intentos, 2s base) para 5xx, 429
- **Timeouts**: connect 5s, read 30s
- **Circuit Breaker** (Resilience4j) opcional futuro

### 3. Modelo de Datos (Nuevas Tablas - Migración V74)

```sql
-- Prescripciones MIPRES
CREATE TABLE mipres_prescriptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    contract_id UUID,
    patient_id UUID,
    prescription_number VARCHAR(20) NOT NULL UNIQUE,
    nit VARCHAR(20) NOT NULL,
    prescription_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, AUTHORIZED, REJECTED, EXPIRED
    cups_code VARCHAR(20),
    diagnosis_cie10 VARCHAR(10),
    qty_approved INT,
    unit_price_cop NUMERIC(15,2),
    raw_response JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Suministros reportados
CREATE TABLE mipres_supplies (
    id UUID PRIMARY KEY,
    prescription_id UUID REFERENCES mipres_prescriptions(id),
    organization_id UUID NOT NULL,
    supply_id VARCHAR(50) UNIQUE, -- ID retornado por MinSalud
    prescription_number VARCHAR(20) NOT NULL,
    supply_date DATE NOT NULL,
    cups_code VARCHAR(20),
    quantity INT,
    unit_value_cop NUMERIC(15,2),
    total_value_cop NUMERIC(15,2),
    batch_number VARCHAR(50),
    expiration_date DATE,
    status VARCHAR(20), -- REPORTED, ANULLED, PENDING
    raw_request JSONB,
    raw_response JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Token cache (opcional, si se prefiere BD sobre memoria)
CREATE TABLE mipres_tokens (
    nit VARCHAR(20) PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### 5. Endpoints REST Nuevos

| Método | Path | Descripción | Roles |
|--------|------|-------------|-------|
| POST | `/billing/mipres/prescriptions` | Crear/consultar prescripción | IPS_FACTURADOR, IPS_ADMIN, PHYSICIAN |
| GET | `/billing/mipres/prescriptions/{id}` | Detalle prescripción | IPS_FACTURADOR, IPS_ADMIN, PHYSICIAN |
| GET | `/billing/mipres/prescriptions` | Listar con filtros | IPS_FACTURADOR, IPS_ADMIN |
| POST | `/billing/mipres/supplies` | Reportar suministro | IPS_FACTURADOR, IPS_ADMIN |
| PUT | `/billing/mipres/supplies/{id}/anular` | Anular suministro | IPS_FACTURADOR, IPS_ADMIN |
| GET | `/billing/mipres/supplies` | Listar suministros | IPS_FACTURADOR, IPS_ADMIN |
| GET | `/billing/mipres/reports` | Reportes consolidados | IPS_ADMIN, IPS_FACTURADOR |

### 6. Seguridad
- `@PreAuthorize("hasAnyRole('IPS_ADMIN', 'IPS_FACTURADOR', 'PHYSICIAN', 'ADMIN')")`
- Validación de `TenantContext` (organization_id)
- Auditoría con `AuditService` (acción: `MIPRES_PRESCRIPTION_CREATE`, `MIPRES_SUPPLY_REPORT`)

### 7. Outbox Pattern
- Eventos de dominio: `MIPRES_PRESCRIPTION_CREATED`, `MIPRES_SUPPLY_REPORTED`
- Publicación via `OutboxEventPublisher` (ya existente)

---

## Consecuencias

### Positivas
- Cumplimiento normativo Resolución 740/2024
- Diferenciador comercial para IPS/clínicas
- Trazabilidad completa prescripción → suministro
- Integración nativa con módulo Authorization existente
- Arquitectura reactiva escalable

### Negativas
- Dependencia de credenciales MinSalud (bloqueante externo)
- Complejidad de esquema JSON suministro (anexo técnico extenso)
- Mantenimiento de catálogos SISPRO (actualización semestral)
- Testing requiere sandbox + credenciales reales

---

## Alternativas Consideradas

| Alternativa | Por qué NO |
|-------------|------------|
| Solo stub/producción manual | No cumple requisito normativo automático |
| Librería externa MIPRES | No existe librería oficial Java |
| Batch nocturno batch | Requisito es tiempo real (suministro inmediato) |

---

## Referencias
- Resolución 740/2024 MinSalud
- Anexo Técnico Suministros v1.0 JSON v3.9
- Manual Web Services MIPRES v3.1
- Swagger Testing: `https://tablas.sispro.gov.co/wsmipresnopbs/help`

---

## Próximos Pasos
1. **PO**: Proveer credenciales testing (NIT, PIN Base64, acceso miSeguridadSocial)
2. **Dev**: Crear migración V74 + implementar `MipresHttpClient` real
3. **QA**: Tests contra sandbox con credenciales reales
4. **Deploy**: Configurar variables entorno prod (NIT, PIN Base64)

---

*Fecha: 2026-09-24 | Autor: Backend Team | Próxima revisión: Sprint Planning*