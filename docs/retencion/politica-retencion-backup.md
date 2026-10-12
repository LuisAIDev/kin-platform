# Política Formal de Retención Documental y Protección de Datos — Kin Medical On-Premise

**Versión**: 2.0  
**Fecha de Vigencia**: 2026-10-11  
**Clasificación**: Documento Oficial — Para Presentación en Auditoría Ministerio de Salud  
**Responsable**: DPO de la IPS / Representante Legal IPS  
**Aprobado por**: [Firma Representante Legal IPS] — [Fecha]

---

## 1. Marco Legal y Normativo Aplicable

Esta política se fundamenta en el siguiente marco normativo colombiano vigente:

| Norma | Título | Relevancia para esta Política |
|-------|--------|------------------------------|
| **Resolución 1995 de 1999** | Por la cual se establecen las normas para la organización y manejo de la Historia Clínica | Define la obligatoriedad de la Historia Clínica (HCE), su estructura, contenido mínimo y custodia |
| **Resolución 839 de 2017** (modifica 839/1999) | Por la cual se establecen los lineamientos para la conservación de la Historia Clínica | **Retención mínima obligatoria: 15 años** contados desde la última atención |
| **Ley 1581 de 2012** | Ley Estatutaria de Protección de Datos Personales | Principios de legalidad, finalidad, temporalidad, seguridad, confidencialidad; derechos del titular (habeas data) |
| **Decreto 1377 de 2013** | Reglamenta parcialmente la Ley 1581/2012 | Medidas de seguridad, procedimientos de atención de consultas y reclamos |
| **Resolución 3100 de 2019** | Habilitación de Prestadores de Servicios de Salud | Condiciones de habilitación, obligación de mantener sistemas de información íntegros y disponibles |
| **Resolución 2275 de 2023** | RIPS Electrónico y Factura Electrónica de Venta en Salud | Generación, transmisión y conservación de RIPS y FEV; interoperabilidad UBL 2.1 |
| **Resolución 1888 de 2025** | Interoperabilidad FHIR RDA (Registro de Datos Abiertos) | Estándares de intercambio de información clínica (FHIR R4), conservación de recursos FHIR |

---

## 2. Definiciones

| Término | Definición |
|---------|------------|
| **Historia Clínica Electrónica (HCE)** | Conjunto de documentos, datos e información generados durante la atención en salud, que permiten identificar al usuario, registrar su estado de salud, la evolución del mismo y las decisiones clínicas adoptadas (Res. 1995/1999, Art. 3) |
| **Documento Electrónico de Salud** | Todo registro generado, enviado, recibido o almacenado por medios electrónicos, magnéticos, ópticos o cualquier otra tecnología análoga, que contenga información en salud (Ley 1581/2012, Art. 3) |
| **Retención Documental** | Conservación de documentos durante el tiempo que establece la normativa, garantizando su integridad, autenticidad, disponibilidad y confidencialidad |
| **Backup (Respaldo)** | Copia de seguridad operativa para recuperación ante desastres (RTO/RPO), ciclo corto (30 días local, 15 años archivo) |
| **Archivo Histórico** | Conservación a largo plazo con fines legales, probatorios y de continuidad asistencial (mínimo 15 años HCE) |
| **Soft Delete** | Marcado lógico de eliminación (`deleted_at`) sin borrado físico, mantiene trazabilidad y auditoría |
| **Dato Personal Sensible** | Dato que afecta la intimidad del titular o cuyo uso indebido puede generar discriminación (datos de salud, biométricos, vida sexual) — Ley 1581/2012, Art. 5 |
| **Titular** | Persona natural cuyos datos personales sean objeto de Tratamiento (usuario/paciente) |
| **Responsable del Tratamiento** | Persona natural o jurídica que decide sobre la base de datos y/o el Tratamiento de los datos (la IPS) |
| **Encargado del Tratamiento** | Persona natural o jurídica que realiza el Tratamiento por cuenta del Responsable (KIN como proveedor de software) |
| **DPO (Delegado de Protección de Datos)** | Persona designada por la IPS para velar por el cumplimiento de la Ley 1581/2012 |

---

## 3. Plazos de Retención Obligatorios

