/**
 * Configuración centralizada del módulo "Sobre KIN".
 *
 * Cada tecnología, capa de arquitectura y enlace externo listado aquí fue
 * verificado contra el código real del proyecto (frontend y backend). No se
 * agregan afirmaciones que no puedan demostrarse con la implementación actual.
 * Para mantener el módulo actualizado basta con editar este archivo.
 */

export type CategoryIconName =
  | "code"
  | "server"
  | "database"
  | "chip"
  | "shield"
  | "cloud"
  | "document"
  | "lock"
  | "test"
  | "layers"
  | "search"
  | "trend";

export interface TechItem {
  name: string;
  description: string;
}

export interface TechCategory {
  id: string;
  title: string;
  description: string;
  icon: CategoryIconName;
  items: TechItem[];
}

export interface ArchitectureLayer {
  label: string;
  description: string;
}

export interface ExternalLink {
  label: string;
  href: string;
}

export interface AudienceItem {
  title: string;
  description: string;
}

export interface ShowcaseGroup {
  id: string;
  title: string;
  description: string;
  icon: CategoryIconName;
  items: TechItem[];
  /** Métricas verificadas de QA (opcional). */
  metrics?: {
    frontendTests: number;
    testFiles: number;
    e2e: string;
  };
}

export interface DemonstrateCard {
  id: string;
  icon: CategoryIconName;
  title: string;
  description: string;
}

export interface ArchitectureHubNode {
  label: string;
  description: string;
}

export interface ArchitectureHub {
  root: ArchitectureHubNode;
  branches: ArchitectureHubNode[];
  base: ArchitectureHubNode;
  deploy: ArchitectureHubNode;
}

/**
 * Métricas de QA verificadas en la última ejecución local (`npm test` y
 * `npx playwright test tests/sobre-kin.spec.ts`). Actualizar este bloque
 * cuando cambien los resultados de la suite.
 */
export const QA_METRICS = {
  frontendTests: 399,
  testFiles: 74,
  e2e: "4/4",
};

export const ABOUT_LINKS = {
  /** Repositorio público de KIN (referenciado en render.yaml y en el README del proyecto). */
  github: { label: "GitHub", href: "https://github.com/LuisAIDev/kin-platform" },
  /** Perfil de LinkedIn del creador (referenciado en el README del proyecto). */
  linkedin: {
    label: "LinkedIn",
    href: "https://www.linkedin.com/in/luis-orlando-guerra-gonzalez-49aa30244",
  },
};

