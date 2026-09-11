const statusColors: Record<string, string> = {
  DRAFT: "bg-neutral-100 text-neutral-600",
  IN_PROGRESS: "bg-primary-100 text-primary-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  ARCHIVED: "bg-neutral-200 text-neutral-500",
};

export function statusBadge(status: string): string {
  return statusColors[status] ?? "bg-neutral-100 text-neutral-600";
}
