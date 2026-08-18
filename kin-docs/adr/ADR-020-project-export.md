# ADR-020: Módulo kin.export — exportación estructurada del proyecto (DOCX/PDF/Markdown) con plantillas de referencia y acción desde el chat

**Estado**: **Propuesto** (pendiente de revisión y aprobación)
**Fecha**: 2026-08-18
**Autor**: KIN Architecture Team

> **Alcance**: este ADR propone y congela (una vez aprobada) la arquitectura del módulo
> **`com.kinplatform.kin.export`**: exportación del proyecto real de KIN como documento profesional
> en **DOCX (Apache POI), PDF (OpenPDF) y Markdown**, con modos **complete/summary**, inyección
> determinista de **plantillas de referencia** (`templateDocumentId`) y una **acción de descarga**
> aditiva desde el chat. La fuente principal del documento es **Project + ConsultingReport +
> project_info**; el historial del chat NO es fuente de contenido. La implementación es
> **aditiva** (patrón ADR-011/014/015) y mantiene el principio rector del proyecto:
> **Java decide. El LLM únicamente comunica.**

---

## 1. Contexto y Problema

El botón legacy **"Descargar Reporte PDF"** (`PdfReportButton.tsx`) genera el PDF en el navegador
(jsPDF) usando `project` + **`messages`** (historial de chat) como cuerpo principal. Esto produce:

| # | Problema | Evidencia |
|---|----------|-----------|
| 1 | **Historial incompleto**: el cuerpo del PDF es la ventana de mensajes cargada (`HistoryWindow`, límite de 20), no el proyecto. | `PdfReportButton` itera `messages`; el backend recorta la ventana |
| 2 | **Datos reales ausentes**: el `ConsultingReport` (10 secciones) y `project_info` nunca se usan en la descarga. | Sin referencias a `ReportRepository`/`project_info` en el botón |
| 3 | **Fricción de usuario**: el usuario termina copiando la respuesta del chat y pegándola en Word. | Flujo copiar → pegar → Word |
| 4 | **Sin estructura reutilizable**: no existe exportación en DOCX/Markdown ni plantillas de referencia. | Solo jsPDF client-side |

Se requiere una funcionalidad independiente, desacoplada del historial, que permita descargar el
proyecto real en formatos profesionales, con estructura opcional derivada de un **documento de
referencia** (estructura sí, contenido no), sin inventar información y sin romper los contratos
existentes del dominio central ni del chat.

---

## 2. Decisión Arquitectónica

Se introduce un **bounded context de dominio `com.kinplatform.kin.export`** (POJO puro en el núcleo,
sin Spring/JPA/IA en el modelo, renderizadores y parsers). La arquitectura separa estrictamente
tres planos:

```text
DATOS        → Project + ConsultingReport (SectionFormatter) + project_info → ExportInput
ESTRUCTURA   → ExportTemplate / ProjectExportTemplateParser / ProjectExportTemplateMapper
FORMATO      → ExportRenderer (DOCX·POI, PDF·OpenPDF, Markdown) sobre el modelo neutral
```

### 2.1 Modelo neutral (formato independiente)

`ExportDocument` (título, subtítulo, fecha, secciones) → `ExportSection` → `ExportBlock`
(tipos: `TITLE/SUBTITLE/PARAGRAPH/HIGHLIGHT/BOLD/ITALIC/LIST/TABLE/PAGE_BREAK`) + `ExportTable`.
Los renderizadores consumen SOLO este modelo; la lógica de contenido no conoce DOCX/PDF.

### 2.2 Datos (fuente principal, nunca el historial)

| Fuente | Origen | Uso |
|--------|--------|-----|
| `Project` | `ProjectRepository` | título, descripción, categoría, estado, `viabilityScore`, `aiSummary`, fechas |
| `ConsultingReport` | `ReportRepository.findLatest` | 10 secciones vía `SectionFormatter` existentes (sin duplicar lógica de formato) |
| `project_info` | `ProjectStructuredInfoRepository` | pares section/key/value + `sourceType` → tablas |