const TECH_CATEGORIES: TechCategory[] = [
  {
    id: "desarrollo",
    title: "Desarrollo",
    description: "Lenguajes y frameworks con los que se construye KIN.",
    icon: "code",
    items: [
      { name: "React", description: "Interfaces de usuario componentizadas." },
      { name: "Next.js", description: "Framework full-stack del frontend (App Router)." },
      { name: "TypeScript", description: "Tipado estático estricto en todo el frontend." },
      { name: "Java", description: "Lenguaje principal del backend (Java 17)." },
      { name: "Spring Boot", description: "Framework del backend: REST, seguridad y persistencia." },
      { name: "Node.js", description: "Runtime del tooling y del build del frontend." },
    ],
  },
  {
    id: "backend",
    title: "Backend",
    description: "Servicios y API REST que soportan la plataforma.",
    icon: "server",
    items: [
      { name: "APIs REST", description: "Endpoints bajo /api/v1 consumidos por el frontend." },
      { name: "Spring Security", description: "Autenticación y autorización de la API." },
      { name: "JWT", description: "Sesiones stateless firmadas (jjwt)." },
      { name: "JPA / Hibernate", description: "Persistencia orientada a objetos sobre PostgreSQL." },
      { name: "Arquitectura por capas", description: "Controller → Service → Port/Adapter + dominio puro." },
      { name: "Manejo global de excepciones", description: "GlobalExceptionHandler (@RestControllerAdvice)." },
    ],
  },
  {
    id: "datos",
    title: "Datos",
    description: "Persistencia y gestión de la información de los proyectos.",
    icon: "database",
    items: [
      { name: "PostgreSQL", description: "Base de datos relacional en dev, test y producción." },
      { name: "Migraciones de base de datos", description: "Flyway versiona el esquema (V1…V29)." },
      { name: "Persistencia", description: "Entidades JPA: usuarios, proyectos, reportes, suscripciones." },
      { name: "Datos estructurados del proyecto", description: "Información del proyecto por secciones (project_info)." },
    ],
  },
  {
    id: "inteligencia-artificial",
    title: "Inteligencia artificial",
    description: "Integración real de IA que usa KIN hoy.",
    icon: "chip",
    items: [
      { name: "DeepSeek", description: "Proveedor de IA actual (vía API compatible con OpenAI)." },
      { name: "Spring AI", description: "Integración del modelo con el backend." },
      { name: "Orquestación por etapas", description: "Pipeline de 16 etapas: análisis, conocimiento, scoring, reporte y conversación." },
      { name: "Contexto durable", description: "El contexto del proyecto persiste entre turnos." },
    ],
  },
  {
    id: "calidad",
    title: "Calidad",
    description: "Pruebas automatizadas y verificación del software.",
    icon: "shield",
    items: [
      { name: "Testing automatizado", description: "JUnit 5 + Mockito (backend) y Vitest (frontend)." },
      { name: "Vitest", description: "Tests unitarios del frontend con Testing Library." },
      { name: "Playwright", description: "Tests E2E del flujo completo (login, dashboard, proyectos)." },
      { name: "Pruebas de integración de API", description: "Testcontainers sobre PostgreSQL real." },
      { name: "Cobertura de código", description: "JaCoCo con mínimo 90 % en los módulos de dominio." },
    ],
  },
  {
    id: "devops-cloud",
    title: "DevOps / Cloud",
    description: "Construcción, versionado y despliegue de KIN.",
    icon: "cloud",
    items: [
      { name: "Docker", description: "Contenedores para PostgreSQL, backend y frontend." },
      { name: "Git", description: "Control de versiones del proyecto." },
      { name: "GitHub", description: "Repositorio público y despliegue (LuisAIDev/kin-platform)." },
      { name: "Render", description: "Despliegue en la nube del backend y del frontend." },
    ],
  },
  {
    id: "documentos-exportacion",
    title: "Documentos y exportación",
    description: "Procesamiento de documentos y exportación de reportes.",
    icon: "document",
    items: [
      { name: "Procesamiento de documentos", description: "Carga y análisis de PDF, DOCX y XLSX." },
      { name: "Extracción de texto", description: "Texto extraído de documentos para el proyecto." },
      { name: "Generación de PDF", description: "Reportes exportables a PDF (OpenPDF y jsPDF)." },
      { name: "Generación de DOCX", description: "Exportación del proyecto a Word." },
      { name: "Exportación Markdown", description: "Exportación del proyecto a Markdown." },
    ],
  },
];

const SHOWCASE_GROUPS: ShowcaseGroup[] = [
  {
    id: "frontend",
    title: "Frontend",
    description: "La interfaz de usuario de KIN.",
    icon: "code",
    items: [
      { name: "Next.js", description: "App Router, rutas estáticas y renderizado del lado del servidor." },
      { name: "React", description: "Interfaces de usuario por componentes." },
      { name: "TypeScript", description: "Tipado estático estricto en todo el frontend." },
      { name: "Tailwind CSS", description: "Sistema de estilos del frontend." },
      { name: "Playwright", description: "Pruebas E2E del flujo completo." },
      { name: "Vitest", description: "Pruebas unitarias con Testing Library." },
      { name: "Node.js", description: "Runtime del tooling y del build." },
    ],
  },
  {
    id: "backend",
    title: "Backend",
    description: "Servicios y API REST de la plataforma.",
    icon: "server",
    items: [
      { name: "Java", description: "Lenguaje principal del backend (Java 17)." },
      { name: "Spring Boot", description: "Framework REST, seguridad y persistencia." },
      { name: "Spring Security", description: "Autenticación y autorización de la API." },
      { name: "JPA / Hibernate", description: "Persistencia orientada a objetos." },
      { name: "PostgreSQL", description: "Base de datos relacional." },
      { name: "Flyway", description: "Migraciones versionadas del esquema (V1…V29)." },
      { name: "JWT", description: "Sesiones stateless firmadas (jjwt)." },
    ],
  },
  {
    id: "ia",
    title: "Inteligencia artificial",
    description: "La integración de IA real de KIN.",
    icon: "chip",
    items: [
      { name: "DeepSeek", description: "Proveedor de IA actual (vía API compatible con OpenAI)." },
      { name: "Spring AI", description: "Integración del modelo con el backend." },
      { name: "Orquestación por etapas", description: "Pipeline de 16 etapas que analiza, evalúa y genera el reporte." },
      { name: "Procesamiento de contexto", description: "Contexto durable del proyecto entre turnos de conversación." },
    ],
  },
  {
    id: "qa",
    title: "QA / Testing",
    description: "Validación automatizada y prevención de regresiones.",
    icon: "test",
    items: [
      { name: "Tests unitarios", description: "JUnit 5 + Mockito (backend) y Vitest (frontend)." },
      { name: "Tests de integración", description: "Testcontainers sobre PostgreSQL real." },
      { name: "Pruebas E2E", description: "Playwright sobre login, dashboard y proyectos." },
      { name: "Validación de builds", description: "next build y mvn verify en CI." },
      { name: "Lint", description: "ESLint (frontend) y Checkstyle/SpotBugs/PMD (backend)." },
      { name: "Pruebas de regresión", description: "Suite completa ejecutada en cada iteración." },
    ],
    metrics: QA_METRICS,
  },
  {
    id: "seguridad",
    title: "Seguridad",
    description: "Protección de recursos y manejo de errores.",
    icon: "lock",
    items: [
      { name: "JWT", description: "Tokens firmados para sesiones stateless." },
      { name: "Spring Security", description: "Cadena de filtros y protección de endpoints." },
      { name: "Ownership de proyectos", description: "Acceso controlado por propietario del proyecto." },
      { name: "Autorización", description: "Roles de usuario (FREE, PREMIUM, FACILITADOR, ADMIN)." },
      { name: "Validación de acceso", description: "Verificación de sesión en rutas protegidas." },
      { name: "Manejo de errores", description: "GlobalExceptionHandler con respuestas consistentes." },
    ],
  },
  {
    id: "cloud",
    title: "Cloud / DevOps",
    description: "Despliegue y operación de la aplicación.",
    icon: "cloud",
    items: [
      { name: "Docker", description: "PostgreSQL, backend y frontend en contenedores (docker-compose)." },
      { name: "GitHub Actions", description: "CI/CD: lint, tests, build y E2E en cada push y PR." },
      { name: "Render", description: "Despliegue en la nube del backend y del frontend." },
      { name: "PostgreSQL", description: "Base de datos gestionada en el despliegue." },
    ],
  },
];

