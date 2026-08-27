import Link from "next/link";

/**
 * Selección de rol de la vertical Salud: "Soy Paciente" o "Soy Médico".
 */
export default function RegisterSaludPage() {
  return (
    <main className="flex-1 flex items-center justify-center px-6 py-10">
      <div className="w-full max-w-3xl flex flex-col gap-8">
        <div className="text-center">
          <h1 className="text-2xl font-bold">Crea tu cuenta de salud</h1>
          <p className="mt-1 text-sm text-neutral-500">
            Elige tu perfil. Los médicos requieren verificación de identidad.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <Link
            href="/register/salud/paciente"
            className="group flex flex-col rounded-2xl border-2 border-emerald-200 bg-white p-6 shadow-sm hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 ring-1 ring-emerald-100">
              <svg className="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
              </svg>
            </div>
            <h2 className="mt-5 text-lg font-bold text-neutral-900">Soy Paciente</h2>
            <p className="mt-1 text-sm text-neutral-500 leading-relaxed">
              Accede al triaje digital, tu historial, mensajería segura y citas
              de telemedicina.
            </p>
            <span className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-emerald-600 group-hover:text-emerald-700">
              Registrarme como paciente
              <span aria-hidden="true">→</span>
            </span>
          </Link>

          <Link
            href="/register/salud/medico"
            className="group flex flex-col rounded-2xl border-2 border-emerald-200 bg-white p-6 shadow-sm hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-50 ring-1 ring-emerald-100">
              <svg className="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.332A48.36 48.36 0 0 0 12 9.75c-2.551 0-5.056.2-7.5.582V21M3 21h18M12 6.75h.008v.008H12V6.75Z" />
              </svg>
            </div>
            <h2 className="mt-5 text-lg font-bold text-neutral-900">Soy Médico</h2>
            <p className="mt-1 text-sm text-neutral-500 leading-relaxed">
              Gestiona tus pacientes, alertas de urgencia, mensajería y citas.
              Requiere verificación de tu cédula profesional.
            </p>
            <span className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-emerald-600 group-hover:text-emerald-700">
              Registrarme como médico
              <span aria-hidden="true">→</span>
            </span>
          </Link>
        </div>
      </div>
    </main>
  );
}