`ProjectExportAssembler` (dominio puro) construye `ExportDocument` para `COMPLETE`/`SUMMARY` y
expone helpers (`identificationBlocks`, `blocksFor`) reutilizados por el modo plantilla.

### 2.3 Estructura (plantilla de referencia)

- **`ProjectExportTemplateParser`** (determinista): recibe `filename/mimeType/extractedText` y
  produce `ExportTemplate` (título + `TemplateSection` ordenadas con nivel y *hints* de lista/tabla).
  Detecta títulos, secciones numeradas (`1.`, `1.1`), palabras clave (`CAPÍTULO/SECCIÓN/PARTE`),
  listas y tablas consistentes. **Nunca devuelve el contenido del documento como contenido del
  proyecto**; tablas no reconstruibles se degradan (no se fabrican filas/columnas).
- **`ProjectExportTemplateMapper`** (dominio puro): mapea cada `TemplateSection` → `ExportSection`
  con datos reales de KIN (clasificación por keywords normalizadas; sin coincidencia exacta frágil).
  Sección sin datos equivalentes → bloque `HIGHLIGHT "Pendiente de información"`. **Prohibido
  inventar**: objetivos/cronogramas/presupuestos solo si existen en KIN.
- `ExportMode.TEMPLATE` cuando se usa `templateDocumentId`; los modos `COMPLETE/SUMMARY` quedan
  intactos cuando no hay plantilla.

### 2.4 Formato (renderizadores)

- **DOCX** → Apache POI `XWPF` (dependencia ya presente `poi-ooxml`): portada, títulos, listas,
  tablas, negritas, saltos de página, pie de página. Documento Word real editable.
- **PDF** → **OpenPDF** (`com.github.librepdf:openpdf`, LGPL, puro Java, añadida y justificada):
  multipágina, títulos, tablas, portada y pie de página.
- **Markdown** → misma representación del modelo neutral (sin lógica divergente).
- `ExportRendererFactory` resuelve el renderer por `ExportFormat`.

### 2.5 Endpoints (frontera de aplicación)

```text
GET /projects/{projectId}/export                            → opciones (formatos, hasReport, secciones)
GET /projects/{projectId}/export/docx|pdf|markdown?mode=complete|summary
GET /projects/{projectId}/export/docx|pdf|markdown?templateDocumentId=<id>
```

Devuelven `byte[]` con `Content-Disposition: attachment`; el archivo se genera en memoria (sin
almacenamiento permanente). El nombre deriva del proyecto (`filenameBase` sanitizado), nunca del
documento de referencia.

### 2.6 Acción aditiva desde el chat

- **`ExportIntentDetector`** (heurístico determinista): reglas de palabras clave
  (`descargar/exportar` + `proyecto/documento`; formato por `word|pdf|markdown`; plantilla si
  menciona `plantilla/estructura/documento que subí`). Sin intención clara → `null` (el chat
  responde normal). No es un sistema de comandos.
- **`ExportChatIntentService`** (`@Component`): resuelve el documento `PROCESADO` más reciente del
  proyecto autenticado y adjunta id + nombre.
- **`ChatResponse.action`** (campo aditivo, `null` por defecto): se rellena en `processMessage`
  (no-stream) y en el evento SSE `done`/fallback del streaming, sin romper los contratos síncronos
  ni SSE existentes (compatibilidad total cuando `action = null`).
- Frontend `ExportChatActionCard` reutiliza `exportProjectService.download` + `downloadBlob`.

---

## 3. Fronteras de Seguridad y Contratos

### 3.1 Ownership estricto

- **Proyecto**: `ProjectExportServiceImpl.requireOwned(userId, projectId)` → 404 si el proyecto no
  existe o pertenece a otro usuario (sin filtrar existencia, patrón `ProjectReportController`).
- **Documento de referencia**: `ProjectDocumentService.findOwned(userId, projectId, documentId)`
  verifica ownership del proyecto y que el documento pertenezca a ese proyecto → 404 si es ajeno o
  inexistente. Un documento de otro usuario/proyecto **no se revela**.
- **Documento no utilizable** (`ERROR`, sin `extractedText`) → **400** con mensaje controlado.
- **No autenticado** → **401** (lo garantiza `SecurityConfig`: `anyRequest().authenticated()`).

