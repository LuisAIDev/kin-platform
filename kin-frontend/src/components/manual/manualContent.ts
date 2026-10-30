export const MANUAL_META = {
  badge: "Manual de Usuario",
  title: "Manual de Usuario de KIN",
  subtitle:
    "Tu idea, convertida en un proyecto claro y accionable en menos de 60 minutos.",
  description:
    "Aprende a usar KIN paso a paso: crea tu cuenta, estructura tu proyecto con el chat inteligente, entiende tu score de viabilidad y descarga tus reportes.",
  readingTime: "12 min de lectura",
  updated: "Actualizado en 2026",
} as const;

export const MANUAL_SECTIONS = [
  { id: "bienvenido", label: "Bienvenido a KIN" },
  { id: "que-hace-diferente", label: "¿Qué hace diferente a KIN?" },
  { id: "paso-1", label: "Paso 1: Crea tu cuenta" },
  { id: "paso-2", label: "Paso 2: Tu primer proyecto" },
  { id: "paso-3", label: "Paso 3: Dashboard y Score" },
  { id: "paso-4", label: "Paso 4: Descarga tus reportes" },
  { id: "faq", label: "Preguntas frecuentes" },
  { id: "consejos", label: "Consejos para aprovechar KIN" },
] as const;

export type ManualSectionId = (typeof MANUAL_SECTIONS)[number]["id"];
