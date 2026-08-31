"use client";

/**
 * Badge de contador para el sidebar: solo se renderiza si hay novedades.
 */
export default function NotificationBadge({ count }: { count: number }) {
  if (!count || count <= 0) return null;
  return (
    <span
      className="ml-auto rounded-full bg-red-600 text-white text-[10px] font-bold px-1.5 py-0.5 min-w-[18px] text-center leading-tight"
      aria-label={`${count} novedades`}
    >
      {count > 99 ? "99+" : count}
    </span>
  );
}
