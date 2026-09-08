# ADR-041: Centro de Documentos Clínicos del Paciente y Verificación OMS

## Estado
**Aceptado** — 2026-09-08

## Contexto

El paciente no disponía de gestión documental propia ni de análisis con IA:
la subida solo existía del lado del médico (ADR-036), la página "Documentos"
del paciente era de solo lectura y el módulo IA (ADR-037) operaba sobre
metadatos, no sobre el contenido de los PDFs.

Se porta al dominio de salud el modelo de Empresa ("Agregar documento",
"Importar información", "Descargar PDF" y chat conversacional contextual) y se
añade una **verificación clínica con fuentes oficiales de la OMS** (ICD-API y
GHO) para que la IA no invente códigos CIE ni cifras.

Principios aplicados:
- **Java decide. El LLM únicamente comunica.** (intacto): el ownership se
  valida en backend; el LLM solo explica.
- **Verificación antes de explicar**: cada turno consulta la ICD-API (si está
  configurada) antes de que el modelo genere la respuesta; si la fuente oficial
  no está disponible, la IA lo declara y no inventa datos.
- **Resiliencia**: llamadas externas detrás de un Circuit Breaker (la caída de
  la OMS nunca tumba el chat).
- **Aditividad**: BC nuevo (`kin.health.verification`) + columnas/tabla nueva
  (V42) + endpoint nuevo (`POST /my/upload`); contratos existentes intactos.
- **Auditoría**: cada análisis queda registrado (`AI_ANALYZE_DOCUMENT`).

## Decisión

### Bloque A — Centro de Documentos Clínicos del paciente

1. `/dashboard/patient/documents` deja de ser de solo lectura:
   - **Agregar documento**: `POST /health/documents/my/upload` (multipart). El
     paciente queda como `uploadedBy` y `patient_id` **siempre igual a su
     propio userId**; nunca se acepta un `patientId` ajeno en esta ruta.
   - El texto del PDF se extrae al subir (PDFBox/POI, tolerante a escaneos) y
     se cachea en `clinical_documents.extracted_text` (nunca se expone por API:
     el DTO solo devuelve `analyzable`).
   - **Importar información**: extrae automáticamente los datos relevantes del
     PDF (valores + unidades) y los presenta en la conversación de IA.
   - **Chat IA contextual** por documento: `GET/POST/DELETE
     /health/documents/{documentId}/chat/messages`. La conversación persiste en
     `document_chat_messages` (scope por documento, FK `CASCADE`).
   - **Descargar PDF**: cliente (jsPDF) con resumen del documento + la
     interpretación de la IA + descargo de responsabilidad.
2. En "Mi Salud" se añade un acceso rápido al Centro.

### Bloque B — Verificación clínica con la OMS (ICD-API + GHO)

Nuevo BC `com.kinplatform.kin.health.verification`:

| Pieza | Archivo | Rol |
|-------|---------|-----|
| Propiedades | `WhoVerificationProperties` | `kin.health.verification.*` (icd/gho/breaker) |
| Fachada | `WhoVerificationService` | consulta ICD antes del turno; nunca lanza |
| Puertos | `IcdDiagnosisLookup`, `GlobalHealthStatProvider` | búsqueda CIE y estadística GHO |
| Adaptadores | `IcdApiClient`, `GhoApiClient` | OAuth2 (ICD) y JSON (GHO) |
| Resiliencia | `resilience.CircuitBreaker` | CLOSED → OPEN → HALF_OPEN |

`DocumentChatService` inyecta el resultado de la verificación en el prompt:
- `VERIFIED` → la IA puede citar solo los códigos listados.
- `UNVERIFIED / UNCONFIGURED / UNAVAILABLE` → la IA queda instruida para **no
  inventar códigos CIE-10/11 ni cifras** y para decirlo explícitamente.

## Credenciales y configuración de la OMS

### ICD-API (requiere OAuth2 Client Credentials)
1. Registrarse en https://icd.who.int/icdapi.
2. **View API access key** → obtener `client id` y `client secret`.
3. Configurar por entorno:

