import type { Metadata } from "next";
import "@/styles/globals.css";

export const metadata: Metadata = {
  title: {
    default: "KIN Medical | El Sistema Operativo de la Atención Médica Moderna",
    template: "%s | KIN Medical",
  },
  description:
    "Triaje digital con IA, telemedicina, gestión de pacientes, agenda y documentos clínicos. Escalable desde el consultorio hasta la red hospitalaria. Para médicos, clínicas, hospitales, IPS y EPS.",
  keywords: [
    "sistema operativo médico",
    "gestión clínica",
    "triaje digital",
    "telemedicina",
    "IA médica",
    "historia clínica",
    "plataforma hospitalaria",
    "gestión de pacientes",
    "IPS",
    "EPS",
    "clínicas",
    "hospitales",
  ],
  authors: [{ name: "KIN Platform" }],
  openGraph: {
    title: "KIN Medical — El Sistema Operativo de la Atención Médica Moderna",
    description:
      "Triaje digital, IA clínica, telemedicina y gestión de pacientes. Escalable desde el consultorio hasta la red hospitalaria.",
    url: "https://kin-platform-medical.com",
    siteName: "KIN Medical",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "KIN Medical — El Sistema Operativo de la Atención Médica",
      },
    ],
    locale: "es_ES",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "KIN Medical — El Sistema Operativo de la Atención Médica Moderna",
    description:
      "Triaje digital, IA clínica, telemedicina y gestión de pacientes. Escalable desde el consultorio hasta la red hospitalaria.",
    images: ["/og-image.png"],
  },
  robots: { index: true, follow: true },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className="h-full antialiased">
      <body className="min-h-full w-full flex flex-col">{children}</body>
    </html>
  );
}