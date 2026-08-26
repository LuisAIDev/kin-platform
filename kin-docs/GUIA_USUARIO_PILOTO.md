# Guía de Usuario del Piloto — KIN Health

> Documento operativo para los participantes del piloto clínico de KIN Health
> (triaje digital, diagnóstico diferencial, dashboard de salud, portal médico y
> telemedicina). Este documento **se entrega a pacientes y médicos** que
> participan en la prueba.

## 1. Objetivo del piloto

Validar en un entorno controlado el flujo completo de KIN Health:

1. El **paciente** describe sus síntomas y recibe un triaje de urgencia con
   sugerencia de acción (autocuidado / consultar médico).
2. El **médico** recibe una alerta si el triaje es de urgencia ALTA y revisa el
   resumen clínico del paciente.
3. El **paciente** y el **médico** pueden comunicarse por mensajería segura y
   agendar citas de telemedicina.

La información clínica generada es **de apoyo informativo y no sustituye el
criterio ni el diagnóstico de un profesional de la salud.**

## 2. Roles y accesos

| Rol | Qué puede hacer |
|-----|-----------------|
| **Paciente** | Triaje de síntomas, historial de consultas, dashboard "Mi Salud", mensajería con su médico, solicitar citas |
| **Médico** | Ver pacientes asignados, resúmenes clínicos, alertas de alta urgencia, mensajería, gestionar citas |

### Credenciales

- Los accesos los crea el administrador del piloto con el script
  `POST /admin/health/pilot/setup`.
- Recibirás un correo con tu usuario y una contraseña temporal.
- En el primer inicio de sesión cambia la contraseña.

## 3. Flujo recomendado (paciente)

1. **Regístrate / inicia sesión** en la plataforma.
2. Entra en **Mi Salud** y abre **Triaje de síntomas**.
3. Describe tus síntomas en lenguaje natural (ej. *"tengo fiebre y dolor de
   cabeza"*). El sistema responde con una urgencia y una acción sugerida.
4. Si el triaje lo recomienda, contacta a tu médico por **Mensajes** o pide una
   **cita de telemedicina**.
5. Revisa tu **historial** y **plan de cuidado** en Mi Salud.

### Reglas de uso

- El triaje **nunca es un diagnóstico**. Ante dudas o síntomas graves llama al
  servicio de urgencias de tu región.
- Los mensajes y citas son confidenciales; usa la plataforma solo para
  comunicaciones relacionadas con tu salud.

## 4. Flujo recomendado (médico)

1. **Inicia sesión** en el Portal Médico.
2. Revisa las **alertas de alta urgencia** (aparecen en la parte superior).
3. Consulta la lista de **pacientes asignados** y abre el resumen de cada uno.
4. Responde los mensajes de tus pacientes en el **buzón de telemedicina** y
   confirma o reprograma las **citas**.
5. Si un triaje te parece incorrecto, anótalo en el feedback (botón
   **"Dar feedback"**).

## 5. Feedback

En las pantallas de **Mi Salud** (paciente) y **Portal Médico** (médico) hay un
botón **"Dar feedback"** que abre el formulario de comentarios del piloto.
Úsalo para reportar:

- Errores o resultados que consideres incorrectos.
- Dificultades de uso de la plataforma.
- Sugerencias de mejora.

## 6. Privacidad

- Tus datos de salud están cifrados en reposo y solo los ve tu médico asignado.
- Las métricas del piloto son **agregadas y anonimizadas** (conteos y promedios,
  sin nombres ni correos).
- Puedes solicitar la baja del piloto en cualquier momento contactando al
  administrador.

## 7. Soporte

| Canal | Contacto |
|-------|----------|
| Soporte técnico | soporte@kin-platform.com |
| Emergencia médica | Llama a urgencias de tu región (no uses la plataforma) |