| Tipo de Información | Plazo Mínimo | Base Legal | Observaciones |
|---------------------|--------------|------------|---------------|
| **Historia Clínica Electrónica (HCE) completa** | **15 años** desde la última atención | Res. 839/2017, Art. 12 | Contado desde el alta/última consulta |
| **Consentimientos Informados** | **15 años** | Res. 1995/1999, Art. 15; Res. 839/2017 | Incluye consentimientos quirúrgicos, anestésicos, transfusión, investigación |
| **Datos de Identificación del Paciente** | **15 años** | Res. 839/2017; Ley 1581/2012 | Nombre, documento, fecha nacimiento, contactos |
| **Evoluciones Clínicas / Notas de Enfermería** | **15 años** | Res. 1995/1999 | Parte integral de la HCE |
| **Resultados de Ayudas Diagnósticas (Imágenes, Laboratorio, Patología)** | **15 años** | Res. 1995/1999; Res. 839/2017 | Incluye metadatos DICOM, HL7 |
| **Prescripciones Médicas y Órdenes** | **15 años** | Res. 1995/1999; Res. 2275/2023 | Incluye MIPRES, CUPS, POS |
| **RIPS Generados y FEV Emitidas** | **5 años** | Res. 2275/2023; Código de Comercio Art. 28 | Requisito fiscal y de auditoría DIAN |
| **Registros de Facturación Electrónica (DIAN)** | **5 años** | Estatuto Tributario Art. 632 | CUFE/CUDE, XML UBL 2.1 |
| **Logs de Auditoría y Accesos (audit_logs)** | **5 años** | Res. 3100/2019; Ley 1581/2012 Art. 17 | Trazabilidad de acceso a datos sensibles |
| **Reportes de Eventos Adversos / Notificaciones INVIMA** | **10 años** | Res. 1403/2007; Dec. 4725/2005 | Farmacovigilancia, tecnovigilancia |
| **Contratos y Convenios con EPS / Aseguradores** | **10 años** post-terminación | Código de Comercio Art. 28 | Para auditoría de glosas y conciliaciones |
| **Documentos de Habilitación (Res. 3100/2019)** | **Vigencia + 5 años** | Res. 3100/2019 | Certificados, actas, planes de mejoramiento |

> **Principio de Temporalidad (Ley 1581/2012, Art. 4)**: Los datos solo se conservarán mientras sean necesarios para la finalidad para la cual fueron recolectados. **Excepción**: Cuando la ley exija plazo mayor (ej. 15 años HCE), prevalece el plazo legal.

---

## 4. Modalidades de Conservación

| Etapa | Período | Estado | Acceso | Ubicación | Responsable |
|-------|---------|--------|--------|-----------|-------------|
| **Archivo de Gestión** | 0–5 años | Activo (online) | Frecuente, tiempo real | Base de datos producción (PostgreSQL) + Backups locales (30 días) | IPS (Operativo) |
| **Archivo Central** | Años 6–15 | Semi-activo (nearline) | Bajo demanda (consulta histórica) | Backups mensuales en almacenamiento externo cifrado (S3/NAS/Blob) | IPS (Archivo) |
| **Archivo Histórico** | >15 años | Inactivo (offline) | Solo orden judicial / auditoría | Cinta magnética / Cold storage / Archivo físico legalizado | IPS (Archivo) + Notaría si aplica |

### Criterios de Transición

1. **Año 5 → 6**: Migración automática de backups mensuales a almacenamiento externo (cold tier).
2. **Año 15 → 16**: Evaluación de eliminación controlada (ver Sección 8).
3. **Cualquier momento**: Si existe proceso judicial, investigación administrativa o auditoría activa → **congelar eliminación** hasta resolución.

---

## 5. Responsabilidades

| Actor | Rol | Responsabilidades Clave |
|-------|-----|------------------------|
| **IPS (Cliente)** | **Responsable del Tratamiento** | • Definir y aprobar esta política<br>• Garantizar infraestructura, backups, restauración<br>• Designar DPO<br>• Autorizar eliminaciones controladas<br>• Responder ante autoridades (MinSalud, SIC, DIAN) |
| **Representante Legal IPS** | Firma y aprueba política | • Aprobar versiones y vigencia<br>• Responder solidariamente por cumplimiento |
| **DPO IPS** | Velar por Ley 1581/2012 | • Monitorear cumplimiento<br>• Atender consultas/reclamos de titulares (15 días hábiles)<br>• Reportar incidentes de seguridad (72 horas) |
| **Personal Operativo IPS** | Ejecución técnica | • Verificar backups diarios<br>• Ejecutar pruebas de restauración (6 meses)<br>• Reportar fallos de backup |
| **KIN (Proveedor Software)** | **Encargado del Tratamiento** | • Entregar software con capacidades de retención (soft delete, auditoría, exportación)<br>• Proveer scripts de backup/restore<br>• Documentar procedimientos (Fases 6.3a/b/c)<br>• Soporte técnico 24/7 para emergencias<br>• **NO accede a datos de pacientes** (on-premise) |
| **Proveedor Infraestructura (Cloud/NAS)** | Custodia física/lógica | • Garantizar disponibilidad, integridad, cifrado en reposo (AES-256)<br>• SLA de disponibilidad ≥ 99.9% |

