/**
 * Contenido centralizado de la página pública "/arquitectura".
 *
 * Es una representación pública y seleccionada de la arquitectura de KIN:
 * suficiente para evaluar la ingeniería, sin exponer el plano de
 * implementación. Cada afirmación de este archivo se verificó contra el
 * código y la documentación real del proyecto (README, migraciones Flyway,
 * configuración del pipeline, adaptadores de conocimiento y suite de pruebas).
 * No se incluyen secretos, credenciales, configuraciones privadas, prompts ni
 * nombres internos de clases.
 *
 * Regla de mantenimiento: toda cifra visible (etapas, categorías, ADRs, tests)
 * debe actualizarse aquí cuando el proyecto evolucione, desde un único lugar.
 */

export interface FlowStep {
  label: string;
  description: string;
}

import type { CategoryIconName } from "@/components/about/aboutContent";

export interface ArchitectureLayer {
  name: string;
  description: string;
  badges?: string[];
}

export interface PrincipleItem {
  title: string;
  description: string;
}

export interface MetricItem {
  value: string;
  label: string;
  note?: string;
}

export interface CapabilityCard {
  id: string;
  icon: CategoryIconName;
  title: string;
  description: string;
}

export interface Section {
  eyebrow: string;
  title: string;
  intro: string;
}

/**
 * Indicadores técnicos verificados que aparecen en el hero.
 * Son capacidades, no una lista exhaustiva de dependencias.
 */
export const ARCH_HERO_INDICATORS: string[] = [
  "Java / Spring Boot",
  "Next.js / React / TypeScript",
  "PostgreSQL",
  "Knowledge Engine",
  "IA con guardrails",
  "Testing automatizado",
  "CI/CD",
  "DevSecOps",
  "Cloud deployment",
];

export const ARCH_HERO = {
  badge: "Technical Architecture Showcase",
  title: "Arquitectura técnica de KIN",
  subtitle:
    "Una plataforma real de inteligencia y estructuración estratégica construida con arquitectura determinista, IA aplicada, seguridad, testing automatizado y cloud.",
};

/**
 * Resumen ejecutivo para evaluación técnica: responde en segundos a las
 * preguntas clave de un revisor (problema, arquitectura, IA, control).
 */
export const ARCH_SUMMARY: Section & { items: PrincipleItem[] } = {
  eyebrow: "Resumen ejecutivo",
  title: "KIN en una mirada técnica",
  intro:
    "Un vistazo de 30 segundos para un evaluador técnico: qué problema resuelve, cómo está construido, cómo usa IA y cómo se protege.",
  items: [
    {
      title: "El problema",
      description:
        "Transformar información dispersa de un proyecto o negocio en contexto estructurado, análisis de viabilidad, riesgos, oportunidades y documentación profesional reproducible.",
    },
    {
      title: "La arquitectura",
      description:
        "Clean Architecture y DDD táctico con dominio puro (POJO), backend Spring Boot, frontend Next.js y un pipeline determinista de 16 etapas.",
    },
    {
      title: "La IA",
      description:
        "El modelo de lenguaje comunica y explica; el motor determinista en Java decide. La IA no selecciona fuentes ni ejecuta conexiones arbitrarias.",
    },
    {
      title: "El control",
      description:
        "Knowledge Engine con fuentes autorizadas y protección SSRF, security by design, testing automatizado, observabilidad y DevSecOps.",
    },
  ],
};

