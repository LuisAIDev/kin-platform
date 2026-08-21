# ADR-022: OpenTelemetry — Tracing distribuido opcional (Micrometer Tracing + exportador OTLP)

**Estado**: **Aprobado** (configuración implementada; exportación OTLP **verificada** contra un collector local Jaeger)
**Fecha**: 2026-08-20
**Autor**: KIN Architecture Team

> **Alcance**: este ADR documenta y congela la capacidad de **tracing distribuido opcional** de
> KIN: Micrometer Tracing con bridge OpenTelemetry y exportador OTLP. **Describe la
> configuración real** (`management.tracing.*`, `OTEL_EXPORTER_OTLP_ENDPOINT`) y el **resultado de
> la verificación con un collector local** (Jaeger all-in-one en Docker): la exportación funciona
> cuando se habilita y la aplicación funciona normal cuando está deshabilitada. No introduce
> dependencia obligatoria: sin tracer activo el comportamiento observable no cambia.

---

## Contexto

La plataforma ya dispone de observabilidad de 3 pilares parciales: métricas Micrometer con
prefijo `kin.*` (Actuator + Prometheus), logs estructurados JSON con `correlationId`/`requestId`/
`traceId` (`logstash-logback-encoder`) y probes de readiness/liveness. Faltaba el **tracing
distribuido**: correlacionar un mismo flujo (HTTP → pipeline → IA → DB) a través de componentes y
replicas, con un estándar de facto (OpenTelemetry / W3C Trace Context).

El requisito era **opcional**: KIN no exige un collector externo para operar (dev/test/CI no deben
romperse por ausencia de infraestructura de tracing). Por eso la decisión es una configuración
**opt-in** con default deshabilitado, alineada con la filosofía offline-first de la plataforma
(ADR-014/021).

---

## Decisión

Se integra **Micrometer Tracing con bridge OpenTelemetry** y **exportador OTLP HTTP** mediante las
dependencias del `pom.xml` (gestionadas por Spring Boot 3.2.5 BOM):

- `io.micrometer:micrometer-tracing-bridge-otel`
- `io.opentelemetry:opentelemetry-exporter-otlp`

**Configuración real** (`application.yml`, sección `management.tracing`):

| Propiedad | Valor por defecto | Variable de entorno | Significado |
|-----------|-------------------|---------------------|-------------|
| `management.tracing.enabled` | `false` | `MANAGEMENT_TRACING_ENABLED` | Master switch del tracing |
| `management.tracing.sampling.probability` | `0.0` | `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | Probabilidad de muestreo; con `0` no se generan trazas |
| `management.otlp.tracing.endpoint` | `${OTEL_EXPORTER_OTLP_ENDPOINT:}` | `OTEL_EXPORTER_OTLP_ENDPOINT` | Endpoint OTLP HTTP del collector |

**Semántica del endpoint (importante)**: el exportador OTLP HTTP de Micrometer publica en la URL
**exacta** configurada. El endpoint debe incluir la ruta del señal, es decir
`http://<collector>:4318/v1/traces` (estándar OTLP HTTP). Si se configura el host sin ruta
(`http://<collector>:4318`), el exportador publica en `/` y el collector responde **404**
(verificado). Por eso `.env.example` y el README documentan el endpoint con `/v1/traces`.

**Comportamiento**:

1. **Deshabilitado (default)**: `management.tracing.enabled=false` → no hay tracer activo, no se
   inicializa el exportador y **no hay ningún intento de conexión OTLP**. Los logs JSON siguen
   usando `CorrelationContext` (sin dependencia del tracer). La app arranca y opera normal sin
   collector.
2. **Habilitado sin endpoint**: `MANAGEMENT_TRACING_ENABLED=true` sin `OTEL_EXPORTER_OTLP_ENDPOINT`
   → no hay exportación (documentado en `application.yml`: "sin endpoint no hay exportación").
3. **Habilitado con endpoint**: las peticiones HTTP entrantes se instrumentan
   automáticamente (Micrometer observa el filtro de servlet/seguridad) y los spans se exportan por
   OTLP al collector configurado.

No se agrega instrumentación manual en el dominio: la observación automática de Spring Boot
(MVC + Security) cubre el flujo HTTP; el dominio puro `kin.*` permanece sin dependencia de
OpenTelemetry.

---

## Verificación (red real con collector local)

Se verificó la exportación real con un **collector local** (Docker) y la aplicación real
(perfil `dev`, backend Spring Boot en `:8080`):

