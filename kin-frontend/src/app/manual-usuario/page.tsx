import type { Metadata } from "next";
import Link from "next/link";
import Navbar from "@/components/layout/Navbar";
import ManualSidebar from "@/components/manual/ManualSidebar";
import { MANUAL_META } from "@/components/manual/manualContent";

export const metadata: Metadata = {
  title: "Manual de Usuario",
  description: MANUAL_META.description,
  keywords: [
    "manual de usuario KIN",
    "cómo usar KIN",
    "guía de KIN",
    "estructurar un proyecto",
    "score de viabilidad",
    "reportes PDF",
  ],
  alternates: { canonical: "/manual-usuario" },
  robots: { index: true, follow: true },
  openGraph: {
    title: `${MANUAL_META.title} | KIN`,
    description: MANUAL_META.description,
    type: "article",
    locale: "es_ES",
    siteName: "KIN — Knowledge, Innovation & Navigation",
  },
  twitter: {
    card: "summary",
    title: `${MANUAL_META.title} | KIN`,
    description: MANUAL_META.description,
  },
};

const FAQ_ITEMS = [
  {
    question: "¿La IA toma las decisiones por mí?",
    answer:
      "No. La IA no decide. El motor determinista en Java es quien calcula el score, evalúa la viabilidad, identifica riesgos y determina el siguiente paso del análisis. La IA solo redacta y conversa. Tú tomas las decisiones finales.",
  },
  {
    question: "¿Es realmente gratis?",
    answer:
      "Sí. El Nivel 1 es gratuito y totalmente funcional. Puedes crear tu proyecto, conversar con KIN, obtener tu análisis y generar tu documentación sin pagar. Los planes superiores existen para quienes necesitan más capacidad o herramientas adicionales, no para desbloquear lo básico.",
  },
  {
    question: "¿KIN garantiza que mi proyecto tenga éxito?",
    answer:
      "No. KIN es una herramienta de apoyo para estructurar y analizar tu idea. Te ayuda a tomar mejores decisiones, pero el éxito depende del mercado, la ejecución, el equipo y muchos otros factores. No garantiza resultados financieros, financiación ni rentabilidad.",
  },
  {
    question: "¿Qué significa “Java decide, el LLM únicamente comunica”?",
    answer:
      "Significa que KIN separa dos responsabilidades. El motor en Java hace los cálculos y aplica las reglas del negocio. El modelo de lenguaje (LLM) se encarga de la conversación y de explicar los resultados. Así se evitan cifras inventadas y se garantiza un análisis confiable y reproducible.",
  },
  {
    question: "¿Y si no sé la respuesta a una pregunta?",
    answer:
      "Puedes decirlo con confianza. Responde “no lo tengo calculado” o da un estimado. KIN seguirá guiándote y te ayudará a construir el contexto con la información disponible.",
  },
  {
    question: "¿Puedo usar KIN para cualquier tipo de proyecto?",
    answer:
      "Sí. KIN se adapta a 19 categorías, desde Agroindustria y Fintech hasta Salud, Tecnología, Comercio, Educación, Turismo e Impacto Social. El análisis se ajusta al contexto de tu categoría.",
  },
  {
    question: "¿Necesito instalar algo?",
    answer:
      "No. KIN es 100 % digital y funciona desde el navegador. Solo necesitas internet y una cuenta.",
  },
  {
    question: "¿Puedo editar mi proyecto después?",
    answer:
      "Sí. Puedes volver al proyecto, actualizar la información, continuar la conversación y regenerar el análisis y los documentos cuando lo necesites.",
  },
  {
    question: "¿Cuánto tarda el proceso?",
    answer:
      "La primera versión estructurada de tu proyecto puede estar lista en menos de 60 minutos, dependiendo de la complejidad de tu idea y de la información que tengas disponible.",
  },
  {
    question: "¿Qué diferencia hay entre el reporte y el Documento de Inversión?",
    answer:
      "El reporte es el análisis completo de tu proyecto: viabilidad, score, riesgos, oportunidades y recomendaciones. El Documento de Inversión es un memo ejecutivo y confidencial orientado a presentar la oportunidad a inversionistas o socios.",
  },
  {
    question: "¿Mis datos están seguros?",
    answer:
      "KIN aplica medidas de seguridad para proteger tu información y trata tus datos conforme a su Política de Privacidad. Tu proyecto es privado y solo tú puedes acceder a él desde tu cuenta.",
  },
  {
    question: "¿Puedo usar KIN desde el celular?",
    answer:
      "Sí. La plataforma está diseñada para funcionar en computador, tableta y celular a través del navegador.",
  },
  {
    question: "¿KIN reemplaza a un consultor o a un asesor financiero?",
    answer:
      "No. KIN estructura y analiza tu idea con rigor, pero no sustituye el criterio de un profesional. Es una herramienta de apoyo para que llegues mejor preparado a esas conversaciones.",
  },
] as const;

