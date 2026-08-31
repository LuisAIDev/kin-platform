# GUIA_IA_ASISTENCIAL.md — Para Médicos KIN

## ¿Qué es la IA Asistencial?

El módulo de **IA Asistencial** (Área 13, ADR-037) ofrece cinco tipos de asistencia automatizada por IA para apoyar tu trabajo clínico. La IA **nunca genera diagnósticos, probabilidades, riesgos ni recomendaciones** — solo resume, organiza, explica o redacta información basada en datos clínicos existentes que ya están en el sistema.

### Cinco tipos de asistencia disponibles

| Tipo | Endpoint | Qué hace |
|------|----------|----------|
| **Resumen** | `POST /api/v1/health/aiassist/patients/{patientId}/summary` | Genera un resumen conciso (5 viñetas) del historial del paciente para leer antes de la consulta |
| **Organizar síntomas** | `POST /api/v1/health/aiassist/patients/{patientId}/organize` (si está disponible) | Agrupa los síntomas reportados por categorías respiratorios, digestivos, musculares, etc. |
| **Preparar consulta** | `POST /api/v1/health/aiassist/patients/{patientId}/consultation-prep` | Genera una lista de preguntas basadas en información faltante o dudosa del historial de triajes |
| **Explicar diagnóstico** | `POST /api/v1/health/aiassist/differential-explain` | Explica los hallazgos del diagnóstico diferencial en lenguaje sencillo y tranquilizador para el paciente |
| **Redactar mensaje** | `POST /api/v1/health/aiassist/patients/{patientId}/draft-message` | Redacta un mensaje para el paciente basándose en una recomendación médica que tú proporciones |

### ¿Cómo usarlo en la práctica?

1. **Selecciona un paciente** en el Portal Médico
2. **Haz clic en "Generar resumen"** — la IA crea un resumen de los triajes realizados, planes de seguimiento, citas y documentos compartidos
3. **Haz clic en "Preparar consulta"** — la IA genera preguntas sobre información faltante o dudosa basada en el historial de triajes
4. **Haz clic en "Redactar mensaje"** — proporciónale una recomendación médica y la IA redacta un mensaje claro y amable para el paciente (tú siempre puedes editar o descartarlo)
5. **Usa "Explicar diagnóstico"** — si tienes un resultado de diagnóstico diferencial, la IA lo explica en lenguaje que el paciente pueda entender

### Flujo de trabajo recomendado

```
Paciente → Consulta → Triajes → [Botón IA] → Revisar salida IA → Editar si necesario → Enviar al paciente
```

### Consideraciones importantes

- **La IA nunca reemplaza tu juicio clínico**: Siempre revisa la salida antes de usarla
- **Todo queda registrado**: Cada operación de IA se audita automáticamente (puedes ver los logs en el panel de administración)
- **Acceso controlado**: Solo pueden acceder los médicos con relación `ACTIVE` al paciente asignado
- **Modo deshabilitado**: Si el módulo está desactivado en configuración, los botones no aparecerán
- **Configuración**: Los parámetros (maxTokens, temperature, modelo) se configuran en `kin.health.aiassist.*`

### Limitaciones conocidas

- La IA puede repetir información del historial sin añadir nuevos insights
- Los prompts están optimizados para español, pero pueden tener variaciones en terminología médica
- El rendimiento depende de la disponibilidad del proveedor de IA (DeepSeek por defecto)
- **Tiempo de respuesta típico**: 8-15 segundos (el stage Consultor tiene timeout de 60s)

### ¿Cuándo NO usarlo?

- Si el paciente no tiene historial de triajes previo
- Si la información clínica es insuficiente o está incompleta
- En situaciones de emergencia donde se requiere decisión inmediata sin asistencia de IA
- **Nunca** para generar nuevos diagnósticos o modificar el plan de tratamiento

### Preguntas frecuentes

**¿Puedo confiar ciegamente en el resumen de la IA?**
No. Revisa siempre el resumen en busca de precision y completitud. La IA puede omitir detalles o agrupar de forma diferente a como lo harías tú.

**¿La salida de la IA es vinculante?**
No. La salida es de apoyo y siempre puedes editarla o descartarla. El registro queda en el historial para auditar qué se generó.

**¿Puedo usar esto con cualquier paciente?**
Solo con pacientes a los que tengas una relación `ACTIVE` asignada. El sistema validará esto automáticamente.

**¿Qué pasa si la IA falla?**
El sistema muestra un mensaje de error y el botón vuelve al estado normal. No se guarda información parcial.

### Próximos pasos

- Probar todos los tipos de asistencia con tus pacientes
- Comentar en el equipo si algún prompt necesita ajustes
- Configurar los parámetros `kin.health.aiassist.*` según tus preferencias
- Feedback oficial a través del canal correspondiente