```
WHO_ICD_ENABLED=true
WHO_ICD_CLIENT_ID=<client id>
WHO_ICD_CLIENT_SECRET=<client secret>
WHO_ICD_TOKEN_ENDPOINT=https://icdaccessmanagement.who.int/connect/token
WHO_ICD_BASE_URL=https://id.who.int
WHO_ICD_SCOPE=icdapi_access
```

Opcional: `WHO_ICD_RELEASE_ID` (release CIE-11 MMS, p. ej. `2024-01`),
`WHO_ICD_API_VERSION` (header `API-Version`, default `v2`), `WHO_ICD_MAX_RESULTS`,
`WHO_ICD_MAX_QUERIES_PER_TURN`.

### GHO (no requiere token)
```
WHO_GHO_ENABLED=true
WHO_GHO_BASE_URL=https://apps.who.int/gho/athena/api/GHO
```
Indicadores automáticos (opcional, lista vacía por defecto):
```yaml
kin.health.verification.gho.indicators:
  - indicatorCode: NCD_BMI_30A   # ejemplo (usar códigos reales del catálogo GHO)
    label: Obesidad (IMC ≥ 30)
    countryCode: ""              # vacío = dimensión GLOBAL
```

### Circuit Breaker
```
WHO_CIRCUIT_FAILURE_THRESHOLD=3
WHO_CIRCUIT_OPEN_TIMEOUT_MILLIS=30000
```

### API REST nueva

| Método | Endpoint | Rol | Descripción |
|--------|----------|-----|-------------|
| `POST` | `/health/documents/my/upload` | PATIENT, ADMIN | Subida propia (multipart) |
| `GET` | `/health/documents/{id}/chat/messages` | owner | Historial de la conversación |
| `POST` | `/health/documents/{id}/chat/messages` | owner | Envía turno (verifica OMS antes de responder) |
| `DELETE` | `/health/documents/{id}/chat/messages` | owner | Limpia la conversación |

### Migración Flyway V42
- `ALTER TABLE clinical_documents ADD COLUMN extracted_text TEXT`
- Tabla `document_chat_messages (id, document_id FK CASCADE, user_id FK, role,
  content, created_at)` + índice `(document_id, created_at)`.

## Seguridad (ownership)
- Un paciente solo accede a documentos con `patientId == su userId` (o el
  médico asociado/uploader/ADMIN). Validación **en backend**
  (`DocumentService.requireAccessibleDocument`) en subida, descarga, borrado,
  historial y chat.
- El texto extraído **no** viaja al cliente; el prompt del modelo se arma en
  servidor con el contenido del documento **solo** del documento validado.
- Auditoría (`AI_ANALYZE_DOCUMENT`) con `resourceId = documentId` y
  `patientId` de contexto.

## Alternativas consideradas
| Opción | Decisión |
|--------|----------|
| Streaming SSE tipo chat de Empresa | ❌ Bloqueante JSON (patrón AIAssist de salud); UI + tests más simples |
| RAG/embeddings para documentos | ❌ No necesario: el texto del documento (acotado a 60k chars) cabe en el contexto del proveedor |
| OCR (Tesseract) para PDFs escaneados | ❌ **BLOCKED**: no hay OCR en el stack; los escaneos se detectan y se informa al paciente |
| OpenFDA | ❌ Fuera de alcance; no requiere token pero no aporta la verificación CIE |

## Consecuencias
**Positivas**: paciente autónomo para guardar y entender sus exámenes; IA
verificada contra fuentes oficiales; degradación elegante sin credenciales OMS.
**Negativas**: la ICD-API requiere alta manual y release mantenible; el
parser de hallazgos es heurístico (la verificación real mejora cuando se
estructura el texto); sin OCR los PDFs escaneados no son analizables.

## Referencias
- Migración `V42__create_document_chat_and_extracted_text.sql`
- `kin-docs/ADR-036_CLINICAL_DOCUMENTS.md`
- `kin-docs/ADR-037_AI_ASSIST.md`
- `kin-docs/ADR-040_PATIENT_CAPABILITY_DECOUPLING.md`
- Documentación oficial ICD-API v2: https://icd.who.int/icdapi
