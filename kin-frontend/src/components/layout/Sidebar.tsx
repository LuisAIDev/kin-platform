"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { useState } from "react";

const NAV_ITEMS = [
  { label: "Mis Proyectos", href: "/dashboard/projects" },
  { label: "Nuevo Proyecto", href: "/dashboard/projects/new" },
];

const ADMIN_ITEM = { label: "Administración", href: "/dashboard/admin/pricing" };

export default function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const user = typeof window !== "undefined" ? authService.getUser() : null;
  const isAdmin = user?.role === "ADMIN";
  const allItems = isAdmin ? [...NAV_ITEMS, ADMIN_ITEM] : NAV_ITEMS;

  const handleLogout = () => {
    authService.logout();
    router.push("/login");
  };

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (href: string) => {
    if (href === "/dashboard/projects/new") {
      return pathname === "/dashboard/projects/new";
    }
    if (href === "/dashboard/admin/pricing") {
      return pathname.startsWith("/dashboard/admin");
    }
    return pathname.startsWith("/dashboard/projects") && pathname !== "/dashboard/projects/new";
  };

  return (
    <>
      <aside className="hidden lg:flex lg:flex-col lg:w-60 xl:w-64 border-r border-neutral-200 bg-white shrink-0">
        <div className="px-6 pt-6 pb-4 flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-primary-600 flex items-center justify-center shrink-0">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="4 7 12 12 4 17" />
              <polyline points="12 7 20 12 12 17" />
            </svg>
          </div>
          <Link
            href="/dashboard/projects"
            className="text-lg font-bold tracking-tight"
          >
            KIN
          </Link>
        </div>

        <nav className="flex-1 px-3 space-y-1">
          {allItems.map((item) => {
            const active = isActive(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`block rounded-lg px-3 py-2 text-sm font-medium transition ${
                  active
                    ? "bg-primary-600 text-white"
                    : "text-neutral-600 hover:bg-primary-50 hover:text-primary-700"
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
          className="text-lg font-bold tracking-tight flex items-center gap-2"
        >
          <div className="w-6 h-6 rounded-md bg-primary-600 flex items-center justify-center shrink-0">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="4 7 12 12 4 17" />
              <polyline points="12 7 20 12 12 17" />
            </svg>
          </div>
          KIN
        </Link>

        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="flex items-center justify-center w-10 h-10 rounded-lg hover:bg-neutral-100 transition"
          aria-label={mobileMenuOpen ? "Cerrar menú" : "Abrir menú"}
        >
          {mobileMenuOpen ? (
            <svg className="w-5 h-5 text-neutral-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          ) : (
            <svg className="w-5 h-5 text-neutral-600" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3 6h18M3 12h18M3 18h18" />
            </svg>
          )}
        </button>
      </nav>

      {mobileMenuOpen && (
        <div className="lg:hidden border-b border-neutral-200 bg-white px-4 py-3 space-y-1">
          {allItems.map((item) => {
            const active = isActive(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className={`block rounded-lg px-3 py-3 text-sm font-medium transition min-h-11 flex items-center ${
                  active
                    ? "bg-primary-600 text-white"
                    : "text-neutral-600 hover:bg-primary-50 hover:text-primary-700"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
          <button
            onClick={() => {
              setMobileMenuOpen(false);
              handleLogout();
            }}
            className="w-full rounded-lg px-3 py-3 text-sm font-medium text-neutral-500 hover:bg-neutral-100 hover:text-red-600 transition text-left min-h-11"
          >
            Cerrar sesión
          </button>
        </div>
      )}
    </>
  );
}