| Escenario | Configuración | Resultado |
|-----------|---------------|-----------|
| **Habilitado** | `MANAGEMENT_TRACING_ENABLED=true`, `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0`, `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces`; Jaeger `jaegertracing/all-in-one:1.57` en `:4318` | **13 trazas exportadas** a Jaeger (servicio `kin-backend`; en una muestra de 5 trazas, 23 spans con operaciones `http get /pricing-plans`, `http get /auth/me`, `security filterchain before/after`, `authorize request`, `secured request`). Consulta `GET http://localhost:16686/api/traces?service=kin-backend` devolvió 13 trazas. **Exportación real confirmada** |
| **Endpoint sin ruta** | `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318` | Exportador publica en `/` → Jaeger responde **404** (`HttpExporter - Failed to export spans`). Confirma la necesidad de `/v1/traces` |
| **Deshabilitado** | Sin variables de tracing (default) | Backend arranca y responde `HEALTH OK`; tras tráfico, Jaeger mantiene las **13 trazas** previas (0 nuevas); **sin líneas de exportador OTLP** en el log. La app funciona normal sin collector |

**Procedimiento de verificación** (documentado en `README.md` → Observabilidad):

```bash
# 1. Collector local (Jaeger all-in-one, OTLP HTTP en :4318, UI en :16686)
docker run -d --name kin-jaeger -p 4317:4317 -p 4318:4318 -p 16686:16686 \
  -e COLLECTOR_OTLP_ENABLED=true jaegertracing/all-in-one:1.57

# 2. Backend con tracing habilitado (endpoint con ruta /v1/traces)
MANAGEMENT_TRACING_ENABLED=true \
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0 \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces \
mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Generar tráfico y verificar los spans en Jaeger
curl http://localhost:8080/api/v1/pricing-plans
curl "http://localhost:16686/api/traces?service=kin-backend&limit=10"
```

---

## Alternativas consideradas

| Alternativa | Rechazo |
|-------------|---------|
| **Tracing siempre activo / collector obligatorio** | Rompe dev/test/CI offline; contradice el principio offline-first de KIN. Rechazada |
| **ZIPKIN como único backend** | OTLP es el estándar actual (W3C Trace Context) y el bridge OTel permite Jaeger/OTel Collector/Tempo sin cambiar el código. Rechazada la exclusividad ZIPKIN |
| **Instrumentación manual en el dominio** | El dominio `kin.*` es POJO puro (decisión #1 del baseline); la observación automática cubre el flujo sin contaminar el dominio. Rechazada la manual en dominio |

---

## Consecuencias

### Positivas

- **Exportación real verificada** (no "preparada"): trazas HTTP de `kin-backend` llegan a un
  collector local Jaeger vía OTLP HTTP.
- **Zero-ops por defecto**: deshabilitado no inicia tracer ni exportador; sin collector la app
  funciona normal.
- **Compatibilidad**: Jaeger, OTel Collector, Tempo u otro backend OTLP son intercambiables vía
  `OTEL_EXPORTER_OTLP_ENDPOINT` (solo cambia la configuración, no el código).
- Sin dependencia en el dominio ni cambios en contratos congelados.

### Negativas

- El endpoint debe incluir la ruta `/v1/traces` (semántica Micrometer); un valor sin ruta no
  exporta (404). Documentado en `.env.example` y README.
- El sampling por defecto es `0.0`; el operador debe elevarlo explícitamente para generar trazas
  en producción (decisión intencional para limitar costo/volumen).

---

## Regla que modifica

**ADR aditivo** de observabilidad: añade la capacidad de tracing distribuido opcional a
`BASELINE_ARCHITECTURE.md` (sección Observabilidad). No modifica contratos congelados ni el
dominio `kin.*`.

## Cumplimiento

- Configuración verificada por `TracingConfigurationTest` (3 tests verdes): tracing deshabilitado
  por defecto, `management.otlp.tracing.endpoint` ligado a `OTEL_EXPORTER_OTLP_ENDPOINT`.
- Exportación real verificada con Jaeger local (13 trazas; spans HTTP de `kin-backend`).
- Deshabilitado verificado (health OK; sin exportador en log; sin trazas nuevas en Jaeger).
- Suite completa backend **verde** (`./mvnw clean verify`, BUILD SUCCESS) tras este ADR.
- **Estado del ADR**: **Aprobado**.
