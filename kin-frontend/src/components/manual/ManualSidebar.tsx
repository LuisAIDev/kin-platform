"use client";

import { useEffect, useState } from "react";
import { MANUAL_SECTIONS, type ManualSectionId } from "./manualContent";

export default function ManualSidebar() {
  const [open, setOpen] = useState(false);
  const [activeId, setActiveId] = useState<ManualSectionId>("bienvenido");

  useEffect(() => {
    if (typeof IntersectionObserver === "undefined") return;

    const sections = MANUAL_SECTIONS.map((section) =>
      document.getElementById(section.id),
    ).filter((element): element is HTMLElement => element !== null);

    if (sections.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

        if (visible[0]) {
          setActiveId(visible[0].target.id as ManualSectionId);
        }
      },
      { rootMargin: "-96px 0px -65% 0px", threshold: [0, 1] },
    );

    sections.forEach((section) => observer.observe(section));
    return () => observer.disconnect();
  }, []);

  return (
    <aside className="lg:sticky lg:top-24 lg:h-[calc(100vh-7rem)] lg:overflow-y-auto lg:py-10">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-controls="manual-toc"
        className="flex w-full items-center justify-between rounded-xl border border-neutral-200 bg-white px-4 py-3 text-left text-sm font-semibold text-neutral-900 shadow-sm transition-colors hover:border-primary-200 lg:hidden"
      >
        <span>En esta página</span>
        <svg
          aria-hidden="true"
          viewBox="0 0 20 20"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          className={`h-4 w-4 text-neutral-400 transition-transform ${open ? "rotate-180" : ""}`}
        >
          <path d="M5 7.5 10 12.5 15 7.5" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>

      <nav
        id="manual-toc"
        aria-label="Índice del manual"
        className={`${open ? "mt-3 block" : "hidden"} rounded-xl border border-neutral-200 bg-white p-3 shadow-sm lg:mt-0 lg:block lg:border-0 lg:bg-transparent lg:p-0 lg:shadow-none`}
      >
        <p className="hidden px-3 text-xs font-semibold uppercase tracking-widest text-neutral-400 lg:block">
          En esta página
        </p>
        <ul className="mt-0 space-y-0.5 lg:mt-4">
          {MANUAL_SECTIONS.map((section) => {
            const isActive = activeId === section.id;
            return (
              <li key={section.id}>
                <a
                  href={`#${section.id}`}
                  aria-current={isActive ? "location" : undefined}
                  onClick={() => setOpen(false)}
                  className={`block rounded-lg px-3 py-2 text-sm transition-colors ${
                    isActive
                      ? "bg-primary-50 font-semibold text-primary-700"
                      : "font-medium text-neutral-500 hover:bg-neutral-50 hover:text-neutral-900"
                  }`}
                >
                  {section.label}
                </a>
              </li>
            );
          })}
        </ul>

        <div className="mt-4 hidden border-t border-neutral-100 pt-4 lg:block">
          <a
            href="#empieza"
            className="block rounded-lg bg-primary-600 px-3 py-2 text-center text-sm font-semibold text-white transition-colors hover:bg-primary-500"
          >
            Empezar gratis ahora
          </a>
        </div>
      </nav>
    </aside>
  );
}
