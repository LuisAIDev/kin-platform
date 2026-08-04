import EnterpriseDashboard from "@/components/enterprise/EnterpriseDashboard";
import "@/components/enterprise/enterprise.css";

type Props = {
  params: Promise<{ id: string }>;
};

export default async function EnterprisePage({ params }: Props) {
  const { id } = await params;
  return (
    <main className="flex-1 overflow-y-auto">
      <EnterpriseDashboard projectId={id} />
    </main>
  );
}