const DEMONSTRATE_CARDS: DemonstrateCard[] = [
  {
    id: "analisis",
    icon: "search",
    title: "Análisis",
    description: "Convertir una necesidad real en componentes y funcionalidades.",
  },
  {
    id: "arquitectura",
    icon: "layers",
    title: "Arquitectura",
    description: "Separar responsabilidades y construir módulos mantenibles.",
  },
  {
    id: "desarrollo",
    icon: "code",
    title: "Desarrollo",
    description: "Construcción full-stack de funcionalidades reales.",
  },
  {
    id: "ia",
    icon: "chip",
    title: "IA",
    description:
      "Integración de modelos y servicios de IA dentro de una arquitectura de software.",
  },
  {
    id: "qa",
    icon: "test",
    title: "QA",
    description: "Validación automatizada y pruebas de regresión.",
  },
  {
    id: "seguridad",
    icon: "lock",
    title: "Seguridad",
    description: "Autenticación, autorización y protección de recursos.",
  },
  {
    id: "cloud",
    icon: "cloud",
    title: "Cloud",
    description: "Despliegue y operación de una aplicación real.",
  },
  {
    id: "evolucion",
    icon: "trend",
    title: "Evolución",
    description:
      "Analizar problemas existentes, proponer mejoras y evolucionar el sistema sin romper funcionalidades.",
  },
];

