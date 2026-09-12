import Sidebar from "@/components/layout/Sidebar";
import Header from "@/components/layout/Header";
import SessionGuard from "@/components/auth/SessionGuard";
import RoleGuard from "@/components/auth/RoleGuard";
import ToastProvider from "@/components/ui/ToastProvider";

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
            <div className="flex-1 flex flex-col min-w-0">
              <Header />
              <main className="flex-1 flex flex-col">{children}</main>
            </div>
          </div>
        </ToastProvider>
      </RoleGuard>
    </SessionGuard>
  );
}