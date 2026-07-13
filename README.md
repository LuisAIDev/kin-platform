<div align="center">

# 🌱 KIN Platform

### Knowledge, Innovation & Networking

**Plataforma de gestión y validación de proyectos con asistencia de IA integrada**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16-000000?style=flat&logo=next.js&logoColor=white)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=flat&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[![GitHub](https://img.shields.io/badge/GitHub-LuisAIDev-181717?style=flat&logo=github&logoColor=white)](https://github.com/LuisAIDev)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Conectemos-0A66C2?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/luis-orlando-guerra-gonzalez-49aa30244)

[Características](#-características) •
[Arquitectura](#-arquitectura) •
[Stack Tecnológico](#-stack-tecnológico) •
[Instalación](#-instalación-local) •
[API](#-api-endpoints) •
[Roadmap](#-roadmap)

</div>

---

## 📋 Sobre el proyecto

**KIN** es una plataforma full-stack para la gestión y validación de proyectos, diseñada para acompañar al usuario desde la idea inicial hasta la estructuración de un proyecto viable, con un asistente de IA integrado que guía la conversación, evalúa la información y aporta contexto en tiempo real.

El proyecto fue construido desde cero como ejercicio de portafolio para consolidar un stack backend enterprise (**Java + Spring Boot**) combinado con un frontend moderno (**Next.js + TypeScript**), aplicando arquitectura en capas, autenticación stateless con JWT, y buenas prácticas de seguridad y despliegue con Docker.

> 🧪 **Estado actual:** cobertura completa de tests unitarios (36 tests, 0 fallos). Seguridad reforzada con rate limiting y headers HTTP. Diseño responsive mobile-first verificado en rango amplio de dispositivos. El despliegue en producción sigue pendiente como siguiente fase (ver [Roadmap](#-roadmap)).

---

## ✨ Características

- 🔐 **Autenticación segura** con JWT y contraseñas cifradas con BCrypt, sesión stateless
- 📁 **Gestión de proyectos** — creación, edición, listado y eliminación (CRUD completo)
- 🤖 **Chat con IA integrado** por proyecto, con historial persistente y metadata estructurada
- 📊 **Score de viabilidad** generado por IA para cada proyecto
- 🎭 **Roles de usuario** diferenciados: `FREE`, `PREMIUM`, `FACILITADOR`, `ADMIN`
- 🐳 **Contenerizado con Docker** — backend, frontend y base de datos orquestados con Docker Compose
- 🔄 **Doble entorno de base de datos** — H2 embebida para desarrollo ágil, PostgreSQL para producción
- 🛡️ **Gestión de secretos** vía variables de entorno, sin credenciales expuestas en el código
- 🚦 **Rate limiting** en endpoints de autenticación para protección contra fuerza bruta
- 🛡️ **Headers de seguridad HTTP** (CSP, X-Frame-Options, HSTS, X-Content-Type-Options)
- ❤️ **Health check endpoint** vía Spring Boot Actuator
- 🔧 **Panel de administración** para gestionar planes de precios (rol `ADMIN`)
- 📄 **Paginación de proyectos** con respuesta paginada estándar
- 📱 **Diseño responsive mobile-first**, verificado en rango amplio de dispositivos reales

---

## 🏗️ Arquitectura

El backend sigue una arquitectura en capas clásica de aplicaciones enterprise Java:

```
┌─────────────────────────────────────────────────────┐
│                     FRONTEND                         │
│         Next.js 16 (App Router) + TypeScript          │
│              Tailwind CSS · fetch nativo               │
└───────────────────────┬─────────────────────────────┘
                         │  REST API (JWT Bearer)
┌───────────────────────▼─────────────────────────────┐
│                     BACKEND                           │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │
│  │  Controller  │→│   Service    │→│  Repository   │  │
│  │  (REST API)  │  │  (Lógica de  │  │ (Spring Data │  │
│  │              │  │   negocio)   │  │     JPA)      │  │
│  └─────────────┘  └─────────────┘  └──────┬───────┘  │
│         ↑                                    │        │
│    JwtAuthenticationFilter              Hibernate      │
│    (Spring Security)                                   │
└──────────────────────────────────────────────┼────────┘
                                                 │
                          ┌──────────────────────▼──────────────────────┐
                          │   H2 (dev, file-based)  /  PostgreSQL 16     │
                          │              (producción, Docker)            │
                          └───────────────────────────────────────────────┘
```

**Módulos del backend:** `auth`, `user`, `project`, `chat`, `ai` — cada uno con su propio Controller, Service, DTOs y (donde aplica) Repository, siguiendo separación de responsabilidades.

**Módulos del frontend (`services/`):**

```
session.ts   → sin dependencias · maneja token, sesión y logout forzado
    ↑
  api.ts     → cliente HTTP con manejo automático de 401/403
    ↑
 auth.ts / chat.ts / projects.ts  → servicios por dominio
```

---

## 🛠️ Stack Tecnológico

### Backend

| Categoría | Tecnología |
|---|---|
| Lenguaje / Runtime | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Seguridad | Spring Security + JWT (autenticación stateless), BCrypt |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos (dev) | H2 (file-based, embebida) |
| Base de datos (prod) | PostgreSQL 16 |
| IA | Integración con motor local (Ollama / Llama 3.2) |
| Testing | JUnit 5, Mockito, Reactor Test |
| Monitoreo | Spring Boot Actuator |
| Build | Maven (Maven Wrapper) |

### Frontend

| Categoría | Tecnología |
|---|---|
| Framework | Next.js 16 (App Router) |
| Librería UI | React 19 |
| Lenguaje | TypeScript 5 (`strict` mode) |
| Estilos | Tailwind CSS 4 |
| Cliente HTTP | `fetch` nativo con wrapper propio |

### Infraestructura

| Categoría | Tecnología |
|---|---|
| Contenedores | Docker + Docker Compose |
| Orquestación | 3 servicios: `postgres-db`, `kin-backend`, `kin-frontend` |

---

## 📁 Estructura del proyecto

```
proyecto-kin/
├── kin-backend/            # API REST — Spring Boot
│   └── src/main/java/com/kinplatform/
│       ├── auth/            # Autenticación y JWT
│       ├── user/            # Entidad y roles de usuario
│       ├── project/         # CRUD de proyectos
│       ├── chat/            # Chat IA y persistencia de mensajes
│       ├── ai/               # Orquestación del motor de IA
│       └── common/          # Configuración, seguridad, excepciones
├── kin-frontend/           # Cliente — Next.js + TypeScript
│   └── src/
│       ├── app/              # Rutas (App Router): auth, dashboard
│       ├── components/      # Componentes reutilizables
│       └── services/         # Capa de comunicación con la API
├── kin-database/            # Scripts de inicialización PostgreSQL
├── docker-compose.yml       # Orquestación de los 3 servicios
└── .env.example              # Variables de entorno documentadas
```

---

## 🚀 Instalación local

### Requisitos previos

- Java 17+
- Node.js 20+
- Maven (o usar el wrapper incluido `mvnw`)
- (Opcional) Docker y Docker Compose para el entorno productivo

### 1. Clonar el repositorio

```bash
git clone https://github.com/LuisAIDev/kin-platform.git
cd kin-platform
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
```

Completa `.env` con tus propios valores (JWT secret, credenciales de base de datos, API key del motor de IA si aplica).

### 3. Levantar el backend

```bash
cd kin-backend
./mvnw spring-boot:run
```

El backend queda disponible en `http://localhost:8080/api/v1`, usando H2 como base de datos local (no requiere instalación adicional).

### 4. Levantar el frontend

En otra terminal:

```bash
cd kin-frontend
npm install
npm run dev
```

El frontend queda disponible en `http://localhost:3000`.

### 5. (Alternativa) Levantar todo con Docker

```bash
docker compose up --build
```

Esto orquesta PostgreSQL, backend y frontend en contenedores usando las variables definidas en `.env`.

---

## 🔌 API Endpoints

| Endpoint | Método | Auth | Descripción |
|---|---|---|---|
| `/auth/register` | `POST` | No | Registro de nuevo usuario |
| `/auth/login` | `POST` | No | Inicio de sesión, devuelve JWT |
| `/auth/me` | `GET` | Bearer JWT | Datos del usuario autenticado |
| `/projects` | `GET` / `POST` | Bearer JWT | Listar / crear proyectos |
| `/projects/{id}` | `GET` / `PUT` / `DELETE` | Bearer JWT | CRUD de un proyecto específico |
| `/projects/{id}/chat` | `POST` | Bearer JWT | Enviar mensaje al asistente IA |
| `/projects/{id}/messages` | `GET` / `DELETE` | Bearer JWT | Historial / limpieza de mensajes |

---

## 🧪 Testing

### Cómo ejecutar los tests

```bash
cd kin-backend
./mvnw test
```

### Resumen

**36 tests unitarios, 0 fallos**, cubriendo las siguientes áreas del backend:

| Módulo | Tests | Cobertura principal |
|---|---|---|
| `auth` | 5 | Registro, login, cambio de contraseña |
| `common.security` | 10 | Filtro JWT (5) + Rate Limiting (5) |
| `project` | 7 | CRUD completo, paginación, validación de propietario |
| `pricing` | 4 | CRUD de planes de precios, control de acceso por rol |
| `ai` | 5 | Motor de IA (Ollama), fallback mock, streaming reactivo |
| `chat` | 5 | Orquestación del chat con streaming SSE (`SseEmitter`) |

---

## 📸 Capturas de pantalla

> _Próximamente — capturas del login, dashboard de proyectos y chat con IA._

---

## 🗺️ Roadmap

- [x] Autenticación JWT con roles de usuario
- [x] CRUD completo de proyectos
- [x] Chat con IA integrado y persistencia de historial
- [x] Contenerización con Docker Compose
- [ ] Despliegue en producción (backend en Render/Railway, frontend en Vercel)
- [ ] Migraciones versionadas con Flyway
- [x] Cobertura de tests unitarios e integración (JUnit + Mockito + Reactor Test)
- [x] Generación de reportes PDF de viabilidad
- [x] Panel de administración para rol `ADMIN`

---

## 👤 Autor

**Luis Orlando Guerra González**
Desarrollador full-stack en formación — SENA
📍 Cartagena, Colombia (trabajo remoto)

[![GitHub](https://img.shields.io/badge/GitHub-LuisAIDev-181717?style=flat&logo=github&logoColor=white)](https://github.com/LuisAIDev)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Luis%20Orlando%20Guerra-0A66C2?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/luis-orlando-guerra-gonzalez-49aa30244)

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.

<div align="center">

⭐ Si este proyecto te resulta interesante, considera darle una estrella en GitHub

</div>