---

## 6. Cómo el Software KIN Cumple los Requisitos

| Requisito Legal | Funcionalidad en KIN Medical | Evidencia Técnica |
|-----------------|------------------------------|-------------------|
| **Retención 15 años HCE** (Res. 839/2017) | **Soft Delete universal** — todas las entidades críticas (`patients`, `encounters`, `encounter_evolutions`, `clinical_documents`, `medical_orders`, `diagnoses`, `informed_consents`, `referrals`, `discharge_summaries`) tienen `deleted_at` + `deletion_reason`. **Ningún registro se borra físicamente**. | Código: `BaseEntity.deleted_at`, `BaseEntity.deletion_reason`; Migraciones V75, V77, V78 |
| **Integridad y Disponibilidad** (Res. 1995/1999, Res. 3100/2019) | Base de datos PostgreSQL ACID; Constraints FK, CHECK, UNIQUE; Triggers `updated_at`; Replicación streaming opcional | Migraciones V1–V83; `update_updated_at_column()` trigger |
| **Backup Automático Diario** (Res. 839/2017) | Scripts `backup-postgres.sh/.ps1` con `pg_dump -Fc`, verificación `pg_restore --list`, rotación configurable | `scripts/backup-postgres.sh`, `scripts/backup-postgres.ps1` |
| **Restauración Verificada** (Res. 839/2017) | Scripts `restore-postgres.sh/.ps1` con confirmación, `--clean --if-exists --no-owner`, healthcheck post-restore | `scripts/restore-postgres.sh`, `scripts/restore-postgres.ps1` |
| **Plan de Recuperación (RTO 4h / RPO 24h)** | Documento `procedimiento-recuperacion.md` con pasos Linux/Windows, casos ransomware/migración, healthchecks | `docs/retencion/procedimiento-recuperacion.md` |
| **Pruebas Periódicas** (Res. 839/2017) | Plantilla `plantilla-registro-pruebas.md`, registro obligatorio cada 6 meses | `docs/retencion/plantilla-registro-pruebas.md` |
| **Auditoría de Accesos** (Ley 1581/2012 Art. 17, Res. 3100/2019) | Tabla `audit_logs` inmutable: usuario, acción, recurso, timestamp, IP, metadata JSON | `AuditService`, `AuditLog` entity, triggers inmutables |
| **Trazabilidad Inmutable** | Triggers `created_at`/`updated_at` protegidos; `deleted_at` soft delete; sin `TRUNCATE`/`DROP` en producción | `update_updated_at_column()` function; migraciones V75+ |
| **Exportación de Datos (Habeas Data)** (Ley 1581/2012 Art. 8) | Endpoints `/api/v1/health/data-export`, `/data-rectification`, `/data-deletion` con flujo aprobado | `DataExportRequest`, `DataRectificationRequest`, `DataDeletionRequest` entities |
| **Consentimientos Informados Digitales** (Res. 1995/1999, Res. 839/2017) | Entidad `informed_consents` con firma digital hash, testigos, versión documento, vigencia | `informed_consents` table (Migración V75) |
| **RIPS / FEV / Interoperabilidad** (Res. 2275/2023, Res. 1888/2025) | Generación UBL 2.1 Ext 2 (sector salud), FHIR R4 resources, validación XSD SISPRO | Módulo `billing/rips`, `ubl/`, validadores SISPRO |
| **Cifrado en Reposo y Tránsito** (Ley 1581/2012 Art. 17, Res. 3100/2019) | TLS 1.3 obligatorio; AES-256 en backups externos; secretos en Vault/ENV | Docker TLS, `pg_dump` comprimido, variables de entorno |

---

## 7. Procedimiento Operativo Estándar (POE)

### 7.1 Backup Diario Automático (03:00 AM)

```bash
# Crontab (ejecutado como usuario kin-backup)
0 3 * * * /opt/kin/scripts/backup-postgres.sh >> /var/log/kin-backup.log 2>&1
```

