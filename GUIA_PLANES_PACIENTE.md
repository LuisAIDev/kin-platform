# Guía de Planes para Pacientes (SALUD_PERSONAL)

## Resumen

Esta guía describe el sistema de planes de pago para pacientes en la vertical SALUD_PERSONAL de KIN Platform.

## Planes Disponibles

### 1. Personal Free (Gratis)
- **Precio**: $0/mes
- **Triajes**: 3 por mes
- **Almacenamiento**: 50 MB
- **IA**: Básica (FLASH)
- **PDF Export**: No
- **Compartir triajes**: No
- **Soporte**: Básico
- **Trial**: No

### 2. Personal+ ($9/mes)
- **Precio**: $9/mes
- **Triajes**: Ilimitados
- **Almacenamiento**: 5,000 MB (5 GB)
- **IA**: Avanzada (PRO)
- **PDF Export**: Sí
- **Compartir triajes**: Sí
- **Soporte**: Básico
- **Trial**: 14 días gratis

## Cómo Contratar

1. Accede a `/dashboard/patient/plans`
2. Haz clic en "Contratar por $9/mes" en el plan Personal+
3. Serás redirigido a Stripe para completar el pago
3. Si el plan tiene trial, tendrás 14 días gratis antes del primer cargo
4. Tras el pago, la suscripción se activa automáticamente vía webhook

## Cancelar Suscripción

1. Ve a `/dashboard/patient/plans`
2. En tu plan activo (Personal+), haz clic en "Cancelar suscripción"
3. Confirma la cancelación
5. Tu suscripción volverá a Personal Free inmediatamente

## Estado de Suscripción

En el dashboard de paciente (`/dashboard/patient/health`) verás un banner con:
- Plan actual (Personal Free / Personal+)
- Estado: Activa / Inactiva
- Triajes usados / límite mensual
- Almacenamiento usado / límite
- Presupuesto IA usado / restante
- Nivel de IA (FLASH / PRO)
- Beneficios: PDF Export, Compartir triajes, IA Avanzada
- Fecha de próxima renovación

## Detalles de Límites

### Triajes
- **Personal Free**: 3 triajes/mes
- **Personal+**: Ilimitado

### Almacenamiento
- **Personal Free**: 50 MB
- **Personal+**: 5,000 MB (5 GB)
- El uso se calcula sumando el tamaño de todos los documentos subidos en `clinical_documents`

### IA (Presupuesto mensual)
- **Personal Free**: $0.50 USD/mes
- **Personal+**: $5.00 USD/mes
- Nivel FLASH (gratis) vs PRO (pago)

## Preguntas Frecuentes

### ¿Qué pasa si cancelo mi suscripción?
Vuelves inmediatamente al plan **Personal Free**. Pierdes:
- Triajes ilimitados → 3/mes
- 5 GB almacenamiento → 50 MB
- IA PRO → FLASH
- Exportar PDF
- Compartir triajes

### ¿Puedo volver a suscribirme después?
Sí, en cualquier momento desde `/dashboard/patient/plans`

### ¿El trial de 14 días es realmente gratis?
Sí, no se cobra nada durante los 14 días. Si cancelas antes de que termine, no se te cobra nada.

### ¿Puedo cambiar de plan en cualquier momento?
Sí, puedes actualizar de Free a Personal+ en cualquier momento. El cambio es inmediato.

### ¿Cómo veo mi uso de almacenamiento?
En el dashboard de paciente (`/dashboard/patient/health`) verás una barra de progreso con "Almacenamiento: X MB / Y MB"

## Soporte
Para dudas sobre facturación o planes: soporte@kin-platform.com