"use client";

import { useState, useEffect } from "react";
import Link from "next/link";

interface NavbarProps {
  /**
   * "landing": navegación del sitio público con anclas de la portada.
   * "about": navegación de las páginas públicas secundarias (Sobre KIN / Arquitectura).
   */
  variant?: "landing" | "about";
  /**
   * Ruta actual para marcar aria-current en la variante "about".
   * Por defecto "/sobre-kin" (comportamiento histórico).
   */
  activePath?: string;
}

const NAV_LINK_CLASS =
  "text-sm font-medium text-neutral-500 hover:text-primary-600 transition-colors duration-200";
const NAV_ACTIVE_CLASS =
  "text-sm font-medium text-primary-600 transition-colors duration-200";

export default function Navbar({ variant = "landing", activePath = "/sobre-kin" }: NavbarProps) {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <nav
      className={`sticky top-0 z-50 transition-all duration-300 ${
        scrolled
          ? "bg-white/80 backdrop-blur-xl border-b border-primary-100/50 shadow-sm"
          : "bg-transparent border-b border-transparent"
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link href="/" className="flex-shrink-0 flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-sm">
              <span className="text-white font-bold text-sm tracking-tight">K</span>
            </div>
            <span className="text-lg font-bold text-primary-900 tracking-tight">KIN</span>
          </Link>

          <div className="hidden md:flex items-center gap-8">
            {variant === "about" ? (
              <>
                <Link href="/" className={NAV_LINK_CLASS}>
                  Plataforma
                </Link>
                <Link
                  href="/sobre-kin"
                  aria-current={activePath === "/sobre-kin" ? "page" : undefined}
                  className={activePath === "/sobre-kin" ? NAV_ACTIVE_CLASS : NAV_LINK_CLASS}
                >
                  Sobre KIN
                </Link>
                <Link
                  href="/arquitectura"
                  aria-current={activePath === "/arquitectura" ? "page" : undefined}
                  className={activePath === "/arquitectura" ? NAV_ACTIVE_CLASS : NAV_LINK_CLASS}
                >
                  Arquitectura técnica
                </Link>
              </>
            ) : (
              <>
                <a href="#caracteristicas" className={NAV_LINK_CLASS}>
                  Características
                </a>
                <a href="#precios" className={NAV_LINK_CLASS}>
                  Precios
                </a>
                <a href="#contacto" className={NAV_LINK_CLASS}>
                  Contacto
                </a>
                <Link href="/sobre-kin" className={NAV_LINK_CLASS}>
                  Sobre KIN
                </Link>
              </>
            )}
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/login"
              className="text-sm font-medium text-neutral-600 hover:text-primary-600 transition-colors duration-200 px-3 py-2 rounded-lg"
            >
              Iniciar sesión
            </Link>
            <Link
              href="/register"
              className="text-sm font-semibold text-white bg-primary-600 hover:bg-primary-500 px-4 py-2 rounded-xl transition-all duration-200 shadow-sm hover:shadow-md hover:scale-105 active:scale-95"
            >
              Comenzar gratis
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
}