export const ARCH_PRINCIPLE: Section & { items: PrincipleItem[]; flow: FlowStep[] } = {
  eyebrow: "Principio arquitectónico central",
  title: "Java decide. El LLM únicamente comunica.",
  intro:
    "KIN no delega las decisiones críticas del sistema exclusivamente en el modelo de IA. Un motor determinista controla cada paso del análisis; el modelo de lenguaje actúa como capa de comunicación y explicación de resultados.",
  items: [
    {
      title: "Contexto y reglas",
      description:
        "El motor determinista gobierna el contexto, las reglas de negocio y la evolución de cada turno de conversación.",
    },
    {
      title: "Adquisición de conocimiento",
      description:
        "La selección de fuentes, la validación de seguridad y la incorporación de hechos externos se deciden en código, no por el modelo.",
    },
    {
      title: "Análisis y resultados",
      description:
        "Scoring, riesgos, oportunidades, recomendaciones y la generación del reporte son el resultado de motores deterministas auditables.",
    },
    {
      title: "Comunicación",
      description:
        "El LLM formula preguntas cuando el motor decide preguntar y explica el análisis ya decidido cuando corresponde presentarlo.",
    },
  ],
  flow: [
    { label: "Usuario", description: "Envía información o hace una pregunta sobre su proyecto." },
    { label: "Clasificación y contexto", description: "KIN interpreta y actualiza el contexto del proyecto." },
    { label: "Política determinista", description: "El motor decide en Java la siguiente acción del turno." },
    { label: "Selección de fuentes autorizadas", description: "Solo las fuentes permitidas y pertinentes son consideradas." },
    { label: "Guard de conexión", description: "Validación de seguridad antes de cualquier conexión externa." },
    { label: "Adquisición segura", description: "Obtención de datos desde las fuentes aprobadas." },
    { label: "Validación", description: "Se verifica la respuesta y su contenido antes de aceptarlo." },
    { label: "Evidencia", description: "Los hechos se conservan con trazabilidad." },
    { label: "Enriquecimiento", description: "Ranking y ponderación de los hechos relevantes." },
    { label: "Análisis", description: "Scoring, riesgos, oportunidades y reporte estructurado." },
    { label: "Comunicación", description: "El LLM formula preguntas o explica el análisis decidido." },
  ],
};

export const ARCH_PRINCIPLE_OUTCOMES: string[] = [
  "Resultados consistentes",
  "Trazables",
  "Auditables",
  "Reproducibles",
  "Menos dependientes del proveedor de IA",
];

export const ARCH_PRINCIPLE_BOUNDARY: string[] = [
  "El LLM no es un navegador arbitrario.",
  "El LLM no selecciona URLs libremente.",
  "El LLM no decide qué fuentes son autorizadas.",
  "Java/KIN controla la política y las fuentes.",
];

export const ARCH_DIAGRAM: { title: string; layers: ArchitectureLayer[]; transversal: string[] } = {
  title: "Flujo de arquitectura del sistema",
  layers: [
    {
      name: "Usuario",
      description: "Persona o empresa que evalúa un proyecto a través de KIN.",
    },
    {
      name: "KIN Frontend",
      description: "Interfaz web de la plataforma.",
      badges: ["Next.js", "React", "TypeScript"],
    },
    {
      name: "Backend / Pipeline",
      description: "API, dominio y motor de análisis de KIN.",
      badges: ["Spring Boot", "Java", "16 etapas"],
    },
    {
      name: "Política determinista",
      description: "El motor decide en Java el contexto, la acción y las fuentes.",
      badges: ["Java decide"],
    },
    {
      name: "Knowledge Engine",
      description: "Adquisición de conocimiento externo bajo política de fuentes.",
      badges: ["Allowlist", "SSRF-safe"],
    },
    {
      name: "Fuentes externas seguras",
      description: "Solo fuentes autorizadas, con validación y límites.",
      badges: ["HTTPS", "Controles de red"],
    },
    {
      name: "Evidencia / Enriquecimiento",
      description: "Hechos trazables, seleccionados y ponderados.",
    },
    {
      name: "Comunicación IA",
      description: "El modelo explica el análisis ya decidido.",
      badges: ["Guardrails"],
    },
    {
      name: "Respuesta al usuario",
      description: "Resultado claro, consistente y reproducible.",
    },
  ],
  transversal: [
    "PostgreSQL — persistencia",
    "Redis — caché opcional",
    "Observabilidad — métricas y logs",
    "Seguridad — transversal",
  ],
};

