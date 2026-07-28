import Sidebar from "@/components/layout/Sidebar";
import SessionGuard from "@/components/auth/SessionGuard";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SessionGuard>
      <div className="flex flex-col lg:flex-row min-h-screen">
        <Sidebar />
        <main className="flex-1 flex flex-col">{children}</main>
      </div>
    </SessionGuard>
  );
}
