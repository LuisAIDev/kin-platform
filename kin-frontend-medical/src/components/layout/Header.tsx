"use client";

import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";

/**
 * Barra superior del dashboard de KIN Medical: nombre del usuario y cierre de
 * sesión. El logo/navegación viven en el Sidebar.
 */
export default function Header() {
  const router = useRouter();
  const user = typeof window !== "undefined" ? authService.getUser() : null;

  const handleLogout = async () => {
    await authService.logout();
    router.push("/login");
  };

  const initials = (user?.fullName ?? "")
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");

  return (
    <header className="sticky top-0 z-30 hidden lg:flex items-center justify-between border-b border-neutral-200 bg-white/80 backdrop-blur px-6 py-3">
      <div className="text-sm text-neutral-500">
        {user?.role === "PHYSICIAN" || user?.physicianCapability
          ? "Portal Médico"
          : "Mi Salud"}
      </div>

      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-medical-100 text-medical-700 flex items-center justify-center text-xs font-semibold">
            {initials || "K"}
          </div>
          <span className="text-sm font-medium text-neutral-700">
            {user?.fullName ?? "Usuario"}
          </span>
        </div>
        <button
          onClick={handleLogout}
          className="rounded-lg px-3 py-1.5 text-sm font-medium text-neutral-500 hover:bg-neutral-100 hover:text-red-600 transition"
        >
          Cerrar sesión
        </button>
      </div>
    </header>
  );
}