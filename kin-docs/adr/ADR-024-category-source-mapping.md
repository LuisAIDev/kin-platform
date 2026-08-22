# ADR-024: Mapeo categoría de proyecto → fuentes de conocimiento (selección determinista en Java)

**Estado**: **Aprobado** (diseño e implementación del mecanismo + prueba de concepto con 3 categorías)
**Fecha**: 2026-08-20
**Autor**: KIN Architecture Team

> **Alcance**: este ADR define cómo el pipeline sabe la **categoría del proyecto** y selecciona
> **solo las fuentes de conocimiento pertinentes** a esa categoría — siempre de forma determinista
> (Java decide; el LLM nunca elige ni ejecuta peticiones). Se implementa el mecanismo completo y
> se valida con una **prueba de concepto** (AGROINDUSTRIA, FINTECH, SALUD); las 19 categorías
> completas se documentan en una tabla de cobertura con huecos honestos.

---

## Contexto

KIN expone **19 categorías de proyecto** al crear un proyecto (verificado contra
`V6__create_categories.sql` + `V18__add_project_categories.sql` y el seed de dev; antes se
documentaban 18): Tecnología e Innovación, Empresarial, Agroindustria, Salud, Educación, Impacto
Social, Medio Ambiente, Industria, Gobierno, Fintech, Comercio, Turismo, Gastronomía y Alimentos,
Logística, Creatividad, Marketing Digital, Investigación, Servicios y Otro/Sin clasificar. La
allowlist global de fuentes (ADR-023) tiene metadata `level`/`region`, pero la categoría solo se
usaba con el valor genérico `ECONOMY`. Se necesita que un proyecto de **Agroindustria** consulte
fuentes agrícolas, uno de **Fintech** fuentes financieras, etc., en vez de consultar siempre las 15
fuentes.

El reto técnico: la categoría del proyecto se guarda en `Project.category` (entidad `Category`),
pero el `KnowledgeStage` solo ve el `ProjectContext`. Además, la dimensión `SECTOR` (donde se
volcaba la categoría al crear el contexto) es **sobrescrita por el Analizador** con el giro
detectado en la conversación, perdiéndose la categoría.

---

## Decisión

### D1. Metadata: múltiples categorías por fuente

`SourceConfig.category` (String, casi sin uso) se reemplaza por
**`SourceConfig.categories` (List\<String\>)**:

- **Vacío** = fuente de **contexto general** (se consulta para cualquier categoría): PIB, TRM,
  tipos de cambio, PIB departamental, IPC — macro que informa a casi todos los análisis.
- **Con valores** = fuente **específica** de esas categorías; una fuente puede servir a varias
  (p. ej. `superfinanciera-tasas` → `[FINTECH, EMPRESARIAL]`).

### D2. La categoría viaja por el pipeline sin perderla

1. **`ProjectContext`** gana un campo dedicado **`projectCategory`** (código de categoría) que
   `fromProject(...)` setea al crear el contexto y que el Analizador **no** sobrescribe (a
   diferencia de `SECTOR`). Se serializa aparte de las dimensiones
   (`ProjectContextData.projectCategory`), preservando la completitud (no altera
   `AnalyzedDimension` ni `coverageRatio`).
2. **`KnowledgeRequest`** y **`KnowledgeQuery`** ganan un campo aditivo **`category`**
   (con constructores de conveniencia para no romper call-sites).
3. **`KnowledgeStage.category()`** lee `ProjectContext.projectCategory()` (fallback a `SECTOR`).
4. **`CategoryAwareCompositeKnowledgeSource`** (infraestructura) filtra por `query.category()`:
   una fuente se consulta si no declara categorías (contexto general), si la categoría está vacía
   (sin filtro) o si su lista contiene la categoría del proyecto.

### D3. Regla de selección (Java decide)

```
para cada fuente en el registro:
    si fuente.enabled == false                    → omitir (ADR-025)
    si fuente.categorías está vacía               → consultar (contexto general)
    si request.categoría está vacía               → consultar (sin filtro, compatibilidad)
    si fuente.categorías contiene categoría       → consultar
    si no                                        → omitir (no pertinente a este proyecto)
```