**Parámetros configurables** (variables de entorno):
- `DATABASE_URL` — Connection string PostgreSQL
- `BACKUP_DIR` — `/var/backups/kin` (default)
- `RETENTION_DAYS` — `30` (local), mensuales van a almacenamiento externo

**Verificación automática**: `pg_restore --list` post-dump. Si falla → alerta crítica (Prometheus/Alertmanager).

### 7.2 Backup Mensual de Archivo Histórico (Día 1, 05:00 AM)

```bash
# Copia del backup más reciente del mes a almacenamiento externo (S3/NAS/Blob)
# Cifrado AES-256 obligatorio (GPG / S3 SSE-KMS)
# Retención: 15 años (no eliminar sin autorización)
```

### 7.3 Verificación Semanal de Integridad (Lunes 06:00 AM)

```bash
# 1. Verificar existencia y tamaño de backups últimos 7 días
# 2. pg_restore --list en backup aleatorio de la semana
# 3. Registrar resultado en /var/log/kin-backup-integrity.log
```

### 7.4 Prueba de Restauración Completa (Cada 6 Meses)

**Responsable**: DPO IPS + Soporte KIN  
**Ambiente**: Staging / Preproducción (idéntico a producción)  
**Procedimiento**: Ver `docs/retencion/procedimiento-recuperacion.md` Sección 7  
**Registro obligatorio**: `docs/retencion/pruebas-recuperacion/registro-YYYY-MM-DD.md` usando `plantilla-registro-pruebas.md`

### 7.5 Auditoría Anual de Cumplimiento (Anual, antes de 31-marzo)

| Área | Verificación | Evidencia |
|------|--------------|-----------|
| Retención HCE | Muestreo 50 HC → verificar `deleted_at` NULL, 15 años | Query SQL + captura |
| Backup | Verificar 12 backups mensuales en almacenamiento externo | Lista S3/NAS + checksums |
| Restauración | Prueba completa en staging | Registro `registro-YYYY-MM-DD.md` |
| Auditoría logs | Revisar `audit_logs` completos, sin gaps | Conteo + muestreo |
| Consentimientos | Verificar firmas digitales válidas, vigencia | `informed_consents` query |
| RIPS/FEV | Validar batch reciente contra XSD SISPRO | Resultado validador |

---

## 8. Eliminación Controlada (Posterior a 15 Años)

**PRINCIPIO**: La eliminación **NUNCA** es automática. Requiere proceso formal documentado.

### 8.1 Condiciones Previas (Todas Obligatorias)

1. ✅ Transcurridos **15 años** desde la última atención del paciente (Res. 839/2017)
2. ✅ **No existe** proceso judicial, administrativo, disciplinario o investigación en curso relacionado con el paciente
3. ✅ **No existen** glosas, reclamaciones, conciliaciones pendientes con EPS/aseguradores
4. ✅ **No existe** solicitud de acceso, rectificación o supresión pendiente del titular (Ley 1581/2012)
5. ✅ **Autorización escrita** del Comité de Historias Clínicas de la IPS (Acta firmada)
6. ✅ **Autorización del DPO** IPS (firma en acta)

### 8.2 Procedimiento de Eliminación

1. Generar **listado de candidatos** (query SQL: pacientes con última atención > 15 años, sin alertas)
2. **Verificar individualmente** cada candidato contra condiciones 8.1
3. **Acta de Eliminación** firmada por: Presidente Comité HCE + DPO + Representante Legal
4. **Ejecución técnica** (solo DBA autorizado):
   ```sql
   -- SOLO tras acta firmada y verificación triple
   UPDATE patients SET deleted_at = NOW(), deletion_reason = 'Eliminación controlada 15 años - Acta N° XXX/202X' WHERE id = '...';
   -- Repetir para tablas relacionadas (evoluciones, documentos, órdenes, etc.)
   ```
5. **Registro en Libro de Eliminaciones** (físico y digital): fecha, acta, responsable, cantidad registros
6. **Certificado de Destrucción** (si aplica medio físico/notarial)

### 8.3 Prohibiciones Absolutas

- ❌ Eliminación masiva sin acta individualizada
- ❌ Eliminación antes de 15 años (salvo orden judicial expresa)
- ❌ `TRUNCATE`, `DROP TABLE`, `DELETE` físico en tablas clínicas
- ❌ Eliminación durante auditoría, investigación o litigio activo

---

