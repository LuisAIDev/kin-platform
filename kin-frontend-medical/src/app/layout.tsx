import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";
import "@/styles/globals.css";
import { ThemeProvider } from "@/components/providers/ThemeProvider";
import { Providers } from "./providers";

const inter = Inter({ subsets: ["latin"] });

const SITE_URL = "https://www.kin-platform-medical.com";
const SITE_NAME = "KIN Medical";
const SITE_TITLE = "KIN Medical | El Sistema Operativo de la Atención Médica Moderna";
const SITE_DESCRIPTION =
  "Triaje digital con IA, telemedicina, historia clínica, agenda y gestión de pacientes. Plataforma clínica escalable desde el consultorio hasta la red hospitalaria. Para médicos, clínicas, hospitales, IPS y EPS.";

const GOOGLE_VERIFICATION = process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION;
const BING_VERIFICATION = process.env.NEXT_PUBLIC_BING_SITE_VERIFICATION;

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: SITE_TITLE,
    template: "%s | KIN Medical",
  },
  description: SITE_DESCRIPTION,
  keywords: [
    "sistema operativo médico",
    "software médico",
    "gestión clínica",
    "triaje digital",
    "triaje con IA",
    "telemedicina",
    "historia clínica electrónica",
    "gestión de pacientes",
    "plataforma hospitalaria",
    "software para clínicas",
    "IPS",
    "EPS",
    "médicos",
    "hospitales",
    "Colombia",
    "Latinoamérica",
  ],
  authors: [{ name: "KIN Platform", url: "https://kin-platform.com" }],
  creator: "KIN Platform",
  publisher: "KIN Platform",
  category: "Salud",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    type: "website",
    locale: "es_ES",
    url: SITE_URL,
    siteName: SITE_NAME,
    title: SITE_TITLE,
    description:
      "Triaje digital, IA clínica, telemedicina y gestión de pacientes. Escalable desde el consultorio hasta la red hospitalaria.",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "KIN Medical — El Sistema Operativo de la Atención Médica",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: SITE_TITLE,
    description:
      "Triaje digital, IA clínica, telemedicina y gestión de pacientes. Escalable desde el consultorio hasta la red hospitalaria.",
    images: ["/og-image.png"],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-image-preview": "large",
      "max-snippet": -1,
      "max-video-preview": -1,
    },
  },
  verification: {
    ...(GOOGLE_VERIFICATION ? { google: GOOGLE_VERIFICATION } : {}),
    ...(BING_VERIFICATION ? { other: { "msvalidate.01": BING_VERIFICATION } } : {}),
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#ffffff",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className="h-full antialiased">
      <body className={`min-h-full w-full flex flex-col ${inter.className}`}>
        <ThemeProvider>
          <Providers>{children}</Providers>
        </ThemeProvider>
      </body>
    </html>
  );
}