Las fuentes se consultan en orden de `priority` descendente (empate = orden de configuración,
determinista). La caché Redis aísla por categoría: `queryKey` incluye `query.category()` (mismo
tema en categorías distintas → hechos distintos → claves distintas).

### D4. Cobertura por categoría (estado real, ADR-023 §Colombia + Nivel 1)

| Categoría | Fuentes específicas aplicables | Estado |
|---|---|---|
| Agroindustria | insumos agrícolas, exportaciones de café, Finagro desembolsos | ✅ Probada (POC) |
| Fintech | Superfinanciera tasas, Superfinanciera cartera, Finagro desembolsos | ✅ Probada (POC) |
| Salud | MinSalud Saludatos (talento humano) | ✅ Probada (POC) |
| Empresarial | Confecámaras empresas, tasas, cartera, Finagro | ✅ **POC end-to-end (2ª ronda)** |
| Comercio | Confecámaras empresas, exportaciones de café | ✅ **POC end-to-end (2ª ronda)** |
| Logística | Aerocivil transporte aéreo, exportaciones de café (reuso) | ✅ **POC end-to-end (2ª ronda)** |
| Tecnología e Innovación | MinTIC internet fijo, MinTIC internet móvil | ✅ **POC end-to-end (2ª ronda)** |
| Investigación | MinCiencias proyectos de investigación e innovación | ✅ **POC end-to-end (2ª ronda)** |
| Medio Ambiente | IDEAM calidad del aire | ✅ Fuente probada |
| **Gobierno** | **SECOP — origen de recursos de contratación (Colombia Compra Eficiente)** | ✅ **Nueva (Fase 1B), probada + shadow** |
| **Servicios** | Confecámaras empresas (reuso) | ✅ **Nueva (Fase 1B), probada + shadow** |
| **Marketing Digital** | MinTIC internet fijo y móvil (reuso) | ✅ **Nueva (Fase 1B), probada + shadow** |
| **Gastronomía** | insumos agrícolas (reuso: precios de insumos alimentarios) | ✅ **Nueva (Fase 1B), probada + shadow (reuso)** |
| Industria | (sin fuente tabular: DANE no publica IPI/manufacturera en datos.gov.co) | 🔴 Descartada (motivo documentado) |
| Educación, Impacto Social, Turismo, Creatividad, Otro | (solo contexto general; sin fuente nacional tabular) | ⏳ Pendiente (motivo documentado) |

Todas las categorías reciben al menos el **contexto general** (Banco Mundial + ECB + TRM + PIB
departamental + IPC INE). El mecanismo está listo: agregar cobertura específica a una categoría es
**una entrada de configuración** (fuente con `categories: [X]`), sin cambiar la arquitectura.

### D5. Huecos investigados (sin inventar fuentes)

| Categoría | Candidata investigada | Estado |
|---|---|---|
| Salud | MinSalud/INS vía datos.gov.co (Saludatos THS) | ✅ Verificada e integrada |
| Medio Ambiente | IDEAM vía datos.gov.co (Calidad del Aire) | ✅ Verificada e integrada |
| Logística | Aerocivil transporte aéreo (datos.gov.co); transporte marítimo | ✅ Integrada / 🔴 marítimo `403 no tabular` |
| Tecnología | MinTIC internet fijo y móvil (datos.gov.co) | ✅ Verificada e integrada |
| Investigación | MinCiencias proyectos de investigación (datos.gov.co) | ✅ Verificada e integrada |
| Industria | DANE IPI / encuesta manufacturera | 🔴 Descartada: no publicada como dataset tabular en datos.gov.co |
| Educación | MinEducación/ICFES en datos.gov.co | ⏳ Solo datasets locales (matrícula municipal); sin serie nacional tabular clara |
| Gobierno | Función Pública / presupuestos | ⏳ Solo presupuestos municipales |
| Turismo | MinCIT / ProColombia | ⏳ Solo datasets locales (hoteles por municipio) |
| Impacto Social / Creatividad / Marketing / Logística / Servicios | — | ⏳ Sin fuente oficial con API tabular relevante identificada |

