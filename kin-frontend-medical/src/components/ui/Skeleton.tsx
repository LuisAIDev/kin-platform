"use client";

export default function Skeleton({ className = "" }: { className?: string }) {
  return (
    <div
      role="status"
      aria-label="Cargando"
      className={`animate-pulse rounded-lg bg-neutral-200 ${className}`}
    />
  );
}