export const ARCH_PIPELINE: Section & { count: number; stages: FlowStep[] } = {
  eyebrow: "Motor de inteligencia",
  title: "De la información al conocimiento estructurado",
  intro:
    "KIN transforma la información dispersa de un proyecto en conocimiento estructurado mediante un pipeline determinista de etapas, verificado en la configuración del pipeline del proyecto.",
  count: 16,
  stages: [
    { label: "Contexto", description: "Se extrae y actualiza la información relevante del proyecto." },
    { label: "Triaje (Salud)", description: "Evaluación digital de síntomas para la vertical de salud." },
    { label: "Diagnóstico diferencial", description: "Condiciones probables y pruebas sugeridas (vertical de salud)." },
    { label: "Evaluación", description: "Se mide qué dimensiones del proyecto están cubiertas." },
    { label: "Decisión", description: "El motor decide la siguiente acción del turno en Java." },
    { label: "Entrevista estratégica", description: "Preguntas dirigidas por reglas para completar el contexto." },
    { label: "Adquisición de conocimiento", description: "Hechos externos verificados desde fuentes autorizadas." },
    { label: "Deduplicación", description: "Eliminación de hechos duplicados o casi duplicados." },
    { label: "Enriquecimiento", description: "Selección y ponderación de los hechos relevantes por categoría." },
    { label: "Scoring", description: "Score de viabilidad por categoría y dimensión." },
    { label: "Recomendaciones", description: "Recomendaciones deduplicadas y priorizadas." },
    { label: "Riesgos", description: "Riesgos con severidad, probabilidad y nivel." },
    { label: "Oportunidades", description: "Oportunidades por mercado, innovación, finanzas y más." },
    { label: "Reporte", description: "Generación del informe de consultoría estructurado." },
    { label: "Comunicación", description: "El LLM comunica y explica el análisis decidido." },
    { label: "Eventos", description: "Publicación de eventos de dominio sobre el resultado." },
  ],
};

/**
 * Catálogo real de categorías de proyecto (V6__create_categories.sql +
 * V18__add_project_categories.sql → 19 categorías). La categoría impulsa la
 * selección determinista de conocimiento y el análisis.
 */
export const ARCH_CATEGORIES: Section & { count: number; items: string[] } = {
  eyebrow: "Catálogo de proyectos",
  title: "19 categorías que guían el análisis",
  intro:
    "Cada proyecto se clasifica en una de 19 categorías. La categoría es la base de la selección determinista: el motor elige qué fuentes de conocimiento y qué análisis aplican a cada proyecto.",
  count: 19,
  items: [
    "Tecnología e Innovación",
    "Empresarial",
    "Agroindustria",
    "Salud",
    "Educación",
    "Impacto Social",
    "Medio Ambiente",
    "Industria",
    "Gobierno",
    "Fintech",
    "Comercio",
    "Turismo",
    "Gastronomía",
    "Logística",
    "Creatividad",
    "Marketing Digital",
    "Investigación",
    "Servicios",
    "Otro / Sin clasificar",
  ],
};

export const ARCH_KNOWLEDGE: Section & {
  flow: FlowStep[];
  principles: string[];
  capabilities: string[];
  categoryExamples: { category: string; knowledge: string }[];
} = {
  eyebrow: "Knowledge Engine",
  title: "Knowledge Engine — conocimiento externo controlado",
  intro:
    "KIN puede incorporar conocimiento externo actualizado mediante fuentes autorizadas, con selección determinista y validación de seguridad antes de cualquier conexión.",
  flow: [
    { label: "Categoría del proyecto", description: "El sistema lee la categoría del proyecto en análisis." },
    { label: "Selección determinista", description: "Java decide qué fuentes son pertinentes." },
    { label: "Fuentes autorizadas", description: "Solo dominios incluidos en una política de fuentes controlada." },
    { label: "Guard de conexión", description: "Controles de red que bloquean destinos internos o peligrosos." },
    { label: "Obtención segura", description: "Peticiones HTTPS acotadas hacia las fuentes aprobadas." },
    { label: "Validación", description: "Se verifica la respuesta, el contenido y los límites." },
    { label: "Evidencia", description: "Los hechos se incorporan como evidencia trazable." },
    { label: "Deduplicación y caché", description: "Hechos únicos, con caché opcional para latencia." },
    { label: "Enriquecimiento", description: "Los hechos relevantes alimentan el análisis." },
  ],
  principles: [
    "Java decide qué fuentes son pertinentes.",
    "El LLM no selecciona URLs arbitrarias.",
    "El LLM no ejecuta peticiones HTTP.",
    "Las fuentes pasan por allowlist y protección SSRF.",
  ],
  capabilities: [
    "Fuentes registradas con metadata",
    "Política determinista de fuentes",
    "Asociación fuente ↔ categoría",
    "Reutilización de una fuente en varias categorías",
    "Allowlist de fuentes autorizadas",
    "Validación de respuestas y contenido",
    "Protección SSRF fail-closed",
    "Trazabilidad de evidencia por hecho",
    "Caché con métricas de hit/miss",
    "Métricas por fuente (éxito, fallo, latencia)",
  ],
  categoryExamples: [
    { category: "Agroindustria", knowledge: "información agrícola y de insumos" },
    { category: "Fintech", knowledge: "indicadores financieros y tasas" },
    { category: "Salud", knowledge: "información sanitaria" },
    { category: "Tecnología", knowledge: "indicadores digitales y conectividad" },
    { category: "Investigación", knowledge: "información científica" },
    { category: "Logística", knowledge: "transporte y comercio" },
  ],
};

