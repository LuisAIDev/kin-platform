# ADR-051: Validación RIPS según Resolución 2275 de 2023

## Estado
Aceptado

## Contexto
La Resolución 2275 de 2023 (28 dic 2023) del Ministerio de Salud y Protección Social de Colombia estableció el nuevo estándar para el Reporte Individual de Prestación de Servicios de Salud (RIPS) como soporte de la Factura Electrónica de Venta (FEV) en salud.

Cambios clave respecto a la Resolución 1552 de 2013:
- Migración de XML/XSD puro a **JSON + XML/UBL** (extensión DIAN)
- El RIPS se envía como extensión UBL dentro de la factura electrónica
- Estructura de 11 campos del sector salud (5 obligatorios + 6 condicionales)
- 6 reglas de validación críticas de negocio

El Anexo Técnico 1 de la Resolución 2275 define:
1. Campos obligatorios: CODIGO_PRESTADOR, MODALIDAD_PAGO, COBERTURA_PLAN_BENEFICIOS, FECHA_INICIO_PERIODO, FECHA_FIN_PERIODO
2. Campos condicionales: NUMERO_CONTRATO, NUMERO_POLIZA, COPAGO, CUOTA_MODERADORA, PAGOS_COMPARTIDOS, ANTICIPO
3. 6 reglas de validación de negocio
4. Tablas de referencia SISPRO: modalidadPago, coberturaPlan, conceptoRecaudo, TipoIdPISIS

## Decisión
Implementar `RipsValidator` como componente de validación de reglas de negocio del Anexo Técnico 1, integrado en el pipeline de generación RIPS existente.

### Componentes creados:
1. **Migración V71**: 4 tablas de referencia SISPRO con seed oficial
   - `sispro_modalidad_pago` (5 códigos del inciso 3.2)
   - `sispro_cobertura_plan` (15 códigos del inciso 3.3)
   - `sispro_concepto_recaudo` (4 códigos)
   - `sispro_tipo_id` (9 códigos comunes TipoIdPISIS)

2. **Entidades y Repositories JPA** para cada tabla

3. **RipsValidator** con validaciones:
   - 5 campos obligatorios siempre presentes
   - 6 campos condicionales según contexto
   - Regla 1: Exclusividad NUMERO_CONTRATO XOR NUMERO_POLIZA
   - Regla 2: Formato monetario (sin símbolos, sin separadores miles, decimal con punto, no negativos)
   - Regla 3: Formato fecha AAAA-MM-DD estricto
   - Regla 4: Facturas multiusuario → misma MODALIDAD_PAGO y COBERTURA_PLAN_BENEFICIOS
   - Regla 5: Valores acumulados coinciden con totales RIPS (estructura lista)
   - Regla 6: TIPO_OPERACION códigos válidos (SS-CUFE, SS-CUDE, SS-POS, SS-SNum, SS-Recaudo, SS-Reporte, SS-SinAporte)
   - Validación contra tablas SISPRO (modalidad, cobertura, tipo identificación)

4. **Integración en RipsGenerationOrchestrator**: pipeline de 3 validadores
   - XsdValidator (estructura XML)
   - BusinessRuleValidator (reglas de negocio internas)
   - RipsValidator (reglas Anexo Técnico 1)

## Consecuencias

### Positivas
- RIPS validados **antes** de envío a MinSalud/DIAN
- Errores detectados por campo con contexto (fila, campo, valor, regla)
- Reducción significativa de rechazos por errores de formato/reglas
- Tablas de referencia actualizables sin deploy (datos en BD)
- Tests automatizados (19 casos) cubriendo reglas críticas

### Negativas
- Requiere mantener tablas SISPRO actualizadas (cambios normativos)
- Complejidad adicional en pipeline de validación (3 validadores)
- Validación de valores acumulados (Regla 5) requiere totales reales de RIPS generados

## Alternativas consideradas
1. **Solo XSD**: No cubre reglas de negocio (exclusividad, multiusuario, monetarios)
2. **Validación en base de datos**: Difícil de testear y mantener
3. **Validación post-generación (batch job)**: Detección tardía, requiere re-procesamiento

## Referencias
- Resolución 2275 de 2023 (MinSalud) — Anexo Técnico 1
- Resolución 058 de 2020 (Contaduría) — FEV
- Resolución 000042 de 2020 — Anexo Técnico RIPS
- SISPRO: https://web.sispro.gov.co (tablas de referencia oficiales)

## Implementación
- Commit V71: `V71__create_sispro_reference_tables.sql`
- Entidades: `SisproModalidadPago`, `SisproCoberturaPlan`, `SisproConceptoRecaudo`, `SisproTipoId`
- Repositories: `Sispro*Repository`
- Validator: `RipsValidator` (inyección de 4 repositories)
- Tests: `RipsValidatorTest` (19 casos)
- Integración: `RipsGenerationOrchestrator` llama a `ripsValidator.validate()`

## Próximos pasos
- TD-CAT-4: Descargar XSD oficiales MinSalud v2024 y ajustar `RipsJsonToXmlMapper`
- Automatizar actualización semestral de tablas SISPRO