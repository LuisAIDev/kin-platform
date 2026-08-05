import Sidebar from "@/components/layout/Sidebar";
import SessionGuard from "@/components/auth/SessionGuard";
import ToastProvider from "@/components/ui/ToastProvider";
import OnboardingChecklist from "@/components/onboarding/OnboardingChecklist";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SessionGuard>
      <ToastProvider>
        <div className="flex flex-col lg:flex-row min-h-screen">
          <Sidebar />
          <div className="flex-1 flex flex-col">
            <OnboardingChecklist />
            <main className="flex-1 flex flex-col">{children}</main>
          </div>
        </div>
      </ToastProvider>
    </SessionGuard>
  );
}
