"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import Header from "@/components/layout/Header";
import { Heart, Users, Calendar, FileText, MessageCircle, Shield, Settings } from "lucide-react";

type CardLink = {
  href: string;
  title: string;
  description: string;
  icon: React.ElementType;
  badge?: string;
};

const CARD_LINKS: CardLink[] = [
  { href: "/dashboard/patient/triage", title: "Mi Triaje", description: "Evaluacion de sintomas y consulta digital", icon: Heart, badge: "Proximoamente" },
  { href: "/dashboard/patient/appointments", title: "Mis Citas", description: "Agenda y proximas citas medicas", icon: Calendar },
  { href: "/dashboard/patient/documents", title: "Mis Documentos", description: "Historial clinico y resultados", icon: FileText },
  { href: "/dashboard/patient/followup", title: "Mi Seguimiento", description: "Planes de seguimiento y control", icon: Shield },
  { href: "/dashboard/patient/messages", title: "Mis Mensajes", description: "Comunicacion con el equipo medico", icon: MessageCircle },
  { href: "/dashboard/patient/plans", title: "Mis Planes", description: "Planes de salud y opciones de pago", icon: Settings, badge: "Proximoamente" },
];

const ICON_MAP: Record<string, React.ElementType> = {
  "/dashboard/patient/triage": Heart,
  "/dashboard/patient/appointments": Calendar,
  "/dashboard/patient/documents": FileText,
  "/dashboard/patient/followup": Shield,
  "/dashboard/patient/messages": MessageCircle,
  "/dashboard/patient/plans": Settings,
};

function getBadgeClass(isProximoamente: boolean) {
  if (isProximoamente) {
    return "bg-neutral-100 text-neutral-400";
  }
  return "bg-medical-100 text-medical-600";
}

function getBackgroundClass(isProximoamente: boolean) {
  if (isProximoamente) {
    return "bg-neutral-50";
  }
  return "bg-medical-50";
}

export default function PatientDashboardPage() {
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    async function init() {
      try {
        const currentUser = await authService.fetchCurrentUser();
        if (!currentUser || currentUser.role !== "PATIENT") {
          router.push("/login");
          setLoading(false);
          return;
        }
        setUser(currentUser);
      } catch (err) {
        setError("Error al cargar los datos del usuario");
      } finally {
        setLoading(false);
      }
    }

    init();
  }, [router]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-medical-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen p-6 text-red-600">
        <h2 className="text-lg font-medium mb-2">Error</h2>
        <p className="text-sm">{error}</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-red-600">Usuario no identificado</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen">
      <Header />

      <main className="flex-1 py-8 px-4 lg:px-6">
        <div className="max-w-7xl mx-auto">
          <div className="mb-8">
            <h1 className="text-3xl lg:text-4xl font-bold text-neutral-900">
              Bienvenido, {user.fullName ?? "Paciente"}
            </h1>
            <p className="text-neutral-600 mt-2">Tu panel de salud personal</p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {CARD_LINKS.map((card) => {
              const isProximoamente = card.badge === "Proximoamente";
              const badgeClass = getBadgeClass(isProximoamente);
              const backgroundClass = getBackgroundClass(isProximoamente);
              const IconComponent = ICON_MAP[card.href] || Heart;

              return (
                <div
                  key={card.href}
                  className="group rounded-lg border border-neutral-300 overflow-hidden hover:shadow-lg transition-shadow"
                >
                  <a
                    href={card.href}
                    className="block rounded-t-lg"
                  >
                    <div className={`p-5 ${backgroundClass}`}>
                      <div className={`flex-shrink-0 h-10 w-10 rounded-lg flex items-center justify-center ${badgeClass}`}>
                        <IconComponent className="h-5 w-5" />
                      </div>
                      <div className="flex-1 flex flex-col pt-1">
                        <h3 className="font-medium text-neutral-900">
                          {card.title}
                        </h3>
                        <p className="text-sm text-neutral-500 line-clamp-1">
                          {card.description}
                        </p>
                      </div>
                    </div>
                  </a>

                  {isProximoamente && (
                    <div
                      className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-medical-600 to-transparent p-3 text-xs font-medium text-medical-600"
                    >
                      Proximoamente
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* Secondary actions section */}
          <div className="mt-8 pt-8 border-t border-neutral-200">
            <p className="text-sm text-neutral-500">
              Mas informacion disponible en tu perfil o contacta al soporte si
              necesitas ayuda adicional.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}