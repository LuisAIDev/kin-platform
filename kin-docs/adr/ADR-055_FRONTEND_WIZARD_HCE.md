# ADR-055: Frontend Wizard HCE — Arquitectura de 7 pasos

## Contexto

El backend HCE (Historia Clínica Electrónica) está completo al 100% (14 entidades, 14 servicios, 12 controllers) y cumple con Resolución 839/1995. Sin embargo, el frontend médico (kin-frontend-medical) no tenía interfaz para que los médicos crearan, editaran y cerraran encounters. Sin wizard frontend, no se puede vender el módulo HCE a IPS formales.

**Decisión:** Implementar un Wizard multi-paso de 7 pasos usando React Hook Form + Zod, conectado al backend existente via React Query + API REST.

## Decisión

### Arquitectura del Wizard

**Ruta:** `/dashboard/physician/hce/[encounterId]/edit`

**7 Pasos:**

| Paso | Componente | Endpoint Backend | Validaciones Clave |
|------|------------|------------------|-------------------|
| 1 | IdentificationStep | PUT `/patients/{patientId}/identification` | Documento, nombres, fecha nacimiento obligatorios |
| 2 | MotiveStep | PUT `/encounters/{encounterId}` | Tipo encuentro + motivo (500 chars) |
| 3 | IllnessStep | PUT `/encounters/{encounterId}/anamnesis` | onsetDatetime no futura, severidad 1-10, 14 sistemas |
| 4 | HistoryStep | POST `/patients/{patientId}/history` | 7 tabs: Alergias, Cirugías, Medicamentos, Vacunas, Familiares, Tóxicos, Gineco-Obstétricos |
| 5 | PhysicalExamStep | PUT `/encounters/{encounterId}/physical-exam` | 10 vitales con rangos clínicos, BMI automático, 10 sistemas |
| 6 | DiagnosisPlanStep | CRUD `/diagnoses`, `/treatment-plan`, `/orders` | CIE-10 regex, 1 solo PRINCIPAL, CUPS obligatorio para procedimientos |
| 7 | ClosingStep | POST `/encounters/{encounterId}/close` | Requiere DX PRINCIPAL + Plan, confirmación irreversible |

### Stack Tecnológico

- **Next.js 16** + React 19 + TypeScript strict
- **React Hook Form** para formularios con validación en tiempo real
- **Zod** para esquemas de validación (mirror de DTOs backend)
- **React Query (TanStack Query)** para caché, invalidación y autoguardado
- **Tailwind CSS 4** + componentes UI propios (Stepper, Modal, CollapsibleSection)
- **Lucide React** para iconografía
- **Cypress** para E2E tests

### Patrones de Diseño

1. **Stepper visual** con indicador de paso actual, completados y pendientes
2. **Navegación** con botones Anterior/Siguiente/Finalizar, validación de pasos requeridos
3. **CollapsibleSection** reutilizable para secciones colapsables
3. **Autoguardado** cada 30 segundos via React Query mutations
4. **Formularios controlados** con React Hook Form + ZodResolver
5. **Modales** para CRUD de elementos (diagnósticos, antecedentes, órdenes)
4. **Validación progresiva**: botón Siguiente deshabilitado si paso actual inválido
5. **Autoguardado** con feedback visual "Guardado automático" + timestamp
5. **Responsive**: mobile-first (375px), tablet (768px), desktop (1440px)

### Validaciones Críticas (Reglas de Negocio)

1. **Diagnóstico PRINCIPAL único**: Solo 1 por encounter, radio button desmarca anterior
2. **Cierre de encounter**: Bloqueado sin DX PRINCIPAL + Plan de manejo
3. **CUPS obligatorio**: Para PROCEDURE, LAB_EXAM, IMAGING
4. **Rangos clínicos**: BP 50-300, FC 30-250, Temp 30-45°C, SpO2 50-100%, Glasgow 3-15, Dolor 0-10
6. **CIE-10**: Regex `^[A-Z]\d{2}(\.\d{1,2})?$`
7. **Autosave**: Debounce 30s, persistencia tras refresh

## Consecuencias Positivas

- **UX guiada**: Médicos no se pierden en formularios largos
- **Validación progresiva**: Errores detectados temprano, no al final
- **Autoguardado**: Cero pérdida de datos por cierre accidental
- **Validación backend-frontend sincronizada**: Zod schemas = DTOs backend
- **Testing exhaustivo**: 144 tests unitarios + 5 E2E

## Consecuencias Negativas

- **Complejidad**: 7 componentes + modales + hooks + schemas = ~2000 líneas
- **Testing E2E**: Cypress requiere servidor local + configuración CI/CD
- **Bundle size**: Zod + React Hook Form + React Query añaden ~50KB gzipped
- **Mantenimiento**: Cambios en DTOs backend requieren actualizar schemas Zod

## Referencias

- Resolución 839/1995 (Historia Clínica Colombia)
- ADR-001: Arquitectura HCE Backend
- ADR-032: ContentCipher (cifrado mensajes)
- Resolución 1581/2012 (Protección de Datos Colombia)

## Estado

**COMPLETADO** — 7/7 pasos implementados y testeados

| Día | Paso | Tests | Commit |
|-----|------|-------|--------|
| 23 | Setup + Steps 1-2 | 27 | 011d60d |
| 24 | Steps 3-4 (Illness + History) | 67 | 38c62dc |
| 25 | Steps 5-6 (PhysicalExam + DxPlan) | 65 | a34e607 |
| 26 | Step 7 + Cypress E2E | 12 + 5 E2E | f90f854 |

**Total: 144 tests unitarios + 5 E2E = 149 tests**

---

**Autor:** Agente KIN  
**Fecha:** 2026-09-28  
**Estado:** APROBADO por PO