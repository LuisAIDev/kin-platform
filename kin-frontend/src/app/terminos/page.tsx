import Link from "next/link";
import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Términos y Condiciones | KIN Platform",
  description: "Términos y Condiciones de uso de KIN Platform - Plataforma SaaS de análisis empresarial con IA",
};

export default function TerminosPage() {
  return (
    <main className="min-h-screen bg-white">
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
        <header className="mb-12 text-center">
          <h1 className="text-3xl font-bold tracking-tight text-neutral-900 sm:text-4xl">
            Términos y Condiciones
          </h1>
          <p className="mt-3 text-sm text-neutral-500">
            Última actualización: 13 de octubre de 2026
          </p>
        </header>

        <article className="prose prose-neutral max-w-none">
          <section>
            <h2>1. Aceptación de los Términos</h2>
            <p>
              Al acceder y usar KIN Platform (kin-platform.com), usted acepta
              quedar vinculado por estos Términos y Condiciones ("Términos"). Si no está
              de acuerdo con alguna parte, no debe usar el Servicio.
            </p>
          </section>

          <section>
            <h2>2. Descripción del Servicio</h2>
            <p>
              KIN Platform es una plataforma SaaS (Software as a Service) de análisis
              empresarial asistida por inteligencia artificial. Permite a los usuarios:
            </p>
            <ul>
              <li>Crear y estructurar proyectos empresariales mediante chat con IA.</li>
              <li>Generar reportes ejecutivos (Executive Report, análisis DOFA).</li>
              <li>Exportar documentos en formatos DOCX, PDF y PPTX.</li>
              <li>Evaluar la viabilidad técnica y comercial de proyectos (scoring 0-100).</li>
              <li>Identificar oportunidades, riesgos y recomendaciones estratégicas.</li>
            </ul>
          </section>

          <section>
            <h2>3. Cuenta y Registro</h2>
            <ul>
              <li>Debe ser mayor de 18 años y tener capacidad legal para contratar.</li>
              <li>Debe proporcionar información veraz, exacta y completa.</li>
              <li>Es responsable de mantener la confidencialidad de su contraseña.</li>
              <li>Debe notificarnos inmediatamente de cualquier uso no autorizado.</li>
              <li>Al registrarse, acepta recibir comunicaciones transaccionales y de seguridad.</li>
            </ul>
          </section>

          <section>
            <h2>4. Planes y Pagos</h2>
            <ul>
              <li>Ofrecemos planes: <strong>GRATIS</strong> (hasta 3 proyectos), <strong>STANDARD</strong> ($25 USD/mes), <strong>PREMIUM</strong> ($35 USD/mes).</li>
              <li>Los pagos se procesan mediante <strong>Stripe</strong>. No almacenamos datos de tarjeta.</li>
              <li>Los precios están en USD. No hay reembolsos por períodos parciales.</li>
              <li>Puede cancelar en cualquier momento; el acceso continúa hasta el fin del período pagado.</li>
              <li>Los precios pueden cambiar con 30 días de aviso previo.</li>
            </ul>
          </section>

          <section>
            <h2>5. Uso Aceptable</h2>
            <p>Usted se compromete a NO:</p>
            <ul>
              <li>Usar el Servicio para fines ilegales o no autorizados.</li>
              <li>Intentar acceder sin autorización a sistemas, datos o cuentas de otros.</li>
              <li>Transmitir virus, malware o código malicioso.</li>
              <li>Interferir con la integridad o rendimiento del Servicio.</li>
              <li>Usar la IA para generar contenido ilegal, difamatorio, ofensivo o que viole derechos de propiedad intelectual.</li>
            </ul>
            <p>Nos reservamos el derecho de suspender o terminar cuentas que violen estas normas.</p>
          </section>

          <section>
            <h2>6. Propiedad Intelectual</h2>
            <ul>
              <li>Usted retiene la propiedad de los contenidos que cree en la plataforma (sus proyectos, textos, datos).</li>
              <li>Nos concede una licencia mundial, no exclusiva y gratuita para alojar, procesar y mostrar sus contenidos solo para prestarle el Servicio.</li>
              <li>KIN Platform, su tecnología, marca, código y diseño son propiedad exclusiva de KIN y están protegidos por leyes de propiedad intelectual.</li>
            </ul>
          </section>

          <section>
            <h2>7. Limitación de Responsabilidad</h2>
            <p>
              EN LA MEDIDA MÁXIMA PERMITIDA POR LA LEY, KIN PLATFORM NO SERÁ RESPONSABLE
              POR DAÑOS INDIRECTOS, INCIDENTALES, ESPECIALES, CONSECUENTES O PUNITIVOS,
              INCLUYENDO PÉRDIDA DE BENEFICIOS, DATOS, USO, FONDO DE COMERCIO U OTROS
              INTANGIBLES, RESULTANTES DE: (A) EL USO O LA IMPOSIBILIDAD DE USAR EL SERVICIO;
              (B) ACCESO NO AUTORIZADO A SUS DATOS; (C) DECLARACIONES O CONDUCTA DE TERCEROS.
              NUESTRA RESPONSABILIDAD TOTAL NO EXCEDERÁ EL MONTO PAGADO POR USTED EN LOS
              ÚLTIMOS 12 MESES.
            </p>
          </section>

          <section>
            <h2>8. Modificaciones</h2>
            <p>
              Podemos modificar estos Términos en cualquier momento. Publicaremos la versión
              actualizada en esta página con la fecha de "Última actualización". El uso
              continuado del Servicio después de los cambios constituye aceptación.
            </p>
          </section>

          <section>
            <h2>9. Ley Aplicable y Jurisdicción</h2>
            <p>
              Estos Términos se rigen por las leyes de la <strong>República de Colombia</strong>.
              Cualquier controversia se someterá a los jueces de <strong>Bogotá D.C.</strong>,
              renunciando a cualquier otro fuero.
            </p>
          </section>

          <section>
            <h2>10. Contacto</h2>
            <p>
              Si tiene preguntas sobre estos Términos, contáctenos en:
            </p>
            <ul>
              <li>Email: <a href="mailto:hola@kin-platform.com">hola@kin-platform.com</a></li>
              <li>Sitio web: <a href="https://www.kin-platform.com">https://www.kin-platform.com</a></li>
            </ul>
          </section>
        </article>

        <footer className="mt-16 pt-8 border-t border-neutral-200 text-center text-sm text-neutral-500">
          <p>© 2026 KIN Platform. Todos los derechos reservados.</p>
        </footer>
      </div>
    </main>
  );
}