export const ABOUT_CONTENT = {
  hero: {
    badge: "Knowledge, Innovation & Navigation",
    title: "Sobre KIN",
    subtitle: "Una plataforma construida para aprender, crear y aportar.",
  },
  whatIs: {
    eyebrow: "La plataforma",
    title: "¿Qué es KIN?",
    paragraphs: [
      "KIN es una plataforma de inteligencia para apoyar la estructuración, análisis y evolución de proyectos de emprendedores, empresarios y organizaciones.",
      "Fue concebida como un proyecto tecnológico orientado a aplicar conocimientos de desarrollo de software, inteligencia artificial, arquitectura de sistemas, bases de datos, seguridad, automatización y pruebas de software en una solución real.",
      "KIN no representa únicamente una aplicación terminada. Representa un proceso continuo de aprendizaje, construcción y evolución tecnológica.",
    ],
  },
  creator: {
    eyebrow: "El proyecto",
    title: "Creado por",
    name: "Luis Orlando Guerra González",
    paragraphs: [
      "KIN fue concebida y desarrollada por Luis Orlando Guerra González, desarrollador de software en formación y Tecnólogo de Análisis y Desarrollo de Software, con interés en el desarrollo full-stack, la calidad del software y la inteligencia artificial.",
      "Es un proyecto construido de forma autodidacta y disciplinada: cada módulo, integración y prueba representa práctica real sobre problemas concretos, no solo teoría.",
      "Su objetivo es continuar creciendo dentro de la ingeniería de software, trabajar junto a equipos de desarrollo, recibir code reviews y críticas constructivas, aprender de profesionales con más experiencia y aportar lo aprendido durante la construcción de KIN.",
    ],
  },
  purpose: {
    title: "Mi propósito",
    paragraphs: [
      "Mi objetivo con KIN no es solamente construir una aplicación, sino demostrar que el aprendizaje constante puede convertirse en soluciones tecnológicas capaces de aportar valor a la sociedad.",
      "Quiero continuar creciendo como desarrollador, aprender de otros profesionales y formar parte de equipos de ingeniería donde pueda aportar lo que he aprendido y, al mismo tiempo, seguir aprendiendo de personas con mayor experiencia.",
      "La tecnología cambia constantemente. Por eso considero que un buen desarrollador nunca termina de aprender.",
    ],
  },
  moreThanApp: {
    eyebrow: "Aprendizaje en práctica",
    title: "Más que una aplicación",
    paragraphs: [
      "KIN representa un proceso de aprendizaje llevado a la práctica.",
      "Cada módulo, cada integración, cada prueba y cada problema resuelto forma parte de una búsqueda continua por comprender cómo se construyen sistemas de software cada vez más sólidos, seguros, mantenibles y útiles.",
      "El proyecto seguirá evolucionando junto con las necesidades de sus usuarios y con la evolución de la tecnología.",
    ],
  },
  technology: {
    eyebrow: "Stack real",
    title: "Tecnologías y capacidades demostradas",
    intro:
      "Cada tecnología de esta lista está presente en el código actual de KIN. No se muestran herramientas que solo aparezcan en documentación.",
    categories: TECH_CATEGORIES,
  },
  architecture: {
    eyebrow: "Sistema real",
    title: "Arquitectura de KIN",
    intro:
      "Representación de las capas que existen hoy en el código, sin componentes inventados.",
    hub: {
      root: { label: "KIN", description: "Plataforma full-stack" },
      branches: [
        { label: "Frontend", description: "Next.js · React · TypeScript" },
        { label: "Backend", description: "Spring Boot · Java 17" },
        { label: "IA", description: "Orquestador de 16 etapas" },
      ],
      base: { label: "PostgreSQL", description: "Persistencia · contexto · reportes" },
      deploy: { label: "Cloud / Docker", description: "Render · GitHub Actions · docker-compose" },
    },
    components: [
      { label: "Entrada del usuario", description: "Formularios y chat del frontend" },
      { label: "Frontend", description: "Next.js 16 · React 19 · TypeScript" },
      { label: "API REST", description: "Endpoints /api/v1 protegidos con JWT" },
      { label: "Backend", description: "Spring Boot: controladores, servicios, puertos y adaptadores" },
      { label: "Seguridad", description: "Spring Security + JWT + ownership de proyectos" },
      { label: "Persistencia", description: "JPA / Hibernate sobre PostgreSQL (Flyway V1…V29)" },
      { label: "IA", description: "DeepSeek vía Spring AI + pipeline de 16 etapas" },
      { label: "Documentos", description: "Carga y extracción de texto (PDF, DOCX, XLSX)" },
      { label: "Reportes y exportación", description: "PDF, DOCX y Markdown desde el reporte del proyecto" },
      { label: "Testing", description: "JUnit, Vitest, Playwright y Testcontainers" },
      { label: "Despliegue", description: "Docker, GitHub Actions y Render" },
    ],
    flows: [
      {
        id: "sistema",
        title: "Arquitectura del sistema",
        layers: [
          { label: "Usuario", description: "Navegador web" },
          { label: "Frontend KIN", description: "Next.js 16 · React 19 · TypeScript" },
          { label: "API REST", description: "/api/v1 · JWT (Spring Security)" },
          { label: "Spring Boot", description: "Controladores y servicios · Java 17" },
          { label: "Servicios · Seguridad · Persistencia", description: "Puertos, adaptadores y entidades JPA" },
          { label: "PostgreSQL", description: "Base de datos relacional" },
        ],
      },
      {
        id: "ia",
        title: "Integración de IA",
        layers: [
          { label: "KIN", description: "Frontend + backend" },
          { label: "Backend", description: "Orquestador de conversación y pipeline" },
          { label: "Orquestación IA", description: "16 etapas del pipeline inteligente" },
          { label: "Modelo / proveedor", description: "DeepSeek vía Spring AI" },
          { label: "Contexto durable", description: "Persistencia del contexto en PostgreSQL" },
        ],
      },
    ],
  },
  vision: {
    eyebrow: "Futuro",
    title: "Una visión construida para evolucionar",
    paragraphs: [
      "KIN ha sido concebida con una visión de evolución a largo plazo.",
      "La intención no es predecir cómo será la tecnología dentro de 15 o 20 años, sino construir una base suficientemente sólida para que nuevas capacidades puedan incorporarse a medida que evolucionen las necesidades de los usuarios y el ecosistema tecnológico.",
      "El software cambia constantemente. Por eso la arquitectura debe permitir aprender, adaptarse y evolucionar.",
    ],
    timeline: [
      "Arquitectura inicial",
      "Seguridad",
      "IA",
      "Automatización",
      "QA",
      "Cloud",
      "Escalabilidad",
      "Evolución continua",
    ],
  },
  audience: {
    eyebrow: "Usuarios",
    title: "Para quién se construye",
    items: [
      {
        title: "Emprendedores",
        description: "Personas que están estructurando y desarrollando nuevas ideas.",
      },
      {
        title: "Empresarios",
        description: "Personas que necesitan analizar y evolucionar proyectos existentes.",
      },
      {
        title: "Organizaciones",
        description:
          "Equipos que pueden utilizar herramientas tecnológicas para estructurar información y apoyar procesos de análisis.",
      },
    ],
  },
  showcase: {
    eyebrow: "Engineering Showcase",
    title: "Engineering Showcase",
    intro:
      "Las áreas de ingeniería que KIN implementa hoy, verificadas en el código del proyecto.",
    groups: SHOWCASE_GROUPS,
  },
  demonstrates: {
    eyebrow: "Capacidades",
    title: "Lo que KIN demuestra",
    intro:
      "Cada tarjeta corresponde a una capacidad respaldada por el código y el flujo real del proyecto.",
    cards: DEMONSTRATE_CARDS,
  },
  visionProfessional: {
    eyebrow: "Crecimiento profesional",
    title: "Mi visión como desarrollador",
    paragraphs: [
      "Mi objetivo profesional es trabajar con equipos de ingeniería, aprender buenas prácticas de equipos con experiencia y participar en proyectos reales donde las decisiones de arquitectura, testing, seguridad y calidad importan.",
      "Quiero recibir code reviews, aprender arquitectura de software, fortalecer mis habilidades de testing, seguridad, DevOps y cloud, y continuar evolucionando en inteligencia artificial.",
      "En tecnología nunca se termina de aprender: cada sistema real enseña algo nuevo, y eso es lo que busco en un equipo profesional.",
    ],
  },
  whyBuilt: {
    eyebrow: "Origen",
    title: "Por qué construí KIN",
    paragraphs: [
      "KIN nació del interés por aprender haciendo: construir una solución real que apoye a emprendedores, empresarios y organizaciones, y que a la vez demuestre mediante hechos lo que puedo construir.",
      "No fue creada con un objetivo comercial. Fue creada para practicar análisis, arquitectura, desarrollo full-stack, integración con IA, seguridad, testing y evolución constante sobre un problema real.",
      "KIN no representa el final de mi aprendizaje. Representa una evidencia de lo que he aprendido hasta ahora y una plataforma desde la cual quiero seguir aprendiendo.",
    ],
  },
  keepLearning: {
    eyebrow: "Futuro",
    title: "Quiero seguir aprendiendo",
    intro:
      "Mi siguiente paso no es dejar de aprender porque construí KIN. Es precisamente lo contrario.",
    areas: [
      "Arquitectura de software",
      "Sistemas distribuidos",
      "Cloud",
      "DevOps",
      "QA automation",
      "Inteligencia artificial",
      "Seguridad",
      "Performance",
      "Observabilidad",
      "Trabajo colaborativo",
      "Code review",
    ],
    closing:
      "Son áreas que quiero profundizar con práctica real y con equipos que me permitan crecer, no áreas que afirmo dominar.",
  },
  closing: {
    lines: [
      "KIN no representa el final de mi aprendizaje.",
      "Representa una evidencia de hasta dónde puedo llegar cuando decido aprender, construir y perseverar.",
    ],
    after: "Y todavía queda mucho por aprender.",
  },
  profile: {
    name: "Luis Orlando Guerra González",
    role: "Desarrollador de software",
    description:
      "Apasionado por el desarrollo de software, la inteligencia artificial, la calidad del software y la construcción de soluciones tecnológicas orientadas a resolver problemas reales.",
  },
};