export const ARCH_AI: Section & { pillars: PrincipleItem[]; items: FlowStep[] } = {
  eyebrow: "IA aplicada",
  title: "IA aplicada con guardrails",
  intro:
    "KIN utiliza IA como componente de una arquitectura mayor, no como sustituto de la arquitectura. El modelo se integra por backend y su salida se valida antes de llegar al usuario.",
  pillars: [
    {
      title: "Control determinista",
      description: "Las decisiones críticas del sistema se toman en Java, no en el modelo.",
    },
    {
      title: "Conocimiento externo",
      description: "El conocimiento llega solo desde fuentes autorizadas y validadas.",
    },
    {
      title: "Evidencia",
      description: "Los hechos usados por el análisis conservan trazabilidad.",
    },
    {
      title: "Comunicación IA",
      description: "El modelo interpreta y comunica según el flujo diseñado por KIN.",
    },
  ],
  items: [
    {
      label: "Integración por backend",
      description: "El LLM se consume a través del servidor, nunca directamente desde el navegador.",
    },
    {
      label: "Guardrails deterministas",
      description: "Detección de inyección de prompts, jailbreak y solicitudes inseguras sin LLM.",
    },
    {
      label: "Validación de respuestas",
      description: "La respuesta del modelo se valida; ante respuestas inválidas se reintenta o se entrega una respuesta segura determinista.",
    },
    {
      label: "Fallback",
      description: "Ante un fallo del proveedor, el sistema responde con una respuesta determinista en español.",
    },
    {
      label: "Decisión separada de la comunicación",
      description: "La lógica de negocio se decide en Java; el modelo solo comunica los resultados.",
    },
  ],
};

export const ARCH_SECURITY: Section & { measures: string[]; ssrf: string; connectionFlow: FlowStep[] } = {
  eyebrow: "Security by Design",
  title: "Seguridad integrada en la arquitectura",
  intro:
    "La seguridad no es una capa final de KIN: está distribuida en autenticación, autorización, red, persistencia, IA y despliegue.",
  measures: [
    "Autenticación stateless con JWT y contraseñas con BCrypt",
    "CORS de origen controlado",
    "Rate limiting por IP en endpoints sensibles",
    "Headers de seguridad HTTP (CSP, HSTS, frame-ancestors)",
    "Validación de entradas y manejo global de errores",
    "Aislamiento por propietario de proyecto (protección IDOR)",
    "HTTPS obligatorio en conexiones externas",
    "Allowlist de fuentes autorizadas",
    "Protección SSRF con guard de conexión",
    "Validación de resolución DNS (fail-closed)",
    "Bloqueo de redes privadas, loopback y link-local",
    "Bloqueo de endpoints de metadata de cloud",
    "Revalidación de redirecciones",
    "Timeouts acotados y límites de tamaño de respuesta",
    "Validación de content-type en respuestas",
    "Gestión de secretos por variables de entorno",
    "Análisis estático, scanning de dependencias y secret scanning",
    "Logs sin credenciales ni datos sensibles",
  ],
  ssrf:
    "Las conexiones externas están restringidas mediante una política de fuentes autorizadas y controles de red que bloquean destinos internos o potencialmente peligrosos.",
  connectionFlow: [
    { label: "URL candidata", description: "El sistema evalúa una conexión solo si fue seleccionada por la política." },
    { label: "Política de fuentes", description: "El dominio debe estar en la allowlist de fuentes autorizadas." },
    { label: "Resolución DNS", description: "Todas las direcciones resueltas deben ser públicas (fail-closed)." },
    { label: "Verificación de IP", description: "Se bloquean IPs privadas, loopback, link-local y metadata cloud." },
    { label: "HTTPS y límites", description: "Conexión cifrada con timeouts y límites de tamaño." },
    { label: "Redirecciones", description: "Cada redirección se revalida bajo las mismas reglas." },
    { label: "Validación de respuesta", description: "HTTP 2xx y content-type permitido antes de aceptar." },
    { label: "Evidencia", description: "El contenido aceptado se incorpora como hecho trazable." },
  ],
};

