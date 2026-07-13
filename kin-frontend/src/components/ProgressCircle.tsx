"use client";

export default function ProgressCircle({ percentage }: { percentage: number }) {
  const r = 18;
  const circ = 2 * Math.PI * r;
  const offset = circ - (percentage / 100) * circ;

  const color =
    percentage >= 80 ? "#16a34a" : percentage >= 50 ? "#4f46e5" : "#dc2626";

  return (
    <div className="flex items-center gap-2">
      <svg width="48" height="48" viewBox="0 0 48 48" className="shrink-0">
        <circle
          cx="24"
          cy="24"
          r={r}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth="4"
        />
        <circle
          cx="24"
          cy="24"
          r={r}
          fill="none"
          stroke={color}
          strokeWidth="4"
          strokeDasharray={circ}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform="rotate(-90 24 24)"
          className="transition-all duration-500"
        />
        <text
          x="24"
          y="24"
          textAnchor="middle"
          dominantBaseline="central"
          fontSize="10"
          fontWeight="bold"
          fill={color}
        >
          {percentage}%
        </text>
      </svg>
      <div>
        <div className="text-xs font-semibold text-neutral-500 uppercase tracking-wide">
          Progreso
        </div>
        <div
          className="text-sm font-bold"
          style={{ color }}
        >
          {percentage}%
        </div>
      </div>
    </div>
  );
}
