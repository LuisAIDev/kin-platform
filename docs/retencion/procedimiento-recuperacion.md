# Procedimiento de Recuperación ante Desastres — Kin Medical On-Premise

**Versión**: 1.0  
**Fecha**: 2026-10-11  
**Aplicable a**: Kin Medical On-Premise (IPS colombianas)

---

## 1. Objetivo y Alcance

Este documento describe el procedimiento oficial para recuperar los datos de Kin Medical ante eventos de desastre. Aplica a todas las IPS que operan Kin Medical en modalidad On-Premise.

**Lo que restaura este procedimiento**:
- Base de datos PostgreSQL completa (usuarios, pacientes, historia clínica, RIPS, facturación, auditoría)
- Configuración de la aplicación (variables de entorno, archivos estáticos)

**Lo que NO restaura** (responsabilidad de la IPS):
- Sistema operativo y parches de seguridad
- Infraestructura (servidores, red, firewall, certificados SSL)
- Configuración de Docker / Kubernetes externa a la aplicación

---

## 2. Escenarios Cubiertos

| Código | Escenario | Descripción | Prioridad |
|--------|-----------|-------------|-----------|
| DR-01 | Fallo de disco duro del servidor | Pérdida total del almacenamiento primario | Crítica |
| DR-02 | Corrupción de base de datos | Errores de integridad, tablas dañadas, transacciones incompletas | Alta |
| DR-03 | Borrado accidental de datos | `DROP TABLE`, `DELETE` masivo por error humano | Alta |
| DR-04 | Ataque ransomware / malware | Cifrado o destrucción de datos por actor malicioso | Crítica |
| DR-05 | Migración a nuevo servidor | Movimiento planificado a hardware/nube distinta | Media |

---

## 3. Objetivos de Recuperación (RTO / RPO)

| Métrica | Objetivo | Comentario |
|---------|----------|------------|
| **RTO** (Recovery Time Objective) | **4 horas** | Tiempo máximo desde detección hasta servicio operativo |
| **RPO** (Recovery Point Objective) | **24 horas** | Pérdida máxima de datos (backup diario a las 03:00 AM) |

> **Nota**: Si la IPS requiere RPO < 24h, debe configurar backups más frecuentes (ej: cada 6h) modificando el crontab y `RETENTION_DAYS`.

---

## 4. Prerequisitos

Antes de iniciar cualquier recuperación, verificar:

- [ ] **Backup reciente disponible** (archivo `.dump` generado por `backup-postgres.sh/.ps1`)
- [ ] **Acceso al servidor PostgreSQL** (puerto 5432 accesible desde el host de restauración)
- [ ] **Credenciales `DATABASE_URL`** (usuario, password, host, puerto, base de datos)
- [ ] **Conexión a internet** (para descargar `pg_restore` / Docker si no están instalados)
- [ ] **Permisos de administrador** (root / Administrator) en el servidor
- [ ] **Espacio en disco suficiente** (mínimo 3x el tamaño del backup)

---

## 5. Procedimiento Paso a Paso

### 5.1 Preparación Común (Todos los Escenarios)

```bash
# 1. Identificar el backup a restaurar (el más reciente válido)
ls -lh /var/backups/kin/
# Ejemplo salida:
# kin_backup_20260111_030000.dump  2.1G
# kin_backup_20260110_030000.dump  2.0G

# 2. Exportar DATABASE_URL (ajustar credenciales)
export DATABASE_URL="postgresql://kin_user:kin_pass@localhost:5432/kin_db"

# 3. Verificar conectividad a PostgreSQL
pg_isready -d "$DATABASE_URL"
```

---

### 5.2 Restauración — Linux (DR-01, DR-02, DR-03, DR-04)

```bash
# 1. Detener la aplicación Kin
docker stop kin-backend

# 2. (Opcional) Verificar integridad del backup antes de restaurar
pg_restore --list /var/backups/kin/kin_backup_20260111_030000.dump | head -20

# 3. Ejecutar restauración
./scripts/restore-postgres.sh /var/backups/kin/kin_backup_20260111_030000.dump
# Responder 'yes' cuando solicite confirmación

# 4. Verificar integridad post-restauración
psql "$DATABASE_URL" -c "SELECT COUNT(*) FROM users;"
psql "$DATABASE_URL" -c "SELECT COUNT(*) FROM patients;"
psql "$DATABASE_URL" -c "SELECT COUNT(*) FROM rips_generation_batches;"

# 5. Rearrancar la aplicación
docker start kin-backend

# 6. Healthcheck
curl -f http://localhost:8080/api/v1/actuator/health
```

---

### 5.3 Restauración — Windows PowerShell (DR-01 a DR-05)

```powershell
# 1. Detener la aplicación
docker stop kin-backend

# 2. Configurar variables
$env:DATABASE_URL = "postgresql://kin_user:kin_pass@localhost:5432/kin_db"

# 3. Verificar backup
pg_restore --list "C:\backups\kin\kin_backup_20260111_030000.dump" | Select-Object -First 20

# 4. Ejecutar restauración (usa -Force para automatizar)
.\scripts\restore-postgres.ps1 -BackupFile "C:\backups\kin\kin_backup_20260111_030000.dump" -Force

# 5. Verificar
docker start kin-backend
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/actuator/health" -Method Get
```