export const ARCH_QUALITY: Section & { metrics: MetricItem[]; tools: string[] } = {
  eyebrow: "Engineering Quality",
  title: "Calidad verificada con pruebas reales",
  intro:
    "KIN se valida con pruebas unitarias, de integración y E2E. Las métricas corresponden a la ejecución más reciente de la suite del proyecto.",
  metrics: [
    { value: "2.832", label: "tests backend ejecutados", note: "2.856 descubiertos (última suite registrada)" },
    { value: "399", label: "tests frontend", note: "74 archivos de prueba" },
    { value: "≥90 %", label: "cobertura de instrucciones", note: "en los módulos de dominio (JaCoCo)" },
    { value: "E2E", label: "Playwright", note: "login, dashboard y Sobre KIN" },
  ],
  tools: [
    "JUnit 5",
    "Mockito",
    "Testcontainers (PostgreSQL real)",
    "Testcontainers (Redis real)",
    "Vitest",
    "React Testing Library",
    "Playwright",
    "JaCoCo",
    "ESLint",
    "Spotless",
    "Checkstyle",
    "PMD",
    "SpotBugs",
  ],
};

export const ARCH_DEVSECOPS: Section & { flow: FlowStep[]; tools: string[] } = {
  eyebrow: "DevSecOps & CI/CD",
  title: "Calidad y seguridad en cada cambio",
  intro:
    "KIN no depende de que «funcione en mi máquina»: cada cambio pasa por pruebas, puertas de calidad, análisis estático, escaneo de seguridad y despliegue automatizado.",
  flow: [
    { label: "Código", description: "Cambios en el repositorio mediante push y pull requests." },
    { label: "Tests", description: "Suites de backend, frontend y E2E." },
    { label: "Quality gates", description: "Cobertura mínima exigida y reglas de calidad." },
    { label: "Static analysis", description: "Análisis estático y formato consistente del código." },
    { label: "Security checks", description: "Escaneo de secretos, dependencias y análisis CodeQL." },
    { label: "Build", description: "Compilación y artefactos reproducibles." },
    { label: "Deployment", description: "Despliegue automático a la infraestructura en la nube." },
    { label: "Observabilidad", description: "Monitoreo de métricas y logs después del despliegue." },
  ],
  tools: ["GitHub Actions", "SonarQube", "CodeQL", "Gitleaks", "OWASP Dependency Check", "Dependabot", "Renovate"],
};

export const ARCH_OBSERVABILITY: Section & { items: string[] } = {
  eyebrow: "Observabilidad",
  title: "Comportamiento del sistema, medido",
  intro:
    "La observabilidad permite analizar el comportamiento del sistema en producción sin depender únicamente de pruebas manuales.",
  items: [
    "Spring Actuator (health, info, métricas)",
    "Micrometer con métricas de aplicación (kin.*)",
    "Logs estructurados (JSON) con correlación por petición",
    "Tracing distribuido opcional (OpenTelemetry)",
    "Métricas del pipeline: latencias, errores, reintentos y timeouts",
    "Percentiles p50 / p95 / p99 en el Knowledge Engine",
    "Métricas de caché: hit y miss",
    "Métricas de rate limiting",
  ],
};

export const ARCH_CLOUD: Section & { flow: FlowStep[] } = {
  eyebrow: "Cloud Architecture",
  title: "Infraestructura en la nube",
  intro:
    "KIN se despliega como contenedores sobre infraestructura gestionada, con migraciones versionadas y despliegue automatizado.",
  flow: [
    { label: "Frontend → Cloud", description: "Next.js en producción con dominio propio (kin-platform.com)." },
    { label: "Backend → Cloud", description: "Spring Boot desplegado en la nube con healthcheck." },
    { label: "Database → Managed PostgreSQL", description: "Base de datos gestionada con migraciones Flyway." },
    { label: "Containers → Docker", description: "PostgreSQL, backend y frontend como contenedores." },
    { label: "CI/CD → GitHub Actions", description: "Despliegue y verificación automatizados." },
  ],
};

