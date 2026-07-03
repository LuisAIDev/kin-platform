"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";

const NAV_ITEMS = [
  { label: "Mis Proyectos", href: "/dashboard/projects" },
  { label: "Nuevo Proyecto", href: "/dashboard/projects/new" },
];

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();

  const handleLogout = () => {
    authService.logout();
    router.push("/login");
  };

  return (
    <>
      <aside className="hidden lg:flex lg:flex-col lg:w-60 xl:w-64 border-r border-neutral-200 bg-white shrink-0">
        <div className="px-6 pt-6 pb-4">
          <Link
            href="/dashboard/projects"
            className="text-lg font-bold tracking-tight"
          >
            KIN
          </Link>
        </div>

        <nav className="flex-1 px-3 space-y-1">
          {NAV_ITEMS.map((item) => {
            const active = item.href === "/dashboard/projects/new"
              ? pathname === item.href
              : pathname.startsWith("/dashboard/projects");
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`block rounded-lg px-3 py-2 text-sm font-medium transition ${
                  active
                    ? "bg-neutral-900 text-white"
                    : "text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="px-3 pb-6">
          <button
            onClick={handleLogout}
            className="w-full rounded-lg px-3 py-2 text-sm font-medium text-neutral-500 hover:bg-neutral-100 hover:text-red-600 transition text-left"
          >
            Cerrar sesión
          </button>
        </div>
      </aside>

      <nav className="lg:hidden flex items-center justify-between border-b border-neutral-200 bg-white px-4 py-3 shrink-0">
        <Link
          href="/dashboard/projects"
          className="text-lg font-bold tracking-tight"
        >
          KIN
        </Link>

        <div className="flex items-center gap-2">
          {NAV_ITEMS.map((item) => {
            const active = item.href === "/dashboard/projects/new"
              ? pathname === item.href
              : pathname.startsWith("/dashboard/projects");
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium transition ${
                  active
                    ? "bg-neutral-900 text-white"
                    : "text-neutral-600 hover:bg-neutral-100"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
          <button
            onClick={handleLogout}
            className="text-sm text-neutral-500 hover:text-red-600 transition ml-1"
          >
            Salir
          </button>
        </div>
      </nav>
    </>
  );
}