### 3.2 Manejo centralizado de excepciones

`GlobalExceptionHandler` (capa común) garantiza códigos HTTP estables sin fugas de stack traces ni
secretos:

| Excepción | Código | Caso |
|-----------|--------|------|
| `ResponseStatusException` | preserva su status (404/400) | exportación: proyecto/documento/modo |
| `MethodArgumentTypeMismatchException` | 400 | `templateDocumentId`/UUID malformado |
| `IllegalArgumentException` | 400 | validaciones existentes |

Estos dos handlers evitan que dichas excepciones caigan en el manejador genérico de
`RuntimeException` (500). La política CORS permite los orígenes de producción (`kin-platform.com` y
`www.kin-platform.com`, verificado con preflight y descarga real).

### 3.3 Contratos no modificados

- `/chat` y `/chat/stream` siguen respondiendo igual; `action` es opcional (`null`).
- `PdfReportButton` (legacy), reportes, enterprise, historial, `project_info` y autenticación
  **no cambian**. La exportación convive con el botón legacy.
- El historial del chat **no** es fuente del documento exportado (solo complementa si una
  funcionalidad existente lo requiere; `kin.export` no lo referencia).

---

## 4. Consecuencias

### 4.1 Positivas

- **Mantenibilidad**: modelo neutral + renderizadores desacoplados; añadir un formato nuevo no toca
  el contenido.
- **Reutilización**: `SectionFormatter` (10 existentes) y la infraestructura de documentos
  (`project_documents`, extracción PDFBox/POI) se reutilizan, sin duplicar lógica.
- **Aditividad total**: `ChatResponse.action`, `ExportMode.TEMPLATE`, `findOwned` y handlers de
  excepciones son aditivos; ningún contrato congelado se modifica.
- **No fabricación**: secciones sin datos → omitidas o "Pendiente de información"; determinista.
- **Compatibilidad**: el botón legacy convive; `mode=complete|summary` sin `templateDocumentId`
  comporta exactamente como antes.

### 4.2 Limitaciones técnicas documentadas

| Limitación | Detalle |
|------------|---------|
| **Tablas en plantillas** | Solo se reconstruyen si el texto extraído conserva filas tabuladas/`|` con nº de columnas consistente; en caso contrario se degrada a texto (no se fabrican celdas) |
| **Documentos escaneados** | PDF sin capa de texto → `ERROR` en el upload → rechazado como plantilla (400); sin OCR |
| **`extractedText` truncado** | Máximo 1.000.000 caracteres (`MAX_EXTRACTED_CHARS`); la estructura se parsea del texto disponible |
| **Detector de intención** | Heurístico v1 (palabras clave); no es un sistema de comandos y puede no detectar parafraseos |
| **Mapeo de plantilla** | Determinista (sin IA); la IA solo podría decidir mapeo estructural, nunca contenido |

### 4.3 Estado de pruebas y despliegue

- Backend: `./mvnw clean test` → **2758 tests** (0 fallos, 0 errores); `mvn package` BUILD SUCCESS.
- Frontend: `npm test` → **279 tests** (52 archivos); `npm run lint` sin errores; `npm run build`
  ✓ Compiled successfully.
- Despliegue controlado verificado (solo lectura en smoke): backend `kin-backend` **live** en
  Render (commits `0120d15`, `328c6da`, `88367a7`); frontend **Vercel** (`www.kin-platform.com`,
  proxy Cloudflare) con el chunk de `/dashboard/projects/[id]` conteniendo el módulo
  (`Exportar proyecto`, modal, tarjeta del chat, `templateDocumentId`); CORS sin bloqueo desde
  `https://www.kin-platform.com` y `https://kin-platform.com`.
- **Sin commit/deploy pendiente**: la implementación ya está desplegada; este ADR solo la
  documenta formalmente.

---

## Estado

**PROPUESTO** — pendiente de revisión y aprobación. Tras su aprobación, las decisiones de este ADR
quedan congeladas y constituyen contrato para el módulo `kin.export`. No se modifican contratos
congelados: todos los cambios sancionados son aditivos.
