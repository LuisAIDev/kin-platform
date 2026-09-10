import type { Metadata } from "next";
import "@/styles/globals.css";

export const metadata: Metadata = {
  title: "KIN Medical — Infraestructura inteligente para tu práctica clínica",
  description:
    "Gestiona pacientes, agenda, seguimiento y telemedicina en un único lugar.",
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