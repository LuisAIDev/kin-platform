# KIN — Instalación On-Premise

## Requisitos
- Docker + Docker Compose
- 4 GB RAM mínimo, 20 GB disco
- Puerto 8080 disponible

## Instalación
1. Copiar `.env.example` a `.env`
2. Editar `.env` con sus credenciales
3. Ejecutar: `docker-compose -f docker-compose.onpremise.yml up -d`
4. Verificar: http://localhost:8080/api/v1/actuator/health

## Productos
- `KIN_PRODUCT=medical` → HCE, RIPS, MIPRES, triage, telemedicina
- `KIN_PRODUCT=platform` → Pipeline, Enterprise, Chat, Proyectos
- `KIN_PRODUCT=all` → Ambos módulos