const PROSE_CLASS =
  "prose prose-neutral max-w-none prose-p:text-neutral-600 prose-p:leading-7 prose-li:my-1 prose-li:text-neutral-600 prose-li:leading-7 prose-strong:text-neutral-900 prose-a:font-medium prose-a:text-primary-600 prose-a:no-underline hover:prose-a:underline prose-ol:my-5 prose-ul:my-5 prose-blockquote:border-primary-300 prose-blockquote:font-normal prose-blockquote:not-italic prose-blockquote:text-neutral-700";

const H2_CLASS =
  "mt-0 text-2xl font-bold tracking-tight text-neutral-900 sm:text-3xl";
const H3_CLASS = "mt-8 text-lg font-semibold text-neutral-900";

export default function ManualUsuarioPage() {
  return (
    <>
      <Navbar variant="about" activePath="/manual-usuario" />

      <main>
        {/* ── Hero ─────────────────────────────────────────── */}
        <section className="relative overflow-hidden border-b border-neutral-100">
          <div className="absolute inset-0 bg-gradient-to-br from-primary-50/70 via-white to-accent-50/40" />
          <div className="relative mx-auto max-w-5xl px-4 py-16 text-center sm:px-6 sm:py-20 lg:px-8">
            <p className="text-sm font-semibold uppercase tracking-widest text-primary-600">
              {MANUAL_META.badge}
            </p>
            <h1 className="mt-4 text-4xl font-extrabold tracking-tight text-neutral-900 sm:text-5xl">
              {MANUAL_META.title}
            </h1>
            <p className="mx-auto mt-5 max-w-3xl text-lg leading-8 text-neutral-500 sm:text-xl">
              {MANUAL_META.subtitle}
            </p>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-x-4 gap-y-2 text-sm text-neutral-400">
              <span>{MANUAL_META.updated}</span>
              <span aria-hidden="true">·</span>
              <span>{MANUAL_META.readingTime}</span>
              <span aria-hidden="true">·</span>
              <span>KIN Empresas</span>
            </div>
          </div>
        </section>

        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="lg:grid lg:grid-cols-[16rem_minmax(0,1fr)] lg:gap-12 xl:grid-cols-[18rem_minmax(0,1fr)]">
            <ManualSidebar />

            <article className={`${PROSE_CLASS} min-w-0 max-w-3xl pb-16 lg:py-10`}>
              {/* ── Bienvenido ─────────────────────────────── */}
              <section id="bienvenido" className="scroll-mt-24">
                <h2 className={H2_CLASS}>Bienvenido a KIN</h2>
                <p>
                  KIN significa{" "}
                  <strong>Knowledge, Innovation &amp; Navigation</strong>{" "}
                  (Conocimiento, Innovación y Navegación). Es una plataforma
                  digital que te ayuda a transformar esas ideas que tienes
                  dispersas —en notas, conversaciones o en la cabeza— en un
                  proyecto estructurado, analizable y listo para tomar
                  decisiones.
                </p>
                <p>
                  No necesitas ser experto en finanzas, tecnología ni gestión de
                  proyectos. KIN te acompaña con un chat inteligente, te hace
                  las preguntas correctas y organiza tus respuestas en un
                  análisis profesional.
                </p>
                <p>
                  En menos de 60 minutos puedes pasar de “tengo una idea” a
                  “tengo un proyecto con score, análisis de viabilidad,
                  riesgos, oportunidades y documentos descargables”.
                </p>
              </section>

              {/* ── ¿Qué hace diferente? ───────────────────── */}
              <section
                id="que-hace-diferente"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>¿Qué hace diferente a KIN?</h2>

                <h3 className={H3_CLASS}>1. KIN no inventa: calcula.</h3>
                <p>
                  Aquí está el corazón de la plataforma:{" "}
                  <strong>“Java decide, el LLM únicamente comunica”</strong>.
                </p>

                <div className="not-prose my-8 overflow-hidden rounded-2xl border border-primary-200 bg-white shadow-sm">
                  <div className="flex items-center gap-2 border-b border-primary-100 bg-primary-50/70 px-5 py-3">
                    <span
                      aria-hidden="true"
                      className="h-2 w-2 rounded-full bg-primary-500"
                    />
                    <span className="font-mono text-[11px] font-semibold uppercase tracking-widest text-primary-700">
                      Principio rector
                    </span>
                  </div>
                  <div className="px-5 py-6 sm:px-6">
                    <p className="text-xl font-bold tracking-tight text-primary-900 sm:text-2xl">
                      Java decide. El LLM únicamente comunica.
                    </p>
                    <div className="mt-5 grid gap-3 sm:grid-cols-2">
                      <div className="rounded-xl border border-primary-100 bg-primary-50/50 p-4">
                        <p className="text-xs font-semibold uppercase tracking-widest text-primary-700">
                          Motor determinista · Java
                        </p>
                        <p className="mt-2 text-sm leading-6 text-neutral-600">
                          Calcula, evalúa la viabilidad, detecta riesgos y
                          aplica las reglas del negocio.
                        </p>
                      </div>
                      <div className="rounded-xl border border-neutral-200 bg-neutral-50/60 p-4">
                        <p className="text-xs font-semibold uppercase tracking-widest text-neutral-500">
                          Capa de comunicación · IA
                        </p>
                        <p className="mt-2 text-sm leading-6 text-neutral-600">
                          Conversa contigo y explica en lenguaje claro lo que el
                          motor ya determinó.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <p>
                  Imagina una calculadora y un redactor trabajando juntos. La
                  calculadora (el motor determinista escrito en Java) hace las
                  cuentas, evalúa la viabilidad, detecta riesgos y aplica las
                  reglas del negocio. El redactor (la IA) solo se encarga de
                  conversar contigo y explicarte los resultados en lenguaje
                  claro.
                </p>
                <p>Eso significa que:</p>
                <ul>
                  <li>
                    Los cálculos financieros y la evaluación de viabilidad{" "}
                    <strong>no los inventa una IA</strong>.
                  </li>
                  <li>
                    Los resultados son <strong>consistentes, trazables y
                    auditables</strong>: con los mismos datos, obtienes el mismo
                    análisis.
                  </li>
                  <li>
                    La IA <strong>no puede “alucinar”</strong> cifras ni
                    decisiones: solo redacta y comunica lo que el motor ya
                    determinó.
                  </li>
                  <li>
                    Puedes confiar en el documento que descargas porque refleja
                    un análisis estructurado, no una respuesta improvisada.
                  </li>
                </ul>

                <h3 className={H3_CLASS}>2. El plan gratuito es útil de verdad.</h3>
                <p>
                  El Nivel 1 (gratuito) no es una versión mutilada ni una demo.
                  Es totalmente funcional: puedes crear tu proyecto, conversar
                  con KIN, obtener tu análisis y generar tu documentación sin
                  pagar. Los planes superiores añaden capacidad y herramientas
                  adicionales, no te quitan lo esencial.
                </p>

                <h3 className={H3_CLASS}>3. Se adapta a tu industria.</h3>
                <p>
                  KIN trabaja con <strong>19 categorías de proyecto</strong>,
                  entre ellas Agroindustria, Fintech, Salud, Tecnología e
                  Innovación, Comercio, Logística, Gastronomía, Educación,
                  Turismo, Medio Ambiente, Impacto Social y más. Así, las
                  preguntas y el análisis se ajustan al contexto real de tu
                  idea.
                </p>

                <h3 className={H3_CLASS}>4. Es 100 % digital.</h3>
                <p>
                  Todo el proceso ocurre en línea: cero papel, cero
                  instalaciones y acceso desde cualquier dispositivo con
                  internet.
                </p>

                <h3 className={H3_CLASS}>5. Te ayuda a decidir, no decide por ti.</h3>
                <p>
                  KIN es una herramienta de apoyo. Estructura tu idea y te
                  muestra información valiosa para que tomes mejores
                  decisiones. No garantiza éxito empresarial, financiación ni
                  rentabilidad: eso depende de ti, del mercado y de la
                  ejecución.
                </p>
              </section>

              {/* ── Paso 1 ─────────────────────────────────── */}
              <section
                id="paso-1"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>
                  Paso 1: Creando tu cuenta e iniciando sesión
                </h2>
                <p>Crear tu cuenta toma menos de dos minutos.</p>
                <ol>
                  <li>
                    Entra a{" "}
                    <a
                      href="https://kin-platform.com"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      kin-platform.com
                    </a>
                    .
                  </li>
                  <li>Selecciona <strong>Crear cuenta</strong> o <strong>Registrarte</strong>.</li>
                  <li>
                    Completa tus datos básicos: nombre, correo electrónico y
                    contraseña.
                  </li>
                  <li>Acepta las condiciones y la política de privacidad.</li>
                  <li>
                    Revisa tu correo y confirma tu cuenta mediante el enlace de
                    verificación.
                  </li>
                  <li>Inicia sesión con tu correo y contraseña.</li>
                </ol>

                <h3 className={H3_CLASS}>Recomendaciones para tu cuenta</h3>
                <ul>
                  <li>
                    Usa un correo que revises con frecuencia: allí llegarán la
                    verificación y los avisos importantes.
                  </li>
                  <li>Crea una contraseña segura y no la compartas.</li>
                  <li>
                    Si olvidas tu contraseña, usa la opción{" "}
                    <strong>“¿Olvidaste tu contraseña?”</strong> en la pantalla
                    de inicio de sesión.
                  </li>
                  <li>
                    Si no recibes el correo de verificación, revisa la carpeta
                    de spam o solicita un nuevo envío desde la plataforma.
                  </li>
                </ul>
                <p>
                  Al entrar por primera vez verás tu panel principal y el
                  acceso para crear un nuevo proyecto.
                </p>
              </section>

              {/* ── Paso 2 ─────────────────────────────────── */}
              <section
                id="paso-2"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>
                  Paso 2: Tu primer proyecto (El Chat Inteligente)
                </h2>
                <p>
                  Este es el momento en el que tu idea empieza a tomar forma.
                </p>

                <h3 className={H3_CLASS}>2.1 Crea el proyecto</h3>
                <ol>
                  <li>
                    En tu dashboard, selecciona <strong>Nuevo proyecto</strong>.
                  </li>
                  <li>
                    Escribe un <strong>título</strong> claro. Ejemplo: “Café
                    orgánico para exportación” o “App de finanzas para jóvenes”.
                  </li>
                  <li>
                    Añade una <strong>descripción breve</strong> con tu idea en
                    una o dos frases.
                  </li>
                  <li>Elige la categoría que mejor represente tu proyecto.</li>
                  <li>Confirma la creación.</li>
                </ol>
                <p>
                  No te preocupes si el título o la descripción no son
                  perfectos: podrás ajustarlos después.
                </p>

                <h3 className={H3_CLASS}>2.2 Conversa con KIN</h3>
                <p>
                  Al abrir el proyecto entrarás al chat inteligente. KIN no te
                  lanza un formulario largo ni te pide un plan de negocio desde
                  cero. Te hace <strong>preguntas guiadas, una a la vez</strong>,
                  para entender tu idea como lo haría un consultor.
                </p>
                <p>Escríbele con naturalidad. Por ejemplo:</p>
                <blockquote>
                  <p>
                    “Quiero crear una marca de café orgánico para exportar a
                    Europa. Tengo acceso a productores en Huila y un presupuesto
                    inicial de 20.000 dólares.”
                  </p>
                </blockquote>
                <p>
                  KIN analizará tu mensaje, identificará qué información falta y
                  decidirá cuál es la siguiente pregunta más útil.
                </p>

                <h3 className={H3_CLASS}>2.3 ¿Qué tipo de preguntas te hará?</h3>
                <p>
                  Dependiendo de tu categoría y de lo que ya hayas contado, KIN
                  puede preguntarte sobre:
                </p>
                <ul>
                  <li>El problema o necesidad que resuelve tu proyecto.</li>
                  <li>
                    Tu cliente ideal: quién es, dónde está y qué necesita.
                  </li>
                  <li>Tu propuesta de valor: por qué te elegirían a ti.</li>
                  <li>El mercado: tamaño, competencia y tendencias.</li>
                  <li>El modelo de ingresos: cómo piensas ganar dinero.</li>
                  <li>Los costos e inversión inicial.</li>
                  <li>El equipo y las capacidades con las que cuentas.</li>
                  <li>Los riesgos que identificas.</li>
                  <li>Tus objetivos a corto y mediano plazo.</li>
                </ul>

                <h3 className={H3_CLASS}>2.4 Cómo responder para sacar el máximo provecho</h3>
                <ul>
                  <li>
                    <strong>Responde en tus propias palabras.</strong> No hay
                    respuestas “correctas”; hay información útil.
                  </li>
                  <li>
                    <strong>Da cifras, aunque sean aproximadas.</strong> “Unos
                    5.000 clientes potenciales” es mejor que “muchos”.
                  </li>
                  <li>
                    <strong>Sé específico con el contexto.</strong> País,
                    ciudad, público, canal de venta, precios.
                  </li>
                  <li>
                    <strong>Si no tienes un dato, dilo.</strong> Puedes
                    responder “no lo tengo calculado” o dar un estimado
                    preliminar. KIN seguirá guiándote.
                  </li>
                  <li>
                    <strong>Puedes adjuntar documentos</strong> si tienes
                    información adicional: estudios, cotizaciones,
                    presentaciones o notas.
                  </li>
                  <li>
                    <strong>No necesitas escribir todo de una vez.</strong> La
                    conversación se construye turno a turno y puedes retomarla
                    cuando quieras.
                  </li>
                </ul>

                <h3 className={H3_CLASS}>2.5 ¿Cuándo termina la conversación?</h3>
                <p>
                  KIN avanza a medida que reúne suficiente contexto. En el panel
                  lateral verás cómo crece el <strong>progreso del proyecto</strong>{" "}
                  y cómo aparecen el score y el análisis. Cuando el motor
                  determinista considera que la información es suficiente,
                  genera el reporte y habilita los documentos.
                </p>
                <p>
                  Recuerda: la IA no decide cuándo cerrar el análisis. Es el
                  motor en Java quien evalúa la información y determina el
                  siguiente paso.
                </p>
              </section>

              {/* ── Paso 3 ─────────────────────────────────── */}
              <section
                id="paso-3"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>
                  Paso 3: Entendiendo tu Dashboard y el Score
                </h2>
                <p>
                  Tu dashboard es el centro de control del proyecto. Aquí puedes
                  ver, de un vistazo, qué tan completo está tu análisis y qué
                  tan sólida es tu idea según los criterios de KIN.
                </p>

                <h3 className={H3_CLASS}>3.1 El progreso (%)</h3>
                <p>
                  La barra o círculo de progreso indica{" "}
                  <strong>qué porcentaje de la información clave</strong> ha
                  logrado estructurar KIN. No es una nota de calidad de tu idea:
                  es una medida de completitud.
                </p>
                <ul>
                  <li>
                    <strong>Progreso bajo:</strong> aún faltan datos
                    importantes. El chat te seguirá preguntando.
                  </li>
                  <li>
                    <strong>Progreso medio:</strong> ya hay una base sólida,
                    pero quedan dimensiones por cubrir.
                  </li>
                  <li>
                    <strong>Progreso alto:</strong> el análisis está completo y
                    los documentos están disponibles.
                  </li>
                </ul>
                <p>
                  Un progreso alto no significa que tu proyecto sea exitoso;
                  significa que KIN tiene suficiente información para analizarlo
                  con rigor.
                </p>

                <h3 className={H3_CLASS}>3.2 El Score de viabilidad (0–100)</h3>
                <p>
                  El <strong>score</strong> es una evaluación estructurada de la
                  viabilidad de tu proyecto. Se calcula con el motor
                  determinista de KIN a partir de los datos que proporcionaste y
                  de criterios definidos para tu categoría.
                </p>
                <p>El score se acompaña de un análisis por dimensiones, como:</p>
                <ul>
                  <li>Mercado</li>
                  <li>Innovación</li>
                  <li>Viabilidad</li>
                  <li>Finanzas</li>
                  <li>Riesgo</li>
                  <li>Escalabilidad</li>
                  <li>Equipo</li>
                  <li>Sostenibilidad</li>
                </ul>
                <p>
                  Cada dimensión te dice dónde estás fuerte y dónde conviene
                  profundizar. Un score de 62, por ejemplo, no es “un 62 sobre
                  100 para tu sueño”: es una señal de que el proyecto tiene una
                  base razonable, pero probablemente necesita ajustes en
                  dimensiones específicas.
                </p>

                <h3 className={H3_CLASS}>
                  3.3 ¿Por qué puedes confiar en la viabilidad que te muestra KIN?
                </h3>
                <p>
                  Porque no la escribe la IA. La calcula un motor en Java con
                  reglas explícitas.
                </p>
                <ul>
                  <li>
                    <strong>Consistencia:</strong> si cambias un dato, el
                    análisis se recalcula de forma predecible.
                  </li>
                  <li>
                    <strong>Trazabilidad:</strong> el resultado no depende del
                    estado de ánimo del modelo ni de una respuesta creativa.
                  </li>
                  <li>
                    <strong>Auditabilidad:</strong> las reglas y los cálculos
                    pueden revisarse.
                  </li>
                  <li>
                    <strong>Separación de responsabilidades:</strong> la IA solo
                    redacta la explicación; el número lo decide el motor.
                  </li>
                </ul>
                <div className="not-prose my-6 rounded-xl border border-primary-100 bg-primary-50/60 px-5 py-4">
                  <p className="text-sm font-semibold leading-6 text-primary-900">
                    Dicho de forma simple: KIN te muestra un análisis, no una
                    opinión generada por IA.
                  </p>
                </div>

                <h3 className={H3_CLASS}>3.4 La barra lateral: Analytics, Insights y más</h3>
                <p>En el menú lateral encontrarás herramientas complementarias:</p>
                <ul>
                  <li>
                    <strong>Analytics:</strong> métricas de uso de la
                    plataforma: actividad, sesiones, mensajes y evolución de tu
                    trabajo.
                  </li>
                  <li>
                    <strong>Insights:</strong> conclusiones y patrones
                    detectados a partir de tus conversaciones: temas
                    recurrentes, longitud de las sesiones y comportamiento de
                    uso.
                  </li>
                  <li>
                    <strong>Recommendations:</strong> recomendaciones
                    priorizadas para mejorar tu proyecto.
                  </li>
                  <li>
                    <strong>Reports:</strong> acceso a tus reportes generados.
                  </li>
                  <li>
                    <strong>Plans / Subscription:</strong> gestión de tu plan y
                    de los límites de uso.
                  </li>
                  <li>
                    <strong>Settings:</strong> configuración de tu cuenta.
                  </li>
                </ul>
                <p>
                  Analytics e Insights te ayudan a entender{" "}
                  <strong>cómo estás usando KIN</strong>. El análisis de
                  viabilidad de tu proyecto, en cambio, vive dentro de cada
                  proyecto.
                </p>

                <h3 className={H3_CLASS}>3.5 Tu información estructurada</h3>
                <p>
                  Además del chat, el proyecto tiene una sección de{" "}
                  <strong>información estructurada</strong>: datos clave como
                  inversión inicial, mercado, equipo o modelo de negocio. Puedes
                  revisarla, completarla y confirmar los datos importados para
                  que el análisis sea más preciso.
                </p>
              </section>

              {/* ── Paso 4 ─────────────────────────────────── */}
              <section
                id="paso-4"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>Paso 4: Descargando tus Reportes</h2>
                <p>
                  Cuando tu proyecto alcanza suficiente contexto, KIN genera
                  documentación profesional que puedes descargar y compartir.
                </p>

                <h3 className={H3_CLASS}>4.1 Reporte en PDF</h3>
                <p>
                  Dentro del proyecto, en el panel lateral, encontrarás el botón
                  para <strong>generar y descargar el reporte en PDF</strong>.
                </p>
                <p>El reporte incluye, según el estado del proyecto:</p>
                <ul>
                  <li>Resumen ejecutivo.</li>
                  <li>Análisis de viabilidad y score.</li>
                  <li>Riesgos identificados.</li>
                  <li>Oportunidades.</li>
                  <li>Recomendaciones.</li>
                  <li>Dimensiones analizadas y hallazgos principales.</li>
                </ul>
                <p>Pasos:</p>
                <ol>
                  <li>Abre tu proyecto.</li>
                  <li>
                    Ubica el botón de <strong>reporte PDF</strong> en el panel
                    lateral.
                  </li>
                  <li>Genera el documento y espera a que esté listo.</li>
                  <li>Descárgalo y ábrelo con cualquier lector de PDF.</li>
                </ol>

                <h3 className={H3_CLASS}>4.2 Documento de Inversión (Investment Memo)</h3>
                <p>
                  El <strong>Documento de Inversión</strong> es un memo
                  confidencial pensado para presentar tu proyecto a
                  inversionistas, socios o aliados.
                </p>
                <ol>
                  <li>Abre tu proyecto.</li>
                  <li>
                    Selecciona <strong>Ver Documento de Inversión</strong>.
                  </li>
                  <li>Revisa el contenido generado.</li>
                  <li>Descárgalo o imprímelo desde la vista del documento.</li>
                </ol>
                <p>
                  Este documento resume la oportunidad, el modelo de negocio, el
                  mercado, los riesgos y los aspectos clave para una
                  conversación de inversión. Recuerda que es material de apoyo:
                  no sustituye la debida diligencia ni garantiza financiación.
                </p>

                <h3 className={H3_CLASS}>4.3 Exportar el proyecto</h3>
                <p>
                  También puedes exportar el proyecto como documento
                  estructurado en distintos formatos, según lo que necesites:
                </p>
                <ul>
                  <li>
                    <strong>PDF</strong> para compartir y presentar.
                  </li>
                  <li>
                    <strong>DOCX</strong> para editar y adaptar.
                  </li>
                  <li>
                    <strong>Markdown</strong> para trabajar con herramientas
                    técnicas o de documentación.
                  </li>
                </ul>
                <p>
                  La exportación puede hacerse en modo{" "}
                  <strong>completo</strong> o <strong>resumen</strong>, para que
                  elijas el nivel de detalle.
                </p>

                <h3 className={H3_CLASS}>4.4 Documentos Enterprise</h3>
                <p>
                  Si necesitas un paquete más completo, el{" "}
                  <strong>Enterprise Dashboard</strong> te permite generar
                  documentos de negocio como Lean Canvas, DOFA, plan financiero,
                  plan de mercado, roadmap, matriz de riesgos, KPIs y plan de
                  innovación, con exportación en <strong>PDF, DOCX y PPTX</strong>.
                </p>

                <h3 className={H3_CLASS}>4.5 Antes de descargar: revisa y ajusta</h3>
                <ul>
                  <li>Verifica que los datos del proyecto estén actualizados.</li>
                  <li>
                    Revisa la información estructurada y confirma lo que
                    corresponda.
                  </li>
                  <li>
                    Si cambias supuestos importantes, vuelve a generar el
                    análisis.
                  </li>
                  <li>Descarga la versión más reciente antes de compartirla.</li>
                </ul>
              </section>

              {/* ── FAQ ────────────────────────────────────── */}
              <section
                id="faq"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>Preguntas Frecuentes (FAQ)</h2>
                <div className="not-prose mt-8 divide-y divide-neutral-100 overflow-hidden rounded-2xl border border-neutral-200 bg-white">
                  {FAQ_ITEMS.map((item) => (
                    <details key={item.question} className="group px-5 py-4">
                      <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-sm font-semibold text-neutral-900 [&::-webkit-details-marker]:hidden">
                        <span>{item.question}</span>
                        <svg
                          aria-hidden="true"
                          viewBox="0 0 20 20"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="1.8"
                          className="h-4 w-4 shrink-0 text-neutral-400 transition-transform group-open:rotate-45"
                        >
                          <path
                            d="M10 4.5v11M4.5 10h11"
                            strokeLinecap="round"
                          />
                        </svg>
                      </summary>
                      <p className="mt-3 text-sm leading-6 text-neutral-600">
                        {item.answer}
                      </p>
                    </details>
                  ))}
                </div>
              </section>

              {/* ── Consejos ───────────────────────────────── */}
              <section
                id="consejos"
                className="mt-12 scroll-mt-24 border-t border-neutral-100 pt-12"
              >
                <h2 className={H2_CLASS}>
                  Consejos para aprovechar KIN al máximo
                </h2>
                <ol>
                  <li>
                    <strong>Empieza con una idea concreta.</strong> Mientras más
                    claro sea el problema que resuelves, mejores serán las
                    preguntas.
                  </li>
                  <li>
                    <strong>Responde con datos, aunque sean estimados.</strong>{" "}
                    Las cifras ayudan a que el análisis sea más útil.
                  </li>
                  <li>
                    <strong>No busques la respuesta perfecta.</strong> La
                    conversación se construye; puedes corregir y profundizar
                    después.
                  </li>
                  <li>
                    <strong>Revisa el análisis por dimensiones.</strong> El
                    score general es útil, pero las dimensiones te dicen dónde
                    actuar.
                  </li>
                  <li>
                    <strong>Descarga y comparte.</strong> Un proyecto
                    documentado genera mejores conversaciones con socios,
                    clientes e inversionistas.
                  </li>
                  <li>
                    <strong>Vuelve a KIN cuando cambien tus supuestos.</strong>{" "}
                    Si cambias precios, mercado o costos, actualiza el proyecto
                    y regenera el análisis.
                  </li>
                </ol>
              </section>

              {/* ── CTA ────────────────────────────────────── */}
              <section
                id="empieza"
                className="not-prose mt-16 scroll-mt-24 overflow-hidden rounded-3xl bg-gradient-to-br from-primary-600 via-primary-700 to-primary-900 px-6 py-12 text-center shadow-xl sm:px-10 sm:py-16"
              >
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary-200">
                  Empieza hoy
                </p>
                <h2 className="mt-3 text-3xl font-bold tracking-tight text-white sm:text-4xl">
                  Tu idea no necesita ser perfecta. Necesita estructura.
                </h2>
                <p className="mx-auto mt-4 max-w-2xl text-base leading-7 text-primary-100">
                  Entra a KIN, crea tu cuenta y descubre en menos de 60 minutos
                  cómo se ve tu proyecto cuando pasa de la cabeza al papel
                  —mejor dicho, a un dashboard claro, un score confiable y
                  documentos listos para compartir.
                </p>
                <Link
                  href="/register"
                  className="mt-8 inline-flex items-center justify-center rounded-xl bg-white px-6 py-3 text-sm font-semibold text-primary-700 shadow-sm transition-transform hover:scale-105 hover:bg-primary-50 active:scale-95"
                >
                  Empezar gratis ahora
                </Link>
                <p className="mt-5 text-sm font-medium text-primary-200">
                  KIN: Knowledge, Innovation &amp; Navigation.
                </p>
              </section>
            </article>
          </div>
        </div>
      </main>

      <footer className="border-t border-neutral-100 bg-neutral-50/50">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="flex flex-col items-center justify-between gap-5 sm:flex-row">
            <Link href="/" className="flex items-center gap-2.5">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 shadow-sm">
                <span className="text-sm font-bold tracking-tight text-white">
                  K
                </span>
              </span>
              <span className="text-lg font-bold tracking-tight text-primary-900">
                KIN
              </span>
            </Link>
            <nav
              aria-label="Enlaces de la página"
              className="flex flex-wrap items-center justify-center gap-5"
            >
              <Link
                href="/"
                className="text-sm font-medium text-neutral-500 transition-colors hover:text-primary-600"
              >
                Plataforma
              </Link>
              <Link
                href="/sobre-kin"
                className="text-sm font-medium text-neutral-500 transition-colors hover:text-primary-600"
              >
                Sobre KIN
              </Link>
              <Link
                href="/manual-usuario"
                aria-current="page"
                className="text-sm font-medium text-primary-600 transition-colors"
              >
                Manual
              </Link>
              <Link
                href="/arquitectura"
                className="text-sm font-medium text-neutral-500 transition-colors hover:text-primary-600"
              >
                Arquitectura técnica
              </Link>
            </nav>
            <p className="text-sm text-neutral-500">
              © {new Date().getFullYear()} KIN — Knowledge, Innovation &amp;
              Navigation.
            </p>
          </div>
        </div>
      </footer>
    </>
  );
}
