"use client";

import type { TimelineEvent } from "@/services/intelligence/types";

export default function TimelineView({ events }: { events: TimelineEvent[] }) {
  if (events.length === 0) {
    return <p className="text-sm text-neutral-500">Aún no hay actividad registrada.</p>;
  }
  return (
    <ol className="relative space-y-4 border-l border-neutral-200 pl-4">
      {events.map((event) => (
        <li key={event.id} className="relative">
          <span className="absolute -left-[21px] top-1.5 h-2 w-2 rounded-full bg-primary-500" aria-hidden="true" />
          <p className="text-sm font-medium text-neutral-800">{event.name}</p>
          <p className="text-xs text-neutral-400">{new Date(event.timestamp).toLocaleString()}</p>
        </li>
      ))}
    </ol>
  );
}
