"use client";

import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";

/**
 * Pantalla de espera que muestra RoleGuard cuando la cuenta de un médico está
 * pendiente (o rechazada) de verificación de identidad por un administrador.
 */
export default function AccountReviewScreen() {
  const router = useRouter();

  const handleLogout = async () => {
    await authService.logout();
    router.push("/login");
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-50 px-6">
      <div className="w-full max-w-md flex flex-col items-center gap-4 rounded-2xl border border-neutral-200 bg-white p-8 text-center shadow-sm">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-amber-100 text-amber-600">
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
            />
          </svg>
        </div>
        <h1 className="text-xl font-bold text-neutral-900">Cuenta en revisión</h1>
        <p className="text-sm text-neutral-600 leading-relaxed">
          Tu cuenta de médico está siendo verificada por nuestro equipo.
          Recibirás un correo cuando sea aprobada. Mientras tanto, el acceso al
          portal está restringido.
        </p>
        <button
          onClick={handleLogout}
          className="mt-2 rounded-lg bg-neutral-100 px-6 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-200 transition min-h-11"
        >
          Cerrar sesión
        </button>
      </div>
    </div>
  );
}
