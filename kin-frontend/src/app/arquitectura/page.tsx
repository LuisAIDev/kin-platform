import type { Metadata } from "next";
import Link from "next/link";
import Navbar from "@/components/layout/Navbar";
import {
  ARCH_HERO,
  ARCH_HERO_INDICATORS,
  ARCH_META,
} from "@/components/architecture/architectureContent";
import SummarySection from "@/components/architecture/SummarySection";
import PrincipleSection from "@/components/architecture/PrincipleSection";
import ArchDiagram from "@/components/architecture/ArchDiagram";
import PipelineSection from "@/components/architecture/PipelineSection";
import CategoriesSection from "@/components/architecture/CategoriesSection";
import KnowledgeSection from "@/components/architecture/KnowledgeSection";
import AiSection from "@/components/architecture/AiSection";
import SecuritySection from "@/components/architecture/SecuritySection";
import QualitySection from "@/components/architecture/QualitySection";
import DevSecOpsSection from "@/components/architecture/DevSecOpsSection";
import ObservabilitySection from "@/components/architecture/ObservabilitySection";
import CloudSection from "@/components/architecture/CloudSection";
import AdrSection from "@/components/architecture/AdrSection";
import CapabilitiesSection from "@/components/architecture/CapabilitiesSection";
import PrivateRepoSection from "@/components/architecture/PrivateRepoSection";

export const metadata: Metadata = {
  title: ARCH_META.title,
  description: ARCH_META.description,
  keywords: ARCH_META.keywords,
  alternates: { canonical: "/arquitectura" },
  robots: { index: true, follow: true },
  openGraph: {
    title: ARCH_META.title,
    description: ARCH_META.description,
    type: "website",
    locale: "es_ES",
    siteName: "KIN — Knowledge, Innovation & Navigation",
  },
  twitter: {
    card: "summary",
    title: ARCH_META.title,
    description: ARCH_META.description,
  },
};

export default function ArquitecturaPage() {
  return (
    <>
      <Navbar variant="about" activePath="/arquitectura" />

      <main>
        {/* ── Hero ─────────────────────────────────────────── */}
        <section className="relative overflow-hidden border-b border-neutral-100">
          <div className="absolute inset-0 bg-gradient-to-br from-primary-50/70 via-white to-accent-50/40" />
          <div className="relative mx-auto max-w-5xl px-4 py-20 text-center sm:px-6 sm:py-28 lg:px-8">
            <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-4">
              {ARCH_HERO.badge}
            </p>
            <h1 className="text-4xl font-extrabold tracking-tight text-neutral-900 sm:text-6xl">
              {ARCH_HERO.title}
            </h1>
            <p className="mx-auto mt-5 max-w-3xl text-lg leading-8 text-neutral-500 sm:text-xl">
              {ARCH_HERO.subtitle}
            </p>
            <ul
              className="mx-auto mt-10 flex max-w-4xl flex-wrap items-center justify-center gap-2.5"
              role="list"
            >
              {ARCH_HERO_INDICATORS.map((indicator) => (
                <li
                  key={indicator}
                  className="rounded-full border border-primary-100 bg-white px-4 py-1.5 text-sm font-medium text-primary-700 shadow-sm"
                >
                  {indicator}
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* ── Resumen ejecutivo ────────────────────────────── */}
        <SummarySection />

        {/* ── Principio arquitectónico central ─────────────── */}
        <PrincipleSection />

        {/* ── Diagrama general de arquitectura ─────────────── */}
        <ArchDiagram />

        {/* ── Motor de inteligencia ────────────────────────── */}
        <PipelineSection />

        {/* ── 19 categorías de proyecto ────────────────────── */}
        <CategoriesSection />

        {/* ── Knowledge Engine ─────────────────────────────── */}
        <KnowledgeSection />

        {/* ── IA aplicada con guardrails ───────────────────── */}
        <AiSection />

        {/* ── Security by Design ───────────────────────────── */}
        <SecuritySection />

        {/* ── Engineering Quality ──────────────────────────── */}
        <QualitySection />

        {/* ── DevSecOps & CI/CD ────────────────────────────── */}
        <DevSecOpsSection />

        {/* ── Observabilidad ───────────────────────────────── */}
        <ObservabilitySection />

        {/* ── Cloud Architecture ───────────────────────────── */}
        <CloudSection />

        {/* ── Architecture Decision Records ────────────────── */}
        <AdrSection />

        {/* ── Capacidades profesionales ────────────────────── */}
        <CapabilitiesSection />

        {/* ── Acceso al código (repositorio privado) ───────── */}
        <PrivateRepoSection />
      </main>

      <footer className="border-t border-neutral-100 bg-neutral-50/50">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <div className="flex flex-col items-center justify-between gap-5 sm:flex-row">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-primary-500 to-primary-700 shadow-sm">
                <span className="text-sm font-bold tracking-tight text-white">K</span>
              </div>
              <span className="text-lg font-bold tracking-tight text-primary-900">KIN</span>
            </div>
            <nav aria-label="Enlaces de la página" className="flex flex-wrap items-center justify-center gap-5">
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
                href="/arquitectura"
                aria-current="page"
                className="text-sm font-medium text-primary-600 transition-colors"
              >
                Arquitectura técnica
              </Link>
            </nav>
            <p className="text-sm text-neutral-500">
              © {new Date().getFullYear()} KIN — Knowledge, Innovation &amp; Navigation.
            </p>
          </div>
        </div>
      </footer>
    </>
  );
}
