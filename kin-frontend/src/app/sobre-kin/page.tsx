import type { Metadata } from "next";
import Link from "next/link";
import Navbar from "@/components/layout/Navbar";
import SectionHeading from "@/components/about/SectionHeading";
import TechnologySection from "@/components/about/TechnologySection";
import ShowcaseSection from "@/components/about/ShowcaseSection";
import ArchitectureSection from "@/components/about/ArchitectureSection";
import DemonstrateSection from "@/components/about/DemonstrateSection";
import CreatorProfileCard from "@/components/about/CreatorProfileCard";
import { ABOUT_CONTENT } from "@/components/about/aboutContent";

export const metadata: Metadata = {
  title: "Sobre KIN",
  description: "Una plataforma construida para aprender, crear y aportar.",
};

export default function SobreKinPage() {
  const content = ABOUT_CONTENT;

  return (
    <>
      <Navbar variant="about" />

      <main>
        {/* ── Hero ─────────────────────────────────────────── */}
        <section className="relative overflow-hidden border-b border-neutral-100">
          <div className="absolute inset-0 bg-gradient-to-br from-primary-50/70 via-white to-accent-50/40" />
          <div className="relative mx-auto max-w-4xl px-4 py-20 text-center sm:px-6 sm:py-28 lg:px-8">
            <p className="text-sm font-semibold text-primary-600 tracking-widest uppercase mb-4">
              {content.hero.badge}
            </p>
            <h1 className="text-4xl font-extrabold tracking-tight text-neutral-900 sm:text-6xl">
              {content.hero.title}
            </h1>
            <p className="mx-auto mt-5 max-w-2xl text-lg leading-8 text-neutral-500 sm:text-xl">
              {content.hero.subtitle}
            </p>
          </div>
        </section>

        {/* ── ¿Qué es KIN? ─────────────────────────────────── */}
        <section className="py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow={content.whatIs.eyebrow} title={content.whatIs.title} />
            <div className="mt-10 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
              {content.whatIs.paragraphs.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
            </div>
          </div>
        </section>

        {/* ── Creado por ───────────────────────────────────── */}
        <section className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow={content.creator.eyebrow} title={content.creator.title} />
            <p className="mt-10 text-center text-xl font-semibold text-neutral-900 sm:text-2xl">
              {content.creator.name}
            </p>
            <div className="mt-8 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
              {content.creator.paragraphs.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
            </div>
          </div>
        </section>

        {/* ── Mi propósito ─────────────────────────────────── */}
        <section className="py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <div className="rounded-2xl border border-primary-100 bg-primary-50/50 p-8 sm:p-12">
              <h2 className="text-2xl font-bold tracking-tight text-neutral-900 sm:text-3xl">
                {content.purpose.title}
              </h2>
              <div className="mt-6 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
                {content.purpose.paragraphs.map((paragraph) => (
                  <p key={paragraph}>{paragraph}</p>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* ── Más que una aplicación ───────────────────────── */}
        <section className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow={content.moreThanApp.eyebrow} title={content.moreThanApp.title} />
            <div className="mt-10 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
              {content.moreThanApp.paragraphs.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
            </div>
          </div>
        </section>

        {/* ── Engineering Showcase ─────────────────────────── */}
        <ShowcaseSection />

        {/* ── Tecnologías y capacidades ────────────────────── */}
        <TechnologySection />

        {/* ── Arquitectura de KIN ──────────────────────────── */}
        <ArchitectureSection />

        {/* ── CTA: arquitectura técnica pública ────────────── */}
        <section className="border-y border-neutral-100 bg-neutral-50/50 py-14 sm:py-16">
          <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
            <h2 className="text-2xl font-bold tracking-tight text-neutral-900 sm:text-3xl">
              Arquitectura técnica de KIN
            </h2>
            <p className="mx-auto mt-3 max-w-2xl text-base leading-7 text-neutral-500">
              Una visión de ingeniería sobre cómo KIN combina arquitectura de software,
              inteligencia artificial determinista, seguridad, testing y cloud.
            </p>
            <Link
              href="/arquitectura"
              className="mt-7 inline-flex items-center gap-2 rounded-xl border border-primary-200 bg-white px-6 py-3 text-sm font-semibold text-primary-700 shadow-sm transition-all duration-200 hover:border-primary-300 hover:bg-primary-50 hover:shadow-md"
            >
              Explorar arquitectura técnica
              <svg
                className="h-4 w-4"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={2}
                stroke="currentColor"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3"
                />
              </svg>
            </Link>
          </div>
        </section>

        {/* ── Lo que KIN demuestra ─────────────────────────── */}
        <DemonstrateSection />

        {/* ── Visión a largo plazo ─────────────────────────── */}
        <section className="py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow={content.vision.eyebrow} title={content.vision.title} />
            <div className="mt-10 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
              {content.vision.paragraphs.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
            </div>
            <ol
              className="mx-auto mt-10 grid max-w-2xl grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4"
              role="list"
            >
              {content.vision.timeline.map((step, index) => (
                <li
                  key={step}
                  className="flex items-center gap-2.5 rounded-lg border border-neutral-200 bg-white px-4 py-3 shadow-sm"
                >
                  <span
                    className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary-50 text-xs font-bold text-primary-700"
                    aria-hidden="true"
                  >
                    {index + 1}
                  </span>
                  <span className="text-sm font-medium text-neutral-700">{step}</span>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* ── Mi visión como desarrollador ─────────────────── */}
        <section className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading
              eyebrow={content.visionProfessional.eyebrow}
              title={content.visionProfessional.title}
            />
            <div className="mt-10 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
              {content.visionProfessional.paragraphs.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
            </div>
          </div>
        </section>

        {/* ── Por qué construí KIN ─────────────────────────── */}
        <section className="py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow={content.whyBuilt.eyebrow} title={content.whyBuilt.title} />
            <div className="mt-10 space-y-5 text-base leading-7 text-neutral-600 sm:text-lg sm:leading-8">
              {content.whyBuilt.paragraphs.map((paragraph) => (
                <p key={paragraph}>{paragraph}</p>
              ))}
            </div>
          </div>
        </section>

        {/* ── Quiero seguir aprendiendo ────────────────────── */}
        <section className="border-y border-neutral-100 bg-neutral-50/50 py-20 sm:py-24">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <SectionHeading
              eyebrow={content.keepLearning.eyebrow}
              title={content.keepLearning.title}
              intro={content.keepLearning.intro}
            />
            <div className="mt-10 flex flex-wrap justify-center gap-3">
              {content.keepLearning.areas.map((area) => (
                <span
                  key={area}
                  className="rounded-full border border-neutral-200 bg-white px-4 py-2 text-sm font-medium text-neutral-700 shadow-sm"
                >
                  {area}
                </span>
              ))}
            </div>
            <p className="mt-8 text-center text-base leading-7 text-neutral-500 sm:text-lg">
              {content.keepLearning.closing}
            </p>
          </div>
        </section>

        {/* ── Para quién se construye ──────────────────────── */}
        <section className="py-20 sm:py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <SectionHeading eyebrow={content.audience.eyebrow} title={content.audience.title} />
            <div className="mx-auto mt-14 grid max-w-4xl grid-cols-1 gap-6 sm:grid-cols-3">
              {content.audience.items.map((item) => (
                <div
                  key={item.title}
                  className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm"
                >
                  <h3 className="text-lg font-semibold text-neutral-900">{item.title}</h3>
                  <p className="mt-3 text-sm leading-6 text-neutral-500">{item.description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Frase final ──────────────────────────────────── */}
        <section className="border-y border-neutral-100 bg-neutral-50/50 py-24 sm:py-32">
          <div className="mx-auto max-w-3xl px-4 text-center sm:px-6 lg:px-8">
            <p className="text-2xl font-semibold leading-relaxed text-neutral-900 sm:text-3xl">
              {content.closing.lines[0]}
            </p>
            <p className="mt-3 text-2xl font-semibold leading-relaxed text-neutral-900 sm:text-3xl">
              {content.closing.lines[1]}
            </p>
            <p className="mt-8 text-base font-medium text-primary-600 sm:text-lg">
              {content.closing.after}
            </p>
          </div>
        </section>

        {/* ── Perfil profesional ───────────────────────────── */}
        <section className="py-20 sm:py-24">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <CreatorProfileCard />
          </div>
        </section>
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
            <p className="text-sm text-neutral-500">
              © {new Date().getFullYear()} KIN — Knowledge, Innovation &amp; Navigation.
            </p>
            <div className="flex items-center gap-5">
              <Link
                href="/arquitectura"
                className="text-sm font-medium text-neutral-500 transition-colors hover:text-primary-600"
              >
                Arquitectura técnica
              </Link>
              <Link
                href="/"
                className="text-sm font-medium text-neutral-500 transition-colors hover:text-primary-600"
              >
                Volver al inicio
              </Link>
            </div>
          </div>
        </div>
      </footer>
    </>
  );
}