---

### 5.3 Escenario Específico: Ataque Ransomware (DR-04)

**Pasos adicionales obligatorios**:

1. **Aislar el servidor** de la red inmediatamente (desconectar cable / bloquear firewall).
2. **No pagar rescate** — restaurar desde backup limpio.
3. **Verificar backup** no está cifrado: `pg_restore --list backup.dump` debe listar tablas.
4. **Restaurar en servidor limpio** (reinstalar OS si es necesario).
5. **Cambiar todas las credenciales** (DB, Docker, API keys, certificados).
5. **Notificar a autoridades** (CSIRT Colombia, Policía Cibernética).
6. **Ejecutar restauración estándar** (sección 5.2 o 5.3).

---

### 5.4 Escenario Específico: Migración a Nuevo Servidor (DR-05)

1. Preparar nuevo servidor con mismos requisitos (Docker, PostgreSQL client).
2. Copiar backup más reciente al nuevo servidor (`scp`, USB, NAS).
3. Ejecutar restauración estándar (5.2 o 5.3).
5. Actualizar DNS / balanceador de carga para apuntar al nuevo servidor.
6. Verificar certificados SSL vigentes.
7. Ejecutar pruebas de humo completas (ver sección 6).

---

## 6. Verificación Post-Recuperación

Ejecutar **todas** las verificaciones antes de dar servicio por restablecido:

| Verificación | Comando | Resultado Esperado |
|--------------|---------|-------------------|
| Healthcheck API | `curl -f http://localhost:8080/api/v1/actuator/health` | `{"status":"UP"}` |
| Login de usuario | UI o `curl -X POST /api/v1/auth/login` | Token JWT válido |
| Paciente de prueba | `psql "$DATABASE_URL" -c "SELECT * FROM patients LIMIT 1;"` | Registro visible |
| Generación RIPS | `curl -X POST /api/v1/billing/rips/generate ...` | Batch status `GENERATING` → `VALID` |
| Auditar acceso | `psql "$DATABASE_URL" -c "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 5;"` | Registros recientes |

> **Si alguna verificación falla**: NO dar servicio por restablecido. Revisar logs (`docker logs kin-backend`), repetir restauración con backup anterior, o escalar (sección 8).

---

## 7. Pruebas Periódicas de Recuperación

| Frecuencia | Acción | Responsable |
|------------|--------|-------------|
| **Cada 6 meses** | Restaurar backup en ambiente de staging/preproducción | DBA IPS / Soporte KIN |
| **Anual** | Simulación completa de desastre (RTO/RPO real) | DBA IPS + Soporte KIN |
| **Post-cambio mayor** | Restaurar tras migración, upgrade PostgreSQL, cambio de infra | DBA IPS |

**Registro obligatorio**: Cada prueba debe documentarse en `docs/retencion/pruebas-recuperacion/` usando la plantilla `plantilla-registro-pruebas.md`.

---

## 8. Contactos de Escalamiento

| Rol | Contacto | Disponibilidad |
|-----|----------|----------------|
| **Soporte KIN (Nivel 1)** | soporte@kin.health / +57 3XX XXX XXXX | 8:00-18:00 COT (días hábiles) |
| **Soporte KIN (Nivel 2 - Emergencia)** | emergencias@kin.health / +57 3XX XXX XXXX | 24/7 (solo incidentes Críticos) |
| **DBA de la IPS** | [Completar por la IPS] | Según política IPS |
| **Proveedor Infraestructura (Cloud/NAS)** | [Completar por la IPS] | Según SLA proveedor |
| **CSIRT Colombia / Policía Cibernética** | csirt.gov.co / 165 | 24/7 (ciberincidentes) |

---

## 9. Registro de Incidentes y Recuperaciones

Cada incidente real o prueba debe registrarse en: `docs/retencion/pruebas-recuperacion/registro-YYYY-MM-DD.md`

**Campos obligatorios**:

| Campo | Descripción |
|-------|-------------|
| **Fecha/Hora inicio** | ISO 8601 (ej: 2026-10-11T14:30:00-05:00) |
| **Escenario** | DR-01 a DR-05 |
| **Backup usado** | Archivo `.dump` (nombre + timestamp) |
| **RTO real** | Tiempo desde detección hasta healthcheck UP |
| **RPO real** | Datos perdidos (hora del último backup vs hora del incidente) |
| **Éxito** | SÍ / NO / PARCIAL |
| **Observaciones** | Errores, lecciones aprendidas, acciones correctivas |
| **Responsable** | Nombre y rol |

---

## 10. Referencias

- [Política de Retención y Backup](../politica-retencion-backup.md)
- [Plantilla de Registro de Pruebas](plantilla-registro-pruebas.md)
- Scripts: `scripts/backup-postgres.sh`, `scripts/restore-postgres.sh`, `scripts/restore-postgres.ps1`
- Resolución 839/2017 (Historia Clínica)
- Ley 1581/2012 (Protección de Datos Personales)