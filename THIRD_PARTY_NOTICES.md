# THIRD PARTY NOTICES — KIN Platform

Este documento registra la atribución de los **componentes de terceros** utilizados por KIN.
KIN **no reclama propiedad** sobre ninguno de ellos: todos pertenecen a sus respectivos autores
y se distribuyen bajo sus propias licencias.

Las versiones listadas son las declaradas en `kin-backend/pom.xml`,
`kin-frontend/package.json` y la configuración del repositorio en el momento de este inventario.
Las versiones gestionadas por Spring Boot (parent `spring-boot-starter-parent 3.2.5`) no se
repiten aquí salvo cuando se fijan explícitamente.

> Este documento es de **atribución documental** y no sustituye los textos de licencia de cada
> componente. Para redistribución, consultar la licencia concreta de cada dependencia.

---

## BACKEND

Dependencias principales de `kin-backend/pom.xml`:

| Componente | Versión | Licencia | Finalidad | Fuente de licencia |
|---|---|---|---|---|
| Spring Boot (parent + starters web, data-jpa, validation, security, actuator, data-redis, test) | 3.2.5 | Apache-2.0 | Framework backend, seguridad, JPA, health/metrics, Redis | https://github.com/spring-projects/spring-boot |
| Spring AI (`spring-ai-openai`, `spring-ai-client-chat`, `spring-ai-retry`) | 1.1.7 | Apache-2.0 | Proveedor LLM (DeepSeek, modelo OpenAI-compatible) | https://github.com/spring-projects/spring-ai |
| PostgreSQL JDBC Driver (`org.postgresql:postgresql`) | gestionada por Spring Boot | BSD-2-Clause (PostgreSQL License) | Conexión a PostgreSQL/Neon | https://jdbc.postgresql.org/ |
| JJWT (`io.jsonwebtoken:jjwt-*`) | 0.12.5 | Apache-2.0 | Emisión/validación de tokens JWT | https://github.com/jwtk/jjwt |
| Micrometer Registry Prometheus | gestionada por Spring Boot | Apache-2.0 | Métricas `/actuator/prometheus` | https://micrometer.io/ |
| Logstash Logback Encoder | 7.4 | Apache-2.0 | Logs estructurados JSON | https://github.com/logfellow/logstash-logback-encoder |
| SpotBugs Annotations | 4.8.3 | LGPL-2.1-only | Anotaciones para análisis estático | https://github.com/spotbugs/spotbugs |
| Springdoc OpenAPI Starter WebMVC UI | 2.5.0 | Apache-2.0 | Documentación OpenAPI de la API REST | https://springdoc.org/ |
| Stripe Java SDK (`com.stripe:stripe-java`) | 24.0.0 | MIT | Integración de pagos Stripe | https://github.com/stripe/stripe-java |
| Flyway Core + Flyway Database PostgreSQL | 11.20.3 | Apache-2.0 | Migraciones de base de datos | https://github.com/flyway/flyway |
| Lombok (`org.projectlombok:lombok`) | gestionada por Spring Boot | MIT | Reducción de boilerplate en Java | https://projectlombok.org/ |
| springboot3-dotenv (`me.paulschwarz`) | 5.1.0 | MIT | Carga de variables de entorno desde `.env` | https://github.com/paulschwarz/springboot3-dotenv |
| Reactor Test (`io.projectreactor:reactor-test`) | gestionada por Spring Boot | Apache-2.0 | Tests de flujos reactivos | https://github.com/reactor/reactor-core |
| Testcontainers (`junit-jupiter`, `postgresql`) | gestionada por Spring Boot (1.19.7) | MIT | PostgreSQL real en tests de integración | https://github.com/testcontainers/testcontainers-java |

---

## FRONTEND

Dependencias de `kin-frontend/package.json`:

| Componente | Versión | Licencia | Finalidad | Fuente de licencia |
|---|---|---|---|---|
| Next.js | 16.2.9 | MIT | Framework React (App Router) | https://github.com/vercel/next.js |
| React | 19.2.4 | MIT | Librería UI | https://github.com/facebook/react |
| React DOM | 19.2.4 | MIT | Renderizado de React en el navegador | https://github.com/facebook/react |
| jsPDF | 4.2.1 | MIT | Generación de PDF (exportación de reportes) | https://github.com/parallax/jsPDF |
| Tailwind CSS | 4 | MIT | Estilos utilitarios | https://github.com/tailwindlabs/tailwindcss |
| TypeScript | 5 | Apache-2.0 | Tipado del frontend | https://github.com/microsoft/TypeScript |

---

## TESTING (FRONTEND)

Dependencias de desarrollo de `kin-frontend/package.json`:

