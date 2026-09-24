import { InstitutionalDashboard } from "../../../components/institutional/InstitutionalDashboard";

export default function InstitutionalPage() {
  return (
    <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold tracking-tight text-neutral-900">Panel institucional</h1>
      <p className="mt-1 text-sm text-neutral-600">
        Administración de tu IPS: sedes, equipo médico y KPIs.
      </p>
      <div className="mt-6">
        <InstitutionalDashboard />
      </div>
    </main>
  );
}
