import Sidebar from "@/components/layout/Sidebar";
import Header from "@/components/layout/Header";
import SessionGuard from "@/components/auth/SessionGuard";
import RoleGuard from "@/components/auth/RoleGuard";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SessionGuard>
      <RoleGuard>
        <div className="flex flex-col lg:flex-row min-h-screen">
          <Sidebar />
          <div className="flex-1 flex flex-col min-w-0">
            <Header />
            <main className="flex-1 flex flex-col">{children}</main>
          </div>
        </div>
      </RoleGuard>
    </SessionGuard>
  );
}