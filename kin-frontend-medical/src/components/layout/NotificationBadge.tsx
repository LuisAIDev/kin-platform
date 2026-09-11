"use client";

interface NotificationBadgeProps {
  count: number;
}

export default function NotificationBadge({ count }: NotificationBadgeProps) {
  if (!count || count === 0) return null;

  return (
    <span className="flex-shrink-0 w-5 h-5 rounded-full bg-emerald-600 text-white text-[10px] font-semibold flex items-center justify-center">
      {count > 99 ? "99+" : count}
    </span>
  );
}