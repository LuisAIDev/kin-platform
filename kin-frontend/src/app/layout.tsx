import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const SITE_URL = "https://www.kin-platform.com";
const SITE_NAME = "KIN — Knowledge, Innovation & Navigation";
const SITE_TITLE = "KIN | Knowledge, Innovation & Navigation";
const SITE_DESCRIPTION =
  "Estructura tu proyecto en menos de 60 minutos con asistencia de IA. Análisis de viabilidad, scoring objetivo y reportes profesionales en PDF para empresas y emprendedores.";

const GOOGLE_VERIFICATION = process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION;
const BING_VERIFICATION = process.env.NEXT_PUBLIC_BING_SITE_VERIFICATION;

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: SITE_TITLE,
    template: "%s | KIN",
  },
  description: SITE_DESCRIPTION,
  keywords: [
    "estructuración de proyectos",
    "gestión de proyectos con IA",
    "asistente de IA para empresas",
    "análisis de viabilidad",
    "scoring de proyectos",
    "plan de negocio con IA",
    "herramientas para emprendedores",
    "reportes PDF profesionales",
  ],
  authors: [{ name: "KIN Platform", url: SITE_URL }],
  creator: "KIN Platform",
  publisher: "KIN Platform",
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
      "Estructura tu proyecto en menos de 60 minutos con asistencia de IA. Análisis de viabilidad, scoring y reportes en PDF.",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "KIN — Knowledge, Innovation & Navigation",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: SITE_TITLE,
    description:
      "Estructura tu proyecto en menos de 60 minutos con asistencia de IA. Análisis de viabilidad, scoring y reportes en PDF.",
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
    <html
      lang="es"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="min-h-full w-full flex flex-col">{children}</body>
    </html>
  );
}