export const ARCH_ADR: Section & { count: number; topics: string[] } = {
  eyebrow: "Documentación arquitectónica",
  title: "Architecture Decision Records",
  intro:
    "KIN documenta sus decisiones arquitectónicas en ADRs para garantizar trazabilidad, evolución controlada, decisiones explícitas y reducción de deuda arquitectónica.",
  count: 32,
  topics: ["Seguridad", "Knowledge Engine", "Observabilidad", "Caché", "Arquitectura", "IA"],
};

export const ARCH_CAPABILITIES: Section & { cards: CapabilityCard[] } = {
  eyebrow: "Evaluación profesional",
  title: "¿Qué demuestra KIN desde el punto de vista profesional?",
  intro:
    "Cada tarjeta describe evidencia real del proyecto. No son declaraciones: son capacidades verificables en el sistema.",
  cards: [
    {
      id: "backend",
      icon: "server",
      title: "Backend Engineering",
      description: "Spring Boot, arquitectura por capas, servicios, puertos y adaptadores.",
    },
    {
      id: "arquitectura",
      icon: "layers",
      title: "Software Architecture",
      description: "Clean Architecture, DDD táctico, bounded contexts y pipeline pattern.",
    },
    {
      id: "frontend",
      icon: "code",
      title: "Frontend Engineering",
      description: "Next.js App Router, React, TypeScript strict, Tailwind y SSR.",
    },
    {
      id: "api",
      icon: "search",
      title: "API REST",
      description: "Diseño y seguridad de una API REST bajo un prefijo versionado.",
    },
    {
      id: "ddd",
      icon: "layers",
      title: "Domain-Driven Design",
      description: "Dominio puro (POJO) con fachadas y motores de dominio aislados.",
    },
    {
      id: "ai",
      icon: "chip",
      title: "AI Engineering",
      description: "IA integrada con guardrails, fallback y decisiones deterministas.",
    },
    {
      id: "knowledge",
      icon: "database",
      title: "Knowledge Systems",
      description: "Adquisición de conocimiento externo con política, validación y evidencia.",
    },
    {
      id: "seguridad",
      icon: "lock",
      title: "Security Engineering",
      description: "Autenticación, autorización, rate limiting y protección SSRF.",
    },
    {
      id: "qa",
      icon: "test",
      title: "QA Automation",
      description: "Pruebas unitarias, integración y E2E con Playwright.",
    },
    {
      id: "devsecops",
      icon: "shield",
      title: "DevSecOps",
      description: "CI/CD con análisis estático y escaneo de seguridad en cada cambio.",
    },
    {
      id: "cloud",
      icon: "cloud",
      title: "Cloud",
      description: "Contenedores, base gestionada y despliegue en la nube.",
    },
    {
      id: "observabilidad",
      icon: "trend",
      title: "Observabilidad",
      description: "Métricas, logs estructurados, percentiles y tracing distribuido.",
    },
  ],
};

export const ARCH_PRIVATE_REPO = {
  eyebrow: "Acceso controlado",
  title: "¿Quieres revisar el código?",
  description:
    "El código fuente de KIN se mantiene en un repositorio privado como medida de protección de propiedad intelectual y seguridad. Esta página ofrece una vista técnica de alto nivel de la arquitectura, las decisiones de ingeniería y las capacidades del sistema, sin exponer código fuente ni configuraciones sensibles. Para procesos técnicos, entrevistas o evaluaciones profesionales, puede proporcionarse acceso controlado cuando corresponda.",
  ctaLabel: "Solicitar evaluación técnica",
  ctaHref: "mailto:lguerragonzalez42@gmail.com?subject=Evaluación%20técnica%20de%20KIN%20—%20Arquitectura",
};

export const ARCH_META = {
  title: "KIN — Arquitectura técnica | Software, IA y Cloud",
  description:
    "Conoce la arquitectura técnica de KIN: Java, Spring Boot, Next.js, IA aplicada, Knowledge Engine, seguridad, testing automatizado, DevSecOps y cloud.",
  keywords: [
    "KIN Platform",
    "arquitectura de software",
    "inteligencia artificial",
    "Knowledge Engine",
    "Java",
    "Spring Boot",
    "Next.js",
    "React",
    "PostgreSQL",
    "Redis",
    "DevSecOps",
    "software architecture",
  ],
};
