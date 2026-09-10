"use client";

type Props = {
  title: string;
  description?: string;
  action?: React.ReactNode;
};

export default function EmptyState({ title, description, action }: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-neutral-100 text-2xl" aria-hidden="true">
        📭
      </div>
      <h2 className="mt-4 text-lg font-semibold text-neutral-900">{title}</h2>
      {description && <p className="mt-1 max-w-md text-sm text-neutral-500">{description}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}
