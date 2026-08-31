### IA Automatizaciones (Área 14)
El módulo de Automatizaciones (Área 14, ADR-038) permite a médicos y administradores definir reglas condicionales del tipo SI ocurre X, ENTONCES haz Y. Las reglas son deterministas y escritas en Java, sin intervención de IA en la lógica de negocio.
**Endpoints principales:**
- POST /api/v1/health/automation/rules - Crear regla
- GET /api/v1/health/automation/rules - Listar reglas
- PUT /api/v1/health/automation/rules/{id} - Actualizar regla
- PUT /api/v1/health/automation/rules/{id}/toggle - Activar/Desactivar
- DELETE /api/v1/health/automation/rules/{id} - Eliminar regla
- GET /api/v1/admin/health/automation/logs - Ver logs de ejecución (ADMIN)
