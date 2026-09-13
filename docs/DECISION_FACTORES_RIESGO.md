# Decisión: Factores de Riesgo en Diagnóstico Diferencial

**Fecha:** 2026-09-13
**Estado:** Fase 1 completada

## Contexto

El diagnóstico diferencial original tenía 8 factores de riesgo. Se amplió a 38 en el frontend.

## Decisión

**Fase 1 (actual):** Solo UI. Los factores se muestran y registran, pero NO afectan el cálculo de probabilidades porque no tienen pesos en la tabla `risk_factors`.

**Razón:** Los pesos clínicos requieren validación por un médico del equipo antes de insertarse. Inventar pesos generaría diagnósticos erróneos y riesgo legal.

## Fórmula actual (sin pesos nuevos)

p' = p + weight · (1 - p)

Donde `weight` solo existe para los factores originales (8). Los 30 nuevos tienen `weight = 0` implícito.

## Fase 2 (pendiente)

- Reclutar médico asesor (puede ser un early adopter de KIN).
- Documentar pesos por condición con base en guías clínicas (OMS, ministerios, sociedades médicas).
- Insertar los pesos en la tabla `risk_factors` con validación.
- Probar con casos clínicos reales.

## Fase 3 (V2)

- Ajustar pesos con base en datos reales de la plataforma.
- Calibración automática (machine learning) si aplica.

## Referencias

- Fórmula: `DifferentialEngine.adjust()`
- Tabla: `risk_factors` (factor, weight, description)
- Frontend: `DifferentialSection.tsx` líneas 21-30