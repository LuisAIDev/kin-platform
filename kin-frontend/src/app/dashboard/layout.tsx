import Sidebar from "@/components/layout/Sidebar";
import SessionGuard from "@/components/auth/SessionGuard";
import RoleGuard from "@/components/auth/RoleGuard";
import ToastProvider from "@/components/ui/ToastProvider";
import MaybeOnboarding from "@/components/layout/MaybeOnboarding";
import PhysicianApplicationWidget from "@/components/physician/PhysicianApplicationWidget";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SessionGuard>
      <RoleGuard>
        <ToastProvider>
          <div className="flex flex-col lg:flex-row min-h-screen">
            <Sidebar />
            <div className="flex-1 flex flex-col">
              <MaybeOnboarding />
              {/* Solicitud de capacidad profesional para cuentas existentes. */}
              <PhysicianApplicationWidget />
              <main className="flex-1 flex flex-col">{children}</main>
            </div>
          </div>
        </ToastProvider>
      </RoleGuard>
    </SessionGuard>
  );
}
