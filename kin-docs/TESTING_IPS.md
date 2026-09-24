# TESTING IPS — Solicitud de Acceso Beta (clínicas/hospitales)

## Alcance
Formulario público de captura de leads IPS: `POST /api/v1/institutional/inquiries`
(persiste en `institutional_inquiries`, auto-respuesta al solicitante y
notificación interna a `contacto@kin-platform.com`).

## Rate limiting (TAREA 1)
El endpoint público está protegido por `RateLimitingFilter` con límite
**5 solicitudes / minuto por IP** (`RateLimitProperties.defaultLimits()`).

| Aspecto | Valor |
|---|---|
| Prefijo | `/institutional/inquiries` |
| Máximo | 5 / ventana |
| Ventana | 60 s |
| Respuesta al exceder | HTTP **429** + `code=RATE_LIMITED` + `Retry-After` |
| Whitelist | `RATE_LIMIT_WHITELIST` (IPs exentas) |

### Test automatizado
`src/test/java/com/kinplatform/common/security/RateLimitingFilterTest.java`

- `institutionalInquiries_dentroDelLimite_deberiaPasar` → 5 requests pasan (sin 429).
- `institutionalInquiries_excedido_deberiaDevolver429` → 6ª request → 429.

Ejecutar:
```powershell
.\mvnw test -Dtest=RateLimitingFilterTest
```

## Flujo funcional (E2E)
1. Frontend `/clinicas/beta` → `POST ${NEXT_PUBLIC_API_URL}/institutional/inquiries`.
2. Backend valida (`@NotBlank`/`@Email`), guarda (status `PENDING`) y envía
   auto-respuesta + notificación interna (fallos de email no pierden el lead).
3. Verificación en BD:
   ```sql
   SELECT id, ips_name, email, status, created_at
   FROM institutional_inquiries ORDER BY created_at DESC LIMIT 5;
   ```

## Evidencia de producción (2026-09-23)
```
POST /api/v1/institutional/inquiries -> 201
{"id":"fdcc5747-...","status":"PENDING","message":"Hemos recibido tu solicitud. Te contactaremos en 48h para agendar una demo."}

DB: IPS Test | test@example.com | PENDING | 2026-09-23 20:06:57
```
Auto-respuesta confirmada por el Product Owner.

## Pendientes de prueba
- Usar un email REAL (no `example.com`) para verificar visualmente el correo.
- Prueba de carga >100 requests para confirmar 429 en producción.
