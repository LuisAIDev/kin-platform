import { ABOUT_CONTENT, ABOUT_LINKS } from "./aboutContent";

/**
 * Tarjeta de perfil profesional del creador. Enlaza el perfil de GitHub y el de
 * LinkedIn del creador. El repositorio fuente de KIN es privado y no se enlaza.
 */
export default function CreatorProfileCard() {
  const { profile } = ABOUT_CONTENT;

  return (
    <div className="mx-auto max-w-2xl rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm sm:p-10">
      <div className="flex flex-col items-center gap-5 text-center sm:flex-row sm:items-start sm:text-left">
        <div
          className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-primary-500 to-primary-700 text-xl font-bold text-white shadow-sm"
          aria-hidden="true"
        >
          LG
        </div>
        <div>
          <h3 className="text-lg font-semibold text-neutral-900">{profile.name}</h3>
          <p className="mt-0.5 text-sm font-medium text-primary-600">{profile.role}</p>
          <p className="mt-3 text-sm leading-6 text-neutral-500">{profile.description}</p>

          <div className="mt-5 flex flex-wrap justify-center gap-3 sm:justify-start">
            {[ABOUT_LINKS.github, ABOUT_LINKS.linkedin].map((link) => (
              <a
                key={link.label}
                href={link.href}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 px-3.5 py-2 text-sm font-medium text-neutral-700 transition hover:border-primary-300 hover:text-primary-700 focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none"
              >
                {link.label}
                <svg
                  className="h-3.5 w-3.5"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={2}
                  stroke="currentColor"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M13.5 6H5.25A2.25 2.25 0 0 0 3 8.25v10.5A2.25 2.25 0 0 0 5.25 21h10.5A2.25 2.25 0 0 0 18 18.75V10.5m-10.5 6L21 3m0 0h-5.25M21 3v5.25"
                  />
                </svg>
              </a>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