## 9. Auditoría y Cumplimiento — Evidencia para Inspección

### 9.1 Documentos a Presentar en Auditoría (MinSalud, SIC, DIAN, Secretaría de Salud)

| Documento | Frecuencia | Ubicación |
|-----------|------------|-----------|
| Esta Política (versión vigente, firmada) | Siempre | `docs/retencion/politica-retencion-backup.md` |
| Procedimiento de Recuperación | Siempre | `docs/retencion/procedimiento-recuperacion.md` |
| Registro de Pruebas de Restauración (últimas 3) | Semestral | `docs/retencion/pruebas-recuperacion/registro-*.md` |
| Actas de Comité HCE (autorizaciones eliminación) | Según ocurrencia | Archivo IPS / Digital |
| Logs de Backup (últimos 12 meses) | Continua | `/var/log/kin-backup.log`, `/var/log/kin-backup-integrity.log` |
| Logs de Auditoría (últimos 5 años) | Continua | Tabla `audit_logs` (exportable CSV/JSON) |
| Certificados de Cifrado Backups Externos | Anual | Proveedor S3/NAS |
| Actas de Comité HCE (autorizaciones eliminación) | Según ocurrencia | Archivo IPS |
| Designación DPO (Ley 1581/2012 Art. 21) | Vigente | Archivo IPS / Publicado web IPS |
| Procedimiento Atención Consultas/Reclamos (Ley 1581 Art. 15) | Vigente | Publicado web IPS |

### 9.2 Consultas y Reclamos de Titulares (Ley 1581/2012, Art. 14–15)

- **Canal**: Email DPO / Formulario web IPS / Escrito físico
- **Plazo respuesta**: **15 días hábiles** (prorrogable 8 días más)
- **Contenido**: Confirmar existencia de datos, finalidad, origen, destinatarios, derechos (acceso, rectificación, supresión, revocatoria)
- **Registro**: En `audit_logs` con acción `DATA_ACCESS_REQUEST` / `DATA_RECTIFICATION_REQUEST` / `DATA_DELETION_REQUEST`

### 9.3 Notificación de Incidentes de Seguridad (Ley 1581/2012, Art. 17; Res. 3100/2019)

| Nivel | Plazo Notificación | Destinatario |
|-------|-------------------|--------------|
| **Crítico** (fuga masiva, ransomware confirmado) | **72 horas** | SIC (Superintendencia de Industria y Comercio), MinSalud, Titulares afectados |
| **Alto** (acceso no autorizado, pérdida parcial) | **5 días hábiles** | SIC, DPO IPS |
| **Medio/Bajo** (intento fallido, anomalía) | **Mensual (informe consolidado)** | DPO IPS, Comité Seguridad |

---

## 10. Firmas y Aprobación

| Rol | Nombre | Cargo | Firma | Fecha |
|-----|--------|-------|-------|-------|
| **Representante Legal IPS** | _________________________ | Representante Legal | _________________________ | ____/____/______ |
| **DPO IPS** | _________________________ | Delegado Protección Datos | _________________________ | ____/____/______ |
| **Director Científico / Médico** | _________________________ | Dir. Científico | _________________________ | ____/____/______ |
| **Gerente TI / Sistemas IPS** | _________________________ | Gerente TI | _________________________ | ____/____/______ |

---

## 11. Historial de Versiones

| Versión | Fecha | Autor | Cambios Principales | Aprobado Por |
|---------|-------|-------|---------------------|--------------|
| 1.0 | 2026-10-11 | Equipo KIN | Versión inicial (borrador backup) | — |
| 2.0 | 2026-10-11 | Equipo KIN | Política formal completa: marco legal 7 normas, definiciones, plazos, modalidades, responsabilidades, cumplimiento KIN, POE, eliminación controlada, auditoría, firmas | [Pendiente] |

---

## Anexos Referenciados (Documentos Separados)

1. **Anexo A** — `procedimiento-recuperacion.md` (Plan de Recuperación ante Desastres)
2. **Anexo B** — `plantilla-registro-pruebas.md` (Plantilla Pruebas de Restauración)
3. **Anexo C** — `scripts/backup-postgres.sh` / `.ps1` (Scripts Backup)
4. **Anexo D** — `scripts/restore-postgres.sh` / `.ps1` (Scripts Restauración)
5. **Anexo E** — `scripts/crontab.example` (Configuración Cron)
6. **Anexo F** — `procedimiento-recuperacion.md` Sección 9 (Contactos Escalamiento)