### D6. Hallazgo corregido en la integración REST (2ª ronda)

El pipeline pasaba el **nombre** de la categoría (`Category.name`, p. ej. "Tecnología e
Innovación") al `ProjectContext`, mientras la configuración usa el **código** (`Category.code`,
p. ej. `TECNOLOGIA`). Los nombres con acentos/espacios no coincidían con los códigos. Fix:
`ChatOrchestratorServiceImpl` ahora pasa **`Category.code`**, y el matcheo en
`CategoryAwareCompositeKnowledgeSource` normaliza acentos/minúsculas (defensa en profundidad).

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Pasar la categoría por `SECTOR`** (como se hacía) | El Analizador la sobrescribe con el giro detectado en la conversación → se pierde la señal. Rechazada |
| **Añadir un valor al enum `AnalyzedDimension`** | Cambia `values().length` → altera `coverageRatio`/completitud. Rechazada: campo dedicado en `ProjectContext` |
| **Selección de fuentes en el dominio (`SourceRegistry`)** | El dominio no conoce la config de categorías (infra). Rechazada: filtro en el adaptador compuesto |
| **Modificar `SourceConfig.category` a un único valor** | Una fuente de tasas sirve a Fintech y Empresarial → se necesita lista. Rechazada |

---

## Consecuencias

### Positivas

- **Selección determinista y por categoría**: cada proyecto consulta solo lo pertinente (menos
  llamadas HTTP, menos ruido, caché más precisa).
- **Extensible sin arquitectura**: cubrir una categoría nueva = una fuente con `categories`.
- **Categoría robusta**: sobrevive al Analizador (campo dedicado en `ProjectContext`, serializado).
- **Seguridad intacta**: allowlist + SSRF sin cambios; el LLM no participa.

### Negativas

- **Huecos honestos**: 7 categorías quedan solo con contexto general hasta encontrar fuentes
  oficiales con API tabular real.
- La categoría se propaga por dos records de dominio (`KnowledgeRequest`/`KnowledgeQuery`) y por
  el `ProjectContext` (campo aditivo, serializado).
- La caché aísla por categoría: el mismo tema en categorías distintas genera claves distintas
  (correcto, pero duplica almacenamiento si un tema se consulta en varias categorías).

---

## Regla que modifica

**ADR aditivo** sobre ADR-021/023: `SourceConfig.categories` (lista) sustituye a `category`;
`KnowledgeRequest`/`KnowledgeQuery` ganan `category` (aditivo); `ProjectContext` gana
`projectCategory` (campo dedicado, serializado); `KnowledgeStage` la propaga y el
`CategoryAwareCompositeKnowledgeSource` filtra. No modifica contratos congelados
(`SourceValidator`, `SourceConnectionGuard`, `KnowledgeEngine` intactos); `AnalyzedDimension` sin
cambios (completitud preservada).

## Cumplimiento

- **Prueba de concepto end-to-end** (staging, red real):
  - Engine: `KnowledgeCategoryMappingTest` (3/3): AGROINDUSTRIA consulta solo general+agro;
    FINTECH solo general+financieras; SALUD solo general+salud.
  - REST real: proyecto creado en **AGROINDUSTRIA** → turno de chat → la caché Redis del turno
    contiene hechos de `insumos-agricolas`, `dane-exportaciones-cafe`, `finagro-desembolsos` +
    contexto general, y **no** incluye cartera/tasas/saludatos/calidad-del-aire.
  - Unit: `CategoryAwareCompositeKnowledgeSourceTest` (5/5) + `JpaContextRepositoryTest` (4/4,
    serialización de `projectCategory`).
- Suite completa backend **verde** (`./mvnw clean verify`).
- **Producción**: `KNOWLEDGE_EXTERNAL_ENABLED=false` por defecto; el perfil `staging` activa la
  selección por categoría solo en pruebas.
- **Estado del ADR**: **Aprobado**.
