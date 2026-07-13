const categoryColors: Record<string, string> = {
  SOCIAL: "bg-rose-100 text-rose-700",
  EMPRENDIMIENTO: "bg-amber-100 text-amber-700",
  EMPRESARIAL: "bg-sky-100 text-sky-700",
  PERSONAL: "bg-violet-100 text-violet-700",
};

const statusColors: Record<string, string> = {
  DRAFT: "bg-neutral-100 text-neutral-600",
  IN_PROGRESS: "bg-primary-100 text-primary-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  ARCHIVED: "bg-neutral-200 text-neutral-500",
};

export function categoryBadge(category: string): string {
  return categoryColors[category] ?? "bg-neutral-100 text-neutral-600";
}

export function statusBadge(status: string): string {
  return statusColors[status] ?? "bg-neutral-100 text-neutral-600";
}
