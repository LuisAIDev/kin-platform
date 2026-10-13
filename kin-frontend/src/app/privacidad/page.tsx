import Link from "next/link";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Política de Privacidad | KIN Platform",
  description: "Política de Privacidad de KIN Platform - Cumplimiento Ley 1581/2012 Colombia",
};

export default function PrivacidadPage() {
  return (
    <main className="min-h-screen bg-white">
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
        <header className="mb-12 text-center">
          <h1 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
            Política de Privacidad
          </h1>
          <p className="mt-3 text-sm text-neutral-500">
            Última actualización: 13 de octubre de 2026
          </p>
        </header>

        <article className="prose prose-neutral max-w-none">
          <section>
            <h2>1. Responsable del Tratamiento</h2>
            <p>
              <strong>KIN Platform</strong> (en adelante, "KIN", "nosotros" o "el Responsable"),
              con domicilio en Bogotá D.C., República de Colombia, es el Responsable del
              Tratamiento de sus Datos Personales.
            </p>
            <p>
              Contacto del Delegado de Protección de Datos (DPO):
              <a href="mailto:hola@kin-platform.com">hola@kin-platform.com</a>
            </p>
          </section>

          <section>
            <h2>2. Datos que Recolectamos</h2>
            <p>Recolectamos las siguientes categorías de Datos Personales:</p>
            <ul>
              <li><strong>Datos de identificación:</strong> nombre completo, correo electrónico.</li>
              <li><strong>Datos de autenticación:</strong> contraseña hasheada (nunca en texto plano), tokens de sesión.</li>
              <li><strong>Datos de proyecto:</strong> información que usted ingresa voluntariamente al describir sus proyectos (descripción, sector, problema, solución, cliente objetivo, modelo de ingresos, competencia, propuesta de valor, modelo de ingresos).</li>
              <li><strong>Datos de uso:</strong> interacciones con la IA, proyectos creados, documentos generados, mensajes de chat.</li>
              <li><strong>Datos de facturación (si suscribe un plan de pago):</strong> nombre, dirección, NIT/RUT, datos de tarjeta (procesados por Stripe, nosotros no almacenamos números de tarjeta).</li>
              <li><strong>Datos técnicos:</strong> dirección IP, tipo de navegador, sistema operativo, logs de acceso (para seguridad y analytics).</li>
            </ul>
          </section>

          <section>
            <h2>3. Finalidad del Tratamiento</h2>
            <p>Tratamos sus datos para:</p>
            <ul>
              <li>Prestar el Servicio: crear cuenta, autenticar, generar proyectos con IA, exportar documentos.</li>
              <li>Comunicaciones transaccionales: confirmación de registro, verificación de email, restablecimiento de contraseña, notificaciones de facturación.</li>
              <li>Facturación y cobro: procesar pagos vía Stripe, emitir recibos.</li>
              <li>Seguridad y prevención de fraude: detectar accesos anómalos, proteger contra abuso.</li>
              <li>Mejora del Servicio: analizar uso agregado (anonimizado) para mejorar la IA y UX.</li>
              <li>Cumplimiento legal: responder a requerimientos judiciales o administrativos.</li>
            </ul>
          </section>

          <section>
            <h2>4. Base Legal</h2>
            <p>Tratamos sus datos bajo las siguientes bases legales (Ley 1581/2012, Art. 8):</p>
            <ul>
              <li><strong>Ejecución de contrato:</strong> prestar el Servicio según nuestros Términos (Art. 8.1).</li>
              <li><strong>Consentimiento:</strong> para envío de newsletters, mejoras de IA, cookies no esenciales (Art. 8.3).</li>
              <li><strong>Interés legítimo:</strong> seguridad, prevención de fraude, mejora del servicio (Art. 8.2).</li>
              <li><strong>Obligación legal:</strong> facturación, respuesta a autoridades (Art. 8.4).</li>
            </ul>
          </section>

          <section>
            <h2>5. Derechos del Titular (Ley 1581/2012, Art. 8)</h2>
            <p>Usted tiene derecho a:</p>
            <ul>
              <li><strong>Conocer:</strong> acceder a sus datos personales que tratamos.</li>
              <li><strong>Actualizar y rectificar:</strong> corregir datos inexactos o incompletos.</li>
              <li><strong>Suprimir:</strong> solicitar la eliminación de sus datos cuando ya no sean necesarios.</li>
              <li><strong>Revocar el consentimiento:</strong> retirar autorizaciones otorgadas.</li>
              <li><strong>Presentar quejas:</strong> ante la Superintendencia de Industria y Comercio (SIC).</li>
            </ul>
          </section>

          <section>
            <h2>6. Cómo Ejercer sus Derechos</h2>
            <p>Envíe su solicitud a:</p>
            <ul>
              <li>Email: <a href="mailto:hola@kin-platform.com">hola@kin-platform.com</a> (asunto: "Derechos ARCO / Ley 1581")</li>
              <li>Plazo de respuesta: <strong>15 días hábiles</strong> (prorrogables 8 días más).</li>
            </ul>
            <p>Debe incluir: nombre completo, documento de identidad, descripción clara del derecho que ejerce y, de ser el caso, documento que acredite la representación.</p>
          </section>

          <section>
            <h2>7. Conservación de Datos</h2>
            <ul>
              <li>Datos de cuenta y proyecto: mientras la cuenta esté activa + 2 años tras cancelación.</li>
              <li>Datos de facturación: 5 años (exigencia fiscal colombiana).</li>
              <li>Logs de acceso y seguridad: 2 años.</li>
              <li>Datos anonimizados para analytics: indefinidamente (no son datos personales).</li>
            </ul>
            <p>Tras el período, los datos se suprimen o anonimizan irreversiblemente.</p>
          </section>

          <section>
            <h2>8. Seguridad</h2>
            <p>Implementamos medidas técnicas y organizativas apropiadas:</p>
            <ul>
              <li>Conexión HTTPS/TLS 1.3 en todo el sitio.</li>
              <li>Contraseñas hasheadas con bcrypt (costo 12).</li>
              <li>Tokens JWT con expiración corta + refresh tokens rotativos.</li>
              <li>Cookies HttpOnly, Secure, SameSite=Lax.</li>
              <li>Rate limiting en endpoints críticos.</li>
              <li>Auditoría de accesos a datos sensibles (logs inmutables).</li>
              <li>Acceso a producción restringido a personal autorizado (MFA).</li>
            </ul>
            <p><em>Nota:</em> Actualmente no aplicamos cifrado AES-256 en reposo a nivel de base de datos. Está planificado para Q1 2027.</p>
          </section>

          <section>
            <h2>9. Transferencia a Terceros (Encargados del Tratamiento)</h2>
            <p>Compartimos datos exclusivamente con:</p>
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-neutral-200">
                  <th className="pb-2 font-semibold">Proveedor</th>
                  <th className="pb-2 font-semibold">Finalidad</th>
                  <th className="pb-2 font-semibold">Ubicación</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-neutral-100">
                  <td className="py-2">Stripe</td>
                  <td className="py-2">Procesamiento de pagos, facturación</td>
                  <td className="py-2">EE. UU. (certificado PCI DSS Nivel 1)</td>
                </tr>
                <tr className="border-b border-neutral-100">
                  <td className="py-2">DeepSeek / OpenAI</td>
                  <td className="py-2">Modelos de IA para generación de contenido</td>
                  <td className="py-2">EE. UU. / China (cláusulas contractuales tipo)</td>
                </tr>
                <tr className="border-b border-neutral-100">
                  <td className="py-2">Render (Cloud)</td>
                  <td className="py-2">Hosting backend y frontend</td>
                  <td className="py-2">EE. UU. (AWS/GCP, SOC 2 Tipo II)</td>
                </tr>
                <tr className="border-b border-neutral-100">
                  <td className="py-2">SendGrid / SMTP</td>
                  <td className="py-2">Envío de emails transaccionales</td>
                  <td className="py-2">EE. UU.</td>
                </tr>
              </tbody>
            </table>
            <p className="mt-2 text-sm text-neutral-600">
              Todos los proveedores firman cláusulas de protección de datos (DPA) y cláusulas
              contractuales tipo (SCC) para transferencias internacionales.
            </p>
          </section>

          <section>
            <h2>10. Cookies y Tecnologías Similares</h2>
            <ul>
              <li><strong>Esenciales (siempre activas):</strong> sesión, autenticación, CSRF, preferencia de vertical.</li>
              <li><strong>Analíticas (opcionales):</strong> métricas de uso anonimizadas (no implementadas aún).</li>
              <li><strong>Marketing (opcionales):</strong> no usamos cookies de marketing.</li>
            </ul>
            <p>Puede gestionar cookies desde la configuración de su navegador. Desactivar cookies esenciales romperá la funcionalidad del sitio.</p>
          </section>

          <section>
            <h2>11. Cambios a esta Política</h2>
            <p>Actualizaremos esta Política cuando cambien nuestras prácticas o la normativa. Publicaremos la versión revisada aquí con la fecha de "Última actualización". Le notificaremos por email si los cambios son materiales.</p>
          </section>

<section>
            <h2>12. Vigencia</h2>
            <p>Esta Política entra en vigor el <strong>13 de octubre de 2026</strong> y sustituye cualquier versión anterior.</p>
          </section>
        </article>

        <footer className="mt-16 pt-8 border-t border-neutral-200 text-center text-sm text-neutral-500">
          <p>© 2026 KIN Platform. Todos los derechos reservados.
          </p>
        </footer>
      </div>
    </main>
  );
}