| Componente | Versión | Licencia | Finalidad | Fuente de licencia |
|---|---|---|---|---|
| Vitest | 3.1.0 | MIT | Framework de tests unitarios | https://github.com/vitest-dev/vitest |
| @vitest/coverage-v8 | 3.2.7 | MIT | Cobertura de código (V8) | https://github.com/vitest-dev/vitest |
| @vitejs/plugin-react | 4.4.1 | MIT | Plugin de Vite para React | https://github.com/vitejs/vite-plugin-react |
| jsdom | 26.1.0 | MIT | Entorno DOM para tests | https://github.com/jsdom/jsdom |
| Playwright (`@playwright/test`) | 1.61.1 | Apache-2.0 | Tests E2E | https://github.com/microsoft/playwright |
| @testing-library/react | 16.3.0 | MIT | Testing de componentes React | https://github.com/testing-library/react-testing-library |
| @testing-library/jest-dom | 6.6.3 | MIT | Matchers de DOM para testing | https://github.com/testing-library/jest-dom |
| @testing-library/user-event | 14.6.1 | MIT | Interacción de usuario simulada | https://github.com/testing-library/user-event |
| ESLint | 9 | MIT | Linting del frontend | https://github.com/eslint/eslint |
| eslint-config-next | 16.2.9 | MIT | Configuración ESLint de Next.js | https://github.com/vercel/next.js |
| @tailwindcss/postcss | 4 | MIT | Integración PostCSS de Tailwind | https://github.com/tailwindlabs/tailwindcss |
| @types/node, @types/react, @types/react-dom | 20 / 19 | MIT | Tipos de TypeScript (DefinitelyTyped) | https://github.com/DefinitelyTyped/DefinitelyTyped |

> `package-lock.json` contiene las licencias declaradas por cada dependencia transitiva
> (MIT, Apache-2.0, ISC, MIT-0, BSD, entre otras), incluidas sus notificaciones originales.

---

## INFRAESTRUCTURA

| Componente | Licencia | Finalidad | Fuente de licencia |
|---|---|---|---|
| PostgreSQL | PostgreSQL License (BSD-style) | Base de datos relacional (dev/prod) | https://www.postgresql.org/about/licence/ |
| Redis | BSD-3-Clause | Caché distribuida opcional (`RedisKnowledgeRepository`, deshabilitada por defecto) | https://redis.io/ |
| Docker Engine / Compose | Apache-2.0 | Contenerización de la plataforma | https://github.com/docker |
| Imágenes base oficiales (eclipse-temurin, node) | Según la licencia de cada proyecto | Imágenes base de los contenedores | https://adoptium.net/ · https://nodejs.org/ |
| Flyway | Apache-2.0 | Migraciones de esquema (ver BACKEND) | https://github.com/flyway/flyway |
| Render / Vercel / Neon | Términos de servicio de cada plataforma | Despliegue y base de datos en la nube | https://render.com/ · https://vercel.com/ · https://neon.tech/ |

---

## OTRAS HERRAMIENTAS (desarrollo y CI/CD)

| Componente | Versión | Licencia | Finalidad | Fuente de licencia |
|---|---|---|---|---|
| Maven | — | Apache-2.0 | Build del backend | https://maven.apache.org/ |
| JaCoCo (`org.jacoco:jacoco-maven-plugin`) | 0.8.12 | EPL (Eclipse Public License) | Cobertura de código | https://www.jacoco.org/ |
| Spotless Maven Plugin | 2.43.0 | Apache-2.0 | Formato de código | https://github.com/diffplug/spotless |
| Maven Checkstyle Plugin | 3.3.1 | LGPL-2.1-only | Estilo de código | https://maven.apache.org/plugins/maven-checkstyle-plugin/ |
| Maven PMD Plugin | 3.21.2 | BSD-3-Clause | Análisis estático | https://maven.apache.org/plugins/maven-pmd-plugin/ |
| SpotBugs Maven Plugin | 4.8.5.0 | LGPL-2.1-only | Análisis estático de bytecode | https://github.com/spotbugs/spotbugs-maven-plugin |
| OWASP Dependency-Check Maven Plugin | 9.1.0 | Apache-2.0 | Escaneo de vulnerabilidades (perfil `-Powasp`) | https://github.com/jeremylong/DependencyCheck |
| SonarQube / Sonar Scanner | — | LGPL-3.0 | Quality gate (cobertura, duplicación) | https://www.sonarsource.com/ |
| CodeQL (GitHub Actions) | — | GitHub: términos de uso | Análisis de seguridad del código | https://codeql.github.com/ |
| Gitleaks | — | MIT | Detección de secretos en CI | https://github.com/gitleaks/gitleaks |
| Renovate | — | AGPL-3.0 | Actualización automática de dependencias | https://github.com/renovatebot/renovate |
| Dependabot | — | GitHub: términos de servicio | Actualización automática de dependencias | https://github.com/dependabot |

---

## NOTAS

- Las **herramientas de CI/CD** (SonarQube, CodeQL, Gitleaks, Renovate, Dependabot) se utilizan
  como servicios/herramientas en el pipeline; no se distribuyen como parte del producto.
- Los **servicios en la nube** (Render, Vercel, Neon, GitHub Actions) se rigen por los términos
  de servicio de cada proveedor.
- Este inventario se basa en las declaraciones del repositorio y puede requerir actualización al
  cambiar versiones o añadir dependencias.
